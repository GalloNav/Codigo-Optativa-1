package com.vetsync.appointment.application.service;

import com.vetsync.appointment.application.port.in.ListarCitasPorRecordarUseCase;
import com.vetsync.appointment.application.port.out.CitaRepository;
import com.vetsync.appointment.domain.Cita;
import com.vetsync.appointment.domain.RangoDeFechasInvalidoException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Lista las citas programadas dentro de un rango de fechas. El rango se acota a {@value #DIAS_MAXIMOS_DEL_RANGO}
 * días: la respuesta incluye el contacto de los dueños, y sin tope una sola consulta volcaría toda la agenda.
 */
public class ListarCitasPorRecordarService implements ListarCitasPorRecordarUseCase {

    /** Debe ser mayor que la anticipación de los recordatorios de notification-service. */
    static final int DIAS_MAXIMOS_DEL_RANGO = 7;

    private final CitaRepository citaRepository;

    public ListarCitasPorRecordarService(CitaRepository citaRepository) {
        this.citaRepository = citaRepository;
    }

    @Override
    public List<Cita> listarCitasPorRecordar(LocalDateTime desde, LocalDateTime hasta) {
        if (desde.isAfter(hasta)) {
            throw new RangoDeFechasInvalidoException(
                    "desde (" + desde + ") no puede ser posterior a hasta (" + hasta + ")");
        }
        if (Duration.between(desde, hasta).compareTo(Duration.ofDays(DIAS_MAXIMOS_DEL_RANGO)) > 0) {
            throw new RangoDeFechasInvalidoException(
                    "El rango no puede superar " + DIAS_MAXIMOS_DEL_RANGO + " dias");
        }
        return citaRepository.buscarProgramadasEntre(desde, hasta);
    }
}
