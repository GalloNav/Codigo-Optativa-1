package com.vetsync.appointment.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Ubicación de clinic-service (propiedad {@code vetsync.clinic-service.base-url}). */
@ConfigurationProperties(prefix = "vetsync.clinic-service")
public record ClinicServiceProperties(String baseUrl) {
}
