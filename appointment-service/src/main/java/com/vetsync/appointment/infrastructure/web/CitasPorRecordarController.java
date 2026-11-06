package com.vetsync.appointment.infrastructure.web;

import com.vetsync.appointment.application.port.in.ListarCitasPorRecordarUseCase;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Adaptador de entrada HTTP de la consulta de citas por recordar (la consume notification-service). */
@RestController
public class CitasPorRecordarController {

    private final ListarCitasPorRecordarUseCase listarCitasPorRecordar;

    public CitasPorRecordarController(ListarCitasPorRecordarUseCase listarCitasPorRecordar) {
        this.listarCitasPorRecordar = listarCitasPorRecordar;
    }

    @GetMapping("/citas/por-recordar")
    public List<CitaPorRecordarResponse> listar(
            @RequestParam("desde") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam("hasta") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {
        return listarCitasPorRecordar.listarCitasPorRecordar(desde, hasta).stream()
                .map(CitaPorRecordarResponse::desde)
                .toList();
    }
}
