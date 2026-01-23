package com.example.mapreduce.coordinator.repository;

import com.example.mapreduce.coordinator.entity.JobEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobRepository extends JpaRepository<JobEntity, String> {
}
