package com.vetsync.notification.infrastructure.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.vetsync.notification.application.fakes.FakeEnviarRecordatoriosUseCase;
import com.vetsync.notification.application.port.out.AccesoADatosException;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/** Especificación del disparador periódico de recordatorios, sin esperar el paso del tiempo real. */
class RecordatorioSchedulerTest {

    private static final Duration INTERVALO = Duration.ofSeconds(60);

    private FakeEnviarRecordatoriosUseCase casoDeUso;
    private RecordatorioScheduler scheduler;

    @BeforeEach
    void prepararEscenario() {
        casoDeUso = new FakeEnviarRecordatoriosUseCase();
        scheduler = new RecordatorioScheduler(casoDeUso, INTERVALO);
    }

    @Test
    void registraUnaTareaConElIntervaloConfigurado() {
        ScheduledTaskRegistrar registrar = new ScheduledTaskRegistrar();

        scheduler.configureTasks(registrar);

        assertThat(registrar.getFixedDelayTaskList()).singleElement().satisfies(tarea -> {
            assertThat(tarea.getIntervalDuration()).isEqualTo(INTERVALO);
            assertThat(tarea.getInitialDelayDuration()).isEqualTo(INTERVALO);
        });
    }

    @Test
    void cadaEjecucionInvocaElCasoDeUsoUnaVez() {
        scheduler.ejecutar();

        assertThat(casoDeUso.invocaciones()).isEqualTo(1);
    }

    @Test
    void unFalloDeAccesoADatosNoDetieneLaTareaPeriodica() {
        casoDeUso.fallarCon(new AccesoADatosException("appointment-service no responde"));

        assertThatCode(scheduler::ejecutar).doesNotThrowAnyException();
        assertThat(casoDeUso.invocaciones()).isEqualTo(1);
    }
}
