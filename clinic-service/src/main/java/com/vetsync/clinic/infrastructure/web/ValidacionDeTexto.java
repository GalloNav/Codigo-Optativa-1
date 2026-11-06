package com.vetsync.clinic.infrastructure.web;

import java.util.Objects;

/**
 * Reglas comunes de los textos que llegan por HTTP. Los largos reflejan las columnas de la base de datos, para
 * rechazar con un 400 lo que la base rechazaría con un error técnico. Los caracteres de control se rechazan en
 * origen: un salto de línea en un nombre permitiría falsificar líneas en los logs.
 */
final class ValidacionDeTexto {

    private ValidacionDeTexto() {
    }

    /** Texto obligatorio de una sola línea. */
    static void requerirLinea(String valor, String campo, int longitudMaxima) {
        Objects.requireNonNull(valor, campo + " es obligatorio");
        if (valor.isBlank()) {
            throw new IllegalArgumentException(campo + " no puede estar vacio");
        }
        validarLinea(valor, campo, longitudMaxima);
    }

    /** Texto opcional de una sola línea: si viene, respeta el largo y no lleva caracteres de control. */
    static void admitirLinea(String valor, String campo, int longitudMaxima) {
        if (valor != null) {
            validarLinea(valor, campo, longitudMaxima);
        }
    }

    /** Texto opcional de varias líneas: admite saltos de línea y tabuladores, pero no otros caracteres de control. */
    static void admitirParrafo(String valor, String campo, int longitudMaxima) {
        if (valor == null) {
            return;
        }
        validarLargo(valor, campo, longitudMaxima);
        if (valor.codePoints().anyMatch(ValidacionDeTexto::esControlNoPermitidoEnUnParrafo)) {
            throw new IllegalArgumentException(campo + " no puede contener caracteres de control");
        }
    }

    private static void validarLinea(String valor, String campo, int longitudMaxima) {
        validarLargo(valor, campo, longitudMaxima);
        if (valor.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException(campo + " no puede contener caracteres de control");
        }
    }

    private static void validarLargo(String valor, String campo, int longitudMaxima) {
        if (valor.length() > longitudMaxima) {
            throw new IllegalArgumentException(campo + " no puede superar " + longitudMaxima + " caracteres");
        }
    }

    private static boolean esControlNoPermitidoEnUnParrafo(int codigo) {
        return Character.isISOControl(codigo) && codigo != '\n' && codigo != '\r' && codigo != '\t';
    }
}
