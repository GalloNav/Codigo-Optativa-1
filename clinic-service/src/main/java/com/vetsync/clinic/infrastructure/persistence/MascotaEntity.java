package com.vetsync.clinic.infrastructure.persistence;

import com.vetsync.clinic.domain.Mascota;
import com.vetsync.clinic.domain.MascotaConPropietario;
import com.vetsync.clinic.domain.Sexo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Nationalized;
import org.hibernate.type.SqlTypes;

/**
 * Fila de la tabla {@code mascota}. El propietario se mapea dos veces sobre la misma columna: el id para
 * escribir y la asociación (solo lectura) para traer al propietario junto con la mascota.
 */
@Entity
@Table(name = "mascota")
public class MascotaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "propietario_id")
    private long propietarioId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "propietario_id", insertable = false, updatable = false)
    private PropietarioEntity propietario;

    @Nationalized
    private String nombre;

    @Nationalized
    private String especie;

    @Nationalized
    private String raza;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.CHAR)
    private Sexo sexo;

    private LocalDate fechaNacimiento;

    protected MascotaEntity() {
    }

    static MascotaEntity desde(Mascota mascota) {
        MascotaEntity entidad = new MascotaEntity();
        entidad.id = mascota.id();
        entidad.propietarioId = mascota.propietarioId();
        entidad.nombre = mascota.nombre();
        entidad.especie = mascota.especie();
        entidad.raza = mascota.raza();
        entidad.sexo = mascota.sexo();
        entidad.fechaNacimiento = mascota.fechaNacimiento();
        return entidad;
    }

    Mascota aDominio() {
        return new Mascota(id, propietarioId, nombre, especie, raza, sexo, fechaNacimiento);
    }

    /** Solo válido cuando el propietario se trajo junto con la mascota (join fetch). */
    MascotaConPropietario aDominioConPropietario() {
        return new MascotaConPropietario(aDominio(), propietario.aDominio());
    }
}
