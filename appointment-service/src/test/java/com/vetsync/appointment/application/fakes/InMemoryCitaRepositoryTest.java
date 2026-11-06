package com.vetsync.appointment.application.fakes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsync.appointment.domain.Cita;
import com.vetsync.appointment.domain.ConflictoDeHorarioException;
import com.vetsync.appointment.domain.EstadoCita;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * El fake debe cumplir el mismo contrato que {@code CitaRepositoryAdapter} (principio L de AGENTS.md):
 * el índice único filtrado UX_cita_veterinario_inicio se traduce a {@link ConflictoDeHorarioException}.
 */
class InMemoryCitaRepositoryTest {

    private static final LocalDateTime FRANJA = LocalDateTime.of(2030, 6, 11, 9, 0);
    private static final long VETERINARIO_ID = 1L;
    private static final long OTRO_VETERINARIO_ID = 2L;

    private InMemoryCitaRepository citaRepository;

    @BeforeEach
    void prepararEscenario() {
        citaRepository = new InMemoryCitaRepository();
    }

    @Test
    void lanzaConflictoDeHorarioAlGuardarOtraCitaProgramadaDelMismoVeterinarioEnLaMismaFranja() {
        citaRepository.guardar(cita(VETERINARIO_ID, FRANJA, EstadoCita.PROGRAMADA));

        assertThatThrownBy(() -> citaRepository.guardar(cita(VETERINARIO_ID, FRANJA, EstadoCita.PROGRAMADA)))
                .isInstanceOf(ConflictoDeHorarioException.class);
        assertThat(citaRepository.todas()).hasSize(1);
    }

    @Test
    void permiteLaMismaFranjaParaOtroVeterinario() {
        citaRepository.guardar(cita(VETERINARIO_ID, FRANJA, EstadoCita.PROGRAMADA));

        Cita guardada = citaRepository.guardar(cita(OTRO_VETERINARIO_ID, FRANJA, EstadoCita.PROGRAMADA));

        assertThat(guardada.id()).isNotNull();
        assertThat(citaRepository.todas()).hasSize(2);
    }

    @Test
    void permiteReutilizarLaFranjaDeUnaCitaCancelada() {
        citaRepository.guardar(cita(VETERINARIO_ID, FRANJA, EstadoCita.CANCELADA));

        Cita guardada = citaRepository.guardar(cita(VETERINARIO_ID, FRANJA, EstadoCita.PROGRAMADA));

        assertThat(guardada.id()).isNotNull();
        assertThat(citaRepository.todas()).hasSize(2);
    }

    private static Cita cita(long veterinarioId, LocalDateTime inicio, EstadoCita estado) {
        return new Cita(null, veterinarioId, 7L, "Rex", "Ana Perez", "ana@correo.com", inicio, "Vacuna", estado);
    }
}
