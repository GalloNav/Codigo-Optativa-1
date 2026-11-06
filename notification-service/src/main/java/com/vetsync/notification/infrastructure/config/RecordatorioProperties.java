package com.vetsync.notification.infrastructure.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Parámetros de los recordatorios (propiedades {@code vetsync.recordatorio.*}). La anticipación no puede superar
 * el rango máximo que appointment-service acepta en {@code GET /citas/por-recordar}: el servicio falla al arrancar
 * en lugar de recibir un 400 en cada revisión.
 */
@ConfigurationProperties(prefix = "vetsync.recordatorio")
public record RecordatorioProperties(int anticipacionHoras, Duration intervalo) {

    private static final int ANTICIPACION_MAXIMA_EN_HORAS = 7 * 24;

    public RecordatorioProperties {
        if (anticipacionHoras > ANTICIPACION_MAXIMA_EN_HORAS) {
            throw new IllegalArgumentException("vetsync.recordatorio.anticipacion-horas no puede superar "
                    + ANTICIPACION_MAXIMA_EN_HORAS + " horas (el rango máximo de la agenda)");
        }
    }
}
