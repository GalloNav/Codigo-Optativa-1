package com.vetsync.appointment.infrastructure.persistence;

import com.vetsync.appointment.application.port.out.AccesoADatosException;
import com.vetsync.appointment.application.port.out.CitaRepository;
import com.vetsync.appointment.domain.Cita;
import com.vetsync.appointment.domain.ConflictoDeHorarioException;
import com.vetsync.appointment.domain.EstadoCita;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataAccessException;

/** Adaptador de {@link CitaRepository} sobre SQL Server (JPA). */
public class CitaRepositoryAdapter implements CitaRepository {

    /** Error 2601 de SQL Server: violación de un índice único (aquí, UX_cita_veterinario_inicio). */
    private static final int ERROR_INDICE_UNICO = 2601;

    /** Error 2627 de SQL Server: violación de una restricción UNIQUE. */
    private static final int ERROR_RESTRICCION_UNICA = 2627;

    private final CitaJpaRepository citaJpaRepository;

    public CitaRepositoryAdapter(CitaJpaRepository citaJpaRepository) {
        this.citaJpaRepository = citaJpaRepository;
    }

    @Override
    public boolean existeCitaActiva(long veterinarioId, LocalDateTime inicio) {
        try {
            return citaJpaRepository.existsByVeterinarioIdAndInicioAndEstado(
                    veterinarioId, inicio, EstadoCita.PROGRAMADA);
        } catch (DataAccessException causa) {
            throw new AccesoADatosException(
                    "No se pudo consultar la agenda del veterinario " + veterinarioId, causa);
        }
    }

    @Override
    public Cita guardar(Cita cita) {
        try {
            return citaJpaRepository.saveAndFlush(CitaEntity.desde(cita)).aDominio();
        } catch (DataAccessException causa) {
            if (esViolacionDeUnicidad(causa)) {
                throw new ConflictoDeHorarioException(
                        "El veterinario " + cita.veterinarioId() + " ya tiene una cita en " + cita.inicio(), causa);
            }
            throw new AccesoADatosException("No se pudo guardar la cita", causa);
        }
    }

    @Override
    public List<Cita> buscarProgramadasEntre(LocalDateTime desde, LocalDateTime hasta) {
        try {
            return citaJpaRepository
                    .findByEstadoAndInicioBetweenOrderByInicioAscIdAsc(EstadoCita.PROGRAMADA, desde, hasta)
                    .stream()
                    .map(CitaEntity::aDominio)
                    .toList();
        } catch (DataAccessException causa) {
            throw new AccesoADatosException("No se pudo consultar las citas entre " + desde + " y " + hasta, causa);
        }
    }

    private static boolean esViolacionDeUnicidad(Throwable causa) {
        return NestedExceptionUtils.getMostSpecificCause(causa) instanceof SQLException errorSql
                && (errorSql.getErrorCode() == ERROR_INDICE_UNICO
                        || errorSql.getErrorCode() == ERROR_RESTRICCION_UNICA);
    }
}
