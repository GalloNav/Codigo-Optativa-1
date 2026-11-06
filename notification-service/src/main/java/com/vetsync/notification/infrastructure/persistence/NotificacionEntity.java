package com.vetsync.notification.infrastructure.persistence;

import com.vetsync.notification.domain.Notificacion;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.Nationalized;

/** Fila de la tabla {@code notificacion}. La columna tipo no se mapea: usa su valor por defecto. */
@Entity
@Table(name = "notificacion")
public class NotificacionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private long citaId;

    @Nationalized
    private String destinatario;

    @Nationalized
    private String asunto;

    @Nationalized
    private String cuerpo;

    private LocalDateTime enviadaEn;

    protected NotificacionEntity() {
    }

    static NotificacionEntity desde(Notificacion notificacion) {
        NotificacionEntity entidad = new NotificacionEntity();
        entidad.id = notificacion.id();
        entidad.citaId = notificacion.citaId();
        entidad.destinatario = notificacion.destinatario();
        entidad.asunto = notificacion.asunto();
        entidad.cuerpo = notificacion.cuerpo();
        entidad.enviadaEn = notificacion.enviadaEn();
        return entidad;
    }

    Notificacion aDominio() {
        return new Notificacion(id, citaId, destinatario, asunto, cuerpo, enviadaEn);
    }
}
