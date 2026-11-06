package com.vetsync.notification.application.port.out;

import com.vetsync.notification.domain.CitaPorRecordar;
import java.time.LocalDateTime;
import java.util.List;

/** Puerto de salida hacia appointment-service. */
public interface AgendaGateway {

    /** Citas programadas que empiezan entre {@code desde} y {@code hasta}, ambos incluidos. */
    List<CitaPorRecordar> buscarCitasPorRecordar(LocalDateTime desde, LocalDateTime hasta);
}
