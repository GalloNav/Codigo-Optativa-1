package com.vetsync.notification.infrastructure.web;

import com.vetsync.notification.application.port.in.ListarNotificacionesUseCase;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Adaptador de entrada HTTP para consultar los recordatorios enviados: el buzón del correo simulado. */
@RestController
@RequestMapping("/notificaciones")
public class NotificacionController {

    /** Cuántos recordatorios se devuelven si la petición no indica {@code limite}. */
    private static final String LIMITE_POR_DEFECTO = "100";

    private final ListarNotificacionesUseCase listarNotificaciones;

    public NotificacionController(ListarNotificacionesUseCase listarNotificaciones) {
        this.listarNotificaciones = listarNotificaciones;
    }

    @GetMapping
    public List<NotificacionResponse> listar(
            @RequestParam(name = "limite", defaultValue = LIMITE_POR_DEFECTO) int limite) {
        return listarNotificaciones.listarNotificaciones(limite).stream()
                .map(NotificacionResponse::desde)
                .toList();
    }
}
