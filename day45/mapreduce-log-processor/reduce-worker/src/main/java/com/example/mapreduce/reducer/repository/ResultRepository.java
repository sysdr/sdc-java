package com.example.mapreduce.reducer.repository;

import com.example.mapreduce.reducer.entity.ResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResultRepository extends JpaRepository<ResultEntity, Long> {
    List<ResultEntity> findByJobId(String jobId);
}
