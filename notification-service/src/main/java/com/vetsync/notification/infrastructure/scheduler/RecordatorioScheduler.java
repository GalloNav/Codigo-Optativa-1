package com.vetsync.notification.infrastructure.scheduler;

import com.vetsync.notification.application.port.in.EnviarRecordatoriosUseCase;
import com.vetsync.notification.application.port.out.AccesoADatosException;
import com.vetsync.notification.domain.Notificacion;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.IntervalTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * Dispara periódicamente el caso de uso de recordatorios. Spring lo usa como SchedulingConfigurer.
 * La primera revisión ocurre tras un intervalo completo desde el arranque.
 */
public class RecordatorioScheduler implements SchedulingConfigurer {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecordatorioScheduler.class);

    private final EnviarRecordatoriosUseCase enviarRecordatorios;
    private final Duration intervalo;

    public RecordatorioScheduler(EnviarRecordatoriosUseCase enviarRecordatorios, Duration intervalo) {
        this.enviarRecordatorios = enviarRecordatorios;
        this.intervalo = intervalo;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addFixedDelayTask(new IntervalTask(this::ejecutar, intervalo, intervalo));
    }

    /** Una revisión. Si falla el acceso a datos se registra y se reintenta en la siguiente, sin detener la tarea. */
    void ejecutar() {
        try {
            List<Notificacion> enviadas = enviarRecordatorios.enviarRecordatorios();
            if (!enviadas.isEmpty()) {
                LOGGER.info("Recordatorios enviados en esta revision: {}", enviadas.size());
            }
        } catch (AccesoADatosException causa) {
            LOGGER.error("No se pudieron enviar los recordatorios; se reintentara en la proxima revision", causa);
        }
    }
}
