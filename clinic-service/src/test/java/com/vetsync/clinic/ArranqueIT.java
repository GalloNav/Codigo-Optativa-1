package com.vetsync.clinic;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcOperations;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** El contexto arranca contra un SQL Server real y Flyway deja aplicado el esquema de clinic-service. */
@SpringBootTest
@Testcontainers
class ArranqueIT {

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
    void flywayAplicaElEsquemaDeLaClinica() {
        Integer tablas = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sys.tables WHERE name IN ('propietario', 'mascota', 'registro_medico')",
                Integer.class);

        assertThat(tablas).isEqualTo(3);
    }
}
