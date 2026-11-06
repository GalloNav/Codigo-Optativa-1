package com.vetsync.appointment.application.fakes;

import com.vetsync.appointment.application.port.in.ListarCitasPorRecordarUseCase;
import com.vetsync.appointment.domain.Cita;
import java.time.LocalDateTime;
import java.util.List;

/** Fake de {@link ListarCitasPorRecordarUseCase} para probar adaptadores de entrada. */
public class FakeListarCitasPorRecordarUseCase implements ListarCitasPorRecordarUseCase {

    private List<Cita> respuesta = List.of();
    private RuntimeException fallo;
    private LocalDateTime ultimoDesde;
    private LocalDateTime ultimoHasta;

    @Override
    public List<Cita> listarCitasPorRecordar(LocalDateTime desde, LocalDateTime hasta) {
        ultimoDesde = desde;
        ultimoHasta = hasta;
        if (fallo != null) {
            throw fallo;
        }
        return respuesta;
    }

    public void responderCon(List<Cita> citas) {
        respuesta = citas;
    }

    public void fallarCon(RuntimeException error) {
        fallo = error;
    }

    /** El último inicio de rango recibido, o null si nunca se invocó. */
    public LocalDateTime ultimoDesde() {
        return ultimoDesde;
    }

    /** El último fin de rango recibido, o null si nunca se invocó. */
    public LocalDateTime ultimoHasta() {
        return ultimoHasta;
    }

    public void reiniciar() {
        respuesta = List.of();
        fallo = null;
        ultimoDesde = null;
        ultimoHasta = null;
    }
}
