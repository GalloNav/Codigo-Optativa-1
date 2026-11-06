-- appointment-service (vetsync_citas): veterinarios y agenda sin choques de horario (requisito 2).

CREATE TABLE veterinario (
    id           BIGINT IDENTITY(1,1) CONSTRAINT PK_veterinario PRIMARY KEY,
    nombre       NVARCHAR(100) NOT NULL,
    especialidad NVARCHAR(60)  NULL,
    activo       BIT           NOT NULL DEFAULT 1
);

CREATE TABLE cita (
    id                  BIGINT IDENTITY(1,1) CONSTRAINT PK_cita PRIMARY KEY,
    veterinario_id      BIGINT        NOT NULL CONSTRAINT FK_cita_veterinario REFERENCES veterinario(id),
    mascota_id          BIGINT        NOT NULL,   -- referencia lógica a clinic-service (sin FK entre bases)
    mascota_nombre      NVARCHAR(80)  NOT NULL,   -- copia tomada al agendar
    contacto_nombre     NVARCHAR(100) NOT NULL,   -- copia tomada al agendar
    contacto_correo     NVARCHAR(150) NOT NULL,   -- copia tomada al agendar
    inicio              DATETIME2(0)  NOT NULL,   -- hora local de la clínica
    motivo              NVARCHAR(200) NOT NULL,
    modalidad           VARCHAR(15)   NOT NULL DEFAULT 'PRESENCIAL'
                        CONSTRAINT CK_cita_modalidad CHECK (modalidad IN ('PRESENCIAL', 'TELEMEDICINA')),
    enlace_videollamada NVARCHAR(300) NULL,
    estado              VARCHAR(15)   NOT NULL DEFAULT 'PROGRAMADA'
                        CONSTRAINT CK_cita_estado CHECK (estado IN ('PROGRAMADA', 'CANCELADA')),
    creada_en           DATETIME2(0)  NOT NULL DEFAULT SYSDATETIME(),
    -- Franjas fijas de 30 min alineadas a la media hora: "mismo inicio" equivale a "se solapan".
    CONSTRAINT CK_cita_franja CHECK (DATEPART(MINUTE, inicio) % 30 = 0 AND DATEPART(SECOND, inicio) = 0)
);

-- Garantía anti-choque: un veterinario no puede tener dos citas PROGRAMADA en la misma franja.
-- Cancelar una cita libera la franja automáticamente.
CREATE UNIQUE INDEX UX_cita_veterinario_inicio ON cita (veterinario_id, inicio) WHERE estado = 'PROGRAMADA';

-- Consulta de citas próximas a recordar.
CREATE INDEX IX_cita_por_recordar ON cita (inicio) WHERE estado = 'PROGRAMADA';
