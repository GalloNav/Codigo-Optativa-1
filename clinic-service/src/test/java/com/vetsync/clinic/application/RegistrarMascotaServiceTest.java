package com.vetsync.clinic.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsync.clinic.application.fakes.InMemoryMascotaRepository;
import com.vetsync.clinic.application.fakes.InMemoryPropietarioRepository;
import com.vetsync.clinic.application.port.in.RegistrarMascotaComando;
import com.vetsync.clinic.application.port.in.RegistrarMascotaUseCase;
import com.vetsync.clinic.application.service.RegistrarMascotaService;
import com.vetsync.clinic.domain.Mascota;
import com.vetsync.clinic.domain.MascotaConPropietario;
import com.vetsync.clinic.domain.Propietario;
import com.vetsync.clinic.domain.PropietarioNoRegistradoException;
import com.vetsync.clinic.domain.Sexo;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Especificación de {@code registrarMascota}, sin Spring ni base de datos. */
class RegistrarMascotaServiceTest {

    private static final LocalDate NACIMIENTO_DE_REX = LocalDate.of(2020, 3, 15);

    private InMemoryMascotaRepository mascotaRepository;
    private RegistrarMascotaUseCase casoDeUso;
    private Propietario ana;

    @BeforeEach
    void prepararEscenario() {
        InMemoryPropietarioRepository propietarioRepository = new InMemoryPropietarioRepository();
        mascotaRepository = new InMemoryMascotaRepository(propietarioRepository);
        casoDeUso = new RegistrarMascotaService(propietarioRepository, mascotaRepository);
        ana = propietarioRepository.guardar(new Propietario(null, "Ana Perez", "ana@correo.com", null));
    }

    @Test
    void registraLaMascotaDelPropietarioYLaDevuelveConSuPropietario() {
        MascotaConPropietario registrada = casoDeUso.registrarMascota(
                new RegistrarMascotaComando(ana.id(), "Rex", "Perro", "Labrador", Sexo.M, NACIMIENTO_DE_REX));

        assertThat(registrada.propietario()).isEqualTo(ana);
        assertThat(registrada.mascota().id()).isNotNull();
        assertThat(registrada.mascota()).usingRecursiveComparison().ignoringFields("id")
                .isEqualTo(new Mascota(null, ana.id(), "Rex", "Perro", "Labrador", Sexo.M, NACIMIENTO_DE_REX));
        assertThat(mascotaRepository.todas()).containsExactly(registrada.mascota());
    }

    @Test
    void rechazaLaMascotaCuandoElPropietarioNoEstaRegistrado() {
        assertThatThrownBy(() -> casoDeUso.registrarMascota(
                new RegistrarMascotaComando(999L, "Rex", "Perro", null, null, null)))
                .isInstanceOf(PropietarioNoRegistradoException.class);
        assertThat(mascotaRepository.todas()).isEmpty();
    }
}
