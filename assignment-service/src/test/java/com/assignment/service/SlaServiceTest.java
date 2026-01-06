package com.assignment.service;

import com.assignment.entity.SlaRule;
import com.assignment.entity.SlaStatus;
import com.assignment.entity.SlaTracking;
import com.assignment.repository.SlaRuleRepository;
import com.assignment.repository.SlaTrackingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.stubbing.OngoingStubbing;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // fixes UnnecessaryStubbingException from shared stubs in @BeforeEach [web:72]
class SlaServiceTest {

    @Mock
    private SlaTrackingRepository slaTrackingRepository;

    @Mock
    private SlaRuleRepository slaRuleRepository;

    private SlaService slaService;

    @BeforeEach
    void setUp() {
        slaService = new SlaService(slaTrackingRepository, slaRuleRepository);

        // keep these (useful) default stubs, but lenient so tests that don't call save() won't fail [web:73]
        lenient().when(slaTrackingRepository.save(any(SlaTracking.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(slaRuleRepository.save(any(SlaRule.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ---------------- helpers ----------------

    private static SlaRule rule(int responseMinutes, int resolutionHours) {
        SlaRule r = new SlaRule();
        r.setResponseTimeMinutes(responseMinutes);
        r.setResolutionTimeHours(resolutionHours);
        return r;
    }

    private static SlaTracking trackingBase(String ticketId, String ticketNumber) {
        SlaTracking t = new SlaTracking();
        t.setTicketId(ticketId);
        t.setTicketNumber(ticketNumber);
        return t;
    }

    /**
     * Deterministic LocalDateTime.now() with a sequence of values.
     * Uses chained thenReturn to avoid varargs warnings.
     */
    private static MockedStatic<LocalDateTime> mockNow(LocalDateTime... sequence) {
        MockedStatic<LocalDateTime> mocked =
                Mockito.mockStatic(LocalDateTime.class, Mockito.CALLS_REAL_METHODS);

        OngoingStubbing<LocalDateTime> stub = mocked.when(LocalDateTime::now);
        for (LocalDateTime t : sequence) {
            stub = stub.thenReturn(t);
        }
        return mocked;
    }

    /**
     * Reads a JavaBean property value via Introspector (supports getX() and isX()).
     * Falls back to direct field access if no getter exists. [web:22]
     */
    private static Object readProperty(Object bean, String propertyName) {
        try {
            for (PropertyDescriptor pd : Introspector.getBeanInfo(bean.getClass()).getPropertyDescriptors()) {
                if (propertyName.equals(pd.getName())) {
                    Method getter = pd.getReadMethod();
                    if (getter != null) {
                        getter.setAccessible(true);
                        return getter.invoke(bean);
                    }
                }
            }
        } catch (Exception ignored) { }

        try {
            Field f = bean.getClass().getDeclaredField(propertyName);
            f.setAccessible(true);
            return f.get(bean);
        } catch (Exception ignored) { }

        return null;
    }

    private static boolean boolProp(Object bean, String propertyName) {
        return Boolean.TRUE.equals(readProperty(bean, propertyName));
    }

    // ---------------- createSlaTracking ----------------

    @Test
    void createSlaTracking_skipsWhenPriorityNull() {
        SlaTracking res = slaService.createSlaTracking("t1", "T-1", null, "CAT");
        assertNull(res);

        verifyNoInteractions(slaRuleRepository);
        verify(slaTrackingRepository, never()).save(any());
    }

    @Test
    void createSlaTracking_usesCategorySpecificRule() {
        LocalDateTime fixedNow = LocalDateTime.of(2026, 1, 5, 10, 0);
        SlaRule r = rule(30, 2);

        when(slaRuleRepository.findByPriorityAndCategory("HIGH", "PAYMENT"))
                .thenReturn(Optional.of(r));

        try (MockedStatic<LocalDateTime> mocked = mockNow(fixedNow)) {
            SlaTracking res = slaService.createSlaTracking("t1", "T-1", "HIGH", "PAYMENT");

            assertNotNull(res);
            assertEquals("t1", res.getTicketId());
            assertEquals("T-1", res.getTicketNumber());
            assertEquals("HIGH", res.getPriority());
            assertEquals("PAYMENT", res.getCategory());
            assertEquals(fixedNow, res.getSlaStartTime());
            assertEquals(fixedNow.plusMinutes(30), res.getResponseDueAt());
            assertEquals(fixedNow.plusHours(2), res.getResolutionDueAt());
            assertEquals(SlaStatus.ON_TIME, res.getSlaStatus());
        }
    }

    @Test
    void createSlaTracking_fallsBackToDefaultRuleWhenNoCategoryRule() {
        LocalDateTime fixedNow = LocalDateTime.of(2026, 1, 5, 10, 0);
        SlaRule defaultRule = rule(60, 8);

        when(slaRuleRepository.findByPriorityAndCategory("HIGH", "PAYMENT"))
                .thenReturn(Optional.empty());
        when(slaRuleRepository.findByPriorityAndCategoryIsNull("HIGH"))
                .thenReturn(Optional.of(defaultRule));

        try (MockedStatic<LocalDateTime> mocked = mockNow(fixedNow)) {
            SlaTracking res = slaService.createSlaTracking("t1", "T-1", "HIGH", "PAYMENT");
            assertNotNull(res);
            assertEquals(fixedNow.plusMinutes(60), res.getResponseDueAt());
            assertEquals(fixedNow.plusHours(8), res.getResolutionDueAt());
        }
    }

    @Test
    void createSlaTracking_createsDefaultRuleWhenNoneExists() {
        LocalDateTime fixedNow = LocalDateTime.of(2026, 1, 5, 10, 0);

        when(slaRuleRepository.findByPriorityAndCategory("CRITICAL", "OPS"))
                .thenReturn(Optional.empty());
        when(slaRuleRepository.findByPriorityAndCategoryIsNull("CRITICAL"))
                .thenReturn(Optional.empty());

        try (MockedStatic<LocalDateTime> mocked = mockNow(fixedNow)) {
            SlaTracking res = slaService.createSlaTracking("t1", "T-1", "CRITICAL", "OPS");
            assertNotNull(res);
            assertEquals(fixedNow.plusMinutes(15), res.getResponseDueAt());
            assertEquals(fixedNow.plusHours(4), res.getResolutionDueAt());
        }

        verify(slaRuleRepository).save(argThat(rule ->
                "CRITICAL".equals(rule.getPriority())
                        && "OPS".equals(rule.getCategory())
                        && rule.getResponseTimeMinutes() == 15
                        && rule.getResolutionTimeHours() == 4
        ));
    }

    // ---------------- createSlaTrackingOnPriorityAssignment ----------------

    @Test
    void createSlaTrackingOnPriorityAssignment_returnsExistingIfPresent() {
        SlaTracking existing = trackingBase("t1", "T-1");
        when(slaTrackingRepository.findByTicketId("t1")).thenReturn(Optional.of(existing));

        SlaTracking res = slaService.createSlaTrackingOnPriorityAssignment("t1", "T-1", "HIGH", "CAT");
        assertSame(existing, res);

        verify(slaTrackingRepository, never()).save(any());
        verifyNoInteractions(slaRuleRepository);
    }

    @Test
    void createSlaTrackingOnPriorityAssignment_createsIfMissing() {
        LocalDateTime fixedNow = LocalDateTime.of(2026, 1, 5, 10, 0);

        when(slaTrackingRepository.findByTicketId("t1")).thenReturn(Optional.empty());
        when(slaRuleRepository.findByPriorityAndCategory("HIGH", "CAT"))
                .thenReturn(Optional.of(rule(60, 8)));

        try (MockedStatic<LocalDateTime> mocked = mockNow(fixedNow)) {
            SlaTracking res = slaService.createSlaTrackingOnPriorityAssignment("t1", "T-1", "HIGH", "CAT");
            assertNotNull(res);
            assertEquals(fixedNow.plusMinutes(60), res.getResponseDueAt());
        }

        verify(slaTrackingRepository).save(any(SlaTracking.class));
    }

    // ---------------- recordFirstResponse ----------------

    @Test
    void recordFirstResponse_noTracking_noSave() {
        when(slaTrackingRepository.findByTicketId("t1")).thenReturn(Optional.empty());

        slaService.recordFirstResponse("t1");

        verify(slaTrackingRepository, never()).save(any());
    }

    @Test
    void recordFirstResponse_alreadyRecorded_noSave() {
        SlaTracking t = trackingBase("t1", "T-1");
        t.setSlaStartTime(LocalDateTime.of(2026, 1, 5, 10, 0));
        t.setFirstResponseAt(LocalDateTime.of(2026, 1, 5, 10, 5));

        when(slaTrackingRepository.findByTicketId("t1")).thenReturn(Optional.of(t));

        slaService.recordFirstResponse("t1");

        verify(slaTrackingRepository, never()).save(any());
    }

    @Test
    void recordFirstResponse_breached_setsBreachFields() {
        LocalDateTime start = LocalDateTime.of(2026, 1, 5, 10, 0);
        LocalDateTime now = start.plusMinutes(40);

        SlaTracking t = trackingBase("t1", "T-1");
        t.setSlaStartTime(start);
        t.setResponseDueAt(start.plusMinutes(30));
        t.setResolutionDueAt(start.plusHours(10));

        when(slaTrackingRepository.findByTicketId("t1")).thenReturn(Optional.of(t));

        try (MockedStatic<LocalDateTime> mocked = mockNow(now, now, now, now)) {
            slaService.recordFirstResponse("t1");
        }

        assertNotNull(t.getFirstResponseAt());
        assertEquals(40, t.getResponseTimeMinutes());
        assertTrue(boolProp(t, "responseBreached"));
        assertEquals(SlaStatus.BREACHED, t.getSlaStatus());
        assertNotNull(t.getBreachedAt());
        assertEquals("First response exceeded SLA time", t.getBreachReason());

        verify(slaTrackingRepository).save(t);
    }

    @Test
    void recordFirstResponse_withinSla_setsWarningWhenLowRemainingTime() {
        LocalDateTime start = LocalDateTime.of(2026, 1, 5, 10, 0);
        LocalDateTime now = start.plusMinutes(90);

        SlaTracking t = trackingBase("t1", "T-1");
        t.setSlaStartTime(start);

        t.setResponseDueAt(start.plusMinutes(200));
        t.setResolutionDueAt(start.plusMinutes(100)); // total 100, remaining 10 => WARNING

        when(slaTrackingRepository.findByTicketId("t1")).thenReturn(Optional.of(t));

        try (MockedStatic<LocalDateTime> mocked = mockNow(now, now, now, now)) {
            slaService.recordFirstResponse("t1");
        }

        assertFalse(boolProp(t, "responseBreached"));
        assertEquals(SlaStatus.WARNING, t.getSlaStatus());
        verify(slaTrackingRepository).save(t);
    }

    // ---------------- recordResolution ----------------

    @Test
    void recordResolution_noTracking_noSave() {
        when(slaTrackingRepository.findByTicketId("t1")).thenReturn(Optional.empty());

        slaService.recordResolution("t1");

        verify(slaTrackingRepository, never()).save(any());
    }

    @Test
    void recordResolution_alreadyResolved_noSave() {
        SlaTracking t = trackingBase("t1", "T-1");
        t.setResolvedAt(LocalDateTime.of(2026, 1, 5, 12, 0));

        when(slaTrackingRepository.findByTicketId("t1")).thenReturn(Optional.of(t));

        slaService.recordResolution("t1");

        verify(slaTrackingRepository, never()).save(any());
    }

    @Test
    void recordResolution_withinSla_setsMet() {
        LocalDateTime start = LocalDateTime.of(2026, 1, 5, 10, 0);
        LocalDateTime now = start.plusMinutes(90); // 1.50 hours

        SlaTracking t = trackingBase("t1", "T-1");
        t.setSlaStartTime(start);
        t.setResolutionDueAt(start.plusMinutes(120));
        t.setResponseDueAt(start.plusMinutes(30));
        t.setSlaStatus(SlaStatus.ON_TIME);

        when(slaTrackingRepository.findByTicketId("t1")).thenReturn(Optional.of(t));

        try (MockedStatic<LocalDateTime> mocked = mockNow(now, now)) {
            slaService.recordResolution("t1");
        }

        assertNotNull(t.getResolvedAt());
        assertEquals(new BigDecimal("1.50"), t.getResolutionTimeHours());
        assertFalse(boolProp(t, "resolutionBreached"));
        assertEquals(SlaStatus.MET, t.getSlaStatus());

        verify(slaTrackingRepository).save(t);
    }

    @Test
    void recordResolution_breached_setsBreachFields() {
        LocalDateTime start = LocalDateTime.of(2026, 1, 5, 10, 0);
        LocalDateTime now = start.plusMinutes(200);

        SlaTracking t = trackingBase("t1", "T-1");
        t.setSlaStartTime(start);
        t.setResolutionDueAt(start.plusMinutes(120));
        t.setResponseDueAt(start.plusMinutes(30));
        t.setSlaStatus(SlaStatus.ON_TIME);

        when(slaTrackingRepository.findByTicketId("t1")).thenReturn(Optional.of(t));

        try (MockedStatic<LocalDateTime> mocked = mockNow(now, now)) {
            slaService.recordResolution("t1");
        }

        assertTrue(boolProp(t, "resolutionBreached"));
        assertEquals(SlaStatus.BREACHED, t.getSlaStatus());
        assertNotNull(t.getBreachedAt());
        assertEquals("Resolution exceeded SLA time", t.getBreachReason());

        verify(slaTrackingRepository).save(t);
    }

    // ---------------- calculateTimeRemaining ----------------

    @Test
    void calculateTimeRemaining_nullTracking() {
        assertEquals("No SLA set", slaService.calculateTimeRemaining(null));
    }

    @Test
    void calculateTimeRemaining_completedWhenResolved() {
        SlaTracking t = trackingBase("t1", "T-1");
        t.setResolvedAt(LocalDateTime.of(2026, 1, 5, 12, 0));
        assertEquals("Completed", slaService.calculateTimeRemaining(t));
    }

    @Test
    void calculateTimeRemaining_responseStage_breached() {
        LocalDateTime now = LocalDateTime.of(2026, 1, 5, 10, 0);

        SlaTracking t = trackingBase("t1", "T-1");
        t.setResolvedAt(null);
        t.setFirstResponseAt(null);
        t.setResponseDueAt(now.minusMinutes(1));

        try (MockedStatic<LocalDateTime> mocked = mockNow(now)) {
            assertEquals("Breached", slaService.calculateTimeRemaining(t));
        }
    }

    @Test
    void calculateTimeRemaining_responseStage_minutes() {
        LocalDateTime now = LocalDateTime.of(2026, 1, 5, 10, 0);

        SlaTracking t = trackingBase("t1", "T-1");
        t.setResolvedAt(null);
        t.setFirstResponseAt(null);
        t.setResponseDueAt(now.plusMinutes(45));

        try (MockedStatic<LocalDateTime> mocked = mockNow(now)) {
            assertEquals("45 minutes", slaService.calculateTimeRemaining(t));
        }
    }

    @Test
    void calculateTimeRemaining_resolutionStage_hoursMinutes() {
        LocalDateTime now = LocalDateTime.of(2026, 1, 5, 10, 0);

        SlaTracking t = trackingBase("t1", "T-1");
        t.setResolvedAt(null);
        t.setFirstResponseAt(now.minusMinutes(1));
        t.setResolutionDueAt(now.plusMinutes(125)); // 2 hours 5 minutes

        try (MockedStatic<LocalDateTime> mocked = mockNow(now)) {
            assertEquals("2 hours 5 minutes", slaService.calculateTimeRemaining(t));
        }
    }

    @Test
    void calculateTimeRemaining_resolutionStage_daysHours() {
        LocalDateTime now = LocalDateTime.of(2026, 1, 5, 10, 0);

        SlaTracking t = trackingBase("t1", "T-1");
        t.setResolvedAt(null);
        t.setFirstResponseAt(now.minusMinutes(1));
        t.setResolutionDueAt(now.plusMinutes(2 * 1440 + 3 * 60)); // 2 days 3 hours

        try (MockedStatic<LocalDateTime> mocked = mockNow(now)) {
            assertEquals("2 days 3 hours", slaService.calculateTimeRemaining(t));
        }
    }
}
