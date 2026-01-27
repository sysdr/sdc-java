package com.example.alerts.gateway;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertRuleRepository extends JpaRepository<AlertRule, String> {
    List<AlertRule> findByEnabled(Boolean enabled);
}
