package com.notification.service;

import com.notification.dto.NotificationDTO;
import com.notification.dto.UserDTO;
import com.notification.entity.DeliveryChannel;
import com.notification.entity.Notification;
import com.notification.entity.NotificationStatus;
import com.notification.entity.NotificationType;
import com.notification.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private com.notification.service.UserService userService;

    @InjectMocks
    private NotificationService notificationService;

    private static NotificationType anyNotificationType() {
        return NotificationType.values()[0];
    }

    private UserDTO user(String id, String username, String email) {
        UserDTO u = new UserDTO();
        u.setUserId(id);
        u.setUsername(username);
        u.setEmail(email);
        return u;
    }

    private Notification notificationBase() {
        Notification n = new Notification();
        n.setNotificationId("n1");
        n.setUserId("u1");
        n.setUsername("john");
        n.setUserEmail("john@mail.com");
        n.setNotificationType(anyNotificationType());
        n.setEventType("UPDATED");
        n.setTicketId("tid");
        n.setTicketNumber("T-1");
        n.setSubject("sub");
        n.setMessage("msg");
        n.setDeliveryChannel(DeliveryChannel.EMAIL);
        n.setStatus(NotificationStatus.PENDING);
        n.setRetryCount(0);
        n.setIsRead(false);
        n.setCreatedAt(LocalDateTime.now().minusMinutes(10));
        return n;
    }

    @Test
    void createNotification_email_success_setsSent() {
        when(userService.getUserWithFallback("u1", "john"))
                .thenReturn(user("u1", "john", "john@mail.com"));

        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        doNothing().when(emailService).sendEmail(anyString(), anyString(), anyString());

        Notification created = notificationService.createNotification(
                "u1", "john",
                anyNotificationType(),
                "UPDATED",
                "tid", "T-1",
                "sub", "msg",
                DeliveryChannel.EMAIL
        );

        assertNotNull(created);
        assertEquals("u1", created.getUserId());
        assertEquals("john@mail.com", created.getUserEmail());

        verify(emailService).sendEmail("john@mail.com", "sub", "msg");
        verify(notificationRepository, atLeast(2)).save(any(Notification.class));
    }

    @Test
    void createNotification_inApp_marksSent_andSaves() {
        when(userService.getUserWithFallback("u1", "john"))
                .thenReturn(user("u1", "john", "john@mail.com"));

        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Notification created = notificationService.createNotification(
                "u1", "john",
                anyNotificationType(),
                "UPDATED",
                "tid", "T-1",
                "sub", "msg",
                DeliveryChannel.IN_APP
        );

        assertNotNull(created);
        verifyNoInteractions(emailService);
        verify(notificationRepository, atLeast(2)).save(any(Notification.class));
    }

    @Test
    void createNotification_email_sendFails_setsFailed_andIncrementsRetry() {
        when(userService.getUserWithFallback("u1", "john"))
                .thenReturn(user("u1", "john", "john@mail.com"));

        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        doThrow(new RuntimeException("smtp down"))
                .when(emailService).sendEmail(anyString(), anyString(), anyString());

        Notification created = notificationService.createNotification(
                "u1", "john",
                anyNotificationType(),
                "UPDATED",
                "tid", "T-1",
                "sub", "msg",
                DeliveryChannel.EMAIL
        );

        assertNotNull(created);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, atLeast(2)).save(captor.capture());

        Notification lastSaved = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertEquals(NotificationStatus.FAILED, lastSaved.getStatus());
        assertEquals("smtp down", lastSaved.getErrorMessage());
        assertEquals(1, lastSaved.getRetryCount());
    }

    @Test
    void sendNotification_outerCatch_setsFailed() throws Exception {
        // Trigger outer catch via IN_APP branch (sendNotification itself does repository.save)
        Notification n = notificationBase();
        n.setDeliveryChannel(DeliveryChannel.IN_APP);

        // 1st save (inside try) throws -> outer catch should execute
        // 2nd save (inside catch) must succeed, otherwise method will throw
        when(notificationRepository.save(any(Notification.class)))
                .thenThrow(new RuntimeException("db down"))
                .thenAnswer(inv -> inv.getArgument(0)); // consecutive stubbing [web:633]

        var m = NotificationService.class.getDeclaredMethod("sendNotification", Notification.class);
        m.setAccessible(true);

        assertDoesNotThrow(() -> m.invoke(notificationService, n));

        assertEquals(NotificationStatus.FAILED, n.getStatus());
        assertEquals("db down", n.getErrorMessage());

        verify(notificationRepository, times(2)).save(any(Notification.class));
        verifyNoInteractions(emailService);
    }

    @Test
    void getUserNotifications_mapsToDto() {
        Notification n = notificationBase();
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc("u1"))
                .thenReturn(List.of(n));

        List<NotificationDTO> dtos = notificationService.getUserNotifications("u1");

        assertEquals(1, dtos.size());
        assertEquals("n1", dtos.get(0).getNotificationId());
        assertEquals("u1", dtos.get(0).getUserId());
        assertEquals("john", dtos.get(0).getUsername());
    }

    @Test
    void getUnreadNotifications_mapsToDto() {
        Notification n = notificationBase();
        n.setIsRead(false);

        when(notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc("u1"))
                .thenReturn(List.of(n));

        List<NotificationDTO> dtos = notificationService.getUnreadNotifications("u1");

        assertEquals(1, dtos.size());
        assertFalse(dtos.get(0).getIsRead());
    }

    @Test
    void markAsRead_whenPresent_updatesAndSaves() {
        Notification n = notificationBase();
        n.setIsRead(false);

        when(notificationRepository.findById("n1")).thenReturn(Optional.of(n));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        notificationService.markAsRead("n1");

        assertTrue(n.getIsRead());
        assertNotNull(n.getReadAt());
        verify(notificationRepository).save(n);
    }

    @Test
    void markAsRead_whenMissing_doesNothing() {
        when(notificationRepository.findById("n1")).thenReturn(Optional.empty());

        notificationService.markAsRead("n1");

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void markAllAsRead_setsAllRead_andSaveAll() {
        Notification n1 = notificationBase();
        n1.setNotificationId("n1");
        n1.setIsRead(false);

        Notification n2 = notificationBase();
        n2.setNotificationId("n2");
        n2.setIsRead(false);

        when(notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc("u1"))
                .thenReturn(List.of(n1, n2));

        notificationService.markAllAsRead("u1");

        assertTrue(n1.getIsRead());
        assertTrue(n2.getIsRead());
        assertNotNull(n1.getReadAt());
        assertNotNull(n2.getReadAt());

        verify(notificationRepository).saveAll(anyList());
    }

    @Test
    void getUnreadCount_returnsRepoValue() {
        when(notificationRepository.countByUserIdAndIsReadFalse("u1")).thenReturn(5L);

        Long count = notificationService.getUnreadCount("u1");

        assertEquals(5L, count);
        verify(notificationRepository).countByUserIdAndIsReadFalse("u1");
    }
}
