package com.assignment.controller;

import com.assignment.dto.*;
import com.assignment.service.AnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/analytics")
public class AnalyticsController {
    
    @Autowired
    private AnalyticsService analyticsService;
    
    /**
     * Get system overview dashboard
     * GET /admin/analytics/overview
     */
    @GetMapping("/overview")
    public ResponseEntity<SystemOverviewDTO> getSystemOverview() {
        SystemOverviewDTO overview = analyticsService.getSystemOverview();
        return ResponseEntity.ok(overview);
    }
    
    /**
     * Get ticket analytics and trends
     * GET /admin/analytics/tickets?days=7
     */
    @GetMapping("/tickets")
    public ResponseEntity<TicketAnalyticsDTO> getTicketAnalytics(
            @RequestParam(defaultValue = "7") int days) {
        TicketAnalyticsDTO analytics = analyticsService.getTicketAnalytics(days);
        return ResponseEntity.ok(analytics);
    }
    
    /**
     * Get agent performance metrics
     * GET /admin/analytics/agents
     */
    @GetMapping("/agents")
    public ResponseEntity<AgentPerformanceReportDTO> getAgentPerformance() {
        AgentPerformanceReportDTO report = analyticsService.getAgentPerformance();
        return ResponseEntity.ok(report);
    }
    
    /**
     * Get SLA compliance report
     * GET /admin/analytics/sla
     */
    @GetMapping("/sla")
    public ResponseEntity<SlaComplianceReportDTO> getSlaReport() {
        SlaComplianceReportDTO report = analyticsService.getSlaReport();
        return ResponseEntity.ok(report);
    }
    
    /**
     * Get tickets by category breakdown
     * GET /admin/analytics/categories
     */
    @GetMapping("/categories")
    public ResponseEntity<CategoryBreakdownDTO> getCategoryBreakdown() {
        CategoryBreakdownDTO breakdown = analyticsService.getCategoryBreakdown();
        return ResponseEntity.ok(breakdown);
    }
    
    /**
     * Get time-based trends
     * GET /admin/analytics/trends?period=daily&days=30
     */
    @GetMapping("/trends")
    public ResponseEntity<TrendsReportDTO> getTrends(
            @RequestParam(defaultValue = "daily") String period,
            @RequestParam(defaultValue = "30") int days) {
        TrendsReportDTO trends = analyticsService.getTrends(period, days);
        return ResponseEntity.ok(trends);
    }
}
