package com.vetsync.clinic.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsync.clinic.application.fakes.InMemoryMascotaRepository;
import com.vetsync.clinic.application.fakes.InMemoryPropietarioRepository;
import com.vetsync.clinic.application.fakes.InMemoryRegistroMedicoRepository;
import com.vetsync.clinic.application.port.in.ConsultarHistorialMedicoUseCase;
import com.vetsync.clinic.application.service.ConsultarHistorialMedicoService;
import com.vetsync.clinic.domain.Mascota;
import com.vetsync.clinic.domain.MascotaNoEncontradaException;
import com.vetsync.clinic.domain.Propietario;
import com.vetsync.clinic.domain.RegistroMedico;
import com.vetsync.clinic.domain.Sexo;
import com.vetsync.clinic.domain.TipoRegistro;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Especificación de {@code consultarHistorial}, sin Spring ni base de datos. */
class ConsultarHistorialMedicoServiceTest {

    private static final LocalDateTime FECHA = LocalDateTime.of(2030, 6, 1, 10, 0);

    private InMemoryRegistroMedicoRepository registroMedicoRepository;
    private ConsultarHistorialMedicoUseCase casoDeUso;
    private Mascota rex;
    private Mascota michi;

    @BeforeEach
    void prepararEscenario() {
        InMemoryPropietarioRepository propietarioRepository = new InMemoryPropietarioRepository();
        InMemoryMascotaRepository mascotaRepository = new InMemoryMascotaRepository(propietarioRepository);
        registroMedicoRepository = new InMemoryRegistroMedicoRepository();
        casoDeUso = new ConsultarHistorialMedicoService(mascotaRepository, registroMedicoRepository);
        Propietario ana = propietarioRepository.guardar(new Propietario(null, "Ana Perez", "ana@correo.com", null));
        rex = mascotaRepository.guardar(new Mascota(null, ana.id(), "Rex", "Perro", null, Sexo.M, null));
        michi = mascotaRepository.guardar(new Mascota(null, ana.id(), "Michi", "Gato", null, Sexo.H, null));
    }

    @Test
    void devuelveSoloElHistorialDeLaMascotaDelMasRecienteAlMasAntiguo() {
        RegistroMedico primera = guardarEntrada(rex, FECHA, "Primera visita");
        RegistroMedico segunda = guardarEntrada(rex, FECHA.plusDays(2), "Vacuna anual");
        guardarEntrada(michi, FECHA.plusDays(5), "De otra mascota");

        assertThat(casoDeUso.consultarHistorial(rex.id())).containsExactly(segunda, primera);
    }

    @Test
    void devuelveUnaListaVaciaCuandoLaMascotaNoTieneEntradas() {
        assertThat(casoDeUso.consultarHistorial(rex.id())).isEmpty();
    }

    @Test
    void lanzaMascotaNoEncontradaCuandoLaMascotaNoExiste() {
        assertThatThrownBy(() -> casoDeUso.consultarHistorial(999L))
                .isInstanceOf(MascotaNoEncontradaException.class);
    }

    private RegistroMedico guardarEntrada(Mascota mascota, LocalDateTime fecha, String motivo) {
        return registroMedicoRepository.guardar(new RegistroMedico(
                null, mascota.id(), fecha, TipoRegistro.CONSULTA, motivo, null, null, null, null));
    }
}
