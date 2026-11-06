package com.vetsync.clinic.application.service;

import com.vetsync.clinic.application.port.in.ConsultarHistorialMedicoUseCase;
import com.vetsync.clinic.application.port.out.MascotaRepository;
import com.vetsync.clinic.application.port.out.RegistroMedicoRepository;
import com.vetsync.clinic.domain.MascotaNoEncontradaException;
import com.vetsync.clinic.domain.RegistroMedico;
import java.util.List;

/** Consulta el historial médico de una mascota registrada. */
public class ConsultarHistorialMedicoService implements ConsultarHistorialMedicoUseCase {

    private final MascotaRepository mascotaRepository;
    private final RegistroMedicoRepository registroMedicoRepository;

    public ConsultarHistorialMedicoService(
            MascotaRepository mascotaRepository, RegistroMedicoRepository registroMedicoRepository) {
        this.mascotaRepository = mascotaRepository;
        this.registroMedicoRepository = registroMedicoRepository;
    }

    @Override
    public List<RegistroMedico> consultarHistorial(long mascotaId) {
        if (!mascotaRepository.existe(mascotaId)) {
            throw new MascotaNoEncontradaException("La mascota " + mascotaId + " no existe");
        }
        return registroMedicoRepository.buscarPorMascota(mascotaId);
    }
}
