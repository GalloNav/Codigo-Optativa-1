package com.vetsync.clinic.infrastructure.web;

import com.vetsync.clinic.domain.Mascota;
import com.vetsync.clinic.domain.MascotaConPropietario;
import com.vetsync.clinic.domain.Propietario;
import com.vetsync.clinic.domain.Sexo;
import java.time.LocalDate;

/**
 * Mascota con su propietario. Esta forma es el contrato que consume appointment-service
 * ({@code GET /mascotas/{id}}); el teléfono del propietario no se expone.
 */
public record MascotaResponse(
        Long id,
        String nombre,
        String especie,
        String raza,
        Sexo sexo,
        LocalDate fechaNacimiento,
        PropietarioResumen propietario) {

    /** Datos mínimos del propietario que acompañan a la mascota. */
    public record PropietarioResumen(Long id, String nombre, String correo) {
    }

    public static MascotaResponse desde(MascotaConPropietario vista) {
        Mascota mascota = vista.mascota();
        Propietario propietario = vista.propietario();
        return new MascotaResponse(
                mascota.id(),
                mascota.nombre(),
                mascota.especie(),
                mascota.raza(),
                mascota.sexo(),
                mascota.fechaNacimiento(),
                new PropietarioResumen(propietario.id(), propietario.nombre(), propietario.correo()));
    }
}
