package com.vetsync.notification;

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

/** El contexto arranca contra un SQL Server real y el esquema garantiza un único recordatorio por cita. */
@SpringBootTest(properties = "vetsync.recordatorio.intervalo=1h")
@Testcontainers
class ArranqueIT {

    private static final String INSERTAR_NOTIFICACION = """
            INSERT INTO notificacion (cita_id, destinatario, asunto, cuerpo)
            VALUES (1, 'ana@correo.com', 'Recordatorio de cita', 'Tu cita es pronto')
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
    void flywayAplicaElEsquemaDeNotificaciones() {
        Integer tablas = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sys.tables WHERE name = 'notificacion'", Integer.class);

        assertThat(tablas).isEqualTo(1);
    }

    @Test
    void noPermiteDosRecordatoriosParaLaMismaCita() {
        assertThat(jdbc.update(INSERTAR_NOTIFICACION)).isEqualTo(1);

        assertThatThrownBy(() -> jdbc.update(INSERTAR_NOTIFICACION))
                .isInstanceOf(DuplicateKeyException.class);
    }
}
