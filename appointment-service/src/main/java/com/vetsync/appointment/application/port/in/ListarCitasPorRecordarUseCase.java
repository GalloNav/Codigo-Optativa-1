package com.vetsync.appointment.application.port.in;

import com.vetsync.appointment.domain.Cita;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Caso de uso: listar las citas programadas cuyo inicio cae entre {@code desde} y {@code hasta} (ambos incluidos),
 * en orden cronológico. Lo consume notification-service para decidir a quién recordar.
 * Lanza RangoDeFechasInvalidoException si {@code desde} es posterior a {@code hasta}.
 */
public interface ListarCitasPorRecordarUseCase {

    List<Cita> listarCitasPorRecordar(LocalDateTime desde, LocalDateTime hasta);
}
