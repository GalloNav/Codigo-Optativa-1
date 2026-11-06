package com.vetsync.clinic.infrastructure.persistence;

import com.vetsync.clinic.application.port.out.AccesoADatosException;
import com.vetsync.clinic.application.port.out.MascotaRepository;
import com.vetsync.clinic.domain.Mascota;
import com.vetsync.clinic.domain.MascotaConPropietario;
import java.util.Optional;
import org.springframework.dao.DataAccessException;

/** Adaptador de {@link MascotaRepository} sobre SQL Server (JPA). */
public class MascotaRepositoryAdapter implements MascotaRepository {

    private final MascotaJpaRepository mascotaJpaRepository;

    public MascotaRepositoryAdapter(MascotaJpaRepository mascotaJpaRepository) {
        this.mascotaJpaRepository = mascotaJpaRepository;
    }

    @Override
    public Mascota guardar(Mascota mascota) {
        try {
            return mascotaJpaRepository.saveAndFlush(MascotaEntity.desde(mascota)).aDominio();
        } catch (DataAccessException causa) {
            throw new AccesoADatosException("No se pudo guardar la mascota", causa);
        }
    }

    @Override
    public Optional<MascotaConPropietario> buscarConPropietario(long mascotaId) {
        try {
            return mascotaJpaRepository.buscarConPropietarioPorId(mascotaId)
                    .map(MascotaEntity::aDominioConPropietario);
        } catch (DataAccessException causa) {
            throw new AccesoADatosException("No se pudo consultar la mascota " + mascotaId, causa);
        }
    }

    @Override
    public boolean existe(long mascotaId) {
        try {
            return mascotaJpaRepository.existsById(mascotaId);
        } catch (DataAccessException causa) {
            throw new AccesoADatosException("No se pudo comprobar la mascota " + mascotaId, causa);
        }
    }
}
