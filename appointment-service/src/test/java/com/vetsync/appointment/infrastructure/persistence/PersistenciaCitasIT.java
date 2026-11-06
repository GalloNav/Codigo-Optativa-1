package com.vetsync.appointment.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsync.appointment.application.fakes.FakeClinicaGateway;
import com.vetsync.appointment.application.port.in.AgendarCitaComando;
import com.vetsync.appointment.application.port.in.AgendarCitaUseCase;
import com.vetsync.appointment.application.port.out.AccesoADatosException;
import com.vetsync.appointment.application.port.out.CitaRepository;
import com.vetsync.appointment.application.port.out.ContactoDeMascota;
import com.vetsync.appointment.application.port.out.VeterinarioRepository;
import com.vetsync.appointment.application.service.AgendarCitaService;
import com.vetsync.appointment.domain.Cita;
import com.vetsync.appointment.domain.ConflictoDeHorarioException;
import com.vetsync.appointment.domain.EstadoCita;
import com.vetsync.appointment.domain.HorarioDeAtencion;
import com.vetsync.appointment.domain.Veterinario;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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

/**
 * Contrato de {@link CitaRepositoryAdapter} contra un SQL Server real. Comparte el contrato con el fake
 * InMemoryCitaRepository y comprueba que el índice único filtrado se traduce a un conflicto de horario.
 */
@SpringBootTest
@Testcontainers
class PersistenciaCitasIT {

    private static final LocalDateTime FRANJA = LocalDateTime.of(2030, 6, 11, 9, 0);
    private static final long MASCOTA_ID = 7L;
    private static final long VETERINARIO_INEXISTENTE_ID = 987_654L;
    private static final LocalDateTime INICIO_DE_LA_CARGA = LocalDateTime.of(2031, 1, 1, 0, 0);
    private static final int CITAS_CARGADAS = 5_000;
    private static final int MINUTOS_POR_FRANJA = 30;
    private static final int CITA_PROGRAMADA_CARGADA = 3_000;

    @Container
    @ServiceConnection
    static final MSSQLServerContainer<?> SQL_SERVER =
            new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-latest").acceptLicense();

    private final JdbcOperations jdbc;
    private final CitaRepository citaRepository;
    private final VeterinarioRepository veterinarioRepository;
    private long veterinarioId;

    @Autowired
    PersistenciaCitasIT(
            JdbcOperations jdbc,
            CitaJpaRepository citaJpaRepository,
            VeterinarioJpaRepository veterinarioJpaRepository) {
        this.jdbc = jdbc;
        this.citaRepository = new CitaRepositoryAdapter(citaJpaRepository);
        this.veterinarioRepository = new VeterinarioRepositoryAdapter(veterinarioJpaRepository);
    }

    @BeforeEach
    void prepararVeterinario() {
        jdbc.update("DELETE FROM cita");
        jdbc.update("DELETE FROM veterinario");
        veterinarioId = insertarVeterinario("Dra. Prueba");
    }

    @Test
    void guardaLaCitaYLaDevuelveConIdAsignado() {
        Cita guardada = citaRepository.guardar(citaProgramada(veterinarioId, FRANJA));

        assertThat(guardada.id()).isNotNull();
        assertThat(guardada).usingRecursiveComparison().ignoringFields("id")
                .isEqualTo(citaProgramada(veterinarioId, FRANJA));
        assertThat(contarCitas()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT modalidad FROM cita", String.class)).isEqualTo("PRESENCIAL");
    }

    @Test
    void existeCitaActivaEsVerdaderoParaUnaCitaProgramadaEnLaMismaFranja() {
        citaRepository.guardar(citaProgramada(veterinarioId, FRANJA));

        assertThat(citaRepository.existeCitaActiva(veterinarioId, FRANJA)).isTrue();
    }

    @Test
    void existeCitaActivaEsFalsoParaOtraFranjaOParaOtroVeterinario() {
        long otroVeterinarioId = insertarVeterinario("Dr. Otro");
        citaRepository.guardar(citaProgramada(veterinarioId, FRANJA));

        assertThat(citaRepository.existeCitaActiva(veterinarioId, FRANJA.plusMinutes(30))).isFalse();
        assertThat(citaRepository.existeCitaActiva(otroVeterinarioId, FRANJA)).isFalse();
    }

    @Test
    void existeCitaActivaIgnoraLasCitasCanceladas() {
        insertarCitaCancelada(veterinarioId, FRANJA);

        assertThat(citaRepository.existeCitaActiva(veterinarioId, FRANJA)).isFalse();
    }

    @Test
    void guardarLanzaConflictoDeHorarioCuandoLaFranjaYaEstaOcupada() {
        citaRepository.guardar(citaProgramada(veterinarioId, FRANJA));

        assertThatThrownBy(() -> citaRepository.guardar(citaProgramada(veterinarioId, FRANJA)))
                .isInstanceOf(ConflictoDeHorarioException.class)
                .hasCauseInstanceOf(DataAccessException.class);
        assertThat(contarCitas()).isEqualTo(1);
    }

    @Test
    void guardarPermiteReutilizarLaFranjaDeUnaCitaCancelada() {
        insertarCitaCancelada(veterinarioId, FRANJA);

        Cita guardada = citaRepository.guardar(citaProgramada(veterinarioId, FRANJA));

        assertThat(guardada.id()).isNotNull();
        assertThat(contarCitas()).isEqualTo(2);
    }

    @Test
    void guardarNoConfundeUnVeterinarioInexistenteConUnConflictoDeHorario() {
        assertThatThrownBy(() -> citaRepository.guardar(citaProgramada(VETERINARIO_INEXISTENTE_ID, FRANJA)))
                .isInstanceOf(AccesoADatosException.class)
                .hasCauseInstanceOf(DataAccessException.class);
        assertThat(contarCitas()).isZero();
    }

    @Test
    void soloUnaDeDosPeticionesSimultaneasConsigueLaFranja() throws InterruptedException {
        FakeClinicaGateway clinicaGateway = new FakeClinicaGateway();
        clinicaGateway.registrar(MASCOTA_ID, new ContactoDeMascota("Rex", "Ana Perez", "ana@correo.com"));
        Clock reloj = Clock.fixed(LocalDateTime.of(2030, 6, 10, 10, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        AgendarCitaUseCase casoDeUso =
                new AgendarCitaService(
                        citaRepository, veterinarioRepository, clinicaGateway, reloj, new HorarioDeAtencion(8, 18));
        CountDownLatch salida = new CountDownLatch(1);
        Callable<Cita> peticion = () -> {
            salida.await();
            return casoDeUso.agendarCita(new AgendarCitaComando(veterinarioId, MASCOTA_ID, FRANJA, "Control anual"));
        };
        ExecutorService ejecutor = Executors.newFixedThreadPool(2);
        Future<Cita> primera = ejecutor.submit(peticion);
        Future<Cita> segunda = ejecutor.submit(peticion);

        salida.countDown();
        ejecutor.shutdown();
        assertThat(ejecutor.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        assertThat(List.of(primera.state(), segunda.state()))
                .containsExactlyInAnyOrder(Future.State.SUCCESS, Future.State.FAILED);
        Future<Cita> fallida = primera.state() == Future.State.FAILED ? primera : segunda;
        assertThat(fallida.exceptionNow()).isInstanceOf(ConflictoDeHorarioException.class);
        assertThat(contarCitas()).isEqualTo(1);
    }

    @Test
    void buscaLasCitasProgramadasEntreDosMomentosEnOrdenCronologico() {
        LocalDateTime hasta = FRANJA.plusHours(2);
        Cita tarde = citaRepository.guardar(citaProgramada(veterinarioId, hasta));
        Cita temprano = citaRepository.guardar(citaProgramada(veterinarioId, FRANJA));
        citaRepository.guardar(citaProgramada(veterinarioId, hasta.plusMinutes(30)));
        citaRepository.guardar(citaProgramada(veterinarioId, FRANJA.minusMinutes(30)));
        insertarCitaCancelada(veterinarioId, FRANJA.plusHours(1));

        assertThat(citaRepository.buscarProgramadasEntre(FRANJA, hasta)).containsExactly(temprano, tarde);
    }

    @Test
    void existeCitaActivaNoRecorreLaTablaCompleta() {
        cargarCitasEnBloque(veterinarioId);
        LocalDateTime franjaCargada = INICIO_DE_LA_CARGA.plusMinutes(MINUTOS_POR_FRANJA * CITA_PROGRAMADA_CARGADA);
        long recorridosPrevios = recorridosCompletosDeLaTabla();

        boolean existe = citaRepository.existeCitaActiva(veterinarioId, franjaCargada);

        assertThat(existe).isTrue();
        assertThat(recorridosCompletosDeLaTabla()).isEqualTo(recorridosPrevios);
    }

    @Test
    void buscarProgramadasEntreNoRecorreLaTablaCompleta() {
        cargarCitasEnBloque(veterinarioId);
        LocalDateTime desde = INICIO_DE_LA_CARGA.plusMinutes(MINUTOS_POR_FRANJA * CITA_PROGRAMADA_CARGADA);
        long recorridosPrevios = recorridosCompletosDeLaTabla();

        List<Cita> citas = citaRepository.buscarProgramadasEntre(desde, desde.plusDays(1));

        assertThat(citas).isNotEmpty().allSatisfy(cita -> assertThat(cita.estado()).isEqualTo(EstadoCita.PROGRAMADA));
        assertThat(recorridosCompletosDeLaTabla()).isEqualTo(recorridosPrevios);
    }

    @Test
    void guardaAlVeterinarioYLoDevuelveConIdAsignado() {
        Veterinario guardado = veterinarioRepository.guardar(new Veterinario(null, "Ana Gomez", "Cirugia"));

        assertThat(guardado.id()).isNotNull();
        assertThat(guardado).usingRecursiveComparison().ignoringFields("id")
                .isEqualTo(new Veterinario(null, "Ana Gomez", "Cirugia"));
        assertThat(jdbc.queryForObject("SELECT activo FROM veterinario WHERE id = ?", Boolean.class, guardado.id()))
                .isTrue();
    }

    @Test
    void guardaAlVeterinarioSinEspecialidad() {
        Veterinario guardado = veterinarioRepository.guardar(new Veterinario(null, "Luis Mora", null));

        assertThat(veterinarioRepository.buscarTodos()).contains(guardado);
    }

    @Test
    void buscaTodosLosVeterinariosOrdenadosPorNombreYLuegoPorId() {
        jdbc.update("DELETE FROM cita");
        jdbc.update("DELETE FROM veterinario");
        Veterinario zoe = veterinarioRepository.guardar(new Veterinario(null, "Zoe Ruiz", null));
        Veterinario anaPrimera = veterinarioRepository.guardar(new Veterinario(null, "Ana Gomez", "Cirugia"));
        Veterinario anaSegunda = veterinarioRepository.guardar(new Veterinario(null, "Ana Gomez", null));

        assertThat(veterinarioRepository.buscarTodos()).containsExactly(anaPrimera, anaSegunda, zoe);
    }

    @Test
    void existeIndicaSiElVeterinarioEstaRegistrado() {
        assertThat(veterinarioRepository.existe(veterinarioId)).isTrue();
        assertThat(veterinarioRepository.existe(VETERINARIO_INEXISTENTE_ID)).isFalse();
    }

    private long insertarVeterinario(String nombre) {
        return jdbc.queryForObject(
                "INSERT INTO veterinario (nombre) OUTPUT INSERTED.id VALUES (?)", Long.class, nombre);
    }

    private void insertarCitaCancelada(long idVeterinario, LocalDateTime inicio) {
        jdbc.update("""
                INSERT INTO cita (veterinario_id, mascota_id, mascota_nombre, contacto_nombre, contacto_correo,
                                  inicio, motivo, estado)
                VALUES (?, 7, 'Rex', 'Ana Perez', 'ana@correo.com', ?, 'Control anual', 'CANCELADA')
                """, idVeterinario, inicio);
    }

    /**
     * Carga miles de citas (una de cada tres PROGRAMADA, el resto CANCELADA) en franjas consecutivas, para que
     * al optimizador le convenga usar un índice en lugar de recorrer la tabla.
     */
    private void cargarCitasEnBloque(long idVeterinario) {
        jdbc.update("""
                WITH numeros AS (
                    SELECT TOP (?) ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS n
                    FROM sys.all_objects a CROSS JOIN sys.all_objects b)
                INSERT INTO cita (veterinario_id, mascota_id, mascota_nombre, contacto_nombre, contacto_correo,
                                  inicio, motivo, estado)
                SELECT ?, 7, 'Rex', 'Ana Perez', 'ana@correo.com',
                       DATEADD(MINUTE, ? * n, '2031-01-01'), 'Control anual',
                       CASE WHEN n % 3 = 0 THEN 'PROGRAMADA' ELSE 'CANCELADA' END
                FROM numeros
                """, CITAS_CARGADAS, idVeterinario, MINUTOS_POR_FRANJA);
        jdbc.execute("UPDATE STATISTICS cita WITH FULLSCAN");
    }

    /** Veces que alguna consulta de usuario ha recorrido entera la tabla {@code cita} (índice agrupado). */
    private long recorridosCompletosDeLaTabla() {
        return jdbc.queryForObject("""
                SELECT COALESCE(SUM(user_scans), 0) FROM sys.dm_db_index_usage_stats
                WHERE database_id = DB_ID() AND object_id = OBJECT_ID('cita') AND index_id = 1
                """, Long.class);
    }

    private int contarCitas() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM cita", Integer.class);
    }

    private static Cita citaProgramada(long idVeterinario, LocalDateTime inicio) {
        return new Cita(null, idVeterinario, MASCOTA_ID, "Rex", "Ana Perez", "ana@correo.com",
                inicio, "Control anual", EstadoCita.PROGRAMADA);
    }
}
