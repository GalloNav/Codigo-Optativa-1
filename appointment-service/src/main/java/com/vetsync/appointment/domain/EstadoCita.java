package com.vetsync.appointment.domain;

/** Estado de una cita. Solo las {@code PROGRAMADA} ocupan la franja del veterinario. */
public enum EstadoCita {
    PROGRAMADA,
    CANCELADA
}
