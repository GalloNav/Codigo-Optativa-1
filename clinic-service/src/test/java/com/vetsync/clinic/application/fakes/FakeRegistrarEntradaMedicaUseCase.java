package com.vetsync.clinic.application.fakes;

import com.vetsync.clinic.application.port.in.RegistrarEntradaMedicaComando;
import com.vetsync.clinic.application.port.in.RegistrarEntradaMedicaUseCase;
import com.vetsync.clinic.domain.RegistroMedico;

/** Fake de {@link RegistrarEntradaMedicaUseCase} para probar adaptadores de entrada. */
public class FakeRegistrarEntradaMedicaUseCase implements RegistrarEntradaMedicaUseCase {

    private RegistroMedico respuesta;
    private RuntimeException fallo;
    private RegistrarEntradaMedicaComando ultimoComando;

    @Override
    public RegistroMedico registrarEntradaMedica(RegistrarEntradaMedicaComando comando) {
        ultimoComando = comando;
        if (fallo != null) {
            throw fallo;
        }
        return respuesta;
    }

    public void responderCon(RegistroMedico registro) {
        respuesta = registro;
    }

    public void fallarCon(RuntimeException error) {
        fallo = error;
    }

    /** El último comando recibido, o null si nunca se invocó. */
    public RegistrarEntradaMedicaComando ultimoComando() {
        return ultimoComando;
    }

    public void reiniciar() {
        respuesta = null;
        fallo = null;
        ultimoComando = null;
    }
}
