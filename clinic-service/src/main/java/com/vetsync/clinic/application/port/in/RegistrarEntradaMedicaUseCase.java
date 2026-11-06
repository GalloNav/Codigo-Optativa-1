package com.vetsync.clinic.application.port.in;

import com.vetsync.clinic.domain.RegistroMedico;

/** Caso de uso: añadir una entrada al historial médico de una mascota registrada. */
public interface RegistrarEntradaMedicaUseCase {

    RegistroMedico registrarEntradaMedica(RegistrarEntradaMedicaComando comando);
}
