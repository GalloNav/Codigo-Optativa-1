package com.vetsync.appointment.application.port.in;

/** Datos para registrar un veterinario; la especialidad es opcional. */
public record RegistrarVeterinarioComando(String nombre, String especialidad) {
}
