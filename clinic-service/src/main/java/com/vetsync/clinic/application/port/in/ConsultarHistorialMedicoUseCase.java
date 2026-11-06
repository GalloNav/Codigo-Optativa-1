package com.vetsync.clinic.application.port.in;

import com.vetsync.clinic.domain.RegistroMedico;
import java.util.List;

/** Caso de uso: consultar el historial médico de una mascota registrada, de la entrada más reciente a la más antigua. */
public interface ConsultarHistorialMedicoUseCase {

    List<RegistroMedico> consultarHistorial(long mascotaId);
}
