package com.example.mapreduce.coordinator.repository;

import com.example.mapreduce.common.model.TaskStatus;
import com.example.mapreduce.coordinator.entity.TaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<TaskEntity, String> {
    List<TaskEntity> findByJobIdAndTaskType(String jobId, String taskType);
    List<TaskEntity> findByJobIdAndStatus(String jobId, TaskStatus status);
    List<TaskEntity> findByStatus(TaskStatus status);
    long countByJobIdAndTaskTypeAndStatus(String jobId, String taskType, TaskStatus status);
}
