package com.vetsync.clinic.infrastructure.persistence;

import com.vetsync.clinic.domain.RegistroMedico;
import com.vetsync.clinic.domain.TipoRegistro;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.hibernate.annotations.Nationalized;

/** Fila de la tabla {@code registro_medico}. */
@Entity
@Table(name = "registro_medico")
public class RegistroMedicoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private long mascotaId;

    private LocalDateTime fecha;

    @Enumerated(EnumType.STRING)
    private TipoRegistro tipo;

    @Nationalized
    private String motivo;

    @Nationalized
    private String diagnostico;

    @Nationalized
    private String tratamiento;

    private BigDecimal pesoKg;

    private Long veterinarioId;

    protected RegistroMedicoEntity() {
    }

    static RegistroMedicoEntity desde(RegistroMedico registro) {
        RegistroMedicoEntity entidad = new RegistroMedicoEntity();
        entidad.id = registro.id();
        entidad.mascotaId = registro.mascotaId();
        entidad.fecha = registro.fecha();
        entidad.tipo = registro.tipo();
        entidad.motivo = registro.motivo();
        entidad.diagnostico = registro.diagnostico();
        entidad.tratamiento = registro.tratamiento();
        entidad.pesoKg = registro.pesoKg();
        entidad.veterinarioId = registro.veterinarioId();
        return entidad;
    }

    RegistroMedico aDominio() {
        return new RegistroMedico(
                id, mascotaId, fecha, tipo, motivo, diagnostico, tratamiento, pesoKg, veterinarioId);
    }
}
