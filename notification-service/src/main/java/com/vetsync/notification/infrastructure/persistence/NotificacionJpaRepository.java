package com.vetsync.notification.infrastructure.persistence;

import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Detalle interno de persistencia: solo lo usa {@link NotificacionRepositoryAdapter}. */
public interface NotificacionJpaRepository extends JpaRepository<NotificacionEntity, Long> {

    @Query("select n.citaId from NotificacionEntity n where n.citaId in :citaIds")
    List<Long> buscarCitaIdsNotificadas(@Param("citaIds") Collection<Long> citaIds);

    List<NotificacionEntity> findAllByOrderByEnviadaEnDescIdDesc(Limit limite);
}
