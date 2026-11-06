package com.vetsync.clinic.application.port.in;

import com.vetsync.clinic.domain.Propietario;

/** Caso de uso: registrar un propietario. El correo debe ser único. */
public interface RegistrarPropietarioUseCase {

    Propietario registrarPropietario(RegistrarPropietarioComando comando);
}
