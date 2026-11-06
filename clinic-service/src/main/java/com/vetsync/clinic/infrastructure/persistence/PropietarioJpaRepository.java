package com.vetsync.clinic.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/** Detalle interno de persistencia: solo lo usa {@link PropietarioRepositoryAdapter}. */
public interface PropietarioJpaRepository extends JpaRepository<PropietarioEntity, Long> {
}
