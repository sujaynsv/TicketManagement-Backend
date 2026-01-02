package com.assignment.repository;

import com.assignment.entity.SlaStatus;
import com.assignment.entity.SlaTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SlaTrackingRepository extends JpaRepository<SlaTracking, String> {
    
    // Find by ticket ID
    Optional<SlaTracking> findByTicketId(String ticketId);
    
    // Find active SLA trackings (not resolved)
    List<SlaTracking> findByResolvedAtIsNull();
    
    // Find SLA trackings that are about to breach (for warnings)
    @Query("SELECT s FROM SlaTracking s WHERE s.resolvedAt IS NULL AND s.responseDueAt < :time AND s.firstResponseAt IS NULL")
    List<SlaTracking> findResponseSlaAtRisk(LocalDateTime time);
    
    @Query("SELECT s FROM SlaTracking s WHERE s.resolvedAt IS NULL AND s.resolutionDueAt < :time")
    List<SlaTracking> findResolutionSlaAtRisk(LocalDateTime time);
    
    // Find breached SLAs
    List<SlaTracking> findBySlaStatus(SlaStatus status);

    List<SlaTracking> findByResponseBreachedTrue();

    List<SlaTracking> findByResolutionBreachedTrue();

    // Add these methods
    List<SlaTracking> findByResolvedAtIsNotNull();
    long countBySlaStatus(SlaStatus status);

}
