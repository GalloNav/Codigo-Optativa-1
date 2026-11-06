package com.vetsync.clinic.infrastructure.web;

import com.vetsync.clinic.domain.Propietario;

/** Propietario registrado, tal como lo ve el cliente de la API. */
public record PropietarioResponse(Long id, String nombre, String correo, String telefono) {

    public static PropietarioResponse desde(Propietario propietario) {
        return new PropietarioResponse(
                propietario.id(), propietario.nombre(), propietario.correo(), propietario.telefono());
    }
}
