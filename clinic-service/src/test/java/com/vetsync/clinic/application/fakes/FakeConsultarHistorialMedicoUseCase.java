package com.vetsync.clinic.application.fakes;

import com.vetsync.clinic.application.port.in.ConsultarHistorialMedicoUseCase;
import com.vetsync.clinic.domain.RegistroMedico;
import java.util.List;

/** Fake de {@link ConsultarHistorialMedicoUseCase} para probar adaptadores de entrada. */
public class FakeConsultarHistorialMedicoUseCase implements ConsultarHistorialMedicoUseCase {

    private List<RegistroMedico> respuesta = List.of();
    private RuntimeException fallo;
    private Long ultimoMascotaId;

    @Override
    public List<RegistroMedico> consultarHistorial(long mascotaId) {
        ultimoMascotaId = mascotaId;
        if (fallo != null) {
            throw fallo;
        }
        return respuesta;
    }

    public void responderCon(List<RegistroMedico> registros) {
        respuesta = registros;
    }

    public void fallarCon(RuntimeException error) {
        fallo = error;
    }

    /** El último id consultado, o null si nunca se invocó. */
    public Long ultimoMascotaId() {
        return ultimoMascotaId;
    }

    public void reiniciar() {
        respuesta = List.of();
        fallo = null;
        ultimoMascotaId = null;
    }
}
