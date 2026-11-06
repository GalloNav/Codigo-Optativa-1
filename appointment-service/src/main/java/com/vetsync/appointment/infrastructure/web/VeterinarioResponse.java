package com.vetsync.appointment.infrastructure.web;

import com.vetsync.appointment.domain.Veterinario;

/** Veterinario tal como lo ve el cliente de la API. */
public record VeterinarioResponse(Long id, String nombre, String especialidad) {

    public static VeterinarioResponse desde(Veterinario veterinario) {
        return new VeterinarioResponse(veterinario.id(), veterinario.nombre(), veterinario.especialidad());
    }
}
