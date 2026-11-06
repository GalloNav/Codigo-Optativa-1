package com.vetsync.clinic.infrastructure.web;

import com.vetsync.clinic.application.port.out.AccesoADatosException;
import com.vetsync.clinic.domain.CorreoYaRegistradoException;
import com.vetsync.clinic.domain.MascotaNoEncontradaException;
import com.vetsync.clinic.domain.PropietarioNoRegistradoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Único punto que traduce excepciones a respuestas HTTP (ProblemDetail, RFC 7807).
 * Las excepciones estándar de Spring MVC (cuerpo ilegible, método no permitido...) las resuelve la clase base.
 * Los errores técnicos e inesperados se registran aquí y nunca se detallan en la respuesta.
 */
@RestControllerAdvice
public class ManejadorDeErrores extends ResponseEntityExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManejadorDeErrores.class);

    @ExceptionHandler(CorreoYaRegistradoException.class)
    public ProblemDetail alRepetirseElCorreo(CorreoYaRegistradoException excepcion) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, excepcion.getMessage());
    }

    @ExceptionHandler(PropietarioNoRegistradoException.class)
    public ProblemDetail alNoEstarRegistradoElPropietario(PropietarioNoRegistradoException excepcion) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, excepcion.getMessage());
    }

    @ExceptionHandler(MascotaNoEncontradaException.class)
    public ProblemDetail alNoExistirLaMascota(MascotaNoEncontradaException excepcion) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, excepcion.getMessage());
    }

    @ExceptionHandler(AccesoADatosException.class)
    public ProblemDetail alFallarElAccesoADatos(AccesoADatosException excepcion) {
        LOGGER.error("Fallo de acceso a datos", excepcion);
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "El servicio no esta disponible en este momento; intenta de nuevo mas tarde");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail alOcurrirUnErrorInesperado(Exception excepcion) {
        LOGGER.error("Error inesperado", excepcion);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrio un error inesperado");
    }
}
