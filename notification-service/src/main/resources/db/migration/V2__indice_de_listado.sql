-- notification-service (vetsync_notificaciones): índice para listar las notificaciones más recientes.
-- GET /notificaciones ordena por enviada_en e id (ambos descendentes) y devuelve solo las últimas N; con este
-- índice la base lee N filas en orden en lugar de ordenar la tabla entera.

CREATE INDEX IX_notificacion_enviada_en ON notificacion (enviada_en DESC, id DESC);
