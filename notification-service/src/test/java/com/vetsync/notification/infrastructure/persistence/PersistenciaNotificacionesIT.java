package com.vetsync.notification.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsync.notification.application.port.out.AccesoADatosException;
import com.vetsync.notification.application.port.out.NotificacionRepository;
import com.vetsync.notification.domain.Notificacion;
import com.vetsync.notification.domain.RecordatorioYaEnviadoException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.LongStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcOperations;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Contrato de {@link NotificacionRepositoryAdapter} contra un SQL Server real. */
@SpringBootTest(properties = "vetsync.recordatorio.intervalo=1h")
@Testcontainers
class PersistenciaNotificacionesIT {

    private static final LocalDateTime ENVIADA_EN = LocalDateTime.of(2030, 6, 10, 10, 0);
    private static final int NOTIFICACIONES_CARGADAS = 50_000;

    @Container
    @ServiceConnection
    static final MSSQLServerContainer<?> SQL_SERVER =
            new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-latest").acceptLicense();

    private final JdbcOperations jdbc;
    private final NotificacionRepository notificacionRepository;

    @Autowired
    PersistenciaNotificacionesIT(JdbcOperations jdbc, NotificacionJpaRepository notificacionJpaRepository) {
        this.jdbc = jdbc;
        this.notificacionRepository = new NotificacionRepositoryAdapter(notificacionJpaRepository);
    }

    @BeforeEach
    void limpiarLaTabla() {
        jdbc.update("DELETE FROM notificacion");
    }

    @Test
    void guardaLaNotificacionYLaDevuelveConIdAsignado() {
        Notificacion guardada = notificacionRepository.guardar(notificacion(5L, ENVIADA_EN));

        assertThat(guardada.id()).isNotNull();
        assertThat(guardada).usingRecursiveComparison().ignoringFields("id")
                .isEqualTo(notificacion(5L, ENVIADA_EN));
        assertThat(notificacionRepository.buscarRecientes(10)).containsExactly(guardada);
        assertThat(jdbc.queryForObject("SELECT tipo FROM notificacion", String.class))
                .isEqualTo("RECORDATORIO_CITA");
    }

    @Test
    void citasYaNotificadasDevuelveSoloLasCitasQueYaRecibieronSuRecordatorio() {
        notificacionRepository.guardar(notificacion(5L, ENVIADA_EN));
        notificacionRepository.guardar(notificacion(7L, ENVIADA_EN));

        assertThat(notificacionRepository.citasYaNotificadas(List.of(5L, 6L, 7L, 8L)))
                .containsExactlyInAnyOrder(5L, 7L);
    }

    @Test
    void citasYaNotificadasAceptaMasIdsQueElLimiteDeParametrosDeSqlServer() {
        notificacionRepository.guardar(notificacion(5L, ENVIADA_EN));
        notificacionRepository.guardar(notificacion(2_400L, ENVIADA_EN));
        List<Long> citaIds = LongStream.rangeClosed(1, 2_500).boxed().toList();

        assertThat(notificacionRepository.citasYaNotificadas(citaIds)).containsExactlyInAnyOrder(5L, 2_400L);
    }

    @Test
    void buscaLasMasRecientesDeLaMasRecienteALaMasAntigua() {
        Notificacion primera = notificacionRepository.guardar(notificacion(5L, ENVIADA_EN));
        Notificacion segunda = notificacionRepository.guardar(notificacion(6L, ENVIADA_EN.plusHours(1)));
        Notificacion empateConLaSegunda = notificacionRepository.guardar(notificacion(7L, ENVIADA_EN.plusHours(1)));

        assertThat(notificacionRepository.buscarRecientes(10)).containsExactly(empateConLaSegunda, segunda, primera);
    }

    @Test
    void buscarRecientesDevuelveComoMuchoElLimiteIndicadoQuedandoseConLasMasRecientes() {
        notificacionRepository.guardar(notificacion(5L, ENVIADA_EN));
        Notificacion segunda = notificacionRepository.guardar(notificacion(6L, ENVIADA_EN.plusHours(1)));
        Notificacion tercera = notificacionRepository.guardar(notificacion(7L, ENVIADA_EN.plusHours(2)));

        assertThat(notificacionRepository.buscarRecientes(2)).containsExactly(tercera, segunda);
    }

    @Test
    void buscarRecientesNoRecorreLaTablaCompleta() {
        cargarNotificacionesEnBloque();
        long recorridosPrevios = recorridosCompletosDeLaTabla();

        List<Notificacion> recientes = notificacionRepository.buscarRecientes(100);

        assertThat(recientes).hasSize(100);
        assertThat(recorridosCompletosDeLaTabla()).isEqualTo(recorridosPrevios);
    }

    @Test
    void guardarTraduceUnSegundoRecordatorioParaLaMismaCitaARecordatorioYaEnviado() {
        notificacionRepository.guardar(notificacion(5L, ENVIADA_EN));

        assertThatThrownBy(() -> notificacionRepository.guardar(notificacion(5L, ENVIADA_EN.plusHours(1))))
                .isInstanceOf(RecordatorioYaEnviadoException.class)
                .hasCauseInstanceOf(DataAccessException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM notificacion", Integer.class)).isEqualTo(1);
    }

    @Test
    void guardarNoConfundeUnFalloTecnicoConUnRecordatorioYaEnviado() {
        Notificacion conAsuntoDemasiadoLargo = new Notificacion(
                null, 5L, "ana@correo.com", "a".repeat(151), "Hola Ana", ENVIADA_EN);

        assertThatThrownBy(() -> notificacionRepository.guardar(conAsuntoDemasiadoLargo))
                .isInstanceOf(AccesoADatosException.class)
                .hasCauseInstanceOf(DataAccessException.class);
    }

    /** Carga decenas de miles de notificaciones para que al optimizador le convenga usar un índice. */
    private void cargarNotificacionesEnBloque() {
        jdbc.update("""
                WITH numeros AS (
                    SELECT TOP (?) ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS n
                    FROM sys.all_objects a CROSS JOIN sys.all_objects b)
                INSERT INTO notificacion (cita_id, destinatario, asunto, cuerpo, enviada_en)
                SELECT n, 'ana@correo.com', 'Recordatorio de cita: Rex', 'Hola Ana, Rex tiene cita pronto',
                       DATEADD(MINUTE, n, '2030-01-01')
                FROM numeros
                """, NOTIFICACIONES_CARGADAS);
        jdbc.execute("UPDATE STATISTICS notificacion WITH FULLSCAN");
    }

    /** Veces que alguna consulta de usuario ha recorrido entera la tabla {@code notificacion} (índice agrupado). */
    private long recorridosCompletosDeLaTabla() {
        return jdbc.queryForObject("""
                SELECT COALESCE(SUM(user_scans), 0) FROM sys.dm_db_index_usage_stats
                WHERE database_id = DB_ID() AND object_id = OBJECT_ID('notificacion') AND index_id = 1
                """, Long.class);
    }

    private static Notificacion notificacion(long citaId, LocalDateTime enviadaEn) {
        return new Notificacion(null, citaId, "ana@correo.com", "Recordatorio de cita: Rex", "Hola Ana", enviadaEn);
    }
}
