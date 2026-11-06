package com.vetsync.clinic.infrastructure.persistence;

import com.vetsync.clinic.domain.Propietario;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Nationalized;

/** Fila de la tabla {@code propietario}. La columna creado_en usa el valor por defecto de la base de datos. */
@Entity
@Table(name = "propietario")
public class PropietarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Nationalized
    private String nombre;

    @Nationalized
    private String correo;

    private String telefono;

    protected PropietarioEntity() {
    }

    static PropietarioEntity desde(Propietario propietario) {
        PropietarioEntity entidad = new PropietarioEntity();
        entidad.id = propietario.id();
        entidad.nombre = propietario.nombre();
        entidad.correo = propietario.correo();
        entidad.telefono = propietario.telefono();
        return entidad;
    }

    Propietario aDominio() {
        return new Propietario(id, nombre, correo, telefono);
    }
}
