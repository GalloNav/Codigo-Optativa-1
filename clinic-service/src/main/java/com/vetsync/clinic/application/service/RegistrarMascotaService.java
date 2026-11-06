package com.vetsync.clinic.application.service;

import com.vetsync.clinic.application.port.in.RegistrarMascotaComando;
import com.vetsync.clinic.application.port.in.RegistrarMascotaUseCase;
import com.vetsync.clinic.application.port.out.MascotaRepository;
import com.vetsync.clinic.application.port.out.PropietarioRepository;
import com.vetsync.clinic.domain.Mascota;
import com.vetsync.clinic.domain.MascotaConPropietario;
import com.vetsync.clinic.domain.Propietario;
import com.vetsync.clinic.domain.PropietarioNoRegistradoException;

/** Registra mascotas de propietarios existentes. */
public class RegistrarMascotaService implements RegistrarMascotaUseCase {

    private final PropietarioRepository propietarioRepository;
    private final MascotaRepository mascotaRepository;

    public RegistrarMascotaService(PropietarioRepository propietarioRepository, MascotaRepository mascotaRepository) {
        this.propietarioRepository = propietarioRepository;
        this.mascotaRepository = mascotaRepository;
    }

    @Override
    public MascotaConPropietario registrarMascota(RegistrarMascotaComando comando) {
        Propietario propietario = propietarioRepository.buscarPorId(comando.propietarioId())
                .orElseThrow(() -> new PropietarioNoRegistradoException(
                        "El propietario " + comando.propietarioId() + " no esta registrado"));

        Mascota mascota = mascotaRepository.guardar(new Mascota(
                null,
                comando.propietarioId(),
                comando.nombre(),
                comando.especie(),
                comando.raza(),
                comando.sexo(),
                comando.fechaNacimiento()));

        return new MascotaConPropietario(mascota, propietario);
    }
}
