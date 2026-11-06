package com.vetsync.clinic.application.fakes;

import com.vetsync.clinic.application.port.in.RegistrarPropietarioComando;
import com.vetsync.clinic.application.port.in.RegistrarPropietarioUseCase;
import com.vetsync.clinic.domain.Propietario;

/** Fake de {@link RegistrarPropietarioUseCase} para probar adaptadores de entrada. */
public class FakeRegistrarPropietarioUseCase implements RegistrarPropietarioUseCase {

    private Propietario respuesta;
    private RuntimeException fallo;
    private RegistrarPropietarioComando ultimoComando;

    @Override
    public Propietario registrarPropietario(RegistrarPropietarioComando comando) {
        ultimoComando = comando;
        if (fallo != null) {
            throw fallo;
        }
        return respuesta;
    }

    public void responderCon(Propietario propietario) {
        respuesta = propietario;
    }

    public void fallarCon(RuntimeException error) {
        fallo = error;
    }

    /** El último comando recibido, o null si nunca se invocó. */
    public RegistrarPropietarioComando ultimoComando() {
        return ultimoComando;
    }

    public void reiniciar() {
        respuesta = null;
        fallo = null;
        ultimoComando = null;
    }
}
