package com.vetsync.clinic.application.port.out;

import com.vetsync.clinic.domain.Propietario;
import java.util.Optional;

/** Puerto de salida hacia la persistencia de propietarios. */
public interface PropietarioRepository {

    /** Persiste al propietario y lo devuelve con su id asignado. Lanza CorreoYaRegistradoException si el correo existe. */
    Propietario guardar(Propietario propietario);

    Optional<Propietario> buscarPorId(long propietarioId);
}
