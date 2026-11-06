package com.vetsync.appointment.application.fakes;

import com.vetsync.appointment.application.port.in.ListarVeterinariosUseCase;
import com.vetsync.appointment.domain.Veterinario;
import java.util.List;

/** Fake de {@link ListarVeterinariosUseCase} para probar adaptadores de entrada. */
public class FakeListarVeterinariosUseCase implements ListarVeterinariosUseCase {

    private List<Veterinario> respuesta = List.of();

    @Override
    public List<Veterinario> listarVeterinarios() {
        return respuesta;
    }

    public void responderCon(List<Veterinario> veterinarios) {
        respuesta = veterinarios;
    }

    public void reiniciar() {
        respuesta = List.of();
    }
}
