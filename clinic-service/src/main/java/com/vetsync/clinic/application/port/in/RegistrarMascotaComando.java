package com.vetsync.clinic.application.port.in;

import com.vetsync.clinic.domain.Sexo;
import java.time.LocalDate;

/** Datos para registrar una mascota; raza, sexo y fecha de nacimiento son opcionales. */
public record RegistrarMascotaComando(
        long propietarioId,
        String nombre,
        String especie,
        String raza,
        Sexo sexo,
        LocalDate fechaNacimiento) {
}
