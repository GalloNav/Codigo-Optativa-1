package com.vetsync.appointment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.env.Environment;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcOperations;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** El contexto arranca contra un SQL Server real y el esquema garantiza la regla anti-choque de horarios. */
@SpringBootTest
@Testcontainers
class ArranqueIT {

    private static final String INSERTAR_CITA = """
            INSERT INTO cita (veterinario_id, mascota_id, mascota_nombre, contacto_nombre, contacto_correo, inicio, motivo)
            VALUES (?, 1, 'Rex', 'Ana', 'ana@correo.com', '2030-01-01T09:00:00', 'Control')
            """;

    @Container
    @ServiceConnection
    static final MSSQLServerContainer<?> SQL_SERVER =
            new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-latest").acceptLicense();

    private final JdbcOperations jdbc;
    private final Environment entorno;

    @Autowired
    ArranqueIT(JdbcOperations jdbc, Environment entorno) {
        this.jdbc = jdbc;
        this.entorno = entorno;
    }

    @Test
    void elServicioSoloEscuchaEnLaInterfazLocal() {
        assertThat(entorno.getProperty("server.address")).isEqualTo("127.0.0.1");
    }

    @Test
    void flywayAplicaElEsquemaDeCitas() {
        Integer tablas = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sys.tables WHERE name IN ('veterinario', 'cita')", Integer.class);

        assertThat(tablas).isEqualTo(2);
    }

    @Test
    void elIndiceUnicoImpideDosCitasProgramadasEnLaMismaFranja() {
        jdbc.update("INSERT INTO veterinario (nombre) VALUES ('Dra. Prueba')");
        Long veterinarioId = jdbc.queryForObject("SELECT MAX(id) FROM veterinario", Long.class);
        jdbc.update(INSERTAR_CITA, veterinarioId);

        assertThatThrownBy(() -> jdbc.update(INSERTAR_CITA, veterinarioId))
                .isInstanceOf(DuplicateKeyException.class);

        jdbc.update("UPDATE cita SET estado = 'CANCELADA' WHERE veterinario_id = ?", veterinarioId);
        assertThat(jdbc.update(INSERTAR_CITA, veterinarioId)).isEqualTo(1);
    }
}
