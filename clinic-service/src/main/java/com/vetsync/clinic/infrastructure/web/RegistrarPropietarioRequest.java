package com.vetsync.clinic.infrastructure.web;

import com.vetsync.clinic.application.port.in.RegistrarPropietarioComando;
import java.util.regex.Pattern;

/**
 * Cuerpo de {@code POST /propietarios}. Nombre (máximo 100 caracteres) y correo (máximo 150) son obligatorios y el
 * teléfono (máximo 20) es opcional. Los límites reflejan las columnas de la base de datos. Si el cuerpo no cumple,
 * la deserialización falla y Spring responde 400.
 */
public record RegistrarPropietarioRequest(String nombre, String correo, String telefono) {

    private static final int LONGITUD_MAXIMA_DEL_NOMBRE = 100;
    private static final int LONGITUD_MAXIMA_DEL_CORREO = 150;
    private static final int LONGITUD_MAXIMA_DEL_TELEFONO = 20;

    /** Los segmentos del dominio no contienen puntos, así que el patrón no retrocede de forma cuadrática. */
    private static final Pattern CORREO = Pattern.compile("^[^@\\s]+@[^@\\s.]+(\\.[^@\\s.]+)+$");

    public RegistrarPropietarioRequest {
        ValidacionDeTexto.requerirLinea(nombre, "nombre", LONGITUD_MAXIMA_DEL_NOMBRE);
        // El largo se acota antes de aplicar el patrón para limitar su coste ante entradas hostiles.
        ValidacionDeTexto.requerirLinea(correo, "correo", LONGITUD_MAXIMA_DEL_CORREO);
        ValidacionDeTexto.admitirLinea(telefono, "telefono", LONGITUD_MAXIMA_DEL_TELEFONO);
        if (!CORREO.matcher(correo).matches()) {
            throw new IllegalArgumentException("correo no tiene un formato valido");
        }
    }

    public RegistrarPropietarioComando aComando() {
        return new RegistrarPropietarioComando(nombre, correo, telefono);
    }
}
