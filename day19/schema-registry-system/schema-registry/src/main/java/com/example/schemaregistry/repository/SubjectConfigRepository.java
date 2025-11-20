package com.example.schemaregistry.repository;

import com.example.schemaregistry.entity.SubjectConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubjectConfigRepository extends JpaRepository<SubjectConfig, String> {
}
