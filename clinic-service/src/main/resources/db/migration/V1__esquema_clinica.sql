-- clinic-service (vetsync_clinic): propietarios, mascotas e historial médico (requisito 1).

CREATE TABLE propietario (
    id        BIGINT IDENTITY(1,1) CONSTRAINT PK_propietario PRIMARY KEY,
    nombre    NVARCHAR(100) NOT NULL,
    correo    NVARCHAR(150) NOT NULL CONSTRAINT UQ_propietario_correo UNIQUE,
    telefono  VARCHAR(20)   NULL,
    creado_en DATETIME2(0)  NOT NULL DEFAULT SYSDATETIME()
);

CREATE TABLE mascota (
    id               BIGINT IDENTITY(1,1) CONSTRAINT PK_mascota PRIMARY KEY,
    propietario_id   BIGINT       NOT NULL CONSTRAINT FK_mascota_propietario REFERENCES propietario(id),
    nombre           NVARCHAR(80) NOT NULL,
    especie          NVARCHAR(40) NOT NULL,
    raza             NVARCHAR(60) NULL,
    sexo             CHAR(1)      NULL CONSTRAINT CK_mascota_sexo CHECK (sexo IN ('M', 'H')),
    fecha_nacimiento DATE         NULL
);

CREATE INDEX IX_mascota_propietario ON mascota (propietario_id);

-- Historial médico: tabla única de solo-agregar.
CREATE TABLE registro_medico (
    id             BIGINT IDENTITY(1,1) CONSTRAINT PK_registro_medico PRIMARY KEY,
    mascota_id     BIGINT        NOT NULL CONSTRAINT FK_registro_mascota REFERENCES mascota(id),
    fecha          DATETIME2(0)  NOT NULL DEFAULT SYSDATETIME(),
    tipo           VARCHAR(20)   NOT NULL
                   CONSTRAINT CK_registro_tipo CHECK (tipo IN ('CONSULTA', 'VACUNA', 'CIRUGIA', 'DESPARASITACION', 'OTRO')),
    motivo         NVARCHAR(200) NOT NULL,
    diagnostico    NVARCHAR(MAX) NULL,
    tratamiento    NVARCHAR(MAX) NULL,
    peso_kg        DECIMAL(5,2)  NULL CONSTRAINT CK_registro_peso CHECK (peso_kg > 0),
    veterinario_id BIGINT        NULL   -- referencia lógica a appointment-service (sin FK entre bases)
);

CREATE INDEX IX_registro_mascota_fecha ON registro_medico (mascota_id, fecha DESC);
