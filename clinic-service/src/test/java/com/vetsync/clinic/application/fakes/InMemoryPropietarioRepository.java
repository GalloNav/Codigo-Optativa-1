package com.vetsync.clinic.application.fakes;

import com.vetsync.clinic.application.port.out.PropietarioRepository;
import com.vetsync.clinic.domain.CorreoYaRegistradoException;
import com.vetsync.clinic.domain.Propietario;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Fake en memoria de {@link PropietarioRepository}, sin base de datos. */
public class InMemoryPropietarioRepository implements PropietarioRepository {

    private final List<Propietario> propietarios = new ArrayList<>();
    private long ultimoId;

    @Override
    public Propietario guardar(Propietario propietario) {
        if (propietarios.stream().anyMatch(existente -> existente.correo().equals(propietario.correo()))) {
            throw new CorreoYaRegistradoException("El correo " + propietario.correo() + " ya esta registrado");
        }
        ultimoId++;
        Propietario guardado = new Propietario(
                ultimoId, propietario.nombre(), propietario.correo(), propietario.telefono());
        propietarios.add(guardado);
        return guardado;
    }

    @Override
    public Optional<Propietario> buscarPorId(long propietarioId) {
        return propietarios.stream().filter(propietario -> propietario.id() == propietarioId).findFirst();
    }

    /** Solo para las aserciones de las pruebas: todo lo que se ha guardado. */
    public List<Propietario> todos() {
        return List.copyOf(propietarios);
    }
}
