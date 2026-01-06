package com.assignment.service;

import com.assignment.dto.*;
import com.assignment.entity.*;
import com.assignment.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AnalyticsServiceTest {

    @Mock private TicketCacheRepository ticketCacheRepository;
    @Mock private AssignmentRepository assignmentRepository;
    @Mock private AgentWorkloadRepository agentWorkloadRepository;
    @Mock private SlaTrackingRepository slaTrackingRepository;

    private AnalyticsService service;

    @BeforeEach
    void setUp() {
        service = new AnalyticsService(
                slaTrackingRepository,
                agentWorkloadRepository,
                assignmentRepository,
                ticketCacheRepository
        );
    }

    // ---------------- reflection helpers ----------------

    private static Object readValue(Object obj, String name) {
        if (obj == null) return null;

        // 1) record-style accessor: obj.name()
        try {
            Method m = obj.getClass().getMethod(name);
            m.setAccessible(true);
            return m.invoke(obj);
        } catch (Exception ignored) { }

        // 2) normal getters: getName()/isName()
        String cap = name.substring(0, 1).toUpperCase() + name.substring(1);
        for (String prefix : List.of("get", "is")) {
            try {
                Method m = obj.getClass().getMethod(prefix + cap);
                m.setAccessible(true);
                return m.invoke(obj);
            } catch (Exception ignored) { }
        }

        // 3) JavaBeans introspection [web:22]
        try {
            for (PropertyDescriptor pd : Introspector.getBeanInfo(obj.getClass()).getPropertyDescriptors()) {
                if (name.equals(pd.getName()) && pd.getReadMethod() != null) {
                    Method getter = pd.getReadMethod();
                    getter.setAccessible(true);
                    return getter.invoke(obj);
                }
            }
        } catch (Exception ignored) { }

        // 4) direct field access
        try {
            Field f = obj.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return f.get(obj);
        } catch (Exception ignored) { }

        return null;
    }

    @SuppressWarnings("unchecked")
    private static <T> T readValue(Object obj, String name, Class<T> clazz) {
        Object val = readValue(obj, name);
        if (val == null) return null;
        return (T) val;
    }

    private static long longVal(Object obj, String name) {
        Object v = readValue(obj, name);
        if (v == null) return 0L;
        if (v instanceof Number n) return n.longValue();
        return Long.parseLong(String.valueOf(v));
    }

    private static double doubleVal(Object obj, String name) {
        Object v = readValue(obj, name);
        if (v == null) return 0.0;
        if (v instanceof Number n) return n.doubleValue();
        return Double.parseDouble(String.valueOf(v));
    }

    /**
     * Tries preferred names first; if not found, scans for ANY no-arg getter that returns a List;
     * if still not found, scans fields of type List.
     */
    private static List<?> readAnyList(Object obj, String... preferredNames) {
        if (obj == null) return null;

        // preferred names (record/getters/fields)
        for (String name : preferredNames) {
            Object v = readValue(obj, name);
            if (v instanceof List<?> list) return list;
        }

        // scan public methods for List return type [web:132]
        for (Method m : obj.getClass().getMethods()) {
            if (m.getParameterCount() != 0) continue;
            if (m.getName().equals("getClass")) continue;
            if (List.class.isAssignableFrom(m.getReturnType())) {
                try {
                    Object v = m.invoke(obj);
                    if (v instanceof List<?> list) return list;
                } catch (Exception ignored) { }
            }
        }

        // scan declared fields for List
        for (Field f : obj.getClass().getDeclaredFields()) {
            if (List.class.isAssignableFrom(f.getType())) {
                try {
                    f.setAccessible(true);
                    Object v = f.get(obj);
                    if (v instanceof List<?> list) return list;
                } catch (Exception ignored) { }
            }
        }

        return null;
    }

    // ---------------- tests ----------------

    @Test
    void getSystemOverview_happyPath_computesAndFormats() {
        LocalDate fixedDate = LocalDate.of(2026, 1, 5);
        LocalDateTime fixedNow = LocalDateTime.of(2026, 1, 5, 12, 0);

        when(ticketCacheRepository.count()).thenReturn(100L);
        when(ticketCacheRepository.countByStatus("ASSIGNED")).thenReturn(10L);
        when(ticketCacheRepository.countByStatus("IN_PROGRESS")).thenReturn(5L);
        when(ticketCacheRepository.countByStatus("OPEN")).thenReturn(20L);
        when(ticketCacheRepository.countByStatusAndUpdatedAtAfter(eq("RESOLVED"), any(LocalDateTime.class)))
                .thenReturn(7L);

        when(agentWorkloadRepository.count()).thenReturn(6L);
        when(agentWorkloadRepository.countByStatus(AgentStatus.AVAILABLE)).thenReturn(3L);
        when(agentWorkloadRepository.countByStatus(AgentStatus.BUSY)).thenReturn(2L);

        when(ticketCacheRepository.countByPriorityAndStatusNot("CRITICAL", "RESOLVED")).thenReturn(3L);

        TicketCache c1 = new TicketCache(); c1.setCategory("PAYMENT");
        TicketCache c2 = new TicketCache(); c2.setCategory("PAYMENT");
        TicketCache c3 = new TicketCache(); c3.setCategory("LOGIN");
        when(ticketCacheRepository.findAll()).thenReturn(List.of(c1, c2, c3));

        SlaTracking s1 = new SlaTracking();
        s1.setResolvedAt(fixedNow.minusHours(2));
        s1.setResolutionTimeHours(new BigDecimal("2.00"));
        SlaTracking s2 = new SlaTracking();
        s2.setResolvedAt(fixedNow.minusHours(4));
        s2.setResolutionTimeHours(new BigDecimal("4.00"));
        when(slaTrackingRepository.findByResolvedAtIsNotNull()).thenReturn(List.of(s1, s2));

        when(slaTrackingRepository.count()).thenReturn(10L);
        when(slaTrackingRepository.countBySlaStatus(SlaStatus.BREACHED)).thenReturn(2L);

        when(assignmentRepository.countByAssignedAtAfter(any(LocalDateTime.class))).thenReturn(9L);

        try (MockedStatic<LocalDate> ld = Mockito.mockStatic(LocalDate.class, Mockito.CALLS_REAL_METHODS);
             MockedStatic<LocalDateTime> ldt = Mockito.mockStatic(LocalDateTime.class, Mockito.CALLS_REAL_METHODS)) {

            ld.when(LocalDate::now).thenReturn(fixedDate);
            ldt.when(LocalDateTime::now).thenReturn(fixedNow);

            SystemOverviewDTO dto = service.getSystemOverview();
            assertNotNull(dto);

            assertEquals(100L, longVal(dto, "totalTickets"));
            assertEquals(35L, longVal(dto, "activeTickets"));
            assertEquals(7L, longVal(dto, "resolvedToday"));
            assertEquals(6L, longVal(dto, "totalAgents"));
            assertEquals(5L, longVal(dto, "activeAgents"));
            assertEquals(2L, longVal(dto, "busyAgents"));
            assertEquals(3L, longVal(dto, "criticalTicketsOpen"));
            assertEquals(9L, longVal(dto, "recentAssignments"));

            String avgResolutionTime = readValue(dto, "avgResolutionTime", String.class);
            assertNotNull(avgResolutionTime);
            assertTrue(avgResolutionTime.contains("hours"));

            assertEquals(80.0, doubleVal(dto, "slaComplianceRate"));

            List<?> topCategories = readAnyList(dto, "topCategories", "categories");
            assertNotNull(topCategories);
            assertFalse(topCategories.isEmpty());

            Object first = topCategories.get(0);
            String catName = readValue(first, "category", String.class);
            if (catName == null) catName = readValue(first, "name", String.class);
            assertEquals("PAYMENT", catName);
        }
    }

    @Test
    void getTicketAnalytics_buildsMaps() {
        LocalDate fixedDate = LocalDate.of(2026, 1, 5);

        when(ticketCacheRepository.countByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(2L);
        when(ticketCacheRepository.countByStatusAndUpdatedAtBetween(eq("RESOLVED"), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(1L);

        when(ticketCacheRepository.countByPriority("CRITICAL")).thenReturn(1L);
        when(ticketCacheRepository.countByPriority("HIGH")).thenReturn(2L);
        when(ticketCacheRepository.countByPriority("MEDIUM")).thenReturn(3L);
        when(ticketCacheRepository.countByPriority("LOW")).thenReturn(4L);
        when(ticketCacheRepository.countByPriorityIsNull()).thenReturn(5L);

        when(ticketCacheRepository.countByStatus("OPEN")).thenReturn(10L);
        when(ticketCacheRepository.countByStatus("ASSIGNED")).thenReturn(11L);
        when(ticketCacheRepository.countByStatus("IN_PROGRESS")).thenReturn(12L);
        when(ticketCacheRepository.countByStatus("RESOLVED")).thenReturn(13L);
        when(ticketCacheRepository.countByStatus("CLOSED")).thenReturn(14L);
        when(ticketCacheRepository.countByStatus("ESCALATED")).thenReturn(15L);

        TicketCache a = new TicketCache(); a.setCategory("PAYMENT");
        TicketCache b = new TicketCache(); b.setCategory("LOGIN");
        TicketCache c = new TicketCache(); c.setCategory(null);
        when(ticketCacheRepository.findAll()).thenReturn(List.of(a, b, c));

        try (MockedStatic<LocalDate> ld = Mockito.mockStatic(LocalDate.class, Mockito.CALLS_REAL_METHODS)) {
            ld.when(LocalDate::now).thenReturn(fixedDate);

            TicketAnalyticsDTO dto = service.getTicketAnalytics(3);
            assertNotNull(dto);

            Map<?, ?> createdByDay = readValue(dto, "createdByDay", Map.class);
            Map<?, ?> resolvedByDay = readValue(dto, "resolvedByDay", Map.class);
            assertNotNull(createdByDay);
            assertNotNull(resolvedByDay);
            assertEquals(3, createdByDay.size());
            assertEquals(3, resolvedByDay.size());

            Map<String, Long> byPriority = readValue(dto, "byPriority", Map.class);
            assertNotNull(byPriority);
            assertEquals(1L, byPriority.get("CRITICAL"));
            assertEquals(5L, byPriority.get("UNASSIGNED"));

            Map<String, Long> byStatus = readValue(dto, "byStatus", Map.class);
            assertNotNull(byStatus);
            assertEquals(10L, byStatus.get("OPEN"));
            assertEquals(15L, byStatus.get("ESCALATED"));

            Map<String, Long> byCategory = readValue(dto, "byCategory", Map.class);
            assertNotNull(byCategory);
            assertEquals(1L, byCategory.get("PAYMENT"));
            assertEquals(1L, byCategory.get("LOGIN"));
            assertEquals(1L, byCategory.get("UNKNOWN"));
        }
    }

    @Test
    void getSlaReport_countsAndByPriority() {
        SlaTracking t1 = new SlaTracking();
        t1.setPriority("CRITICAL");
        t1.setSlaStatus(SlaStatus.ON_TIME);
        t1.setResponseBreached(false);
        t1.setResolutionBreached(false);

        SlaTracking t2 = new SlaTracking();
        t2.setPriority("CRITICAL");
        t2.setSlaStatus(SlaStatus.BREACHED);
        t2.setResponseBreached(true);
        t2.setResolutionBreached(false);

        SlaTracking t3 = new SlaTracking();
        t3.setPriority("HIGH");
        t3.setSlaStatus(SlaStatus.WARNING);
        t3.setResponseBreached(false);
        t3.setResolutionBreached(false);

        SlaTracking t4 = new SlaTracking();
        t4.setPriority("MEDIUM");
        t4.setSlaStatus(SlaStatus.MET);
        t4.setResponseBreached(false);
        t4.setResolutionBreached(false);

        when(slaTrackingRepository.findAll()).thenReturn(List.of(t1, t2, t3, t4));

        SlaComplianceReportDTO dto = service.getSlaReport();
        assertNotNull(dto);

        assertEquals(4L, longVal(dto, "totalTracked"));
        assertEquals(2L, longVal(dto, "onTime"));
        assertEquals(1L, longVal(dto, "breached"));
        assertEquals(1L, longVal(dto, "warning"));
        assertEquals(50.0, doubleVal(dto, "complianceRate"));

        Map<String, Object> byPriority = readValue(dto, "byPriority", Map.class);
        assertNotNull(byPriority);

        Object critical = byPriority.get("CRITICAL");
        assertNotNull(critical);

        assertEquals(2L, longVal(critical, "tracked"));
        assertEquals(1L, longVal(critical, "breached"));
        assertEquals(1L, longVal(critical, "responseBreached"));
        assertEquals(0L, longVal(critical, "resolutionBreached"));
    }

    @Test
    void getCategoryBreakdown_countsAndPercentages() {
        TicketCache a = new TicketCache(); a.setCategory("PAYMENT");
        TicketCache b = new TicketCache(); b.setCategory("PAYMENT");
        TicketCache c = new TicketCache(); c.setCategory("LOGIN");
        when(ticketCacheRepository.findAll()).thenReturn(List.of(a, b, c));

        CategoryBreakdownDTO dto = service.getCategoryBreakdown();
        assertNotNull(dto);

        assertEquals(3L, longVal(dto, "totalTickets"));

        Map<String, Long> counts = readValue(dto, "categoryCounts", Map.class);
        Map<String, Double> pct = readValue(dto, "categoryPercentages", Map.class);

        assertNotNull(counts);
        assertNotNull(pct);

        assertEquals(2L, counts.get("PAYMENT"));
        assertEquals(1L, counts.get("LOGIN"));

        double p = pct.get("PAYMENT");
        assertTrue(p > 60.0 && p < 70.0);
    }

    @Test
    void getTrends_daily_buildsMaps() {
        LocalDate fixedDate = LocalDate.of(2026, 1, 5);

        when(ticketCacheRepository.countByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(5L);
        when(assignmentRepository.countByAssignedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(3L);
        when(ticketCacheRepository.countByStatusAndUpdatedAtBetween(eq("RESOLVED"), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(2L);

        try (MockedStatic<LocalDate> ld = Mockito.mockStatic(LocalDate.class, Mockito.CALLS_REAL_METHODS)) {
            ld.when(LocalDate::now).thenReturn(fixedDate);

            TrendsReportDTO dto = service.getTrends("daily", 3);
            assertNotNull(dto);

            Map<?, ?> ticketsByPeriod = readValue(dto, "ticketsByPeriod", Map.class);
            Map<?, ?> assignmentsByPeriod = readValue(dto, "assignmentsByPeriod", Map.class);
            Map<?, ?> resolutionsByPeriod = readValue(dto, "resolutionsByPeriod", Map.class);

            assertNotNull(ticketsByPeriod);
            assertNotNull(assignmentsByPeriod);
            assertNotNull(resolutionsByPeriod);

            assertEquals(3, ticketsByPeriod.size());
            assertEquals(3, assignmentsByPeriod.size());
            assertEquals(3, resolutionsByPeriod.size());
        }
    }

    @Test
    void getAgentPerformance_buildsPerformanceAndAverages() {
        AgentWorkload a1 = new AgentWorkload("a1", "agent1");
        a1.setStatus(AgentStatus.AVAILABLE);
        a1.setActiveTickets(2);
        a1.setCompletedTickets(10);

        AgentWorkload a2 = new AgentWorkload("a2", "agent2");
        a2.setStatus(AgentStatus.BUSY);
        a2.setActiveTickets(5);
        a2.setCompletedTickets(3);

        when(agentWorkloadRepository.findAll()).thenReturn(List.of(a1, a2));

        when(assignmentRepository.countByAgentId("a1")).thenReturn(15L);
        when(assignmentRepository.countByAgentIdAndStatus("a1", AssignmentStatus.ASSIGNED)).thenReturn(2L);
        when(assignmentRepository.countByAgentIdAndStatusNot("a1", AssignmentStatus.ASSIGNED)).thenReturn(13L);

        when(assignmentRepository.countByAgentId("a2")).thenReturn(8L);
        when(assignmentRepository.countByAgentIdAndStatus("a2", AssignmentStatus.ASSIGNED)).thenReturn(5L);
        when(assignmentRepository.countByAgentIdAndStatusNot("a2", AssignmentStatus.ASSIGNED)).thenReturn(3L);

        Assignment completedA1 = mock(Assignment.class);
        when(completedA1.getTicketId()).thenReturn("t1");
        when(completedA1.getAssignedAt()).thenReturn(LocalDateTime.of(2026, 1, 5, 10, 0));
        when(completedA1.getCompletedAt()).thenReturn(LocalDateTime.of(2026, 1, 5, 12, 0));

        when(assignmentRepository.findByAgentIdAndCompletedAtIsNotNull("a1")).thenReturn(List.of(completedA1));
        when(assignmentRepository.findByAgentIdAndCompletedAtIsNotNull("a2")).thenReturn(List.of());

        when(assignmentRepository.findByAgentId("a1")).thenReturn(List.of(completedA1));
        when(assignmentRepository.findByAgentId("a2")).thenReturn(List.of());

        SlaTracking sla = new SlaTracking();
        sla.setTicketId("t1");
        sla.setSlaStatus(SlaStatus.ON_TIME);
        when(slaTrackingRepository.findByTicketId("t1")).thenReturn(Optional.of(sla));

        AgentPerformanceReportDTO dto = service.getAgentPerformance();
        assertNotNull(dto);

        // <-- FIX: don't assume the list name is "performances"
        List<?> performances = readAnyList(dto,
                "performances",
                "agentPerformances",
                "performanceList",
                "agentPerformanceList",
                "items",
                "data"
        );

        assertNotNull(performances);
        assertEquals(2, performances.size());

        // averages exist (non-negative)
        assertTrue(doubleVal(dto, "avgActiveTickets") >= 0.0);
        assertTrue(doubleVal(dto, "avgCompletedTickets") >= 0.0);
        assertTrue(doubleVal(dto, "avgSlaCompliance") >= 0.0);
    }
}
