package com.vetsync.clinic.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsync.clinic.application.fakes.InMemoryPropietarioRepository;
import com.vetsync.clinic.application.port.in.RegistrarPropietarioComando;
import com.vetsync.clinic.application.port.in.RegistrarPropietarioUseCase;
import com.vetsync.clinic.application.service.RegistrarPropietarioService;
import com.vetsync.clinic.domain.CorreoYaRegistradoException;
import com.vetsync.clinic.domain.Propietario;
import org.junit.jupiter.api.Test;

/** Especificación de {@code registrarPropietario}, sin Spring ni base de datos. */
class RegistrarPropietarioServiceTest {

    @Test
    void registraAlPropietarioYLoDevuelveConIdAsignado() {
        InMemoryPropietarioRepository propietarioRepository = new InMemoryPropietarioRepository();
        RegistrarPropietarioUseCase casoDeUso = new RegistrarPropietarioService(propietarioRepository);

        Propietario propietario = casoDeUso.registrarPropietario(
                new RegistrarPropietarioComando("Ana Perez", "ana@correo.com", "555-1234"));

        assertThat(propietario.id()).isNotNull();
        assertThat(propietario).usingRecursiveComparison().ignoringFields("id")
                .isEqualTo(new Propietario(null, "Ana Perez", "ana@correo.com", "555-1234"));
        assertThat(propietarioRepository.todos()).containsExactly(propietario);
    }

    @Test
    void propagaElConflictoCuandoElCorreoYaEstaRegistrado() {
        InMemoryPropietarioRepository propietarioRepository = new InMemoryPropietarioRepository();
        RegistrarPropietarioUseCase casoDeUso = new RegistrarPropietarioService(propietarioRepository);
        casoDeUso.registrarPropietario(new RegistrarPropietarioComando("Ana Perez", "ana@correo.com", "555-1234"));

        assertThatThrownBy(() -> casoDeUso.registrarPropietario(
                new RegistrarPropietarioComando("Otra Ana", "ana@correo.com", null)))
                .isInstanceOf(CorreoYaRegistradoException.class);
        assertThat(propietarioRepository.todos()).hasSize(1);
    }
}
