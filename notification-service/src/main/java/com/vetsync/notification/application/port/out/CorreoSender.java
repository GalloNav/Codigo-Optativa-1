package com.vetsync.notification.application.port.out;

/** Puerto de salida para enviar correos. La implementación actual es simulada. */
public interface CorreoSender {

    void enviar(String destinatario, String asunto, String cuerpo);
}
