package com.vetsync.clinic.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Detalle interno de persistencia: solo lo usa {@link RegistroMedicoRepositoryAdapter}. */
public interface RegistroMedicoJpaRepository extends JpaRepository<RegistroMedicoEntity, Long> {

    List<RegistroMedicoEntity> findByMascotaIdOrderByFechaDescIdDesc(long mascotaId);
}
