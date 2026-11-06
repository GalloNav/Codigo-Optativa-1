package com.vetsync.clinic.application.port.in;

import com.vetsync.clinic.domain.MascotaConPropietario;

/** Caso de uso: consultar una mascota con su propietario (lo consume appointment-service). */
public interface ConsultarMascotaUseCase {

    MascotaConPropietario consultarMascota(long mascotaId);
}
