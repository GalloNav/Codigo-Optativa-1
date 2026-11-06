package com.vetsync.appointment.application.service;

import com.vetsync.appointment.application.port.in.ListarVeterinariosUseCase;
import com.vetsync.appointment.application.port.out.VeterinarioRepository;
import com.vetsync.appointment.domain.Veterinario;
import java.util.List;

/** Lista los veterinarios registrados. */
public class ListarVeterinariosService implements ListarVeterinariosUseCase {

    private final VeterinarioRepository veterinarioRepository;

    public ListarVeterinariosService(VeterinarioRepository veterinarioRepository) {
        this.veterinarioRepository = veterinarioRepository;
    }

    @Override
    public List<Veterinario> listarVeterinarios() {
        return veterinarioRepository.buscarTodos();
    }
}
