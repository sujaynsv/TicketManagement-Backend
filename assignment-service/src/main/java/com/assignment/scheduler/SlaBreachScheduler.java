package com.assignment.scheduler;

import com.assignment.entity.SlaStatus;
import com.assignment.entity.SlaTracking;
import com.assignment.repository.SlaTrackingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class SlaBreachScheduler {
    
    private static final Logger log = LoggerFactory.getLogger(SlaBreachScheduler.class);
    
    @Autowired
    private SlaTrackingRepository slaTrackingRepository;
    
    /**
     * Check for SLA breaches every 60 seconds
     */
    @Scheduled(fixedDelay = 60000) // 60 seconds
    @Transactional
    public void checkSlaBreaches() {
        log.info("=== Running SLA breach check ===");
        
        LocalDateTime now = LocalDateTime.now();
        
        // Get all active SLA trackings (not resolved yet)
        List<SlaTracking> activeTrackings = slaTrackingRepository.findByResolvedAtIsNull();
        
        log.info("Found {} active SLA trackings to check", activeTrackings.size());
        
        int responseBreachedCount = 0;
        int resolutionBreachedCount = 0;
        int warningCount = 0;
        
        for (SlaTracking tracking : activeTrackings) {
            boolean updated = false;
            
            // Skip if already marked as BREACHED
            if (tracking.getSlaStatus() == SlaStatus.BREACHED) {
                continue;
            }
            
            // ===== CHECK RESPONSE SLA =====
            if (tracking.getFirstResponseAt() == null) {
                // Response not yet made
                if (now.isAfter(tracking.getResponseDueAt())) {
                    // BREACHED!
                    tracking.setResponseBreached(true);
                    tracking.setSlaStatus(SlaStatus.BREACHED);
                    tracking.setBreachedAt(now);
                    tracking.setBreachReason("Response SLA exceeded");
                    updated = true;
                    responseBreachedCount++;
                    
                    log.warn("RESPONSE SLA BREACHED: Ticket {} - Due: {}, Now: {}", 
                             tracking.getTicketNumber(), 
                             tracking.getResponseDueAt(), 
                             now);
                } else {
                    // Check if WARNING needed (80% time consumed)
                    long totalMinutes = Duration.between(tracking.getSlaStartTime(), tracking.getResponseDueAt()).toMinutes();
                    long elapsedMinutes = Duration.between(tracking.getSlaStartTime(), now).toMinutes();
                    double percentageUsed = (double) elapsedMinutes / totalMinutes;
                    
                    if (percentageUsed >= 0.8 && tracking.getSlaStatus() != SlaStatus.WARNING) {
                        tracking.setSlaStatus(SlaStatus.WARNING);
                        updated = true;
                        warningCount++;
                        log.warn("RESPONSE SLA WARNING: Ticket {} - {}% time consumed", 
                                 tracking.getTicketNumber(), 
                                 Math.round(percentageUsed * 100));
                    }
                }
            }
            
            // ===== CHECK RESOLUTION SLA =====
            if (tracking.getResolvedAt() == null) {
                // Ticket not yet resolved
                if (now.isAfter(tracking.getResolutionDueAt())) {
                    // BREACHED!
                    tracking.setResolutionBreached(true);
                    tracking.setSlaStatus(SlaStatus.BREACHED);
                    if (tracking.getBreachedAt() == null) {
                        tracking.setBreachedAt(now);
                    }
                    tracking.setBreachReason("Resolution SLA exceeded");
                    updated = true;
                    resolutionBreachedCount++;
                    
                    log.warn("RESOLUTION SLA BREACHED: Ticket {} - Due: {}, Now: {}", 
                             tracking.getTicketNumber(), 
                             tracking.getResolutionDueAt(), 
                             now);
                } else if (tracking.getFirstResponseAt() != null) {
                    // Response made, check resolution warning
                    long totalMinutes = Duration.between(tracking.getSlaStartTime(), tracking.getResolutionDueAt()).toMinutes();
                    long elapsedMinutes = Duration.between(tracking.getSlaStartTime(), now).toMinutes();
                    double percentageUsed = (double) elapsedMinutes / totalMinutes;
                    
                    if (percentageUsed >= 0.8 && tracking.getSlaStatus() == SlaStatus.OK) {
                        tracking.setSlaStatus(SlaStatus.WARNING);
                        updated = true;
                        warningCount++;
                        log.warn("RESOLUTION SLA WARNING: Ticket {} - {}% time consumed", 
                                 tracking.getTicketNumber(), 
                                 Math.round(percentageUsed * 100));
                    }
                }
            }
            
            // Save if updated
            if (updated) {
                tracking.setUpdatedAt(LocalDateTime.now());
                slaTrackingRepository.save(tracking);
            }
        }
        
        log.info("SLA check complete - Response Breached: {}, Resolution Breached: {}, Warnings: {}", 
                 responseBreachedCount, resolutionBreachedCount, warningCount);
    }
}
