package com.vetsync.appointment.infrastructure.persistence;

import com.vetsync.appointment.domain.Veterinario;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Nationalized;

/** Fila de la tabla {@code veterinario}. La columna activo no se mapea: usa el valor por defecto de la base. */
@Entity
@Table(name = "veterinario")
public class VeterinarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Nationalized
    private String nombre;

    @Nationalized
    private String especialidad;

    protected VeterinarioEntity() {
    }

    static VeterinarioEntity desde(Veterinario veterinario) {
        VeterinarioEntity entidad = new VeterinarioEntity();
        entidad.id = veterinario.id();
        entidad.nombre = veterinario.nombre();
        entidad.especialidad = veterinario.especialidad();
        return entidad;
    }

    Veterinario aDominio() {
        return new Veterinario(id, nombre, especialidad);
    }
}
