package com.vetsync.appointment.infrastructure.persistence;

import com.vetsync.appointment.domain.Cita;
import com.vetsync.appointment.domain.EstadoCita;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.Nationalized;

/**
 * Fila de la tabla {@code cita}. Las columnas modalidad, enlace_videollamada y creada_en no se mapean:
 * se rellenan con los valores por defecto de la base de datos.
 */
@Entity
@Table(name = "cita")
public class CitaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private long veterinarioId;

    private long mascotaId;

    @Nationalized
    private String mascotaNombre;

    @Nationalized
    private String contactoNombre;

    @Nationalized
    private String contactoCorreo;

    private LocalDateTime inicio;

    @Nationalized
    private String motivo;

    @Enumerated(EnumType.STRING)
    private EstadoCita estado;

    protected CitaEntity() {
    }

    static CitaEntity desde(Cita cita) {
        CitaEntity entidad = new CitaEntity();
        entidad.id = cita.id();
        entidad.veterinarioId = cita.veterinarioId();
        entidad.mascotaId = cita.mascotaId();
        entidad.mascotaNombre = cita.mascotaNombre();
        entidad.contactoNombre = cita.contactoNombre();
        entidad.contactoCorreo = cita.contactoCorreo();
        entidad.inicio = cita.inicio();
        entidad.motivo = cita.motivo();
        entidad.estado = cita.estado();
        return entidad;
    }

    Cita aDominio() {
        return new Cita(id, veterinarioId, mascotaId, mascotaNombre, contactoNombre, contactoCorreo,
                inicio, motivo, estado);
    }
}
