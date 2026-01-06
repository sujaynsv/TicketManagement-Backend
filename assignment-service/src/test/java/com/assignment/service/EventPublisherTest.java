package com.assignment.service;

import com.ticket.event.SlaBreachEvent;
import com.ticket.event.SlaWarningEvent;
import com.ticket.event.TicketAssignedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventPublisherTest {

    private static final String EXCHANGE = "ticket.exchange";

    @Mock
    private RabbitTemplate rabbitTemplate;

    private EventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new EventPublisher(rabbitTemplate);
    }

    // -------- TicketAssignedEvent --------

    @Test
    void publishTicketAssigned_success_sendsMessage() {
        TicketAssignedEvent event = mock(TicketAssignedEvent.class);

        assertDoesNotThrow(() -> publisher.publishTicketAssigned(event));

        verify(rabbitTemplate, times(1))
                .convertAndSend(EXCHANGE, "ticket.assigned", event);
        verifyNoMoreInteractions(rabbitTemplate);
    }

    @Test
    void publishTicketAssigned_failure_doesNotThrow() {
        TicketAssignedEvent event = mock(TicketAssignedEvent.class);

        doThrow(new RuntimeException("boom"))
                .when(rabbitTemplate)
                .convertAndSend(EXCHANGE, "ticket.assigned", event);

        assertDoesNotThrow(() -> publisher.publishTicketAssigned(event));

        verify(rabbitTemplate, times(1))
                .convertAndSend(EXCHANGE, "ticket.assigned", event);
        verifyNoMoreInteractions(rabbitTemplate);
    }

    // -------- SlaWarningEvent --------

    @Test
    void publishSlaWarning_success_sendsMessage() {
        SlaWarningEvent event = mock(SlaWarningEvent.class);

        assertDoesNotThrow(() -> publisher.publishSlaWarning(event));

        verify(rabbitTemplate, times(1))
                .convertAndSend(EXCHANGE, "sla.warning", event);
        verifyNoMoreInteractions(rabbitTemplate);
    }

    @Test
    void publishSlaWarning_failure_doesNotThrow() {
        SlaWarningEvent event = mock(SlaWarningEvent.class);

        doThrow(new RuntimeException("boom"))
                .when(rabbitTemplate)
                .convertAndSend(EXCHANGE, "sla.warning", event);

        assertDoesNotThrow(() -> publisher.publishSlaWarning(event));

        verify(rabbitTemplate, times(1))
                .convertAndSend(EXCHANGE, "sla.warning", event);
        verifyNoMoreInteractions(rabbitTemplate);
    }

    // -------- SlaBreachEvent --------

    @Test
    void publishSlaBreach_success_sendsMessage() {
        SlaBreachEvent event = mock(SlaBreachEvent.class);

        assertDoesNotThrow(() -> publisher.publishSlaBreach(event));

        verify(rabbitTemplate, times(1))
                .convertAndSend(EXCHANGE, "sla.breach", event);
        verifyNoMoreInteractions(rabbitTemplate);
    }

    @Test
    void publishSlaBreach_failure_doesNotThrow() {
        SlaBreachEvent event = mock(SlaBreachEvent.class);

        doThrow(new RuntimeException("boom"))
                .when(rabbitTemplate)
                .convertAndSend(EXCHANGE, "sla.breach", event);

        assertDoesNotThrow(() -> publisher.publishSlaBreach(event));

        verify(rabbitTemplate, times(1))
                .convertAndSend(EXCHANGE, "sla.breach", event);
        verifyNoMoreInteractions(rabbitTemplate);
    }
}
