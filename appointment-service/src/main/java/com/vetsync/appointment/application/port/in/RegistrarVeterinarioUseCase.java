package com.vetsync.appointment.application.port.in;

import com.vetsync.appointment.domain.Veterinario;

/** Caso de uso: registrar un veterinario para poder agendarle citas. */
public interface RegistrarVeterinarioUseCase {

    Veterinario registrarVeterinario(RegistrarVeterinarioComando comando);
}
