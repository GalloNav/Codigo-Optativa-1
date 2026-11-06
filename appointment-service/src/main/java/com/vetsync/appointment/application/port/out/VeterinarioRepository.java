package com.vetsync.appointment.application.port.out;

import com.vetsync.appointment.domain.Veterinario;
import java.util.List;

/** Puerto de salida hacia la persistencia de veterinarios. */
public interface VeterinarioRepository {

    /** Persiste al veterinario y lo devuelve con su id asignado. */
    Veterinario guardar(Veterinario veterinario);

    /** Todos los veterinarios, ordenados por nombre y, a igual nombre, por id. */
    List<Veterinario> buscarTodos();

    /** Indica si existe un veterinario con ese id. */
    boolean existe(long veterinarioId);
}
