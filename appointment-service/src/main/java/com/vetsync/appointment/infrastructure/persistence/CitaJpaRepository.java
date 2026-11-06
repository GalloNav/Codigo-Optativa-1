package com.vetsync.appointment.infrastructure.persistence;

import com.vetsync.appointment.domain.EstadoCita;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Detalle interno de persistencia: solo lo usa {@link CitaRepositoryAdapter}. */
public interface CitaJpaRepository extends JpaRepository<CitaEntity, Long> {

    boolean existsByVeterinarioIdAndInicioAndEstado(long veterinarioId, LocalDateTime inicio, EstadoCita estado);

    List<CitaEntity> findByEstadoAndInicioBetweenOrderByInicioAscIdAsc(
            EstadoCita estado, LocalDateTime desde, LocalDateTime hasta);
}
