package com.vetsync.clinic.application.fakes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsync.clinic.domain.CorreoYaRegistradoException;
import com.vetsync.clinic.domain.Propietario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * El fake debe cumplir el mismo contrato que {@code PropietarioRepositoryAdapter} (principio L de AGENTS.md):
 * el correo es único y repetirlo lanza {@link CorreoYaRegistradoException}.
 */
class InMemoryPropietarioRepositoryTest {

    private InMemoryPropietarioRepository propietarioRepository;

    @BeforeEach
    void prepararEscenario() {
        propietarioRepository = new InMemoryPropietarioRepository();
    }

    @Test
    void lanzaCorreoYaRegistradoAlGuardarUnPropietarioConUnCorreoExistente() {
        propietarioRepository.guardar(new Propietario(null, "Ana Perez", "ana@correo.com", "555-1234"));

        assertThatThrownBy(() -> propietarioRepository.guardar(
                new Propietario(null, "Otra Ana", "ana@correo.com", null)))
                .isInstanceOf(CorreoYaRegistradoException.class);
        assertThat(propietarioRepository.todos()).hasSize(1);
    }

    @Test
    void permiteGuardarPropietariosConCorreosDistintos() {
        propietarioRepository.guardar(new Propietario(null, "Ana Perez", "ana@correo.com", null));

        Propietario guardado = propietarioRepository.guardar(
                new Propietario(null, "Luis Mora", "luis@correo.com", null));

        assertThat(guardado.id()).isNotNull();
        assertThat(propietarioRepository.todos()).hasSize(2);
    }
}
