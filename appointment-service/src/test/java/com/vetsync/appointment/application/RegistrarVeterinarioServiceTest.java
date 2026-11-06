package com.vetsync.appointment.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsync.appointment.application.fakes.InMemoryVeterinarioRepository;
import com.vetsync.appointment.application.port.in.RegistrarVeterinarioComando;
import com.vetsync.appointment.application.port.in.RegistrarVeterinarioUseCase;
import com.vetsync.appointment.application.service.RegistrarVeterinarioService;
import com.vetsync.appointment.domain.Veterinario;
import org.junit.jupiter.api.Test;

/** Especificación de {@code registrarVeterinario}, sin Spring ni base de datos. */
class RegistrarVeterinarioServiceTest {

    @Test
    void registraAlVeterinarioYLoDevuelveConIdAsignado() {
        InMemoryVeterinarioRepository veterinarioRepository = new InMemoryVeterinarioRepository();
        RegistrarVeterinarioUseCase casoDeUso = new RegistrarVeterinarioService(veterinarioRepository);

        Veterinario veterinario = casoDeUso.registrarVeterinario(new RegistrarVeterinarioComando("Ana Gomez", "Cirugia"));

        assertThat(veterinario.id()).isNotNull();
        assertThat(veterinario).usingRecursiveComparison().ignoringFields("id")
                .isEqualTo(new Veterinario(null, "Ana Gomez", "Cirugia"));
        assertThat(veterinarioRepository.todos()).containsExactly(veterinario);
    }
}
