package com.vetsync.clinic.domain;

/** Dueño de una o más mascotas. El {@code id} es nulo hasta que se persiste; el teléfono es opcional. */
public record Propietario(Long id, String nombre, String correo, String telefono) {
}
