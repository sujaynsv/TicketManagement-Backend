package com.assignment.scheduler;

import com.assignment.entity.SlaStatus;
import com.assignment.entity.SlaTracking;
import com.assignment.entity.TicketCache;
import com.assignment.repository.SlaTrackingRepository;
import com.assignment.repository.TicketCacheRepository;
import com.assignment.service.EventPublisher;
import com.ticket.event.SlaBreachEvent;
import com.ticket.event.SlaWarningEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class SlaBreachScheduler {
    
    private static final Logger log = LoggerFactory.getLogger(SlaBreachScheduler.class);
    
    private SlaTrackingRepository slaTrackingRepository;
    
    private TicketCacheRepository ticketCacheRepository;
    
    private EventPublisher eventPublisher;

    public SlaBreachScheduler(SlaTrackingRepository slaTrackingRepository, TicketCacheRepository ticketCacheRepository, EventPublisher eventPublisher){
        this.eventPublisher = eventPublisher;
        this.ticketCacheRepository = ticketCacheRepository;
        this.slaTrackingRepository = slaTrackingRepository;
    }
    
    private Set<String> warningsSent = new HashSet<>();
    private Set<String> breachesSent = new HashSet<>();
    
    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void checkSlaBreaches() {
        log.info("=== Running SLA breach check ===");

        LocalDateTime now = LocalDateTime.now();
        List<SlaTracking> activeTrackings = slaTrackingRepository.findByResolvedAtIsNull();

        log.info("Found {} active SLA trackings to check", activeTrackings.size());

        int responseBreachedCount = 0;
        int resolutionBreachedCount = 0;
        int warningCount = 0;

        for (SlaTracking tracking : activeTrackings) {
            if (tracking.getSlaStatus() == SlaStatus.BREACHED) {
                continue;
            }

            TicketCache ticket = ticketCacheRepository.findByTicketId(tracking.getTicketId())
                    .orElse(null);

            Result responseResult = handleResponseSla(tracking, ticket, now);
            Result resolutionResult = handleResolutionSla(tracking, ticket, now);

            if (responseResult.updated || resolutionResult.updated) {
                tracking.setUpdatedAt(LocalDateTime.now());
                slaTrackingRepository.save(tracking);
            }

            responseBreachedCount += responseResult.breached ? 1 : 0;
            resolutionBreachedCount += resolutionResult.breached ? 1 : 0;
            warningCount += responseResult.warning ? 1 : 0;
            warningCount += resolutionResult.warning ? 1 : 0;
        }

        log.info("SLA check complete - Response Breached: {}, Resolution Breached: {}, Warnings: {}",
                responseBreachedCount, resolutionBreachedCount, warningCount);
    }

    private static class Result {
        boolean updated;
        boolean breached;
        boolean warning;

        Result(boolean updated, boolean breached, boolean warning) {
            this.updated = updated;
            this.breached = breached;
            this.warning = warning;
        }
    }

    private Result handleResponseSla(SlaTracking tracking, TicketCache ticket, LocalDateTime now) {
        boolean updated = false;
        boolean breached = false;
        boolean warning = false;

        if (tracking.getFirstResponseAt() == null) {
            if (now.isAfter(tracking.getResponseDueAt())) {
                tracking.setResponseBreached(true);
                tracking.setSlaStatus(SlaStatus.BREACHED);
                tracking.setBreachedAt(now);
                tracking.setBreachReason("Response SLA exceeded");
                updated = true;
                breached = true;

                log.warn("RESPONSE SLA BREACHED: Ticket {} - Due: {}, Now: {}",
                        tracking.getTicketNumber(),
                        tracking.getResponseDueAt(),
                        now);

                publishResponseBreachEvent(tracking, ticket, now);
            } else {
                long totalMinutes = Duration.between(tracking.getSlaStartTime(), tracking.getResponseDueAt()).toMinutes();
                long elapsedMinutes = Duration.between(tracking.getSlaStartTime(), now).toMinutes();
                double percentageUsed = (double) elapsedMinutes / totalMinutes;

                if (percentageUsed >= 0.8 && tracking.getSlaStatus() != SlaStatus.WARNING) {
                    tracking.setSlaStatus(SlaStatus.WARNING);
                    updated = true;
                    warning = true;
                    log.warn("RESPONSE SLA WARNING: Ticket {} - {}% time consumed",
                            tracking.getTicketNumber(),
                            Math.round(percentageUsed * 100));

                    publishResponseWarningEvent(tracking, ticket, now, percentageUsed);
                }
            }
        }
        return new Result(updated, breached, warning);
    }

    private Result handleResolutionSla(SlaTracking tracking, TicketCache ticket, LocalDateTime now) {
        boolean updated = false;
        boolean breached = false;
        boolean warning = false;

        if (tracking.getResolvedAt() == null) {
            if (now.isAfter(tracking.getResolutionDueAt())) {
                tracking.setResolutionBreached(true);
                tracking.setSlaStatus(SlaStatus.BREACHED);
                if (tracking.getBreachedAt() == null) {
                    tracking.setBreachedAt(now);
                }
                tracking.setBreachReason("Resolution SLA exceeded");
                updated = true;
                breached = true;

                log.warn("RESOLUTION SLA BREACHED: Ticket {} - Due: {}, Now: {}",
                        tracking.getTicketNumber(),
                        tracking.getResolutionDueAt(),
                        now);

                publishResolutionBreachEvent(tracking, ticket, now);
            } else if (tracking.getFirstResponseAt() != null) {
                long totalMinutes = Duration.between(tracking.getSlaStartTime(), tracking.getResolutionDueAt()).toMinutes();
                long elapsedMinutes = Duration.between(tracking.getSlaStartTime(), now).toMinutes();
                double percentageUsed = (double) elapsedMinutes / totalMinutes;

                if (percentageUsed >= 0.8 && tracking.getSlaStatus() == SlaStatus.ON_TIME) {
                    tracking.setSlaStatus(SlaStatus.WARNING);
                    updated = true;
                    warning = true;
                    log.warn("RESOLUTION SLA WARNING: Ticket {} - {}% time consumed",
                            tracking.getTicketNumber(),
                            Math.round(percentageUsed * 100));

                    publishResolutionWarningEvent(tracking, ticket, now, percentageUsed);
                }
            }
        }
        return new Result(updated, breached, warning);
    }
    
    private void publishResponseWarningEvent(SlaTracking tracking, TicketCache ticket, 
                                            LocalDateTime now, double percentageUsed) {
        String key = tracking.getTrackingId() + "-RESPONSE-WARNING";
        if (warningsSent.contains(key)) {
            return;
        }
        
        SlaWarningEvent event = new SlaWarningEvent();
        event.setTrackingId(tracking.getTrackingId());
        event.setTicketId(tracking.getTicketId());
        event.setTicketNumber(tracking.getTicketNumber());
        event.setPriority(tracking.getPriority());
        event.setCategory(tracking.getCategory());
        event.setWarningType("RESPONSE");
        event.setDueAt(tracking.getResponseDueAt());
        event.setMinutesRemaining((int) Duration.between(now, tracking.getResponseDueAt()).toMinutes());
        event.setPercentageTimeUsed(percentageUsed * 100);
        
        if (ticket != null) {
            event.setAssignedAgentId(ticket.getAssignedAgentId());
            event.setAssignedAgentUsername(ticket.getAssignedAgentUsername());
        }
        
        eventPublisher.publishSlaWarning(event);
        warningsSent.add(key);
    }
    
    private void publishResolutionWarningEvent(SlaTracking tracking, TicketCache ticket, 
                                               LocalDateTime now, double percentageUsed) {
        String key = tracking.getTrackingId() + "-RESOLUTION-WARNING";
        if (warningsSent.contains(key)) {
            return;
        }
        
        SlaWarningEvent event = new SlaWarningEvent();
        event.setTrackingId(tracking.getTrackingId());
        event.setTicketId(tracking.getTicketId());
        event.setTicketNumber(tracking.getTicketNumber());
        event.setPriority(tracking.getPriority());
        event.setCategory(tracking.getCategory());
        event.setWarningType("RESOLUTION");
        event.setDueAt(tracking.getResolutionDueAt());
        event.setMinutesRemaining((int) Duration.between(now, tracking.getResolutionDueAt()).toMinutes());
        event.setPercentageTimeUsed(percentageUsed * 100);
        
        if (ticket != null) {
            event.setAssignedAgentId(ticket.getAssignedAgentId());
            event.setAssignedAgentUsername(ticket.getAssignedAgentUsername());
        }
        
        eventPublisher.publishSlaWarning(event);
        warningsSent.add(key);
    }
    
    private void publishResponseBreachEvent(SlaTracking tracking, TicketCache ticket, LocalDateTime now) {
        String key = tracking.getTrackingId() + "-RESPONSE-BREACH";
        if (breachesSent.contains(key)) {
            return;
        }
        
        SlaBreachEvent event = new SlaBreachEvent();
        event.setTrackingId(tracking.getTrackingId());
        event.setTicketId(tracking.getTicketId());
        event.setTicketNumber(tracking.getTicketNumber());
        event.setPriority(tracking.getPriority());
        event.setCategory(tracking.getCategory());
        event.setBreachType("RESPONSE");
        event.setDueAt(tracking.getResponseDueAt());
        event.setBreachedAt(now);
        event.setMinutesOverdue((int) Duration.between(tracking.getResponseDueAt(), now).toMinutes());
        event.setBreachReason(tracking.getBreachReason());
        event.setResponseBreached(true);
        event.setResolutionBreached(false);
        
        if (ticket != null) {
            event.setAssignedAgentId(ticket.getAssignedAgentId());
            event.setAssignedAgentUsername(ticket.getAssignedAgentUsername());
        }
        
        eventPublisher.publishSlaBreach(event);
        breachesSent.add(key);
    }
    
    private void publishResolutionBreachEvent(SlaTracking tracking, TicketCache ticket, LocalDateTime now) {
        String key = tracking.getTrackingId() + "-RESOLUTION-BREACH";
        if (breachesSent.contains(key)) {
            return;
        }
        
        SlaBreachEvent event = new SlaBreachEvent();
        event.setTrackingId(tracking.getTrackingId());
        event.setTicketId(tracking.getTicketId());
        event.setTicketNumber(tracking.getTicketNumber());
        event.setPriority(tracking.getPriority());
        event.setCategory(tracking.getCategory());
        event.setBreachType("RESOLUTION");
        event.setDueAt(tracking.getResolutionDueAt());
        event.setBreachedAt(now);
        event.setMinutesOverdue((int) Duration.between(tracking.getResolutionDueAt(), now).toMinutes());
        event.setBreachReason(tracking.getBreachReason());
        event.setResponseBreached(false);
        event.setResolutionBreached(true);
        
        if (ticket != null) {
            event.setAssignedAgentId(ticket.getAssignedAgentId());
            event.setAssignedAgentUsername(ticket.getAssignedAgentUsername());
        }
        
        eventPublisher.publishSlaBreach(event);
        breachesSent.add(key);
    }
}
