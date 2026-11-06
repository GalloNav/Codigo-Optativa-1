package com.vetsync.clinic.application.fakes;

import com.vetsync.clinic.application.port.in.RegistrarMascotaComando;
import com.vetsync.clinic.application.port.in.RegistrarMascotaUseCase;
import com.vetsync.clinic.domain.MascotaConPropietario;

/** Fake de {@link RegistrarMascotaUseCase} para probar adaptadores de entrada. */
public class FakeRegistrarMascotaUseCase implements RegistrarMascotaUseCase {

    private MascotaConPropietario respuesta;
    private RuntimeException fallo;
    private RegistrarMascotaComando ultimoComando;

    @Override
    public MascotaConPropietario registrarMascota(RegistrarMascotaComando comando) {
        ultimoComando = comando;
        if (fallo != null) {
            throw fallo;
        }
        return respuesta;
    }

    public void responderCon(MascotaConPropietario mascota) {
        respuesta = mascota;
    }

    public void fallarCon(RuntimeException error) {
        fallo = error;
    }

    /** El último comando recibido, o null si nunca se invocó. */
    public RegistrarMascotaComando ultimoComando() {
        return ultimoComando;
    }

    public void reiniciar() {
        respuesta = null;
        fallo = null;
        ultimoComando = null;
    }
}
