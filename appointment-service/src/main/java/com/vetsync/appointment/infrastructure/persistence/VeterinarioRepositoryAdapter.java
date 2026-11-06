package com.vetsync.appointment.infrastructure.persistence;

import com.vetsync.appointment.application.port.out.AccesoADatosException;
import com.vetsync.appointment.application.port.out.VeterinarioRepository;
import com.vetsync.appointment.domain.Veterinario;
import java.util.List;
import org.springframework.dao.DataAccessException;

/** Adaptador de {@link VeterinarioRepository} sobre SQL Server (JPA). */
public class VeterinarioRepositoryAdapter implements VeterinarioRepository {

    private final VeterinarioJpaRepository veterinarioJpaRepository;

    public VeterinarioRepositoryAdapter(VeterinarioJpaRepository veterinarioJpaRepository) {
        this.veterinarioJpaRepository = veterinarioJpaRepository;
    }

    @Override
    public Veterinario guardar(Veterinario veterinario) {
        try {
            return veterinarioJpaRepository.saveAndFlush(VeterinarioEntity.desde(veterinario)).aDominio();
        } catch (DataAccessException causa) {
            throw new AccesoADatosException("No se pudo guardar al veterinario", causa);
        }
    }

    @Override
    public List<Veterinario> buscarTodos() {
        try {
            return veterinarioJpaRepository.findAllByOrderByNombreAscIdAsc().stream()
                    .map(VeterinarioEntity::aDominio)
                    .toList();
        } catch (DataAccessException causa) {
            throw new AccesoADatosException("No se pudo consultar a los veterinarios", causa);
        }
    }

    @Override
    public boolean existe(long veterinarioId) {
        try {
            return veterinarioJpaRepository.existsById(veterinarioId);
        } catch (DataAccessException causa) {
            throw new AccesoADatosException("No se pudo comprobar al veterinario " + veterinarioId, causa);
        }
    }
}
