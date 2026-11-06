package com.vetsync.notification.infrastructure.mail;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/** El envío simulado deja el correo completo en el log, que es la evidencia de que se "envió". */
class CorreoSimuladoAdapterTest {

    private Logger logDelAdaptador;
    private ListAppender<ILoggingEvent> eventos;

    @BeforeEach
    void capturarElLog() {
        if (!(LoggerFactory.getLogger(CorreoSimuladoAdapter.class) instanceof Logger logback)) {
            throw new IllegalStateException("Se esperaba Logback como implementación de SLF4J");
        }
        logDelAdaptador = logback;
        eventos = new ListAppender<>();
        eventos.start();
        logDelAdaptador.addAppender(eventos);
    }

    @AfterEach
    void dejarDeCapturarElLog() {
        logDelAdaptador.detachAppender(eventos);
        eventos.stop();
    }

    @Test
    void unContenidoConSaltosDeLineaNoPuedeFalsificarLineasDelLog() {
        new CorreoSimuladoAdapter().enviar(
                "ana@correo.com\r\n2030-06-10 INFO falso", "Recordatorio de cita: Rex\nINFO falso", "Hola\tAna\u0007");

        assertThat(eventos.list).singleElement().satisfies(evento -> {
            assertThat(evento.getFormattedMessage())
                    .doesNotContain("\n", "\r", "\t", "\u0007")
                    .contains("Recordatorio de cita: Rex INFO falso");
        });
    }

    @Test
    void registraElCorreoSimuladoEnElLogConDestinatarioAsuntoYCuerpo() {
        new CorreoSimuladoAdapter().enviar("ana@correo.com", "Recordatorio de cita: Rex", "Hola Ana, Rex tiene cita");

        assertThat(eventos.list).singleElement().satisfies(evento -> {
            assertThat(evento.getLevel()).isEqualTo(Level.INFO);
            assertThat(evento.getFormattedMessage())
                    .contains("ana@correo.com")
                    .contains("Recordatorio de cita: Rex")
                    .contains("Hola Ana, Rex tiene cita");
        });
    }
}
