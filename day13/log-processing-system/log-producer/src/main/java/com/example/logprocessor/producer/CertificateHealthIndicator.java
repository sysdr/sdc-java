package com.example.logprocessor.producer;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Enumeration;

@Component
public class CertificateHealthIndicator implements HealthIndicator {
    
    private static final long WARNING_DAYS = 7;

    @Override
    public Health health() {
        try {
            // Check keystore certificate expiration
            String keystorePath = System.getenv().getOrDefault(
                "KEYSTORE_PATH", "/etc/ssl/certs/keystore.jks");
            String keystorePassword = System.getenv().getOrDefault(
                "KEYSTORE_PASSWORD", "changeit");

            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(new FileInputStream(keystorePath), 
                keystorePassword.toCharArray());

            Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                X509Certificate cert = (X509Certificate) keyStore.getCertificate(alias);
                
                if (cert != null) {
                    long daysUntilExpiry = ChronoUnit.DAYS.between(
                        Instant.now(), 
                        cert.getNotAfter().toInstant()
                    );

                    if (daysUntilExpiry < WARNING_DAYS) {
                        return Health.down()
                            .withDetail("certificate", alias)
                            .withDetail("daysUntilExpiry", daysUntilExpiry)
                            .withDetail("expiryDate", cert.getNotAfter())
                            .build();
                    }
                }
            }

            return Health.up()
                .withDetail("certificateCheck", "All certificates valid")
                .build();
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
