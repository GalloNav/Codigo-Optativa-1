package com.vetsync.appointment.application.port.in;

import com.vetsync.appointment.domain.Veterinario;
import java.util.List;

/** Caso de uso: listar los veterinarios registrados, ordenados por nombre. */
public interface ListarVeterinariosUseCase {

    List<Veterinario> listarVeterinarios();
}
