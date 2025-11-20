package com.example.schemaregistry.repository;

import com.example.schemaregistry.entity.SchemaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SchemaRepository extends JpaRepository<SchemaEntity, Long> {
    
    Optional<SchemaEntity> findBySubjectAndVersion(String subject, Integer version);
    
    Optional<SchemaEntity> findByFingerprint(String fingerprint);
    
    @Query("SELECT s FROM SchemaEntity s WHERE s.subject = :subject ORDER BY s.version DESC")
    List<SchemaEntity> findAllBySubjectOrderByVersionDesc(@Param("subject") String subject);
    
    @Query("SELECT COALESCE(MAX(s.version), 0) FROM SchemaEntity s WHERE s.subject = :subject")
    Integer findMaxVersionBySubject(@Param("subject") String subject);
    
    @Query("SELECT s FROM SchemaEntity s WHERE s.subject = :subject AND s.version = " +
           "(SELECT MAX(s2.version) FROM SchemaEntity s2 WHERE s2.subject = :subject)")
    Optional<SchemaEntity> findLatestBySubject(@Param("subject") String subject);
    
    @Query("SELECT DISTINCT s.subject FROM SchemaEntity s ORDER BY s.subject")
    List<String> findAllSubjects();
    
    @Query("SELECT s.version FROM SchemaEntity s WHERE s.subject = :subject ORDER BY s.version")
    List<Integer> findVersionsBySubject(@Param("subject") String subject);
    
    boolean existsBySubjectAndFingerprint(String subject, String fingerprint);
    
    void deleteBySubject(String subject);
}
