package com.vetsync.appointment.application.port.in;

import com.vetsync.appointment.domain.Cita;

/**
 * Caso de uso: agendar una cita sin choques de horario (requisito 2).
 *
 * <p>Contrato: el inicio debe ser estrictamente posterior a la hora actual y caer en una franja de 30 minutos
 * alineada a la media hora dentro del horario de atención (FranjaHorariaInvalidaException si no),
 * el veterinario debe estar registrado,
 * la mascota debe estar registrada en clinic-service y el veterinario no puede tener otra cita programada en la
 * misma franja.
 * Devuelve la cita guardada en estado PROGRAMADA con los datos de contacto copiados de la mascota.
 */
public interface AgendarCitaUseCase {

    Cita agendarCita(AgendarCitaComando comando);
}
