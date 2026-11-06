package com.vetsync.clinic.application.port.in;

import com.vetsync.clinic.domain.MascotaConPropietario;

/** Caso de uso: registrar una mascota de un propietario existente. */
public interface RegistrarMascotaUseCase {

    MascotaConPropietario registrarMascota(RegistrarMascotaComando comando);
}
