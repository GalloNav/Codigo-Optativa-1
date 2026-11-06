package com.vetsync.appointment.application.fakes;

import com.vetsync.appointment.application.port.out.ClinicaGateway;
import com.vetsync.appointment.application.port.out.ContactoDeMascota;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Fake de {@link ClinicaGateway}: solo conoce las mascotas que la prueba registra. */
public class FakeClinicaGateway implements ClinicaGateway {

    private final Map<Long, ContactoDeMascota> mascotas = new HashMap<>();

    /** Solo para preparar el escenario de las pruebas. */
    public void registrar(long mascotaId, ContactoDeMascota contacto) {
        mascotas.put(mascotaId, contacto);
    }

    @Override
    public Optional<ContactoDeMascota> buscarContactoDeMascota(long mascotaId) {
        return Optional.ofNullable(mascotas.get(mascotaId));
    }
}
