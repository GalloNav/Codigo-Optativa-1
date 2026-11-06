package com.vetsync.clinic.domain;

import java.time.LocalDate;

/** Mascota de un propietario. Raza, sexo y fecha de nacimiento son opcionales. */
public record Mascota(
        Long id,
        long propietarioId,
        String nombre,
        String especie,
        String raza,
        Sexo sexo,
        LocalDate fechaNacimiento) {
}
