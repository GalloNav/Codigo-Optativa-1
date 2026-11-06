package com.vetsync.appointment.domain;

/** Veterinario de la clínica. El {@code id} es nulo hasta que se persiste; la especialidad es opcional. */
public record Veterinario(Long id, String nombre, String especialidad) {
}
