package com.vetsync.clinic.infrastructure.web;

import com.vetsync.clinic.application.port.in.RegistrarMascotaComando;
import com.vetsync.clinic.domain.Sexo;
import java.time.LocalDate;

/**
 * Cuerpo de {@code POST /propietarios/{id}/mascotas}. Nombre (máximo 80 caracteres) y especie (máximo 40) son
 * obligatorios; raza (máximo 60), sexo y fecha de nacimiento son opcionales. Los límites reflejan las columnas de
 * la base de datos. Si el cuerpo no cumple, la deserialización falla y Spring responde 400.
 */
public record RegistrarMascotaRequest(
        String nombre,
        String especie,
        String raza,
        Sexo sexo,
        LocalDate fechaNacimiento) {

    private static final int LONGITUD_MAXIMA_DEL_NOMBRE = 80;
    private static final int LONGITUD_MAXIMA_DE_LA_ESPECIE = 40;
    private static final int LONGITUD_MAXIMA_DE_LA_RAZA = 60;

    public RegistrarMascotaRequest {
        ValidacionDeTexto.requerirLinea(nombre, "nombre", LONGITUD_MAXIMA_DEL_NOMBRE);
        ValidacionDeTexto.requerirLinea(especie, "especie", LONGITUD_MAXIMA_DE_LA_ESPECIE);
        ValidacionDeTexto.admitirLinea(raza, "raza", LONGITUD_MAXIMA_DE_LA_RAZA);
    }

    public RegistrarMascotaComando aComando(long propietarioId) {
        return new RegistrarMascotaComando(propietarioId, nombre, especie, raza, sexo, fechaNacimiento);
    }
}
