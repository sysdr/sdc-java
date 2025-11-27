package com.example.metadata.service;

import com.example.metadata.model.HostMetadataDTO;
import com.example.metadata.model.ServiceMetadataDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@Slf4j
public class MetadataManagementService {
    
    private final JdbcTemplate jdbcTemplate;
    private final RedisTemplate<String, String> redisTemplate;
    
    public MetadataManagementService(JdbcTemplate jdbcTemplate, 
                                   RedisTemplate<String, String> redisTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisTemplate = redisTemplate;
    }
    
    public List<HostMetadataDTO> getAllHosts() {
        return jdbcTemplate.query(
            "SELECT hostname, ip_address, datacenter, environment, cost_center FROM host_metadata",
            (rs, rowNum) -> {
                HostMetadataDTO dto = new HostMetadataDTO();
                dto.setHostname(rs.getString("hostname"));
                dto.setIpAddress(rs.getString("ip_address"));
                dto.setDatacenter(rs.getString("datacenter"));
                dto.setEnvironment(rs.getString("environment"));
                dto.setCostCenter(rs.getString("cost_center"));
                return dto;
            }
        );
    }
    
    public HostMetadataDTO createHost(HostMetadataDTO dto) {
        jdbcTemplate.update(
            "INSERT INTO host_metadata (hostname, ip_address, datacenter, environment, cost_center, created_at, updated_at) " +
            "VALUES (?, ?, ?, ?, ?, NOW(), NOW())",
            dto.getHostname(), dto.getIpAddress(), dto.getDatacenter(), 
            dto.getEnvironment(), dto.getCostCenter()
        );
        
        // Update Redis cache
        redisTemplate.opsForValue().set("hostname:" + dto.getIpAddress(), 
            dto.getHostname(), Duration.ofMinutes(5));
        redisTemplate.opsForValue().set("datacenter:" + dto.getHostname(),
            dto.getDatacenter(), Duration.ofHours(1));
        
        log.info("Created host metadata: {}", dto.getHostname());
        return dto;
    }
    
    public List<ServiceMetadataDTO> getAllServices() {
        return jdbcTemplate.query(
            "SELECT service_name, version, deployment_id, team, cost_center FROM service_metadata",
            (rs, rowNum) -> {
                ServiceMetadataDTO dto = new ServiceMetadataDTO();
                dto.setServiceName(rs.getString("service_name"));
                dto.setVersion(rs.getString("version"));
                dto.setDeploymentId(rs.getString("deployment_id"));
                dto.setTeam(rs.getString("team"));
                dto.setCostCenter(rs.getString("cost_center"));
                return dto;
            }
        );
    }
    
    public ServiceMetadataDTO createService(ServiceMetadataDTO dto) {
        jdbcTemplate.update(
            "INSERT INTO service_metadata (service_name, version, deployment_id, team, cost_center, created_at, updated_at) " +
            "VALUES (?, ?, ?, ?, ?, NOW(), NOW())",
            dto.getServiceName(), dto.getVersion(), dto.getDeploymentId(),
            dto.getTeam(), dto.getCostCenter()
        );
        
        log.info("Created service metadata: {}", dto.getServiceName());
        return dto;
    }
}
