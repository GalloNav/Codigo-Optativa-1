package com.vetsync.clinic.application.port.out;

import com.vetsync.clinic.domain.RegistroMedico;
import java.util.List;

/** Puerto de salida hacia la persistencia del historial médico. */
public interface RegistroMedicoRepository {

    /** Persiste la entrada y la devuelve con su id asignado. */
    RegistroMedico guardar(RegistroMedico registro);

    /** Entradas de la mascota, de la más reciente a la más antigua (a igual fecha, la última guardada primero). */
    List<RegistroMedico> buscarPorMascota(long mascotaId);
}
