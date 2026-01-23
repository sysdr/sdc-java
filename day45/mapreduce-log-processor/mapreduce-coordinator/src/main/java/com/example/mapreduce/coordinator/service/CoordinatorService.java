package com.example.mapreduce.coordinator.service;

import com.example.mapreduce.common.model.*;
import com.example.mapreduce.common.util.HashPartitioner;
import com.example.mapreduce.coordinator.entity.JobEntity;
import com.example.mapreduce.coordinator.entity.TaskEntity;
import com.example.mapreduce.coordinator.repository.JobRepository;
import com.example.mapreduce.coordinator.repository.TaskRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

@Service
@Slf4j
public class CoordinatorService {
    
    private final JobRepository jobRepository;
    private final TaskRepository taskRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    
    // Metrics
    private final Counter jobsTotal;
    private final Counter jobsCompleted;
    private final Counter jobsFailed;
    private final Counter mapTasksTotal;
    private final Counter mapTasksCompleted;
    private final Counter mapTasksFailed;
    private final Counter reduceTasksTotal;
    private final Counter reduceTasksCompleted;
    private final Counter reduceTasksFailed;
    private final Timer jobDurationTimer;
    private final AtomicLong jobsRunning = new AtomicLong(0);
    private final AtomicLong mapTasksRunning = new AtomicLong(0);
    private final AtomicLong reduceTasksRunning = new AtomicLong(0);
    
    public CoordinatorService(JobRepository jobRepository, TaskRepository taskRepository,
                             KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper,
                             MeterRegistry meterRegistry) {
        this.jobRepository = jobRepository;
        this.taskRepository = taskRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
        
        // Initialize counters
        this.jobsTotal = Counter.builder("mapreduce_jobs_total")
            .description("Total number of jobs submitted")
            .register(meterRegistry);
        this.jobsCompleted = Counter.builder("mapreduce_jobs_completed_total")
            .description("Total number of jobs completed")
            .register(meterRegistry);
        this.jobsFailed = Counter.builder("mapreduce_jobs_failed_total")
            .description("Total number of jobs failed")
            .register(meterRegistry);
        this.mapTasksTotal = Counter.builder("mapreduce_map_tasks_total")
            .description("Total number of map tasks")
            .register(meterRegistry);
        this.mapTasksCompleted = Counter.builder("mapreduce_map_tasks_completed_total")
            .description("Total number of map tasks completed")
            .register(meterRegistry);
        this.mapTasksFailed = Counter.builder("mapreduce_map_tasks_failed_total")
            .description("Total number of map tasks failed")
            .register(meterRegistry);
        this.reduceTasksTotal = Counter.builder("mapreduce_reduce_tasks_total")
            .description("Total number of reduce tasks")
            .register(meterRegistry);
        this.reduceTasksCompleted = Counter.builder("mapreduce_reduce_tasks_completed_total")
            .description("Total number of reduce tasks completed")
            .register(meterRegistry);
        this.reduceTasksFailed = Counter.builder("mapreduce_reduce_tasks_failed_total")
            .description("Total number of reduce tasks failed")
            .register(meterRegistry);
        this.jobDurationTimer = Timer.builder("mapreduce_job_duration_seconds")
            .description("Job duration in seconds")
            .register(meterRegistry);
        
        // Register gauges
        Gauge.builder("mapreduce_jobs_running", jobsRunning, AtomicLong::get)
            .description("Number of jobs currently running")
            .register(meterRegistry);
        Gauge.builder("mapreduce_map_tasks_running", mapTasksRunning, AtomicLong::get)
            .description("Number of map tasks currently running")
            .register(meterRegistry);
        Gauge.builder("mapreduce_reduce_tasks_running", reduceTasksRunning, AtomicLong::get)
            .description("Number of reduce tasks currently running")
            .register(meterRegistry);
    }
    
    @Transactional
    public Job submitJob(String jobName, String inputTopic, int numMappers, 
                         int numReducers, String mapFunction, String reduceFunction) {
        String jobId = UUID.randomUUID().toString();
        
        JobEntity jobEntity = new JobEntity();
        jobEntity.setJobId(jobId);
        jobEntity.setJobName(jobName);
        jobEntity.setStatus(JobStatus.PENDING);
        jobEntity.setInputTopic(inputTopic);
        jobEntity.setNumMappers(numMappers);
        jobEntity.setNumReducers(numReducers);
        jobEntity.setMapFunction(mapFunction);
        jobEntity.setReduceFunction(reduceFunction);
        jobEntity.setCreatedAt(Instant.now());
        jobEntity.setTotalMapTasks(numMappers);
        jobEntity.setTotalReduceTasks(numReducers);
        jobEntity.setCompletedMapTasks(0);
        jobEntity.setCompletedReduceTasks(0);
        
        jobRepository.save(jobEntity);
        
        // Create map tasks
        createMapTasks(jobId, numMappers, numReducers, mapFunction);
        
        // Update metrics
        jobsTotal.increment();
        jobsRunning.incrementAndGet();
        mapTasksTotal.increment(numMappers);
        
        log.info("Created job {} with {} map tasks", jobId, numMappers);
        
        return toJob(jobEntity);
    }
    
    private void createMapTasks(String jobId, int numMappers, int numReducers, String mapFunction) {
        IntStream.range(0, numMappers).forEach(partition -> {
            TaskEntity task = new TaskEntity();
            task.setTaskId(UUID.randomUUID().toString());
            task.setJobId(jobId);
            task.setTaskType("MAP");
            task.setPartition(partition);
            task.setStatus(TaskStatus.PENDING);
            task.setRetryCount(0);
            task.setCreatedAt(Instant.now());
            taskRepository.save(task);
        });
    }
    
    @Scheduled(fixedDelay = 2000)
    public void schedulePendingTasks() {
        List<TaskEntity> pendingTasks = taskRepository.findByStatus(TaskStatus.PENDING);
        
        pendingTasks.forEach(task -> {
            try {
                if ("MAP".equals(task.getTaskType())) {
                    scheduleMapTask(task);
                } else {
                    scheduleReduceTask(task);
                }
            } catch (Exception e) {
                log.error("Failed to schedule task {}", task.getTaskId(), e);
            }
        });
    }
    
    private void scheduleMapTask(TaskEntity taskEntity) throws Exception {
        JobEntity job = jobRepository.findById(taskEntity.getJobId()).orElseThrow();
        
        MapTask mapTask = new MapTask();
        mapTask.setTaskId(taskEntity.getTaskId());
        mapTask.setJobId(taskEntity.getJobId());
        mapTask.setPartition(taskEntity.getPartition());
        mapTask.setStartOffset(0);
        mapTask.setEndOffset(-1); // Read all available
        mapTask.setNumReducers(job.getNumReducers());
        mapTask.setMapFunction(job.getMapFunction());
        mapTask.setStatus(TaskStatus.PENDING);
        mapTask.setRetryCount(taskEntity.getRetryCount());
        
        String taskJson = objectMapper.writeValueAsString(mapTask);
        kafkaTemplate.send("map-tasks", mapTask.getTaskId(), taskJson);
        
        taskEntity.setStatus(TaskStatus.RUNNING);
        taskEntity.setStartedAt(Instant.now());
        taskRepository.save(taskEntity);
        mapTasksRunning.incrementAndGet();
        
        log.info("Scheduled map task {} for partition {}", mapTask.getTaskId(), mapTask.getPartition());
    }
    
    private void scheduleReduceTask(TaskEntity taskEntity) throws Exception {
        JobEntity job = jobRepository.findById(taskEntity.getJobId()).orElseThrow();
        
        ReduceTask reduceTask = new ReduceTask();
        reduceTask.setTaskId(taskEntity.getTaskId());
        reduceTask.setJobId(taskEntity.getJobId());
        reduceTask.setPartition(taskEntity.getPartition());
        reduceTask.setReduceFunction(job.getReduceFunction());
        reduceTask.setStatus(TaskStatus.PENDING);
        reduceTask.setRetryCount(taskEntity.getRetryCount());
        
        String taskJson = objectMapper.writeValueAsString(reduceTask);
        kafkaTemplate.send("reduce-tasks", reduceTask.getTaskId(), taskJson);
        
        taskEntity.setStatus(TaskStatus.RUNNING);
        taskEntity.setStartedAt(Instant.now());
        taskRepository.save(taskEntity);
        reduceTasksRunning.incrementAndGet();
        
        log.info("Scheduled reduce task {} for partition {}", reduceTask.getTaskId(), reduceTask.getPartition());
    }
    
    @Transactional
    public void completeTask(String taskId) {
        TaskEntity task = taskRepository.findById(taskId).orElse(null);
        if (task == null) return;
        
        task.setStatus(TaskStatus.COMPLETED);
        task.setCompletedAt(Instant.now());
        taskRepository.save(task);
        
        JobEntity job = jobRepository.findById(task.getJobId()).orElseThrow();
        
        if ("MAP".equals(task.getTaskType())) {
            job.setCompletedMapTasks(job.getCompletedMapTasks() + 1);
            mapTasksCompleted.increment();
            mapTasksRunning.decrementAndGet();
            
            // Check if all map tasks completed
            if (job.getCompletedMapTasks() == job.getTotalMapTasks()) {
                job.setStatus(JobStatus.MAP_COMPLETED);
                createReduceTasks(job.getJobId(), job.getNumReducers(), job.getReduceFunction());
                reduceTasksTotal.increment(job.getNumReducers());
                log.info("All map tasks completed for job {}, starting reduce phase", job.getJobId());
            }
        } else {
            job.setCompletedReduceTasks(job.getCompletedReduceTasks() + 1);
            reduceTasksCompleted.increment();
            reduceTasksRunning.decrementAndGet();
            
            // Check if all reduce tasks completed
            if (job.getCompletedReduceTasks() == job.getTotalReduceTasks()) {
                job.setStatus(JobStatus.COMPLETED);
                job.setCompletedAt(Instant.now());
                jobsCompleted.increment();
                jobsRunning.decrementAndGet();
                
                // Record job duration
                if (job.getCreatedAt() != null) {
                    long durationSeconds = java.time.Duration.between(job.getCreatedAt(), job.getCompletedAt()).getSeconds();
                    jobDurationTimer.record(durationSeconds, java.util.concurrent.TimeUnit.SECONDS);
                }
                
                log.info("Job {} completed successfully", job.getJobId());
            }
        }
        
        jobRepository.save(job);
    }
    
    private void createReduceTasks(String jobId, int numReducers, String reduceFunction) {
        IntStream.range(0, numReducers).forEach(partition -> {
            TaskEntity task = new TaskEntity();
            task.setTaskId(UUID.randomUUID().toString());
            task.setJobId(jobId);
            task.setTaskType("REDUCE");
            task.setPartition(partition);
            task.setStatus(TaskStatus.PENDING);
            task.setRetryCount(0);
            task.setCreatedAt(Instant.now());
            taskRepository.save(task);
        });
    }
    
    @Transactional
    public void failTask(String taskId, String errorMessage) {
        TaskEntity task = taskRepository.findById(taskId).orElse(null);
        if (task == null) return;
        
        task.setRetryCount(task.getRetryCount() + 1);
        task.setErrorMessage(errorMessage);
        
        if (task.getRetryCount() >= 3) {
            task.setStatus(TaskStatus.FAILED);
            log.error("Task {} failed after 3 retries", taskId);
            
            // Update metrics
            if ("MAP".equals(task.getTaskType())) {
                mapTasksFailed.increment();
                mapTasksRunning.decrementAndGet();
            } else {
                reduceTasksFailed.increment();
                reduceTasksRunning.decrementAndGet();
            }
            
            // Mark job as failed
            JobEntity job = jobRepository.findById(task.getJobId()).orElseThrow();
            job.setStatus(JobStatus.FAILED);
            jobsFailed.increment();
            jobsRunning.decrementAndGet();
            jobRepository.save(job);
        } else {
            task.setStatus(TaskStatus.PENDING);
            log.warn("Task {} failed, retry count: {}", taskId, task.getRetryCount());
        }
        
        taskRepository.save(task);
    }
    
    public Job getJob(String jobId) {
        JobEntity entity = jobRepository.findById(jobId).orElse(null);
        return entity != null ? toJob(entity) : null;
    }
    
    private Job toJob(JobEntity entity) {
        Job job = new Job();
        job.setJobId(entity.getJobId());
        job.setJobName(entity.getJobName());
        job.setStatus(entity.getStatus());
        job.setInputTopic(entity.getInputTopic());
        job.setNumMappers(entity.getNumMappers());
        job.setNumReducers(entity.getNumReducers());
        job.setMapFunction(entity.getMapFunction());
        job.setReduceFunction(entity.getReduceFunction());
        job.setCreatedAt(entity.getCreatedAt());
        job.setCompletedAt(entity.getCompletedAt());
        job.setTotalMapTasks(entity.getTotalMapTasks());
        job.setCompletedMapTasks(entity.getCompletedMapTasks());
        job.setTotalReduceTasks(entity.getTotalReduceTasks());
        job.setCompletedReduceTasks(entity.getCompletedReduceTasks());
        return job;
    }
}
