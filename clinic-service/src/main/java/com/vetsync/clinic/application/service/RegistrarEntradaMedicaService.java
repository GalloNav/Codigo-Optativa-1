package com.vetsync.clinic.application.service;

import com.vetsync.clinic.application.port.in.RegistrarEntradaMedicaComando;
import com.vetsync.clinic.application.port.in.RegistrarEntradaMedicaUseCase;
import com.vetsync.clinic.application.port.out.MascotaRepository;
import com.vetsync.clinic.application.port.out.RegistroMedicoRepository;
import com.vetsync.clinic.domain.MascotaNoEncontradaException;
import com.vetsync.clinic.domain.RegistroMedico;
import java.time.Clock;
import java.time.LocalDateTime;

/** Añade entradas al historial médico de una mascota registrada, con la fecha del reloj del sistema. */
public class RegistrarEntradaMedicaService implements RegistrarEntradaMedicaUseCase {

    private final MascotaRepository mascotaRepository;
    private final RegistroMedicoRepository registroMedicoRepository;
    private final Clock reloj;

    public RegistrarEntradaMedicaService(
            MascotaRepository mascotaRepository, RegistroMedicoRepository registroMedicoRepository, Clock reloj) {
        this.mascotaRepository = mascotaRepository;
        this.registroMedicoRepository = registroMedicoRepository;
        this.reloj = reloj;
    }

    @Override
    public RegistroMedico registrarEntradaMedica(RegistrarEntradaMedicaComando comando) {
        if (!mascotaRepository.existe(comando.mascotaId())) {
            throw new MascotaNoEncontradaException("La mascota " + comando.mascotaId() + " no existe");
        }

        return registroMedicoRepository.guardar(new RegistroMedico(
                null,
                comando.mascotaId(),
                LocalDateTime.now(reloj),
                comando.tipo(),
                comando.motivo(),
                comando.diagnostico(),
                comando.tratamiento(),
                comando.pesoKg(),
                comando.veterinarioId()));
    }
}
