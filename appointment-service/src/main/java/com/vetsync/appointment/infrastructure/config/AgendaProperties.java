package com.vetsync.appointment.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Horario de atención de la clínica (propiedades {@code vetsync.agenda.*}), en horas enteras de 0 a 24. */
@ConfigurationProperties(prefix = "vetsync.agenda")
public record AgendaProperties(int horaApertura, int horaCierre) {
}
