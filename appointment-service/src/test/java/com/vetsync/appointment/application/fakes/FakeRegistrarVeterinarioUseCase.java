package com.vetsync.appointment.application.fakes;

import com.vetsync.appointment.application.port.in.RegistrarVeterinarioComando;
import com.vetsync.appointment.application.port.in.RegistrarVeterinarioUseCase;
import com.vetsync.appointment.domain.Veterinario;

/** Fake de {@link RegistrarVeterinarioUseCase} para probar adaptadores de entrada. */
public class FakeRegistrarVeterinarioUseCase implements RegistrarVeterinarioUseCase {

    private Veterinario respuesta;
    private RegistrarVeterinarioComando ultimoComando;

    @Override
    public Veterinario registrarVeterinario(RegistrarVeterinarioComando comando) {
        ultimoComando = comando;
        return respuesta;
    }

    public void responderCon(Veterinario veterinario) {
        respuesta = veterinario;
    }

    /** El último comando recibido, o null si nunca se invocó. */
    public RegistrarVeterinarioComando ultimoComando() {
        return ultimoComando;
    }

    public void reiniciar() {
        respuesta = null;
        ultimoComando = null;
    }
}
