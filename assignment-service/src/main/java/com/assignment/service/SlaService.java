package com.assignment.service;

import com.assignment.entity.SlaRule;
import com.assignment.entity.SlaStatus;
import com.assignment.entity.SlaTracking;
import com.assignment.repository.SlaRuleRepository;
import com.assignment.repository.SlaTrackingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class SlaService {
    
    private static final Logger log = LoggerFactory.getLogger(SlaService.class);

    private static final String DEFAULT_PRIORITY = "MEDIUM";
    private static final String BREACHED_LITERAL = "Breached";
    
    private SlaRuleRepository slaRuleRepository;
    
    private SlaTrackingRepository slaTrackingRepository;

    // Inject self proxy for transactional method calls
    private final SlaService self;

    public SlaService(SlaTrackingRepository slaTrackingRepository, SlaRuleRepository slaRuleRepository, SlaService self){
        this.slaRuleRepository = slaRuleRepository;
        this.slaTrackingRepository = slaTrackingRepository;
        this.self = self;
    }

    // For backward compatibility with Spring's proxy injection
    @org.springframework.beans.factory.annotation.Autowired
    public SlaService(SlaTrackingRepository slaTrackingRepository, SlaRuleRepository slaRuleRepository) {
        this.slaRuleRepository = slaRuleRepository;
        this.slaTrackingRepository = slaTrackingRepository;
        this.self = this;
    }
    
    
    @Value("${sla.business-hours.enabled:false}")
    private Boolean businessHoursEnabled;
    
    @Value("${sla.business-hours.start-time:09:00}")
    private String businessStartTime;
    
    @Value("${sla.business-hours.end-time:18:00}")
    private String businessEndTime;
    
    /**
     * Create SLA tracking for a new ticket
     * Only creates tracking if priority is set (by manager)
     */
    @Transactional
    public SlaTracking createSlaTracking(String ticketId, String ticketNumber, 
                                        String priority, String category) {
        // If priority is null, don't create SLA tracking yet
        if (priority == null || priority.trim().isEmpty()) {
            log.info("Skipping SLA tracking for ticket {} - priority not set yet", ticketNumber);
            return null;
        }
        
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
        tracking.setSlaStatus(SlaStatus.ON_TIME); //   Changed from OK
        
        SlaTracking savedTracking = slaTrackingRepository.save(tracking);
        
        log.info("Created SLA tracking for ticket {}: Priority={}, Response due at {}, Resolution due at {}", 
                 ticketNumber, priority, responseDueAt, resolutionDueAt);
        
        return savedTracking;
    }
    
    /**
     * Create SLA tracking when priority is assigned by manager
     */
    @Transactional
    public SlaTracking createSlaTrackingOnPriorityAssignment(String ticketId, String ticketNumber, 
                                                             String priority, String category) {
        // Check if tracking already exists
        Optional<SlaTracking> existingTracking = slaTrackingRepository.findByTicketId(ticketId);
        if (existingTracking.isPresent()) {
            log.info("SLA tracking already exists for ticket {}", ticketNumber);
            return existingTracking.get();
        }
        
        // Create new tracking
        return self.createSlaTracking(ticketId, ticketNumber, priority, category);
    }
    
    /**
     * Get SLA rule for priority and category
     */
    private SlaRule getSlaRule(String priority, String category) {
        // Normalize priority
        String normalizedPriority = priority != null ? priority.toUpperCase() : DEFAULT_PRIORITY;
        
        // Try to find category-specific rule first
        if (category != null && !category.trim().isEmpty()) {
            Optional<SlaRule> categoryRule = slaRuleRepository.findByPriorityAndCategory(normalizedPriority, category);
            if (categoryRule.isPresent()) {
                log.info("Using category-specific SLA rule: priority={}, category={}", normalizedPriority, category);
                return categoryRule.get();
            }
        }
        
        // Fall back to default rule (no category)
        Optional<SlaRule> defaultRule = slaRuleRepository.findByPriorityAndCategoryIsNull(normalizedPriority);
        if (defaultRule.isPresent()) {
            log.info("Using default SLA rule for priority={}", normalizedPriority);
            return defaultRule.get();
        }
        
        // If no rule exists, create default based on priority
        log.info("No SLA rule found, creating default for priority={}", normalizedPriority);
        return createDefaultSlaRule(normalizedPriority, category);
    }
    
    /**
     * Create default SLA rule if none exists
     */
    private SlaRule createDefaultSlaRule(String priority, String category) {
        SlaRule rule = new SlaRule();
        
        // Handle null priority - use MEDIUM as fallback
        String effectivePriority = priority != null ? priority.toUpperCase() : DEFAULT_PRIORITY;
        rule.setPriority(effectivePriority);
        rule.setCategory(category);
        
        // Default SLA times based on priority
        switch (effectivePriority) {
            case "CRITICAL":
                rule.setResponseTimeMinutes(15);
                rule.setResolutionTimeHours(4);
                break;
            case "HIGH":
                rule.setResponseTimeMinutes(60);
                rule.setResolutionTimeHours(8);
                break;
            case DEFAULT_PRIORITY:
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
        
        SlaRule savedRule = slaRuleRepository.save(rule);
        log.info("Created default SLA rule: priority={}, category={}, responseTime={}min, resolutionTime={}hrs",
                effectivePriority, category, rule.getResponseTimeMinutes(), rule.getResolutionTimeHours());
        
        return savedRule;
    }
    
    /**
     * Update SLA tracking when first response is made
     */
    @Transactional
    public void recordFirstResponse(String ticketId) {
        Optional<SlaTracking> trackingOpt = slaTrackingRepository.findByTicketId(ticketId);
        if (trackingOpt.isEmpty()) {
            log.debug("No SLA tracking found for ticket {} - priority may not be set yet", ticketId);
            return;
        }
        
        SlaTracking tracking = trackingOpt.get();
        
        // Already recorded
        if (tracking.getFirstResponseAt() != null) {
            log.debug("First response already recorded for ticket {}", tracking.getTicketNumber());
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
            tracking.setSlaStatus(SlaStatus.BREACHED); //   Stays same
            tracking.setBreachedAt(now);
            tracking.setBreachReason("First response exceeded SLA time");
            log.warn("Response SLA breached for ticket {}: {} minutes (due in {} minutes)", 
                     tracking.getTicketNumber(), minutes, 
                     Duration.between(tracking.getSlaStartTime(), tracking.getResponseDueAt()).toMinutes());
        } else {
            // Update status based on time remaining
            updateSlaStatusBasedOnTimeRemaining(tracking);
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
            log.debug("No SLA tracking found for ticket {} - priority may not be set yet", ticketId);
            return;
        }
        
        SlaTracking tracking = trackingOpt.get();
        
        // Already resolved
        if (tracking.getResolvedAt() != null) {
            log.debug("Ticket {} already marked as resolved", tracking.getTicketNumber());
            return;
        }
        
        LocalDateTime now = LocalDateTime.now();
        tracking.setResolvedAt(now);
        
        // Calculate resolution time in hours
        long minutes = Duration.between(tracking.getSlaStartTime(), now).toMinutes();
        BigDecimal hours = BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        tracking.setResolutionTimeHours(hours);
        
        // Check if breached
        if (now.isAfter(tracking.getResolutionDueAt())) {
            tracking.setResolutionBreached(true);
            tracking.setSlaStatus(SlaStatus.BREACHED); //   Stays same
            if (tracking.getBreachedAt() == null) {
                tracking.setBreachedAt(now);
            }
            tracking.setBreachReason("Resolution exceeded SLA time");
            log.warn("Resolution SLA breached for ticket {}: {} hours", 
                     tracking.getTicketNumber(), hours);
        } else {
            //   Mark as MET if not breached
            if (tracking.getSlaStatus() != SlaStatus.BREACHED) {
                tracking.setSlaStatus(SlaStatus.MET); //   Changed from OK to MET
            }
            log.info("Ticket {} resolved within SLA: {} hours", 
                     tracking.getTicketNumber(), hours);
        }
        
        tracking.setUpdatedAt(LocalDateTime.now());
        slaTrackingRepository.save(tracking);
    }
    
    /**
     * Update SLA status based on time remaining (for WARNING state)
     */
    private void updateSlaStatusBasedOnTimeRemaining(SlaTracking tracking) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dueTime = tracking.getResolvedAt() == null 
                ? tracking.getResolutionDueAt() 
                : tracking.getResponseDueAt();
        
        long totalMinutes = Duration.between(tracking.getSlaStartTime(), dueTime).toMinutes();
        long remainingMinutes = Duration.between(now, dueTime).toMinutes();
        
        if (remainingMinutes < 0) {
            tracking.setSlaStatus(SlaStatus.BREACHED);
        } else if (remainingMinutes < totalMinutes * 0.2) { // Less than 20% time remaining
            tracking.setSlaStatus(SlaStatus.WARNING); //   Set WARNING
        } else {
            tracking.setSlaStatus(SlaStatus.ON_TIME); //   ON_TIME
        }
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
        if (tracking == null) {
            return "No SLA set";
        }
        
        LocalDateTime now = LocalDateTime.now();
        
        // If already resolved, return completed
        if (tracking.getResolvedAt() != null) {
            return "Completed";
        }
        
        // Check response SLA first
        if (tracking.getFirstResponseAt() == null) {
            long minutesRemaining = Duration.between(now, tracking.getResponseDueAt()).toMinutes();
            if (minutesRemaining < 0) {
                return BREACHED_LITERAL;
            }
            return formatTimeRemaining(minutesRemaining);
        }
        
        // Check resolution SLA
        long minutesRemaining = Duration.between(now, tracking.getResolutionDueAt()).toMinutes();
        if (minutesRemaining < 0) {
            return BREACHED_LITERAL;
        }
        return formatTimeRemaining(minutesRemaining);
    }
    
    /**
     * Format time remaining in human-readable format
     */
    private String formatTimeRemaining(long minutes) {
        if (minutes < 0) {
            return BREACHED_LITERAL;
        } else if (minutes < 60) {
            return minutes + " minutes";
        } else if (minutes < 1440) {
            long hours = minutes / 60;
            long mins = minutes % 60;
            return hours + " hours" + (mins > 0 ? " " + mins + " minutes" : "");
        } else {
            long days = minutes / 1440;
            long hours = (minutes % 1440) / 60;
            String hourPlural = hours > 1 ? "s" : "";
            return days + " day" + (days > 1 ? "s" : "") + 
                   (hours > 0 ? " " + hours + " hour" + hourPlural : "");
        }
    }
}
