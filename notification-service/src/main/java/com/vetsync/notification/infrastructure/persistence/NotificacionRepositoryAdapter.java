package com.vetsync.notification.infrastructure.persistence;

import com.vetsync.notification.application.port.out.AccesoADatosException;
import com.vetsync.notification.application.port.out.NotificacionRepository;
import com.vetsync.notification.domain.Notificacion;
import com.vetsync.notification.domain.RecordatorioYaEnviadoException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Limit;

/** Adaptador de {@link NotificacionRepository} sobre SQL Server (JPA). */
public class NotificacionRepositoryAdapter implements NotificacionRepository {

    /** Error 2601 de SQL Server: violación de un índice único. */
    private static final int ERROR_INDICE_UNICO = 2601;

    /** Error 2627 de SQL Server: violación de una restricción UNIQUE (aquí, UQ_notificacion_cita_tipo). */
    private static final int ERROR_RESTRICCION_UNICA = 2627;

    /** SQL Server admite 2100 parámetros por consulta; los ids viajan en lotes muy por debajo de ese tope. */
    private static final int LIMITE_DE_IDS_POR_CONSULTA = 1_000;

    private final NotificacionJpaRepository notificacionJpaRepository;

    public NotificacionRepositoryAdapter(NotificacionJpaRepository notificacionJpaRepository) {
        this.notificacionJpaRepository = notificacionJpaRepository;
    }

    @Override
    public Notificacion guardar(Notificacion notificacion) {
        try {
            return notificacionJpaRepository.saveAndFlush(NotificacionEntity.desde(notificacion)).aDominio();
        } catch (DataAccessException causa) {
            if (esViolacionDeUnicidad(causa)) {
                throw new RecordatorioYaEnviadoException(
                        "La cita " + notificacion.citaId() + " ya tiene registrado su recordatorio", causa);
            }
            throw new AccesoADatosException(
                    "No se pudo guardar la notificacion de la cita " + notificacion.citaId(), causa);
        }
    }

    @Override
    public Set<Long> citasYaNotificadas(Collection<Long> citaIds) {
        List<Long> pendientes = List.copyOf(citaIds);
        Set<Long> notificadas = new HashSet<>();
        try {
            for (int desde = 0; desde < pendientes.size(); desde += LIMITE_DE_IDS_POR_CONSULTA) {
                int hasta = Math.min(desde + LIMITE_DE_IDS_POR_CONSULTA, pendientes.size());
                notificadas.addAll(notificacionJpaRepository.buscarCitaIdsNotificadas(pendientes.subList(desde, hasta)));
            }
            return notificadas;
        } catch (DataAccessException causa) {
            throw new AccesoADatosException("No se pudo comprobar las notificaciones de las citas", causa);
        }
    }

    @Override
    public List<Notificacion> buscarRecientes(int limite) {
        try {
            return notificacionJpaRepository.findAllByOrderByEnviadaEnDescIdDesc(Limit.of(limite)).stream()
                    .map(NotificacionEntity::aDominio)
                    .toList();
        } catch (DataAccessException causa) {
            throw new AccesoADatosException("No se pudo consultar las notificaciones", causa);
        }
    }

    private static boolean esViolacionDeUnicidad(Throwable causa) {
        return NestedExceptionUtils.getMostSpecificCause(causa) instanceof SQLException errorSql
                && (errorSql.getErrorCode() == ERROR_INDICE_UNICO
                        || errorSql.getErrorCode() == ERROR_RESTRICCION_UNICA);
    }
}
