package com.vetsync.clinic.domain;

/** Tipo de entrada del historial médico; coincide con la restricción CK_registro_tipo de la base de datos. */
public enum TipoRegistro {
    CONSULTA,
    VACUNA,
    CIRUGIA,
    DESPARASITACION,
    OTRO
}
