package com.vetsync.clinic.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsync.clinic.application.fakes.InMemoryMascotaRepository;
import com.vetsync.clinic.application.fakes.InMemoryPropietarioRepository;
import com.vetsync.clinic.application.fakes.InMemoryRegistroMedicoRepository;
import com.vetsync.clinic.application.port.in.RegistrarEntradaMedicaComando;
import com.vetsync.clinic.application.port.in.RegistrarEntradaMedicaUseCase;
import com.vetsync.clinic.application.service.RegistrarEntradaMedicaService;
import com.vetsync.clinic.domain.Mascota;
import com.vetsync.clinic.domain.MascotaNoEncontradaException;
import com.vetsync.clinic.domain.Propietario;
import com.vetsync.clinic.domain.RegistroMedico;
import com.vetsync.clinic.domain.Sexo;
import com.vetsync.clinic.domain.TipoRegistro;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Especificación de {@code registrarEntradaMedica}, con reloj fijo y sin Spring ni base de datos. */
class RegistrarEntradaMedicaServiceTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2030, 6, 10, 10, 0);

    private InMemoryRegistroMedicoRepository registroMedicoRepository;
    private RegistrarEntradaMedicaUseCase casoDeUso;
    private Mascota rex;

    @BeforeEach
    void prepararEscenario() {
        InMemoryPropietarioRepository propietarioRepository = new InMemoryPropietarioRepository();
        InMemoryMascotaRepository mascotaRepository = new InMemoryMascotaRepository(propietarioRepository);
        registroMedicoRepository = new InMemoryRegistroMedicoRepository();
        Clock reloj = Clock.fixed(AHORA.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        casoDeUso = new RegistrarEntradaMedicaService(mascotaRepository, registroMedicoRepository, reloj);
        Propietario ana = propietarioRepository.guardar(new Propietario(null, "Ana Perez", "ana@correo.com", null));
        rex = mascotaRepository.guardar(new Mascota(null, ana.id(), "Rex", "Perro", null, Sexo.M, null));
    }

    @Test
    void registraLaEntradaConLaFechaDelRelojYLaDevuelveConIdAsignado() {
        RegistroMedico registro = casoDeUso.registrarEntradaMedica(new RegistrarEntradaMedicaComando(
                rex.id(), TipoRegistro.CONSULTA, "Control anual", "Sano", "Ninguno", new BigDecimal("12.50"), 3L));

        assertThat(registro.id()).isNotNull();
        assertThat(registro).usingRecursiveComparison().ignoringFields("id").isEqualTo(new RegistroMedico(
                null, rex.id(), AHORA, TipoRegistro.CONSULTA, "Control anual", "Sano", "Ninguno",
                new BigDecimal("12.50"), 3L));
        assertThat(registroMedicoRepository.todos()).containsExactly(registro);
    }

    @Test
    void rechazaLaEntradaCuandoLaMascotaNoExiste() {
        assertThatThrownBy(() -> casoDeUso.registrarEntradaMedica(new RegistrarEntradaMedicaComando(
                999L, TipoRegistro.VACUNA, "Vacuna anual", null, null, null, null)))
                .isInstanceOf(MascotaNoEncontradaException.class);
        assertThat(registroMedicoRepository.todos()).isEmpty();
    }
}
