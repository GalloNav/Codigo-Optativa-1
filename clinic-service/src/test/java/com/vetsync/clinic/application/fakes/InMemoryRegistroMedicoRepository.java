package com.vetsync.clinic.application.fakes;

import com.vetsync.clinic.application.port.out.RegistroMedicoRepository;
import com.vetsync.clinic.domain.RegistroMedico;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Fake en memoria de {@link RegistroMedicoRepository}; respeta el orden del contrato (más reciente primero). */
public class InMemoryRegistroMedicoRepository implements RegistroMedicoRepository {

    private final List<RegistroMedico> registros = new ArrayList<>();
    private long ultimoId;

    @Override
    public RegistroMedico guardar(RegistroMedico registro) {
        ultimoId++;
        RegistroMedico guardado = new RegistroMedico(ultimoId, registro.mascotaId(), registro.fecha(),
                registro.tipo(), registro.motivo(), registro.diagnostico(), registro.tratamiento(),
                registro.pesoKg(), registro.veterinarioId());
        registros.add(guardado);
        return guardado;
    }

    @Override
    public List<RegistroMedico> buscarPorMascota(long mascotaId) {
        return registros.stream()
                .filter(registro -> registro.mascotaId() == mascotaId)
                .sorted(Comparator.comparing(RegistroMedico::fecha).thenComparing(RegistroMedico::id).reversed())
                .toList();
    }

    /** Solo para las aserciones de las pruebas: todo lo que se ha guardado. */
    public List<RegistroMedico> todos() {
        return List.copyOf(registros);
    }
}
