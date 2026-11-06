package com.vetsync.clinic.infrastructure.persistence;

import com.vetsync.clinic.application.port.out.AccesoADatosException;
import com.vetsync.clinic.application.port.out.RegistroMedicoRepository;
import com.vetsync.clinic.domain.RegistroMedico;
import java.util.List;
import org.springframework.dao.DataAccessException;

/** Adaptador de {@link RegistroMedicoRepository} sobre SQL Server (JPA). */
public class RegistroMedicoRepositoryAdapter implements RegistroMedicoRepository {

    private final RegistroMedicoJpaRepository registroMedicoJpaRepository;

    public RegistroMedicoRepositoryAdapter(RegistroMedicoJpaRepository registroMedicoJpaRepository) {
        this.registroMedicoJpaRepository = registroMedicoJpaRepository;
    }

    @Override
    public RegistroMedico guardar(RegistroMedico registro) {
        try {
            return registroMedicoJpaRepository.saveAndFlush(RegistroMedicoEntity.desde(registro)).aDominio();
        } catch (DataAccessException causa) {
            throw new AccesoADatosException("No se pudo guardar la entrada del historial médico", causa);
        }
    }

    @Override
    public List<RegistroMedico> buscarPorMascota(long mascotaId) {
        try {
            return registroMedicoJpaRepository.findByMascotaIdOrderByFechaDescIdDesc(mascotaId).stream()
                    .map(RegistroMedicoEntity::aDominio)
                    .toList();
        } catch (DataAccessException causa) {
            throw new AccesoADatosException(
                    "No se pudo consultar el historial médico de la mascota " + mascotaId, causa);
        }
    }
}
