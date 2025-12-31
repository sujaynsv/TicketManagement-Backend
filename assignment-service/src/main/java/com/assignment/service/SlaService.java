package com.assignment.service;

import com.assignment.entity.SlaRule;
import com.assignment.entity.SlaStatus;
import com.assignment.entity.SlaTracking;
import com.assignment.repository.SlaRuleRepository;
import com.assignment.repository.SlaTrackingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

@Service
public class SlaService {
    
    private static final Logger log = LoggerFactory.getLogger(SlaService.class);
    
    @Autowired
    private SlaRuleRepository slaRuleRepository;
    
    @Autowired
    private SlaTrackingRepository slaTrackingRepository;
    
    @Value("${sla.business-hours.enabled}")
    private Boolean businessHoursEnabled;
    
    @Value("${sla.business-hours.start-time}")
    private String businessStartTime;
    
    @Value("${sla.business-hours.end-time}")
    private String businessEndTime;
    
    /**
     * Create SLA tracking for a new ticket
     */
    @Transactional
    public SlaTracking createSlaTracking(String ticketId, String ticketNumber, 
                                        String priority, String category) {
        // Get SLA rule
        SlaRule rule = getSlaRule(priority, category);
        
        LocalDateTime now = LocalDateTime.now();
        
        // Calculate due times
        LocalDateTime responseDueAt = now.plusMinutes(rule.getResponseTimeMinutes());
        LocalDateTime resolutionDueAt = now.plusHours(rule.getResolutionTimeHours());
        
        // Create tracking
        SlaTracking tracking = new SlaTracking();
        tracking.setTicketId(ticketId);
        tracking.setTicketNumber(ticketNumber);
        tracking.setPriority(priority);
        tracking.setCategory(category);
        tracking.setSlaStartTime(now);
        tracking.setResponseDueAt(responseDueAt);
        tracking.setResolutionDueAt(resolutionDueAt);
        tracking.setSlaStatus(SlaStatus.OK);
        
        SlaTracking savedTracking = slaTrackingRepository.save(tracking);
        
        log.info("Created SLA tracking for ticket {}: Response due at {}, Resolution due at {}", 
                 ticketNumber, responseDueAt, resolutionDueAt);
        
        return savedTracking;
    }
    
    /**
     * Get SLA rule for priority and category
     */
    private SlaRule getSlaRule(String priority, String category) {
        // Try to find category-specific rule first
        Optional<SlaRule> categoryRule = slaRuleRepository.findByPriorityAndCategory(priority, category);
        if (categoryRule.isPresent()) {
            return categoryRule.get();
        }
        
        // Fall back to default rule (no category)
        Optional<SlaRule> defaultRule = slaRuleRepository.findByPriorityAndCategoryIsNull(priority);
        if (defaultRule.isPresent()) {
            return defaultRule.get();
        }
        
        // If no rule exists, create default based on priority
        return createDefaultSlaRule(priority);
    }
    
    /**
     * Create default SLA rule if none exists
     */
    private SlaRule createDefaultSlaRule(String priority) {
        SlaRule rule = new SlaRule();
        rule.setPriority(priority);
        
        // Default SLA times based on priority
        switch (priority.toUpperCase()) {
            case "CRITICAL":
                rule.setResponseTimeMinutes(15);
                rule.setResolutionTimeHours(4);
                break;
            case "HIGH":
                rule.setResponseTimeMinutes(60);
                rule.setResolutionTimeHours(8);
                break;
            case "MEDIUM":
                rule.setResponseTimeMinutes(240);
                rule.setResolutionTimeHours(24);
                break;
            case "LOW":
                rule.setResponseTimeMinutes(480);
                rule.setResolutionTimeHours(48);
                break;
            default:
                rule.setResponseTimeMinutes(240);
                rule.setResolutionTimeHours(24);
        }
        
        return slaRuleRepository.save(rule);
    }
    
    /**
     * Update SLA tracking when first response is made
     */
    @Transactional
    public void recordFirstResponse(String ticketId) {
        Optional<SlaTracking> trackingOpt = slaTrackingRepository.findByTicketId(ticketId);
        if (trackingOpt.isEmpty()) {
            log.warn("No SLA tracking found for ticket {}", ticketId);
            return;
        }
        
        SlaTracking tracking = trackingOpt.get();
        
        // Already recorded
        if (tracking.getFirstResponseAt() != null) {
            return;
        }
        
        LocalDateTime now = LocalDateTime.now();
        tracking.setFirstResponseAt(now);
        
        // Calculate response time in minutes
        long minutes = Duration.between(tracking.getSlaStartTime(), now).toMinutes();
        tracking.setResponseTimeMinutes((int) minutes);
        
        // Check if breached
        if (now.isAfter(tracking.getResponseDueAt())) {
            tracking.setResponseBreached(true);
            tracking.setSlaStatus(SlaStatus.BREACHED);
            tracking.setBreachedAt(now);
            tracking.setBreachReason("First response exceeded SLA time");
            log.warn("Response SLA breached for ticket {}: {} minutes (due in {} minutes)", 
                     tracking.getTicketNumber(), minutes, 
                     Duration.between(tracking.getSlaStartTime(), tracking.getResponseDueAt()).toMinutes());
        } else {
            log.info("First response recorded for ticket {} within SLA: {} minutes", 
                     tracking.getTicketNumber(), minutes);
        }
        
        tracking.setUpdatedAt(LocalDateTime.now());
        slaTrackingRepository.save(tracking);
    }
    
    /**
     * Update SLA tracking when ticket is resolved
     */
    @Transactional
    public void recordResolution(String ticketId) {
        Optional<SlaTracking> trackingOpt = slaTrackingRepository.findByTicketId(ticketId);
        if (trackingOpt.isEmpty()) {
            log.warn("No SLA tracking found for ticket {}", ticketId);
            return;
        }
        
        SlaTracking tracking = trackingOpt.get();
        
        // Already resolved
        if (tracking.getResolvedAt() != null) {
            return;
        }
        
        LocalDateTime now = LocalDateTime.now();
        tracking.setResolvedAt(now);
        
        // Calculate resolution time in hours
        long minutes = Duration.between(tracking.getSlaStartTime(), now).toMinutes();
        BigDecimal hours = BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, BigDecimal.ROUND_HALF_UP);
        tracking.setResolutionTimeHours(hours);
        
        // Check if breached
        if (now.isAfter(tracking.getResolutionDueAt())) {
            tracking.setResolutionBreached(true);
            tracking.setSlaStatus(SlaStatus.BREACHED);
            if (tracking.getBreachedAt() == null) {
                tracking.setBreachedAt(now);
            }
            tracking.setBreachReason("Resolution exceeded SLA time");
            log.warn("Resolution SLA breached for ticket {}: {} hours", 
                     tracking.getTicketNumber(), hours);
        } else {
            log.info("Ticket {} resolved within SLA: {} hours", 
                     tracking.getTicketNumber(), hours);
        }
        
        tracking.setUpdatedAt(LocalDateTime.now());
        slaTrackingRepository.save(tracking);
    }
    
    /**
     * Get SLA tracking for a ticket
     */
    public Optional<SlaTracking> getSlaTracking(String ticketId) {
        return slaTrackingRepository.findByTicketId(ticketId);
    }
    
    /**
     * Calculate time remaining until SLA breach
     */
    public String calculateTimeRemaining(SlaTracking tracking) {
        LocalDateTime now = LocalDateTime.now();
        
        // If already resolved, return completed
        if (tracking.getResolvedAt() != null) {
            return "Completed";
        }
        
        // Check response SLA first
        if (tracking.getFirstResponseAt() == null) {
            long minutesRemaining = Duration.between(now, tracking.getResponseDueAt()).toMinutes();
            if (minutesRemaining < 0) {
                return "Breached";
            }
            return formatTimeRemaining(minutesRemaining);
        }
        
        // Check resolution SLA
        long minutesRemaining = Duration.between(now, tracking.getResolutionDueAt()).toMinutes();
        if (minutesRemaining < 0) {
            return "Breached";
        }
        return formatTimeRemaining(minutesRemaining);
    }
    
    /**
     * Format time remaining in human-readable format
     */
    private String formatTimeRemaining(long minutes) {
        if (minutes < 60) {
            return minutes + " minutes";
        } else if (minutes < 1440) {
            return (minutes / 60) + " hours " + (minutes % 60) + " minutes";
        } else {
            long days = minutes / 1440;
            long hours = (minutes % 1440) / 60;
            return days + " days " + hours + " hours";
        }
    }
}
