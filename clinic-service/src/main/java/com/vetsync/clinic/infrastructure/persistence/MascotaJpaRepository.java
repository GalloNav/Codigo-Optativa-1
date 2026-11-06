package com.vetsync.clinic.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Detalle interno de persistencia: solo lo usa {@link MascotaRepositoryAdapter}. */
public interface MascotaJpaRepository extends JpaRepository<MascotaEntity, Long> {

    @Query("select m from MascotaEntity m join fetch m.propietario where m.id = :mascotaId")
    Optional<MascotaEntity> buscarConPropietarioPorId(@Param("mascotaId") long mascotaId);
}
