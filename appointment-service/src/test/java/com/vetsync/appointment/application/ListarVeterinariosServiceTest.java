package com.vetsync.appointment.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsync.appointment.application.fakes.InMemoryVeterinarioRepository;
import com.vetsync.appointment.application.port.in.ListarVeterinariosUseCase;
import com.vetsync.appointment.application.service.ListarVeterinariosService;
import com.vetsync.appointment.domain.Veterinario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Especificación de {@code listarVeterinarios}, sin Spring ni base de datos. */
class ListarVeterinariosServiceTest {

    private InMemoryVeterinarioRepository veterinarioRepository;
    private ListarVeterinariosUseCase casoDeUso;

    @BeforeEach
    void prepararEscenario() {
        veterinarioRepository = new InMemoryVeterinarioRepository();
        casoDeUso = new ListarVeterinariosService(veterinarioRepository);
    }

    @Test
    void devuelveLosVeterinariosOrdenadosPorNombre() {
        Veterinario luis = veterinarioRepository.guardar(new Veterinario(null, "Luis Mora", null));
        Veterinario ana = veterinarioRepository.guardar(new Veterinario(null, "Ana Gomez", "Cirugia"));

        assertThat(casoDeUso.listarVeterinarios()).containsExactly(ana, luis);
    }

    @Test
    void devuelveUnaListaVaciaCuandoNoHayVeterinarios() {
        assertThat(casoDeUso.listarVeterinarios()).isEmpty();
    }
}
