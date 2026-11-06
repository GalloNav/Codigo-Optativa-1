package com.vetsync.clinic.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Validación del correo de {@code POST /propietarios}. El patrón no puede retroceder de forma cuadrática ante
 * entradas hostiles, así que el largo se acota al de la columna antes de aplicarlo.
 */
class RegistrarPropietarioRequestTest {

    private static final String DOMINIO = "@correo.com";
    private static final int LONGITUD_DE_LA_COLUMNA_CORREO = 150;

    @Test
    void aceptaUnCorreoDeExactamenteLaLongitudDeLaColumna() {
        String correo = "a".repeat(LONGITUD_DE_LA_COLUMNA_CORREO - DOMINIO.length()) + DOMINIO;

        assertThat(correo).hasSize(LONGITUD_DE_LA_COLUMNA_CORREO);
        assertThatCode(() -> new RegistrarPropietarioRequest("Ana Perez", correo, null)).doesNotThrowAnyException();
    }

    @Test
    void rechazaUnCorreoMasLargoQueLaColumna() {
        String correo = "a".repeat(LONGITUD_DE_LA_COLUMNA_CORREO - DOMINIO.length() + 1) + DOMINIO;

        assertThatThrownBy(() -> new RegistrarPropietarioRequest("Ana Perez", correo, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("correo");
    }

    @Test
    void rechazaUnDominioConPuntosConsecutivos() {
        assertThatThrownBy(() -> new RegistrarPropietarioRequest("Ana Perez", "ana@correo..com", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("correo");
    }

    @Test
    void rechazaUnNombreDeMasDe100Caracteres() {
        assertThatThrownBy(() -> new RegistrarPropietarioRequest("n".repeat(101), "ana@correo.com", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nombre");
    }

    @Test
    void rechazaUnTelefonoDeMasDe20Caracteres() {
        assertThatThrownBy(() -> new RegistrarPropietarioRequest("Ana Perez", "ana@correo.com", "5".repeat(21)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("telefono");
    }

    @Test
    void aceptaLosLargosMaximosDeNombreYTelefono() {
        assertThatCode(() -> new RegistrarPropietarioRequest("n".repeat(100), "ana@correo.com", "5".repeat(20)))
                .doesNotThrowAnyException();
    }

    @Test
    void rechazaCaracteresDeControlEnNombreYCorreo() {
        assertThatThrownBy(() -> new RegistrarPropietarioRequest("Ana\nINFO falso", "ana@correo.com", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nombre");
        assertThatThrownBy(() -> new RegistrarPropietarioRequest("Ana Perez", "ana@cor\u0007reo.com", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("correo");
    }

    @Test
    void aceptaCorreosConSubdominiosYPuntosEnLaParteLocal() {
        assertThatCode(() -> new RegistrarPropietarioRequest("Ana Perez", "ana.perez@mail.clinica.co", null))
                .doesNotThrowAnyException();
    }
}
