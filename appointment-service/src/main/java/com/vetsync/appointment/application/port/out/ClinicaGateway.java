package com.vetsync.appointment.application.port.out;

import java.util.Optional;

/** Puerto de salida hacia clinic-service. */
public interface ClinicaGateway {

    /** Datos de la mascota y de su dueño, o vacío si la mascota no está registrada. */
    Optional<ContactoDeMascota> buscarContactoDeMascota(long mascotaId);
}
