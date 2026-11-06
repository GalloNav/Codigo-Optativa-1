package com.vetsync.appointment.application.port.out;

import com.vetsync.appointment.domain.Cita;
import java.time.LocalDateTime;
import java.util.List;

/** Puerto de salida hacia la persistencia de citas. */
public interface CitaRepository {

    /** Indica si el veterinario tiene una cita PROGRAMADA que empiece en {@code inicio}. Las canceladas no cuentan. */
    boolean existeCitaActiva(long veterinarioId, LocalDateTime inicio);

    /**
     * Persiste la cita y la devuelve con su id asignado. Lanza ConflictoDeHorarioException si la cita es PROGRAMADA y
     * el veterinario ya tiene otra PROGRAMADA que empieza a la misma hora (lo garantiza también la base de datos).
     */
    Cita guardar(Cita cita);

    /**
     * Citas PROGRAMADA cuyo inicio está entre {@code desde} y {@code hasta}, ambos incluidos, en orden
     * cronológico y, a igual inicio, por id. Las canceladas no cuentan.
     */
    List<Cita> buscarProgramadasEntre(LocalDateTime desde, LocalDateTime hasta);
}
