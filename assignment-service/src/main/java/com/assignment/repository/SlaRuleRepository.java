package com.assignment.repository;

import com.assignment.entity.SlaRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SlaRuleRepository extends JpaRepository<SlaRule, String> {
    
    // Find rule by priority and category
    Optional<SlaRule> findByPriorityAndCategory(String priority, String category);
    
    // Find rule by priority only (for default rules)
    Optional<SlaRule> findByPriorityAndCategoryIsNull(String priority);
}
