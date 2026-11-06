package com.vetsync.clinic.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsync.clinic.application.port.out.AccesoADatosException;
import com.vetsync.clinic.application.port.out.MascotaRepository;
import com.vetsync.clinic.application.port.out.PropietarioRepository;
import com.vetsync.clinic.application.port.out.RegistroMedicoRepository;
import com.vetsync.clinic.domain.CorreoYaRegistradoException;
import com.vetsync.clinic.domain.Mascota;
import com.vetsync.clinic.domain.MascotaConPropietario;
import com.vetsync.clinic.domain.Propietario;
import com.vetsync.clinic.domain.RegistroMedico;
import com.vetsync.clinic.domain.Sexo;
import com.vetsync.clinic.domain.TipoRegistro;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

/** Contrato de los adaptadores de propietarios y mascotas contra un SQL Server real. */
@SpringBootTest
@Testcontainers
class PersistenciaClinicaIT {

    private static final LocalDate NACIMIENTO_DE_REX = LocalDate.of(2020, 3, 15);
    private static final LocalDateTime FECHA = LocalDateTime.of(2030, 6, 1, 10, 0);

    @Container
    @ServiceConnection
    static final MSSQLServerContainer<?> SQL_SERVER =
            new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-latest").acceptLicense();

    private final JdbcOperations jdbc;
    private final PropietarioRepository propietarioRepository;
    private final MascotaRepository mascotaRepository;
    private final RegistroMedicoRepository registroMedicoRepository;

    @Autowired
    PersistenciaClinicaIT(
            JdbcOperations jdbc,
            PropietarioJpaRepository propietarioJpaRepository,
            MascotaJpaRepository mascotaJpaRepository,
            RegistroMedicoJpaRepository registroMedicoJpaRepository) {
        this.jdbc = jdbc;
        this.propietarioRepository = new PropietarioRepositoryAdapter(propietarioJpaRepository);
        this.mascotaRepository = new MascotaRepositoryAdapter(mascotaJpaRepository);
        this.registroMedicoRepository = new RegistroMedicoRepositoryAdapter(registroMedicoJpaRepository);
    }

    @BeforeEach
    void limpiarTablas() {
        jdbc.update("DELETE FROM registro_medico");
        jdbc.update("DELETE FROM mascota");
        jdbc.update("DELETE FROM propietario");
    }

    @Test
    void guardaAlPropietarioYLoDevuelveConIdAsignado() {
        Propietario guardado = propietarioRepository.guardar(ana());

        assertThat(guardado.id()).isNotNull();
        assertThat(guardado).usingRecursiveComparison().ignoringFields("id").isEqualTo(ana());
        assertThat(contar("propietario")).isEqualTo(1);
    }

    @Test
    void buscaAlPropietarioPorId() {
        Propietario guardado = propietarioRepository.guardar(ana());

        assertThat(propietarioRepository.buscarPorId(guardado.id())).contains(guardado);
        assertThat(propietarioRepository.buscarPorId(999_999L)).isEmpty();
    }

    @Test
    void guardarLanzaCorreoYaRegistradoCuandoElCorreoYaExiste() {
        propietarioRepository.guardar(ana());

        assertThatThrownBy(() -> propietarioRepository.guardar(ana()))
                .isInstanceOf(CorreoYaRegistradoException.class)
                .hasCauseInstanceOf(DataAccessException.class);
        assertThat(contar("propietario")).isEqualTo(1);
    }

    @Test
    void guardaLaMascotaConSuSexoYSuFechaDeNacimiento() {
        Propietario ana = propietarioRepository.guardar(ana());

        Mascota guardada = mascotaRepository.guardar(rex(ana.id()));

        assertThat(guardada.id()).isNotNull();
        assertThat(guardada).usingRecursiveComparison().ignoringFields("id").isEqualTo(rex(ana.id()));
        assertThat(jdbc.queryForObject("SELECT sexo FROM mascota", String.class)).isEqualTo("M");
        assertThat(jdbc.queryForObject(
                "SELECT CONVERT(varchar(10), fecha_nacimiento, 23) FROM mascota", String.class))
                .isEqualTo("2020-03-15");
    }

    @Test
    void guardaLaMascotaSinDatosOpcionales() {
        Propietario ana = propietarioRepository.guardar(ana());

        Mascota guardada = mascotaRepository.guardar(new Mascota(null, ana.id(), "Michi", "Gato", null, null, null));

        assertThat(guardada.raza()).isNull();
        assertThat(guardada.sexo()).isNull();
        assertThat(guardada.fechaNacimiento()).isNull();
    }

    @Test
    void buscaLaMascotaConSuPropietario() {
        Propietario ana = propietarioRepository.guardar(ana());
        Mascota rex = mascotaRepository.guardar(rex(ana.id()));

        assertThat(mascotaRepository.buscarConPropietario(rex.id()))
                .contains(new MascotaConPropietario(rex, ana));
        assertThat(mascotaRepository.buscarConPropietario(999_999L)).isEmpty();
    }

    @Test
    void guardarNoAceptaUnaMascotaDeUnPropietarioInexistente() {
        assertThatThrownBy(() -> mascotaRepository.guardar(rex(999_999L)))
                .isInstanceOf(AccesoADatosException.class)
                .hasCauseInstanceOf(DataAccessException.class);
        assertThat(contar("mascota")).isZero();
    }

    @Test
    void existeIndicaSiLaMascotaEstaRegistrada() {
        Mascota rex = guardarRex();

        assertThat(mascotaRepository.existe(rex.id())).isTrue();
        assertThat(mascotaRepository.existe(999_999L)).isFalse();
    }

    @Test
    void guardaLaEntradaMedicaConTodosSusDatos() {
        Mascota rex = guardarRex();
        RegistroMedico esperado = new RegistroMedico(null, rex.id(), FECHA, TipoRegistro.CONSULTA,
                "Control anual", "Sano", "Ninguno", new BigDecimal("12.50"), 3L);

        RegistroMedico guardado = registroMedicoRepository.guardar(esperado);

        assertThat(guardado.id()).isNotNull();
        assertThat(guardado).usingRecursiveComparison().ignoringFields("id").isEqualTo(esperado);
        assertThat(registroMedicoRepository.buscarPorMascota(rex.id())).containsExactly(guardado);
    }

    @Test
    void guardaLaEntradaMedicaConSoloLosDatosObligatorios() {
        Mascota rex = guardarRex();
        RegistroMedico esperado = new RegistroMedico(
                null, rex.id(), FECHA, TipoRegistro.VACUNA, "Vacuna anual", null, null, null, null);

        registroMedicoRepository.guardar(esperado);

        assertThat(registroMedicoRepository.buscarPorMascota(rex.id()))
                .singleElement()
                .usingRecursiveComparison().ignoringFields("id").isEqualTo(esperado);
    }

    @Test
    void buscaElHistorialDeLaMascotaDelMasRecienteAlMasAntiguo() {
        Propietario ana = propietarioRepository.guardar(ana());
        Mascota rex = mascotaRepository.guardar(rex(ana.id()));
        Mascota michi = mascotaRepository.guardar(new Mascota(null, ana.id(), "Michi", "Gato", null, null, null));
        RegistroMedico primera = guardarEntrada(rex.id(), FECHA, "Primera");
        RegistroMedico segunda = guardarEntrada(rex.id(), FECHA.plusDays(2), "Segunda");
        RegistroMedico empateConLaSegunda = guardarEntrada(rex.id(), FECHA.plusDays(2), "Tercera");
        guardarEntrada(michi.id(), FECHA.plusDays(5), "De otra mascota");

        assertThat(registroMedicoRepository.buscarPorMascota(rex.id()))
                .containsExactly(empateConLaSegunda, segunda, primera);
    }

    @Test
    void devuelveUnHistorialVacioParaUnaMascotaSinEntradas() {
        Mascota rex = guardarRex();

        assertThat(registroMedicoRepository.buscarPorMascota(rex.id())).isEmpty();
    }

    @Test
    void guardarNoAceptaUnaEntradaDeUnaMascotaInexistente() {
        assertThatThrownBy(() -> registroMedicoRepository.guardar(
                new RegistroMedico(null, 999_999L, FECHA, TipoRegistro.CONSULTA, "Control", null, null, null, null)))
                .isInstanceOf(AccesoADatosException.class)
                .hasCauseInstanceOf(DataAccessException.class);
        assertThat(contar("registro_medico")).isZero();
    }

    private Mascota guardarRex() {
        Propietario ana = propietarioRepository.guardar(ana());
        return mascotaRepository.guardar(rex(ana.id()));
    }

    private RegistroMedico guardarEntrada(long mascotaId, LocalDateTime fecha, String motivo) {
        return registroMedicoRepository.guardar(new RegistroMedico(
                null, mascotaId, fecha, TipoRegistro.CONSULTA, motivo, null, null, null, null));
    }

    private int contar(String tabla) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + tabla, Integer.class);
    }

    private static Propietario ana() {
        return new Propietario(null, "Ana Perez", "ana@correo.com", "555-1234");
    }

    private static Mascota rex(long propietarioId) {
        return new Mascota(null, propietarioId, "Rex", "Perro", "Labrador", Sexo.M, NACIMIENTO_DE_REX);
    }
}
