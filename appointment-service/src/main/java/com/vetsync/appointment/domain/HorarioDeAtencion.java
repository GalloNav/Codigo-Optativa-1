package com.vetsync.appointment.domain;

import java.time.LocalDateTime;

/**
 * Horario de atención de la clínica, en horas enteras de 0 a 24. La agenda se divide en franjas fijas de
 * {@value #DURACION_FRANJA_MINUTOS} minutos alineadas a la media hora: una cita es admisible si empieza en una
 * de ellas y su franja termina a más tardar a la hora de cierre. Refleja el CHECK {@code CK_cita_franja} de la
 * base de datos, para rechazar en el dominio lo que la base rechazaría.
 */
public record HorarioDeAtencion(int horaApertura, int horaCierre) {

    /** Duración de cada franja; cambiarla exige una migración nueva (AGENTS.md, sección 7). */
    public static final int DURACION_FRANJA_MINUTOS = 30;

    private static final int HORAS_POR_DIA = 24;

    public HorarioDeAtencion {
        if (horaApertura < 0 || horaCierre > HORAS_POR_DIA || horaApertura >= horaCierre) {
            throw new IllegalArgumentException(
                    "El horario de atencion debe cumplir 0 <= apertura < cierre <= " + HORAS_POR_DIA
                            + ", pero es " + horaApertura + " a " + horaCierre);
        }
    }

    /** Indica si una cita puede empezar en {@code inicio}: franja alineada y completa dentro del horario. */
    public boolean admite(LocalDateTime inicio) {
        boolean estaAlineada = inicio.getMinute() % DURACION_FRANJA_MINUTOS == 0
                && inicio.getSecond() == 0
                && inicio.getNano() == 0;
        LocalDateTime apertura = inicio.toLocalDate().atStartOfDay().plusHours(horaApertura);
        LocalDateTime cierre = inicio.toLocalDate().atStartOfDay().plusHours(horaCierre);
        return estaAlineada
                && !inicio.isBefore(apertura)
                && !inicio.plusMinutes(DURACION_FRANJA_MINUTOS).isAfter(cierre);
    }
}
