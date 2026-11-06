package com.vetsync.notification.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Activa las tareas programadas (el recordatorio periódico vive en infrastructure.scheduler). */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
