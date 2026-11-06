-- appointment-service (vetsync_citas): índices de lectura para la agenda.
-- Los índices filtrados de V1 siguen garantizando la unicidad (UX_cita_veterinario_inicio), pero el optimizador
-- no los usa con consultas parametrizadas (estado = ?), así que existeCitaActiva y buscarProgramadasEntre
-- recorrían la tabla entera. Estos índices normales las atienden con una búsqueda; estado va como INCLUDE.

CREATE INDEX IX_cita_veterinario_inicio ON cita (veterinario_id, inicio) INCLUDE (estado);

CREATE INDEX IX_cita_inicio ON cita (inicio) INCLUDE (estado);

-- Filtrado por estado y, por la misma razón, sin uso: IX_cita_inicio lo sustituye.
DROP INDEX IX_cita_por_recordar ON cita;
