package com.vetsync.notification.application.service;

import com.vetsync.notification.application.port.in.ListarNotificacionesUseCase;
import com.vetsync.notification.application.port.out.NotificacionRepository;
import com.vetsync.notification.domain.LimiteDeListadoInvalidoException;
import com.vetsync.notification.domain.Notificacion;
import java.util.List;

/**
 * Lista los recordatorios enviados más recientes. El límite se acota a {@value #LIMITE_MAXIMO}: la tabla crece
 * sin fin y cada fila lleva el cuerpo completo del correo.
 */
public class ListarNotificacionesService implements ListarNotificacionesUseCase {

    static final int LIMITE_MAXIMO = 500;

    private final NotificacionRepository notificacionRepository;

    public ListarNotificacionesService(NotificacionRepository notificacionRepository) {
        this.notificacionRepository = notificacionRepository;
    }

    @Override
    public List<Notificacion> listarNotificaciones(int limite) {
        if (limite < 1 || limite > LIMITE_MAXIMO) {
            throw new LimiteDeListadoInvalidoException("El limite debe estar entre 1 y " + LIMITE_MAXIMO);
        }
        return notificacionRepository.buscarRecientes(limite);
    }
}
