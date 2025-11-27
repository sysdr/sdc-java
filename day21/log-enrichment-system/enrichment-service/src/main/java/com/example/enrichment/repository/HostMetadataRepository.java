package com.example.enrichment.repository;

import com.example.enrichment.entity.HostMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface HostMetadataRepository extends JpaRepository<HostMetadata, Long> {
    Optional<HostMetadata> findByIpAddress(String ipAddress);
    Optional<HostMetadata> findByHostname(String hostname);
}
