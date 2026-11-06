package com.vetsync.clinic.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Reglas de los textos de entrada: largo de la columna y sin caracteres de control (log injection). */
class ValidacionDeTextoTest {

    private static final String CAMPO = "nombre";
    private static final int MAXIMO = 10;
    private static final String EN_EL_MAXIMO = "a".repeat(MAXIMO);
    private static final String SOBRE_EL_MAXIMO = "a".repeat(MAXIMO + 1);

    @Test
    void requerirLineaAceptaUnTextoDeExactamenteElLargoMaximo() {
        assertThatCode(() -> ValidacionDeTexto.requerirLinea(EN_EL_MAXIMO, CAMPO, MAXIMO)).doesNotThrowAnyException();
    }

    @Test
    void requerirLineaRechazaUnTextoMasLargoQueElMaximo() {
        assertThatThrownBy(() -> ValidacionDeTexto.requerirLinea(SOBRE_EL_MAXIMO, CAMPO, MAXIMO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(CAMPO)
                .hasMessageContaining(String.valueOf(MAXIMO));
    }

    @Test
    void requerirLineaRechazaUnTextoNulo() {
        assertThatThrownBy(() -> ValidacionDeTexto.requerirLinea(null, CAMPO, MAXIMO))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining(CAMPO);
    }

    @Test
    void requerirLineaRechazaUnTextoEnBlanco() {
        assertThatThrownBy(() -> ValidacionDeTexto.requerirLinea("   ", CAMPO, MAXIMO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(CAMPO);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Ana\nPerez", "Ana\rPerez", "Ana\tPerez", "Ana\0", "\033[31mAna"})
    void requerirLineaRechazaCaracteresDeControl(String texto) {
        assertThatThrownBy(() -> ValidacionDeTexto.requerirLinea(texto, CAMPO, MAXIMO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(CAMPO)
                .hasMessageContaining("control");
    }

    @Test
    void admitirLineaAceptaUnTextoNuloOEnBlancoPorqueEsOpcional() {
        assertThatCode(() -> ValidacionDeTexto.admitirLinea(null, CAMPO, MAXIMO)).doesNotThrowAnyException();
        assertThatCode(() -> ValidacionDeTexto.admitirLinea("  ", CAMPO, MAXIMO)).doesNotThrowAnyException();
    }

    @Test
    void admitirLineaRechazaUnTextoMasLargoQueElMaximo() {
        assertThatCode(() -> ValidacionDeTexto.admitirLinea(EN_EL_MAXIMO, CAMPO, MAXIMO)).doesNotThrowAnyException();
        assertThatThrownBy(() -> ValidacionDeTexto.admitirLinea(SOBRE_EL_MAXIMO, CAMPO, MAXIMO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(CAMPO);
    }

    @ParameterizedTest
    @ValueSource(strings = {"555\n1234", "555\r1234", "555\0001234"})
    void admitirLineaRechazaCaracteresDeControl(String texto) {
        assertThatThrownBy(() -> ValidacionDeTexto.admitirLinea(texto, CAMPO, 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("control");
    }

    @Test
    void admitirParrafoAceptaSaltosDeLineaYTabuladores() {
        assertThatCode(() -> ValidacionDeTexto.admitirParrafo("Linea 1\r\nLinea 2\n\tSangria", CAMPO, 100))
                .doesNotThrowAnyException();
        assertThatCode(() -> ValidacionDeTexto.admitirParrafo(null, CAMPO, 100)).doesNotThrowAnyException();
    }

    @Test
    void admitirParrafoRechazaUnTextoMasLargoQueElMaximo() {
        assertThatCode(() -> ValidacionDeTexto.admitirParrafo(EN_EL_MAXIMO, CAMPO, MAXIMO)).doesNotThrowAnyException();
        assertThatThrownBy(() -> ValidacionDeTexto.admitirParrafo(SOBRE_EL_MAXIMO, CAMPO, MAXIMO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(CAMPO);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Dosis\0", "\033[31mDosis", "Dosis\u0007"})
    void admitirParrafoRechazaLosOtrosCaracteresDeControl(String texto) {
        assertThatThrownBy(() -> ValidacionDeTexto.admitirParrafo(texto, CAMPO, 100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("control");
    }
}
