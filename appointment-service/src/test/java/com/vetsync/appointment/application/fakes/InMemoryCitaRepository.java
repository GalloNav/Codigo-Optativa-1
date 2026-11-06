package com.vetsync.appointment.application.fakes;

import com.vetsync.appointment.application.port.out.CitaRepository;
import com.vetsync.appointment.domain.Cita;
import com.vetsync.appointment.domain.ConflictoDeHorarioException;
import com.vetsync.appointment.domain.EstadoCita;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Fake en memoria de {@link CitaRepository}: mismo contrato que el adapter de SQL Server, sin base de datos. */
public class InMemoryCitaRepository implements CitaRepository {

    private final List<Cita> citas = new ArrayList<>();
    private long ultimoId;

    @Override
    public boolean existeCitaActiva(long veterinarioId, LocalDateTime inicio) {
        return citas.stream().anyMatch(cita -> cita.estado() == EstadoCita.PROGRAMADA
                && cita.veterinarioId() == veterinarioId
                && cita.inicio().equals(inicio));
    }

    @Override
    public Cita guardar(Cita cita) {
        if (cita.estado() == EstadoCita.PROGRAMADA && existeCitaActiva(cita.veterinarioId(), cita.inicio())) {
            throw new ConflictoDeHorarioException(
                    "El veterinario " + cita.veterinarioId() + " ya tiene una cita en " + cita.inicio());
        }
        ultimoId++;
        Cita guardada = new Cita(
                ultimoId,
                cita.veterinarioId(),
                cita.mascotaId(),
                cita.mascotaNombre(),
                cita.contactoNombre(),
                cita.contactoCorreo(),
                cita.inicio(),
                cita.motivo(),
                cita.estado());
        citas.add(guardada);
        return guardada;
    }

    @Override
    public List<Cita> buscarProgramadasEntre(LocalDateTime desde, LocalDateTime hasta) {
        return citas.stream()
                .filter(cita -> cita.estado() == EstadoCita.PROGRAMADA)
                .filter(cita -> !cita.inicio().isBefore(desde) && !cita.inicio().isAfter(hasta))
                .sorted(Comparator.comparing(Cita::inicio).thenComparing(Cita::id))
                .toList();
    }

    /** Solo para las aserciones de las pruebas: todo lo que se ha guardado. */
    public List<Cita> todas() {
        return List.copyOf(citas);
    }
}
