package com.vetsync.notification.application.service;

import com.vetsync.notification.application.port.in.EnviarRecordatoriosUseCase;
import com.vetsync.notification.application.port.out.AgendaGateway;
import com.vetsync.notification.application.port.out.CorreoSender;
import com.vetsync.notification.application.port.out.NotificacionRepository;
import com.vetsync.notification.domain.CitaPorRecordar;
import com.vetsync.notification.domain.Notificacion;
import com.vetsync.notification.domain.RecordatorioYaEnviadoException;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Envía el recordatorio de las citas próximas, una sola vez por cita. Registra el recordatorio antes de enviarlo:
 * la restricción única de la base de datos garantiza un solo recordatorio por cita aunque haya varias instancias, y
 * si el registro falla no se envía nada (no hay reenvíos en cada revisión). Contrapartida: si un envío fallara
 * después de registrar, ese recordatorio no se reintentaría. El simulador no puede fallar; al sustituirlo por un
 * envío real habrá que decidir cómo compensarlo.
 */
public class EnviarRecordatoriosService implements EnviarRecordatoriosUseCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnviarRecordatoriosService.class);

    private static final DateTimeFormatter FORMATO_DE_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final AgendaGateway agendaGateway;
    private final NotificacionRepository notificacionRepository;
    private final CorreoSender correoSender;
    private final Clock reloj;
    private final Duration anticipacion;

    public EnviarRecordatoriosService(
            AgendaGateway agendaGateway,
            NotificacionRepository notificacionRepository,
            CorreoSender correoSender,
            Clock reloj,
            Duration anticipacion) {
        this.agendaGateway = agendaGateway;
        this.notificacionRepository = notificacionRepository;
        this.correoSender = correoSender;
        this.reloj = reloj;
        this.anticipacion = anticipacion;
    }

    @Override
    public List<Notificacion> enviarRecordatorios() {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        List<CitaPorRecordar> citas = agendaGateway.buscarCitasPorRecordar(ahora, ahora.plus(anticipacion));
        Set<Long> yaNotificadas = notificacionRepository.citasYaNotificadas(
                citas.stream().map(CitaPorRecordar::id).toList());
        List<Notificacion> enviadas = new ArrayList<>();
        for (CitaPorRecordar cita : citas) {
            if (!yaNotificadas.contains(cita.id())) {
                registrarYEnviar(cita, ahora).ifPresent(enviadas::add);
            }
        }
        return enviadas;
    }

    private Optional<Notificacion> registrarYEnviar(CitaPorRecordar cita, LocalDateTime ahora) {
        String asunto = "Recordatorio de cita: " + cita.mascotaNombre();
        String cuerpo = "Hola " + cita.contactoNombre() + ", te recordamos que " + cita.mascotaNombre()
                + " tiene una cita el " + FORMATO_DE_FECHA.format(cita.inicio()) + " (" + cita.motivo() + ").";

        Notificacion registrada;
        try {
            registrada = notificacionRepository.guardar(
                    new Notificacion(null, cita.id(), cita.contactoCorreo(), asunto, cuerpo, ahora));
        } catch (RecordatorioYaEnviadoException yaEnviado) {
            LOGGER.info("El recordatorio de la cita {} ya estaba registrado; se omite", cita.id());
            return Optional.empty();
        }

        correoSender.enviar(cita.contactoCorreo(), asunto, cuerpo);
        return Optional.of(registrada);
    }
}
