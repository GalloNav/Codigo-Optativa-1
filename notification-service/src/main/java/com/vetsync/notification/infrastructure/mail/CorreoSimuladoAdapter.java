package com.vetsync.notification.infrastructure.mail;

import com.vetsync.notification.application.port.out.CorreoSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adaptador de {@link CorreoSender} que simula el envío: deja el correo en el log en vez de mandarlo.
 * Nombres y motivos vienen de fuera, así que se sustituyen los caracteres de control por espacios: nada de lo que
 * se escriba en una cita puede partir ni falsificar una línea del log.
 */
public class CorreoSimuladoAdapter implements CorreoSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(CorreoSimuladoAdapter.class);

    @Override
    public void enviar(String destinatario, String asunto, String cuerpo) {
        LOGGER.info("Correo simulado para {} | Asunto: {} | {}", limpiar(destinatario), limpiar(asunto), limpiar(cuerpo));
    }

    private static String limpiar(String texto) {
        return String.valueOf(texto).codePoints()
                .map(codigo -> Character.isISOControl(codigo) ? ' ' : codigo)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }
}
