package com.vetsync.clinic.application.fakes;

import com.vetsync.clinic.application.port.in.ConsultarMascotaUseCase;
import com.vetsync.clinic.domain.MascotaConPropietario;

/** Fake de {@link ConsultarMascotaUseCase} para probar adaptadores de entrada. */
public class FakeConsultarMascotaUseCase implements ConsultarMascotaUseCase {

    private MascotaConPropietario respuesta;
    private RuntimeException fallo;
    private Long ultimoMascotaId;

    @Override
    public MascotaConPropietario consultarMascota(long mascotaId) {
        ultimoMascotaId = mascotaId;
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

    /** El último id consultado, o null si nunca se invocó. */
    public Long ultimoMascotaId() {
        return ultimoMascotaId;
    }

    public void reiniciar() {
        respuesta = null;
        fallo = null;
        ultimoMascotaId = null;
    }
}
