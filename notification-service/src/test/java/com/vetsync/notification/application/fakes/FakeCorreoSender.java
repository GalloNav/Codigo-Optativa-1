package com.vetsync.notification.application.fakes;

import com.vetsync.notification.application.port.out.CorreoSender;
import java.util.ArrayList;
import java.util.List;

/** Fake de {@link CorreoSender}: no envía nada, solo recuerda los correos que le pidieron enviar. */
public class FakeCorreoSender implements CorreoSender {

    /** Un correo que se pidió enviar. */
    public record CorreoEnviado(String destinatario, String asunto, String cuerpo) {
    }

    private final List<CorreoEnviado> enviados = new ArrayList<>();

    @Override
    public void enviar(String destinatario, String asunto, String cuerpo) {
        enviados.add(new CorreoEnviado(destinatario, asunto, cuerpo));
    }

    public List<CorreoEnviado> enviados() {
        return List.copyOf(enviados);
    }
}
