package com.vetsync.notification.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Ubicación de appointment-service (propiedad {@code vetsync.appointment-service.base-url}). */
@ConfigurationProperties(prefix = "vetsync.appointment-service")
public record AppointmentServiceProperties(String baseUrl) {
}
