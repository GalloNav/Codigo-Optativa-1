package com.vetsync.appointment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsync.appointment.application.fakes.FakeClinicaGateway;
import com.vetsync.appointment.application.fakes.InMemoryCitaRepository;
import com.vetsync.appointment.application.fakes.InMemoryVeterinarioRepository;
import com.vetsync.appointment.application.port.in.AgendarCitaComando;
import com.vetsync.appointment.application.port.in.AgendarCitaUseCase;
import com.vetsync.appointment.application.port.out.ContactoDeMascota;
import com.vetsync.appointment.application.service.AgendarCitaService;
import com.vetsync.appointment.domain.Cita;
import com.vetsync.appointment.domain.ConflictoDeHorarioException;
import com.vetsync.appointment.domain.EstadoCita;
import com.vetsync.appointment.domain.FranjaHorariaInvalidaException;
import com.vetsync.appointment.domain.HorarioDeAtencion;
import com.vetsync.appointment.domain.MascotaNoRegistradaException;
import com.vetsync.appointment.domain.Veterinario;
import com.vetsync.appointment.domain.VeterinarioNoEncontradoException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Especificación de {@code agendarCita} (requisito 2), escrita antes que la implementación.
 * Sin Spring ni base de datos: fakes en memoria y reloj fijo.
 * Convención asumida: la cita debe empezar estrictamente después de "ahora".
 */
class AgendarCitaServiceTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2030, 6, 10, 10, 0);
    private static final LocalDateTime FRANJA_LIBRE = LocalDateTime.of(2030, 6, 11, 9, 0);
    private static final long VETERINARIO_ID = 1L;
    private static final long OTRO_VETERINARIO_ID = 2L;
    private static final long VETERINARIO_NO_REGISTRADO_ID = 999L;
    private static final long MASCOTA_ID = 7L;
    private static final long MASCOTA_NO_REGISTRADA_ID = 999L;
    private static final ContactoDeMascota CONTACTO_DE_REX =
            new ContactoDeMascota("Rex", "Ana Perez", "ana@correo.com");

    private InMemoryCitaRepository citaRepository;
    private AgendarCitaUseCase casoDeUso;

    @BeforeEach
    void prepararEscenario() {
        citaRepository = new InMemoryCitaRepository();
        FakeClinicaGateway clinicaGateway = new FakeClinicaGateway();
        clinicaGateway.registrar(MASCOTA_ID, CONTACTO_DE_REX);
        Clock reloj = Clock.fixed(AHORA.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        InMemoryVeterinarioRepository veterinarioRepository = new InMemoryVeterinarioRepository();
        veterinarioRepository.guardar(new Veterinario(null, "Dra. Ana", null)); // recibe el id 1: VETERINARIO_ID
        veterinarioRepository.guardar(new Veterinario(null, "Dr. Luis", null)); // recibe el id 2: OTRO_VETERINARIO_ID
        casoDeUso = new AgendarCitaService(
                citaRepository, veterinarioRepository, clinicaGateway, reloj, new HorarioDeAtencion(8, 18));
    }

    @Test
    void agendaLaCitaComoProgramadaCuandoElVeterinarioEstaLibre() {
        Cita cita = casoDeUso.agendarCita(comando(VETERINARIO_ID, MASCOTA_ID, FRANJA_LIBRE));

        assertThat(cita.id()).isNotNull();
        assertThat(cita).usingRecursiveComparison().ignoringFields("id").isEqualTo(
                new Cita(null, VETERINARIO_ID, MASCOTA_ID, "Rex", "Ana Perez", "ana@correo.com",
                        FRANJA_LIBRE, "Control anual", EstadoCita.PROGRAMADA));
        assertThat(citaRepository.todas()).containsExactly(cita);
    }

    @Test
    void rechazaLaCitaCuandoElVeterinarioYaTieneUnaCitaEnEsaFranja() {
        dadoQueElVeterinarioTieneUnaCita(VETERINARIO_ID, FRANJA_LIBRE);

        assertThatThrownBy(() -> casoDeUso.agendarCita(comando(VETERINARIO_ID, MASCOTA_ID, FRANJA_LIBRE)))
                .isInstanceOf(ConflictoDeHorarioException.class);
        assertThat(citaRepository.todas()).hasSize(1);
    }

    @ParameterizedTest(name = "inicio {0} minutos respecto a ahora")
    @ValueSource(longs = {-1440, -30, 0})
    void rechazaLaCitaCuyoInicioNoEsPosteriorAAhora(long minutosRespectoAAhora) {
        LocalDateTime inicio = AHORA.plusMinutes(minutosRespectoAAhora);

        assertThatThrownBy(() -> casoDeUso.agendarCita(comando(VETERINARIO_ID, MASCOTA_ID, inicio)))
                .isInstanceOf(FranjaHorariaInvalidaException.class);
        assertThat(citaRepository.todas()).isEmpty();
    }

    @ParameterizedTest(name = "inicio {0}")
    @ValueSource(strings = {"2030-06-11T09:15", "2030-06-11T07:30", "2030-06-11T18:00"})
    void rechazaLaCitaCuyoInicioNoCaeEnUnaFranjaDelHorarioDeAtencion(String inicio) {
        assertThatThrownBy(() -> casoDeUso.agendarCita(
                comando(VETERINARIO_ID, MASCOTA_ID, LocalDateTime.parse(inicio))))
                .isInstanceOf(FranjaHorariaInvalidaException.class);
        assertThat(citaRepository.todas()).isEmpty();
    }

    @Test
    void rechazaLaCitaCuandoLaMascotaNoEstaRegistrada() {
        assertThatThrownBy(() -> casoDeUso.agendarCita(
                comando(VETERINARIO_ID, MASCOTA_NO_REGISTRADA_ID, FRANJA_LIBRE)))
                .isInstanceOf(MascotaNoRegistradaException.class);
        assertThat(citaRepository.todas()).isEmpty();
    }

    @Test
    void permiteLaMismaFranjaConOtroVeterinario() {
        dadoQueElVeterinarioTieneUnaCita(VETERINARIO_ID, FRANJA_LIBRE);

        Cita cita = casoDeUso.agendarCita(comando(OTRO_VETERINARIO_ID, MASCOTA_ID, FRANJA_LIBRE));

        assertThat(cita.veterinarioId()).isEqualTo(OTRO_VETERINARIO_ID);
        assertThat(citaRepository.todas()).hasSize(2);
    }

    @Test
    void rechazaLaCitaCuandoElVeterinarioNoEstaRegistrado() {
        assertThatThrownBy(() -> casoDeUso.agendarCita(
                comando(VETERINARIO_NO_REGISTRADO_ID, MASCOTA_ID, FRANJA_LIBRE)))
                .isInstanceOf(VeterinarioNoEncontradoException.class);
        assertThat(citaRepository.todas()).isEmpty();
    }

    private void dadoQueElVeterinarioTieneUnaCita(long veterinarioId, LocalDateTime inicio) {
        citaRepository.guardar(new Cita(null, veterinarioId, MASCOTA_ID, "Rex", "Ana Perez", "ana@correo.com",
                inicio, "Vacuna", EstadoCita.PROGRAMADA));
    }

    private static AgendarCitaComando comando(long veterinarioId, long mascotaId, LocalDateTime inicio) {
        return new AgendarCitaComando(veterinarioId, mascotaId, inicio, "Control anual");
    }
}
