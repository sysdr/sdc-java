package com.example.enrichment.repository;

import com.example.enrichment.entity.ServiceMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ServiceMetadataRepository extends JpaRepository<ServiceMetadata, Long> {
    Optional<ServiceMetadata> findByServiceName(String serviceName);
}
