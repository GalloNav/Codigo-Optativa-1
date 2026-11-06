package com.vetsync.notification.application.fakes;

import com.vetsync.notification.application.port.out.AgendaGateway;
import com.vetsync.notification.domain.CitaPorRecordar;
import java.time.LocalDateTime;
import java.util.List;

/** Fake de {@link AgendaGateway}: devuelve las citas que la prueba configura y recuerda la ventana consultada. */
public class FakeAgendaGateway implements AgendaGateway {

    private List<CitaPorRecordar> citas = List.of();
    private LocalDateTime ultimoDesde;
    private LocalDateTime ultimoHasta;

    @Override
    public List<CitaPorRecordar> buscarCitasPorRecordar(LocalDateTime desde, LocalDateTime hasta) {
        ultimoDesde = desde;
        ultimoHasta = hasta;
        return citas;
    }

    public void responderCon(List<CitaPorRecordar> citasPorRecordar) {
        citas = citasPorRecordar;
    }

    /** El último inicio de ventana consultado, o null si nunca se consultó. */
    public LocalDateTime ultimoDesde() {
        return ultimoDesde;
    }

    /** El último fin de ventana consultado, o null si nunca se consultó. */
    public LocalDateTime ultimoHasta() {
        return ultimoHasta;
    }

    public void reiniciar() {
        citas = List.of();
        ultimoDesde = null;
        ultimoHasta = null;
    }
}
