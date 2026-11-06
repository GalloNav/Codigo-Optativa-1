package com.vetsync.appointment.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Detalle interno de persistencia: solo lo usa {@link VeterinarioRepositoryAdapter}. */
public interface VeterinarioJpaRepository extends JpaRepository<VeterinarioEntity, Long> {

    List<VeterinarioEntity> findAllByOrderByNombreAscIdAsc();
}
