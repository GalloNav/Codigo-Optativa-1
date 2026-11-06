package com.vetsync.clinic.infrastructure.persistence;

import com.vetsync.clinic.application.port.out.AccesoADatosException;
import com.vetsync.clinic.application.port.out.PropietarioRepository;
import com.vetsync.clinic.domain.CorreoYaRegistradoException;
import com.vetsync.clinic.domain.Propietario;
import java.sql.SQLException;
import java.util.Optional;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataAccessException;

/** Adaptador de {@link PropietarioRepository} sobre SQL Server (JPA). */
public class PropietarioRepositoryAdapter implements PropietarioRepository {

    /** Error 2601 de SQL Server: violación de un índice único. */
    private static final int ERROR_INDICE_UNICO = 2601;

    /** Error 2627 de SQL Server: violación de una restricción UNIQUE (aquí, UQ_propietario_correo). */
    private static final int ERROR_RESTRICCION_UNICA = 2627;

    private final PropietarioJpaRepository propietarioJpaRepository;

    public PropietarioRepositoryAdapter(PropietarioJpaRepository propietarioJpaRepository) {
        this.propietarioJpaRepository = propietarioJpaRepository;
    }

    @Override
    public Propietario guardar(Propietario propietario) {
        try {
            return propietarioJpaRepository.saveAndFlush(PropietarioEntity.desde(propietario)).aDominio();
        } catch (DataAccessException causa) {
            if (esViolacionDeUnicidad(causa)) {
                throw new CorreoYaRegistradoException(
                        "El correo " + propietario.correo() + " ya esta registrado", causa);
            }
            throw new AccesoADatosException("No se pudo guardar al propietario", causa);
        }
    }

    @Override
    public Optional<Propietario> buscarPorId(long propietarioId) {
        try {
            return propietarioJpaRepository.findById(propietarioId).map(PropietarioEntity::aDominio);
        } catch (DataAccessException causa) {
            throw new AccesoADatosException("No se pudo consultar al propietario " + propietarioId, causa);
        }
    }

    private static boolean esViolacionDeUnicidad(Throwable causa) {
        return NestedExceptionUtils.getMostSpecificCause(causa) instanceof SQLException errorSql
                && (errorSql.getErrorCode() == ERROR_INDICE_UNICO
                        || errorSql.getErrorCode() == ERROR_RESTRICCION_UNICA);
    }
}
