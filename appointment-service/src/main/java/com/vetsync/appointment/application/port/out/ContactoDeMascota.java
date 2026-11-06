package com.vetsync.appointment.application.port.out;

/** Datos que clinic-service devuelve de una mascota y del dueño al que se le avisará. */
public record ContactoDeMascota(String mascotaNombre, String contactoNombre, String contactoCorreo) {
}
