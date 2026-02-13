package com.example.auth;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public DataInitializer(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) {
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@example.com");
            admin.setPasswordHash(passwordEncoder.encode("admin123"));
            admin.setRoles(Set.of("ADMIN", "SRE"));
            admin.setTeams(Set.of("platform", "security"));
            userRepository.save(admin);
        }

        if (!userRepository.existsByUsername("developer")) {
            User dev = new User();
            dev.setUsername("developer");
            dev.setEmail("dev@example.com");
            dev.setPasswordHash(passwordEncoder.encode("dev123"));
            dev.setRoles(Set.of("DEVELOPER"));
            dev.setTeams(Set.of("payments", "fraud"));
            userRepository.save(dev);
        }

        if (!userRepository.existsByUsername("analyst")) {
            User analyst = new User();
            analyst.setUsername("analyst");
            analyst.setEmail("analyst@example.com");
            analyst.setPasswordHash(passwordEncoder.encode("analyst123"));
            analyst.setRoles(Set.of("ANALYST"));
            analyst.setTeams(Set.of("analytics"));
            userRepository.save(analyst);
        }
    }
}
