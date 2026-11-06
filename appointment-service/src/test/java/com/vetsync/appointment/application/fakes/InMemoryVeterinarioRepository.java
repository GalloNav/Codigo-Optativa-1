package com.vetsync.appointment.application.fakes;

import com.vetsync.appointment.application.port.out.VeterinarioRepository;
import com.vetsync.appointment.domain.Veterinario;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Fake en memoria de {@link VeterinarioRepository}; los ids se asignan desde 1 y el listado respeta el contrato. */
public class InMemoryVeterinarioRepository implements VeterinarioRepository {

    private final List<Veterinario> veterinarios = new ArrayList<>();
    private long ultimoId;

    @Override
    public Veterinario guardar(Veterinario veterinario) {
        ultimoId++;
        Veterinario guardado = new Veterinario(ultimoId, veterinario.nombre(), veterinario.especialidad());
        veterinarios.add(guardado);
        return guardado;
    }

    @Override
    public List<Veterinario> buscarTodos() {
        return veterinarios.stream()
                .sorted(Comparator.comparing(Veterinario::nombre).thenComparing(Veterinario::id))
                .toList();
    }

    @Override
    public boolean existe(long veterinarioId) {
        return veterinarios.stream().anyMatch(veterinario -> veterinario.id() == veterinarioId);
    }

    /** Solo para las aserciones de las pruebas: todo lo que se ha guardado, en orden de inserción. */
    public List<Veterinario> todos() {
        return List.copyOf(veterinarios);
    }
}
