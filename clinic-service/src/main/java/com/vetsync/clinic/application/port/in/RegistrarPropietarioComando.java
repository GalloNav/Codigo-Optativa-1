package com.vetsync.clinic.application.port.in;

/** Datos para registrar un propietario; el teléfono es opcional. */
public record RegistrarPropietarioComando(String nombre, String correo, String telefono) {
}
