package com.vetsync.clinic.application.port.out;

import com.vetsync.clinic.domain.Mascota;
import com.vetsync.clinic.domain.MascotaConPropietario;
import java.util.Optional;

/** Puerto de salida hacia la persistencia de mascotas. */
public interface MascotaRepository {

    /** Persiste la mascota y la devuelve con su id asignado. */
    Mascota guardar(Mascota mascota);

    Optional<MascotaConPropietario> buscarConPropietario(long mascotaId);

    /** Indica si existe una mascota con ese id. */
    boolean existe(long mascotaId);
}
