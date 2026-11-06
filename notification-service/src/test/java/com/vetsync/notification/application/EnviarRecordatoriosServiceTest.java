package com.vetsync.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsync.notification.application.fakes.FakeAgendaGateway;
import com.vetsync.notification.application.fakes.FakeCorreoSender;
import com.vetsync.notification.application.fakes.FakeCorreoSender.CorreoEnviado;
import com.vetsync.notification.application.fakes.InMemoryNotificacionRepository;
import com.vetsync.notification.application.port.in.EnviarRecordatoriosUseCase;
import com.vetsync.notification.application.port.out.AccesoADatosException;
import com.vetsync.notification.application.service.EnviarRecordatoriosService;
import com.vetsync.notification.domain.CitaPorRecordar;
import com.vetsync.notification.domain.Notificacion;
import com.vetsync.notification.domain.RecordatorioYaEnviadoException;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Especificación de {@code enviarRecordatorios} (requisito 3), con reloj fijo y sin Spring ni base de datos. */
class EnviarRecordatoriosServiceTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2030, 6, 10, 10, 0);
    private static final Duration ANTICIPACION = Duration.ofHours(24);
    private static final CitaPorRecordar CITA_DE_REX = new CitaPorRecordar(
            5L, LocalDateTime.of(2030, 6, 10, 18, 0), "Rex", "Ana Perez", "ana@correo.com", "Control anual");
    private static final CitaPorRecordar CITA_DE_MICHI = new CitaPorRecordar(
            6L, LocalDateTime.of(2030, 6, 11, 9, 30), "Michi", "Luis Mora", "luis@correo.com", "Vacuna anual");
    private static final CitaPorRecordar CITA_DE_LUNA = new CitaPorRecordar(
            7L, LocalDateTime.of(2030, 6, 11, 10, 0), "Luna", "Eva Soto", "eva@correo.com", "Revision dental");

    private FakeAgendaGateway agendaGateway;
    private InMemoryNotificacionRepository notificacionRepository;
    private FakeCorreoSender correoSender;
    private EnviarRecordatoriosUseCase casoDeUso;

    @BeforeEach
    void prepararEscenario() {
        agendaGateway = new FakeAgendaGateway();
        notificacionRepository = new InMemoryNotificacionRepository();
        correoSender = new FakeCorreoSender();
        Clock reloj = Clock.fixed(AHORA.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        casoDeUso = new EnviarRecordatoriosService(
                agendaGateway, notificacionRepository, correoSender, reloj, ANTICIPACION);
    }

    @Test
    void enviaUnRecordatorioPorCadaCitaProximaYLoRegistra() {
        agendaGateway.responderCon(List.of(CITA_DE_REX, CITA_DE_MICHI));

        List<Notificacion> enviadas = casoDeUso.enviarRecordatorios();

        assertThat(enviadas).hasSize(2).allSatisfy(notificacion -> {
            assertThat(notificacion.id()).isNotNull();
            assertThat(notificacion.enviadaEn()).isEqualTo(AHORA);
        });
        assertThat(correoSender.enviados()).extracting(CorreoEnviado::destinatario)
                .containsExactly("ana@correo.com", "luis@correo.com");
        assertThat(notificacionRepository.todas()).containsExactlyElementsOf(enviadas);
    }

    @Test
    void redactaElCorreoConLosDatosDeLaCita() {
        agendaGateway.responderCon(List.of(CITA_DE_REX));

        casoDeUso.enviarRecordatorios();

        CorreoEnviado correo = correoSender.enviados().get(0);
        assertThat(correo.asunto()).isEqualTo("Recordatorio de cita: Rex");
        assertThat(correo.cuerpo()).contains("Ana Perez", "Rex", "10/06/2030 18:00", "Control anual");
        assertThat(notificacionRepository.todas()).singleElement().satisfies(notificacion -> {
            assertThat(notificacion.citaId()).isEqualTo(5L);
            assertThat(notificacion.destinatario()).isEqualTo("ana@correo.com");
            assertThat(notificacion.asunto()).isEqualTo(correo.asunto());
            assertThat(notificacion.cuerpo()).isEqualTo(correo.cuerpo());
        });
    }

    @Test
    void consultaLaAgendaDesdeAhoraHastaAhoraMasLaAnticipacion() {
        casoDeUso.enviarRecordatorios();

        assertThat(agendaGateway.ultimoDesde()).isEqualTo(AHORA);
        assertThat(agendaGateway.ultimoHasta()).isEqualTo(AHORA.plusHours(24));
    }

    @Test
    void noVuelveAEnviarElRecordatorioDeUnaCitaYaNotificada() {
        agendaGateway.responderCon(List.of(CITA_DE_REX));
        casoDeUso.enviarRecordatorios();

        List<Notificacion> segundaEjecucion = casoDeUso.enviarRecordatorios();

        assertThat(segundaEjecucion).isEmpty();
        assertThat(correoSender.enviados()).hasSize(1);
        assertThat(notificacionRepository.todas()).hasSize(1);
    }

    @Test
    void enviaSoloElRecordatorioDeLasCitasQueAunNoLoHanRecibido() {
        agendaGateway.responderCon(List.of(CITA_DE_REX));
        casoDeUso.enviarRecordatorios();
        agendaGateway.responderCon(List.of(CITA_DE_REX, CITA_DE_MICHI));

        List<Notificacion> segundaEjecucion = casoDeUso.enviarRecordatorios();

        assertThat(segundaEjecucion).singleElement().satisfies(notificacion ->
                assertThat(notificacion.citaId()).isEqualTo(6L));
        assertThat(correoSender.enviados()).extracting(CorreoEnviado::destinatario)
                .containsExactly("ana@correo.com", "luis@correo.com");
    }

    @Test
    void averiguaEnUnaSolaConsultaQueCitasYaRecibieronSuRecordatorio() {
        agendaGateway.responderCon(List.of(CITA_DE_REX, CITA_DE_MICHI, CITA_DE_LUNA));

        casoDeUso.enviarRecordatorios();

        assertThat(notificacionRepository.consultasDeCitasNotificadas()).isEqualTo(1);
    }

    @Test
    void noEnviaElCorreoSiNoPuedeRegistrarElRecordatorio() {
        agendaGateway.responderCon(List.of(CITA_DE_REX));
        notificacionRepository.fallarAlGuardarLaCita(5L, new AccesoADatosException("base caida"));

        assertThatThrownBy(() -> casoDeUso.enviarRecordatorios()).isInstanceOf(AccesoADatosException.class);

        assertThat(correoSender.enviados()).isEmpty();
    }

    @Test
    void siOtraInstanciaYaRegistroElRecordatorioDeUnaCitaNoLoEnviaYSigueConLasDemas() {
        agendaGateway.responderCon(List.of(CITA_DE_REX, CITA_DE_MICHI));
        notificacionRepository.fallarAlGuardarLaCita(
                5L, new RecordatorioYaEnviadoException("Ya registrado", new IllegalStateException("clave duplicada")));

        List<Notificacion> enviadas = casoDeUso.enviarRecordatorios();

        assertThat(enviadas).singleElement().satisfies(notificacion ->
                assertThat(notificacion.citaId()).isEqualTo(6L));
        assertThat(correoSender.enviados()).extracting(CorreoEnviado::destinatario)
                .containsExactly("luis@correo.com");
    }

    @Test
    void noEnviaNadaCuandoNoHayCitasProximas() {
        assertThat(casoDeUso.enviarRecordatorios()).isEmpty();
        assertThat(correoSender.enviados()).isEmpty();
        assertThat(notificacionRepository.todas()).isEmpty();
    }
}
