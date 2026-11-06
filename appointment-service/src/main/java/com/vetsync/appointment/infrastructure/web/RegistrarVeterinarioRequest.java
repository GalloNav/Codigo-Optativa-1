package com.vetsync.appointment.infrastructure.web;

import com.vetsync.appointment.application.port.in.RegistrarVeterinarioComando;
import java.util.Objects;

/**
 * Cuerpo de {@code POST /veterinarios}. El nombre es obligatorio (máximo 100 caracteres) y la especialidad es
 * opcional (máximo 60). Los límites reflejan las columnas de la base de datos. Si el cuerpo no cumple,
 * la deserialización falla y Spring responde 400.
 */
public record RegistrarVeterinarioRequest(String nombre, String especialidad) {

    private static final int LONGITUD_MAXIMA_DEL_NOMBRE = 100;
    private static final int LONGITUD_MAXIMA_DE_LA_ESPECIALIDAD = 60;

    public RegistrarVeterinarioRequest {
        Objects.requireNonNull(nombre, "nombre es obligatorio");
        if (nombre.isBlank()) {
            throw new IllegalArgumentException("nombre no puede estar vacio");
        }
        if (nombre.length() > LONGITUD_MAXIMA_DEL_NOMBRE) {
            throw new IllegalArgumentException("nombre no puede superar " + LONGITUD_MAXIMA_DEL_NOMBRE + " caracteres");
        }
        if (especialidad != null && especialidad.isBlank()) {
            throw new IllegalArgumentException("especialidad no puede estar vacia si se indica");
        }
        if (especialidad != null && especialidad.length() > LONGITUD_MAXIMA_DE_LA_ESPECIALIDAD) {
            throw new IllegalArgumentException(
                    "especialidad no puede superar " + LONGITUD_MAXIMA_DE_LA_ESPECIALIDAD + " caracteres");
        }
    }

    public RegistrarVeterinarioComando aComando() {
        return new RegistrarVeterinarioComando(nombre, especialidad);
    }
}
