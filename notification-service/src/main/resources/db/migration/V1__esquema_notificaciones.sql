-- notification-service (vetsync_notificaciones): recordatorios de cita por correo simulado (requisito 3).
-- Cada fila es un "correo enviado": el envío es simulado, así que la tabla permite ver qué se habría enviado.

CREATE TABLE notificacion (
    id           BIGINT IDENTITY(1,1) CONSTRAINT PK_notificacion PRIMARY KEY,
    cita_id      BIGINT        NOT NULL,   -- referencia lógica a appointment-service (sin FK entre bases)
    tipo         VARCHAR(30)   NOT NULL DEFAULT 'RECORDATORIO_CITA',
    destinatario NVARCHAR(150) NOT NULL,
    asunto       NVARCHAR(150) NOT NULL,
    cuerpo       NVARCHAR(MAX) NOT NULL,
    enviada_en   DATETIME2(0)  NOT NULL DEFAULT SYSDATETIME(),
    -- Idempotencia: un solo recordatorio por cita, aunque corran varias instancias o se reintente.
    CONSTRAINT UQ_notificacion_cita_tipo UNIQUE (cita_id, tipo)
);
