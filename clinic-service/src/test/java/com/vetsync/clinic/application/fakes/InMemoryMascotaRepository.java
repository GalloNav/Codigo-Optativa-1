package com.vetsync.clinic.application.fakes;

import com.vetsync.clinic.application.port.out.MascotaRepository;
import com.vetsync.clinic.application.port.out.PropietarioRepository;
import com.vetsync.clinic.domain.Mascota;
import com.vetsync.clinic.domain.MascotaConPropietario;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Fake en memoria de {@link MascotaRepository}; resuelve los propietarios con el repositorio que recibe. */
public class InMemoryMascotaRepository implements MascotaRepository {

    private final PropietarioRepository propietarioRepository;
    private final List<Mascota> mascotas = new ArrayList<>();
    private long ultimoId;

    public InMemoryMascotaRepository(PropietarioRepository propietarioRepository) {
        this.propietarioRepository = propietarioRepository;
    }

    @Override
    public Mascota guardar(Mascota mascota) {
        ultimoId++;
        Mascota guardada = new Mascota(ultimoId, mascota.propietarioId(), mascota.nombre(), mascota.especie(),
                mascota.raza(), mascota.sexo(), mascota.fechaNacimiento());
        mascotas.add(guardada);
        return guardada;
    }

    @Override
    public Optional<MascotaConPropietario> buscarConPropietario(long mascotaId) {
        return mascotas.stream()
                .filter(mascota -> mascota.id() == mascotaId)
                .findFirst()
                .flatMap(mascota -> propietarioRepository.buscarPorId(mascota.propietarioId())
                        .map(propietario -> new MascotaConPropietario(mascota, propietario)));
    }

    @Override
    public boolean existe(long mascotaId) {
        return mascotas.stream().anyMatch(mascota -> mascota.id() == mascotaId);
    }

    /** Solo para las aserciones de las pruebas: todo lo que se ha guardado. */
    public List<Mascota> todas() {
        return List.copyOf(mascotas);
    }
}
