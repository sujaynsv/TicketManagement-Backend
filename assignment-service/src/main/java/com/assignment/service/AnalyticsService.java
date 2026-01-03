package com.assignment.service;

import com.assignment.dto.*;
import com.assignment.entity.*;
import com.assignment.repository.*;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {
    

    private static final String STATUS_ASSIGNED = "ASSIGNED";
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_RESOLVED = "RESOLVED";
    
    private static final String PRIORITY_CRITICAL = "CRITICAL";
    private static final String PRIORITY_MEDIUM = "MEDIUM";
    
    private TicketCacheRepository ticketCacheRepository;
    
    private AssignmentRepository assignmentRepository;
    
    private AgentWorkloadRepository agentWorkloadRepository;
    
    private SlaTrackingRepository slaTrackingRepository;

    public AnalyticsService(
        SlaTrackingRepository slaTrackingRepository,
        AgentWorkloadRepository agentWorkloadRepository,
        AssignmentRepository assignmentRepository,
        TicketCacheRepository ticketCacheRepository
    )
    {
        this.slaTrackingRepository=slaTrackingRepository;
        this.agentWorkloadRepository=agentWorkloadRepository;
        this.assignmentRepository=assignmentRepository;
        this.ticketCacheRepository=ticketCacheRepository;
    }
    
    /**
     * Get system overview dashboard
     */
    public SystemOverviewDTO getSystemOverview() {
        // Ticket metrics
        long totalTickets = ticketCacheRepository.count();
        long activeTickets = ticketCacheRepository.countByStatus(STATUS_ASSIGNED) +
                           ticketCacheRepository.countByStatus(STATUS_IN_PROGRESS) +
                           ticketCacheRepository.countByStatus("OPEN");
        
        // Tickets resolved today
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        long resolvedToday = ticketCacheRepository.countByStatusAndUpdatedAtAfter(STATUS_RESOLVED, startOfDay);
        
        // Average resolution time (from SLA tracking)
        Double avgResolutionHours = calculateAverageResolutionTime();
        
        // SLA compliance
        double slaCompliance = calculateSlaCompliance();
        
        // Agent metrics
        long totalAgents = agentWorkloadRepository.count();
        long activeAgents = agentWorkloadRepository.countByStatus(AgentStatus.AVAILABLE) +
                          agentWorkloadRepository.countByStatus(AgentStatus.BUSY);
        long busyAgents = agentWorkloadRepository.countByStatus(AgentStatus.BUSY);
        
        // Critical tickets
        long criticalTicketsOpen = ticketCacheRepository.countByPriorityAndStatusNot(PRIORITY_CRITICAL, STATUS_RESOLVED);
        
        // Top categories
        List<CategoryCount> topCategories = getTopCategories(5);
        
        // Recent activity (assignments in last 24 hours)
        LocalDateTime last24Hours = LocalDateTime.now().minusHours(24);
        long recentAssignments = assignmentRepository.countByAssignedAtAfter(last24Hours);
        
        return new SystemOverviewDTO(
                totalTickets,
                activeTickets,
                resolvedToday,
                avgResolutionHours != null ? String.format("%.1f hours", avgResolutionHours) : "N/A",
                slaCompliance,
                totalAgents,
                activeAgents,
                busyAgents,
                topCategories,
                criticalTicketsOpen,
                recentAssignments
        );
    }
    
    /**
     * Get ticket analytics and trends
     */
    public TicketAnalyticsDTO getTicketAnalytics(int days) {
        
        // Daily ticket counts
        Map<LocalDate, Long> createdByDay = new LinkedHashMap<>();
        Map<LocalDate, Long> resolvedByDay = new LinkedHashMap<>();
        
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();
            
            long created = ticketCacheRepository.countByCreatedAtBetween(dayStart, dayEnd);
            long resolved = ticketCacheRepository.countByStatusAndUpdatedAtBetween(STATUS_RESOLVED, dayStart, dayEnd);
            
            createdByDay.put(date, created);
            resolvedByDay.put(date, resolved);
        }
        
        // By priority
        Map<String, Long> byPriority = new HashMap<>();
        byPriority.put(PRIORITY_CRITICAL, ticketCacheRepository.countByPriority(PRIORITY_CRITICAL));
        byPriority.put("HIGH", ticketCacheRepository.countByPriority("HIGH"));
        byPriority.put(PRIORITY_MEDIUM, ticketCacheRepository.countByPriority(PRIORITY_MEDIUM));
        byPriority.put("LOW", ticketCacheRepository.countByPriority("LOW"));
        byPriority.put("UNASSIGNED", ticketCacheRepository.countByPriorityIsNull());
        
        // By status
        Map<String, Long> byStatus = new HashMap<>();
        byStatus.put("OPEN", ticketCacheRepository.countByStatus("OPEN"));
        byStatus.put(STATUS_ASSIGNED, ticketCacheRepository.countByStatus(STATUS_ASSIGNED));
        byStatus.put(STATUS_IN_PROGRESS, ticketCacheRepository.countByStatus(STATUS_IN_PROGRESS));
        byStatus.put(STATUS_RESOLVED, ticketCacheRepository.countByStatus(STATUS_RESOLVED));
        byStatus.put("CLOSED", ticketCacheRepository.countByStatus("CLOSED"));
        byStatus.put("ESCALATED", ticketCacheRepository.countByStatus("ESCALATED"));
        
        // By category
        Map<String, Long> byCategory = getAllCategoryCounts();
        
        return new TicketAnalyticsDTO(
                createdByDay,
                resolvedByDay,
                byPriority,
                byStatus,
                byCategory
        );
    }
    
    /**
     * Get agent performance metrics
     */
    public AgentPerformanceReportDTO getAgentPerformance() {
        List<AgentWorkload> agents = agentWorkloadRepository.findAll();
        
        List<AgentPerformanceDTO> performances = agents.stream()
                .map(agent -> {
                    // Get agent's assignments
                    long totalAssignments = assignmentRepository.countByAgentId(agent.getAgentId());
                    long activeAssignments = assignmentRepository.countByAgentIdAndStatus(
                            agent.getAgentId(), AssignmentStatus.ASSIGNED);
                    long completedAssignments = assignmentRepository.countByAgentIdAndStatusNot(
                            agent.getAgentId(), AssignmentStatus.ASSIGNED);
                    
                    // Calculate avg resolution time for this agent
                    Double avgResolutionTime = calculateAgentAvgResolutionTime(agent.getAgentId());
                    
                    // SLA compliance for agent
                    double slaCompliance = calculateAgentSlaCompliance(agent.getAgentId());
                    
                    return new AgentPerformanceDTO(
                            agent.getAgentId(),
                            agent.getAgentUsername(),
                            agent.getActiveTickets(),
                            agent.getCompletedTickets(),
                            totalAssignments,
                            activeAssignments,
                            completedAssignments,
                            avgResolutionTime != null ? String.format("%.1f hours", avgResolutionTime) : "N/A",
                            slaCompliance,
                            agent.getStatus().name(),
                            agent.getLastAssignedAt()
                    );
                })
                .sorted(Comparator.comparing(AgentPerformanceDTO::completedTickets).reversed())
                .toList();
        
        // Calculate team averages
        double avgActiveTickets = performances.stream()
                .mapToInt(AgentPerformanceDTO::activeTickets)
                .average()
                .orElse(0.0);
        
        double avgCompletedTickets = performances.stream()
                .mapToInt(AgentPerformanceDTO::completedTickets)
                .average()
                .orElse(0.0);
        
        double avgSlaCompliance = performances.stream()
                .mapToDouble(AgentPerformanceDTO::slaComplianceRate)
                .average()
                .orElse(0.0);
        
        return new AgentPerformanceReportDTO(
                performances,
                avgActiveTickets,
                avgCompletedTickets,
                avgSlaCompliance
        );
    }
    
    /**
     * Get SLA compliance report
     */
    public SlaComplianceReportDTO getSlaReport() {
        List<SlaTracking> allSla = slaTrackingRepository.findAll();
        
        long totalTracked = allSla.size();
        long onTime = allSla.stream()
                .filter(sla -> sla.getSlaStatus() == SlaStatus.ON_TIME || 
                             sla.getSlaStatus() == SlaStatus.MET)
                .count();
        long breached = allSla.stream()
                .filter(sla -> sla.getSlaStatus() == SlaStatus.BREACHED)
                .count();
        long warning = allSla.stream()
                .filter(sla -> sla.getSlaStatus() == SlaStatus.WARNING)
                .count();
        
        double complianceRate = totalTracked > 0 ? (onTime * 100.0 / totalTracked) : 0.0;
        
        // By priority
        Map<String, SlaByPriorityDTO> byPriority = new HashMap<>();
        for (String priority : Arrays.asList(PRIORITY_CRITICAL, "HIGH", PRIORITY_MEDIUM, "LOW")) {
            List<SlaTracking> prioritySla = allSla.stream()
                    .filter(sla -> priority.equals(sla.getPriority()))
                    .toList();
            
            long tracked = prioritySla.size();
            long priorityBreached = prioritySla.stream()
                    .filter(sla -> sla.getSlaStatus() == SlaStatus.BREACHED)
                    .count();
            long responseBreached = prioritySla.stream()
                    .filter(SlaTracking::getResponseBreached)
                    .count();
            long resolutionBreached = prioritySla.stream()
                    .filter(SlaTracking::getResolutionBreached)
                    .count();
            
            double priorityCompliance = tracked > 0 ? 
                    ((tracked - priorityBreached) * 100.0 / tracked) : 0.0;
            
            byPriority.put(priority, new SlaByPriorityDTO(
                    tracked,
                    priorityBreached,
                    responseBreached,
                    resolutionBreached,
                    priorityCompliance
            ));
        }
        
        return new SlaComplianceReportDTO(
                totalTracked,
                onTime,
                breached,
                warning,
                complianceRate,
                byPriority
        );
    }
    
    /**
     * Get category breakdown
     */
    public CategoryBreakdownDTO getCategoryBreakdown() {
        Map<String, Long> categoryCounts = getAllCategoryCounts();
        
        long total = categoryCounts.values().stream().mapToLong(Long::longValue).sum();
        
        Map<String, Double> categoryPercentages = categoryCounts.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> total > 0 ? (e.getValue() * 100.0 / total) : 0.0
                ));
        
        return new CategoryBreakdownDTO(
                categoryCounts,
                categoryPercentages,
                total
        );
    }
    
    /**
     * Get time-based trends
     */
    public TrendsReportDTO getTrends(String period, int days) {
        Map<String, Long> ticketsByPeriod = new LinkedHashMap<>();
        Map<String, Long> assignmentsByPeriod = new LinkedHashMap<>();
        Map<String, Long> resolutionsByPeriod = new LinkedHashMap<>();
        
        if ("daily".equalsIgnoreCase(period)) {
            for (int i = days - 1; i >= 0; i--) {
                LocalDate date = LocalDate.now().minusDays(i);
                String key = date.toString();
                
                LocalDateTime dayStart = date.atStartOfDay();
                LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();
                
                long tickets = ticketCacheRepository.countByCreatedAtBetween(dayStart, dayEnd);
                long assignments = assignmentRepository.countByAssignedAtBetween(dayStart, dayEnd);
                long resolutions = ticketCacheRepository.countByStatusAndUpdatedAtBetween(
                        STATUS_RESOLVED, dayStart, dayEnd);
                
                ticketsByPeriod.put(key, tickets);
                assignmentsByPeriod.put(key, assignments);
                resolutionsByPeriod.put(key, resolutions);
            }
        } else if ("weekly".equalsIgnoreCase(period)) {
            int weeks = days / 7;
            for (int i = weeks - 1; i >= 0; i--) {
                LocalDate weekStart = LocalDate.now().minusWeeks((long) i + 1);
                LocalDate weekEnd = LocalDate.now().minusWeeks(i);
                String key = "Week " + weekStart.toString();
                
                LocalDateTime start = weekStart.atStartOfDay();
                LocalDateTime end = weekEnd.atStartOfDay();
                
                long tickets = ticketCacheRepository.countByCreatedAtBetween(start, end);
                long assignments = assignmentRepository.countByAssignedAtBetween(start, end);
                long resolutions = ticketCacheRepository.countByStatusAndUpdatedAtBetween(
                        STATUS_RESOLVED, start, end);
                
                ticketsByPeriod.put(key, tickets);
                assignmentsByPeriod.put(key, assignments);
                resolutionsByPeriod.put(key, resolutions);
            }
        }
        
        return new TrendsReportDTO(
                period,
                days,
                ticketsByPeriod,
                assignmentsByPeriod,
                resolutionsByPeriod
        );
    }
    
    // Helper methods
    
    private Double calculateAverageResolutionTime() {
        List<SlaTracking> resolved = slaTrackingRepository.findByResolvedAtIsNotNull();
        
        if (resolved.isEmpty()) {
            return null;
        }
        
        return resolved.stream()
                .filter(sla -> sla.getResolutionTimeHours() != null)
                .mapToDouble(sla->sla.getResolutionTimeHours().doubleValue())
                .average()
                .orElse(0.0);
    }
    
    private double calculateSlaCompliance() {
        long total = slaTrackingRepository.count();
        if (total == 0) return 0.0;
        
        long breached = slaTrackingRepository.countBySlaStatus(SlaStatus.BREACHED);
        return ((total - breached) * 100.0 / total);
    }
    
    private Double calculateAgentAvgResolutionTime(String agentId) {
        List<Assignment> completed = assignmentRepository.findByAgentIdAndCompletedAtIsNotNull(agentId);
        
        if (completed.isEmpty()) {
            return null;
        }
        
        return completed.stream()
                .filter(a -> a.getCompletedAt() != null && a.getAssignedAt() != null)
                .mapToDouble(a -> ChronoUnit.HOURS.between(a.getAssignedAt(), a.getCompletedAt()))
                .average()
                .orElse(0.0);
    }
    
    private double calculateAgentSlaCompliance(String agentId) {
        List<Assignment> assignments = assignmentRepository.findByAgentId(agentId);
        
        if (assignments.isEmpty()) {
            return 0.0;
        }
        
        long totalWithSla = 0;
        long breached = 0;
        
        for (Assignment assignment : assignments) {
            Optional<SlaTracking> slaOpt = slaTrackingRepository.findByTicketId(assignment.getTicketId());
            if (slaOpt.isPresent()) {
                totalWithSla++;
                if (slaOpt.get().getSlaStatus() == SlaStatus.BREACHED) {
                    breached++;
                }
            }
        }
        
        return totalWithSla > 0 ? ((totalWithSla - breached) * 100.0 / totalWithSla) : 0.0;
    }
    
    private List<CategoryCount> getTopCategories(int limit) {
        Map<String, Long> categoryCounts = getAllCategoryCounts();
        
        return categoryCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .map(e -> new CategoryCount(e.getKey(), e.getValue()))
                .toList();
    }
    
    private Map<String, Long> getAllCategoryCounts() {
        List<TicketCache> allTickets = ticketCacheRepository.findAll();
        
        return allTickets.stream()
                .collect(Collectors.groupingBy(
                        ticket -> ticket.getCategory() != null ? ticket.getCategory() : "UNKNOWN",
                        Collectors.counting()
                ));
    }
}
