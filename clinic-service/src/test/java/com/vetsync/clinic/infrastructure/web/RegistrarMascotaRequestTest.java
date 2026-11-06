package com.vetsync.clinic.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** Largos de {@code POST /propietarios/{id}/mascotas}: los de las columnas nombre (80), especie (40) y raza (60). */
class RegistrarMascotaRequestTest {

    @Test
    void aceptaLosLargosMaximosDeLasColumnas() {
        assertThatCode(() -> new RegistrarMascotaRequest("n".repeat(80), "e".repeat(40), "r".repeat(60), null, null))
                .doesNotThrowAnyException();
    }

    @Test
    void aceptaUnaMascotaSinRazaSexoNiFechaDeNacimiento() {
        assertThatCode(() -> new RegistrarMascotaRequest("Rex", "Perro", null, null, null))
                .doesNotThrowAnyException();
    }

    @Test
    void rechazaUnNombreDeMasDe80Caracteres() {
        assertThatThrownBy(() -> new RegistrarMascotaRequest("n".repeat(81), "Perro", null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nombre");
    }

    @Test
    void rechazaUnaEspecieDeMasDe40Caracteres() {
        assertThatThrownBy(() -> new RegistrarMascotaRequest("Rex", "e".repeat(41), null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("especie");
    }

    @Test
    void rechazaUnaRazaDeMasDe60Caracteres() {
        assertThatThrownBy(() -> new RegistrarMascotaRequest("Rex", "Perro", "r".repeat(61), null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("raza");
    }

    @Test
    void rechazaCaracteresDeControlEnLosTextos() {
        assertThatThrownBy(() -> new RegistrarMascotaRequest("Rex\nINFO falso", "Perro", null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nombre");
        assertThatThrownBy(() -> new RegistrarMascotaRequest("Rex", "Pe\rrro", null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("especie");
        assertThatThrownBy(() -> new RegistrarMascotaRequest("Rex", "Perro", "Mesti\tzo", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("raza");
    }
}
