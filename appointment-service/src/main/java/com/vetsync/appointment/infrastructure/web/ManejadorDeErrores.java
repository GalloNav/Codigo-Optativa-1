package com.vetsync.appointment.infrastructure.web;

import com.vetsync.appointment.application.port.out.AccesoADatosException;
import com.vetsync.appointment.domain.ConflictoDeHorarioException;
import com.vetsync.appointment.domain.FranjaHorariaInvalidaException;
import com.vetsync.appointment.domain.MascotaNoRegistradaException;
import com.vetsync.appointment.domain.RangoDeFechasInvalidoException;
import com.vetsync.appointment.domain.VeterinarioNoEncontradoException;
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

    @ExceptionHandler(ConflictoDeHorarioException.class)
    public ProblemDetail alHaberConflictoDeHorario(ConflictoDeHorarioException excepcion) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, excepcion.getMessage());
    }

    @ExceptionHandler(FranjaHorariaInvalidaException.class)
    public ProblemDetail alSerInvalidaLaFranja(FranjaHorariaInvalidaException excepcion) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, excepcion.getMessage());
    }

    @ExceptionHandler(MascotaNoRegistradaException.class)
    public ProblemDetail alNoEstarRegistradaLaMascota(MascotaNoRegistradaException excepcion) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, excepcion.getMessage());
    }

    @ExceptionHandler(VeterinarioNoEncontradoException.class)
    public ProblemDetail alNoEstarRegistradoElVeterinario(VeterinarioNoEncontradoException excepcion) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, excepcion.getMessage());
    }

    @ExceptionHandler(RangoDeFechasInvalidoException.class)
    public ProblemDetail alSerInvalidoElRangoDeFechas(RangoDeFechasInvalidoException excepcion) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, excepcion.getMessage());
    }

    @ExceptionHandler(AccesoADatosException.class)
    public ProblemDetail alFallarElAccesoADatos(AccesoADatosException excepcion) {
        LOGGER.error("Fallo de acceso a datos o a otro servicio", excepcion);
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "El servicio no esta disponible en este momento; intenta de nuevo mas tarde");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail alOcurrirUnErrorInesperado(Exception excepcion) {
        LOGGER.error("Error inesperado", excepcion);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrio un error inesperado");
    }
}
