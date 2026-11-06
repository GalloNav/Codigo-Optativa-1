package com.vetsync.appointment.application.fakes;

import com.vetsync.appointment.application.port.in.AgendarCitaComando;
import com.vetsync.appointment.application.port.in.AgendarCitaUseCase;
import com.vetsync.appointment.domain.Cita;

/** Fake de {@link AgendarCitaUseCase} para probar adaptadores de entrada: responde o falla según la prueba. */
public class FakeAgendarCitaUseCase implements AgendarCitaUseCase {

    private Cita respuesta;
    private RuntimeException fallo;
    private AgendarCitaComando ultimoComando;

    @Override
    public Cita agendarCita(AgendarCitaComando comando) {
        ultimoComando = comando;
        if (fallo != null) {
            throw fallo;
        }
        return respuesta;
    }

    /** Solo para preparar el escenario de las pruebas. */
    public void responderCon(Cita cita) {
        respuesta = cita;
    }

    /** Solo para preparar el escenario de las pruebas. */
    public void fallarCon(RuntimeException error) {
        fallo = error;
    }

    /** Solo para las aserciones: el último comando recibido, o null si nunca se invocó. */
    public AgendarCitaComando ultimoComando() {
        return ultimoComando;
    }

    public void reiniciar() {
        respuesta = null;
        fallo = null;
        ultimoComando = null;
    }
}
