package com.vetsync.notification.application.fakes;

import com.vetsync.notification.application.port.out.NotificacionRepository;
import com.vetsync.notification.domain.Notificacion;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Fake en memoria de {@link NotificacionRepository}; el listado respeta el orden y el límite del contrato. */
public class InMemoryNotificacionRepository implements NotificacionRepository {

    private final List<Notificacion> notificaciones = new ArrayList<>();
    private final Map<Long, RuntimeException> fallosAlGuardar = new HashMap<>();
    private long ultimoId;
    private int consultasDeCitasNotificadas;

    @Override
    public Notificacion guardar(Notificacion notificacion) {
        RuntimeException fallo = fallosAlGuardar.get(notificacion.citaId());
        if (fallo != null) {
            throw fallo;
        }
        ultimoId++;
        Notificacion guardada = new Notificacion(ultimoId, notificacion.citaId(), notificacion.destinatario(),
                notificacion.asunto(), notificacion.cuerpo(), notificacion.enviadaEn());
        notificaciones.add(guardada);
        return guardada;
    }

    @Override
    public Set<Long> citasYaNotificadas(Collection<Long> citaIds) {
        consultasDeCitasNotificadas++;
        return notificaciones.stream()
                .map(Notificacion::citaId)
                .filter(citaIds::contains)
                .collect(Collectors.toSet());
    }

    @Override
    public List<Notificacion> buscarRecientes(int limite) {
        return notificaciones.stream()
                .sorted(Comparator.comparing(Notificacion::enviadaEn).thenComparing(Notificacion::id).reversed())
                .limit(limite)
                .toList();
    }

    /** Solo para las pruebas: hace que guardar la notificación de esa cita falle con el error indicado. */
    public void fallarAlGuardarLaCita(long citaId, RuntimeException fallo) {
        fallosAlGuardar.put(citaId, fallo);
    }

    /** Solo para las aserciones de las pruebas: cuántas veces se preguntó qué citas ya recibieron su recordatorio. */
    public int consultasDeCitasNotificadas() {
        return consultasDeCitasNotificadas;
    }

    /** Solo para las aserciones de las pruebas: todo lo que se ha guardado, en orden de inserción. */
    public List<Notificacion> todas() {
        return List.copyOf(notificaciones);
    }
}
