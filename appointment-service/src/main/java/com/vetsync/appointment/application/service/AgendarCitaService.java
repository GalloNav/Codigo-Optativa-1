package com.vetsync.appointment.application.service;

import com.vetsync.appointment.application.port.in.AgendarCitaComando;
import com.vetsync.appointment.application.port.in.AgendarCitaUseCase;
import com.vetsync.appointment.application.port.out.CitaRepository;
import com.vetsync.appointment.application.port.out.ClinicaGateway;
import com.vetsync.appointment.application.port.out.ContactoDeMascota;
import com.vetsync.appointment.application.port.out.VeterinarioRepository;
import com.vetsync.appointment.domain.Cita;
import com.vetsync.appointment.domain.ConflictoDeHorarioException;
import com.vetsync.appointment.domain.EstadoCita;
import com.vetsync.appointment.domain.FranjaHorariaInvalidaException;
import com.vetsync.appointment.domain.HorarioDeAtencion;
import com.vetsync.appointment.domain.MascotaNoRegistradaException;
import com.vetsync.appointment.domain.VeterinarioNoEncontradoException;
import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Agenda citas validando, en este orden, la franja, el veterinario, la mascota y la disponibilidad del veterinario.
 * Lo local y barato va antes que la llamada a clinic-service.
 */
public class AgendarCitaService implements AgendarCitaUseCase {

    private final CitaRepository citaRepository;
    private final VeterinarioRepository veterinarioRepository;
    private final ClinicaGateway clinicaGateway;
    private final Clock reloj;
    private final HorarioDeAtencion horarioDeAtencion;

    public AgendarCitaService(
            CitaRepository citaRepository,
            VeterinarioRepository veterinarioRepository,
            ClinicaGateway clinicaGateway,
            Clock reloj,
            HorarioDeAtencion horarioDeAtencion) {
        this.citaRepository = citaRepository;
        this.veterinarioRepository = veterinarioRepository;
        this.clinicaGateway = clinicaGateway;
        this.reloj = reloj;
        this.horarioDeAtencion = horarioDeAtencion;
    }

    @Override
    public Cita agendarCita(AgendarCitaComando comando) {
        if (!comando.inicio().isAfter(LocalDateTime.now(reloj))) {
            throw new FranjaHorariaInvalidaException(
                    "La cita debe empezar despues de la hora actual: " + comando.inicio());
        }

        if (!horarioDeAtencion.admite(comando.inicio())) {
            throw new FranjaHorariaInvalidaException(
                    "La cita debe empezar en una franja de " + HorarioDeAtencion.DURACION_FRANJA_MINUTOS
                            + " minutos entre las " + horarioDeAtencion.horaApertura() + ":00 y las "
                            + horarioDeAtencion.horaCierre() + ":00: " + comando.inicio());
        }

        if (!veterinarioRepository.existe(comando.veterinarioId())) {
            throw new VeterinarioNoEncontradoException(
                    "El veterinario " + comando.veterinarioId() + " no esta registrado");
        }

        ContactoDeMascota contacto = clinicaGateway.buscarContactoDeMascota(comando.mascotaId())
                .orElseThrow(() -> new MascotaNoRegistradaException(
                        "La mascota " + comando.mascotaId() + " no esta registrada"));

        if (citaRepository.existeCitaActiva(comando.veterinarioId(), comando.inicio())) {
            throw new ConflictoDeHorarioException(
                    "El veterinario " + comando.veterinarioId() + " ya tiene una cita en " + comando.inicio());
        }

        return citaRepository.guardar(new Cita(
                null,
                comando.veterinarioId(),
                comando.mascotaId(),
                contacto.mascotaNombre(),
                contacto.contactoNombre(),
                contacto.contactoCorreo(),
                comando.inicio(),
                comando.motivo(),
                EstadoCita.PROGRAMADA));
    }
}
