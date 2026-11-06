package com.vetsync.clinic.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsync.clinic.application.fakes.InMemoryMascotaRepository;
import com.vetsync.clinic.application.fakes.InMemoryPropietarioRepository;
import com.vetsync.clinic.application.port.in.ConsultarMascotaUseCase;
import com.vetsync.clinic.application.service.ConsultarMascotaService;
import com.vetsync.clinic.domain.Mascota;
import com.vetsync.clinic.domain.MascotaConPropietario;
import com.vetsync.clinic.domain.MascotaNoEncontradaException;
import com.vetsync.clinic.domain.Propietario;
import com.vetsync.clinic.domain.Sexo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Especificación de {@code consultarMascota}, sin Spring ni base de datos. */
class ConsultarMascotaServiceTest {

    private InMemoryPropietarioRepository propietarioRepository;
    private InMemoryMascotaRepository mascotaRepository;
    private ConsultarMascotaUseCase casoDeUso;

    @BeforeEach
    void prepararEscenario() {
        propietarioRepository = new InMemoryPropietarioRepository();
        mascotaRepository = new InMemoryMascotaRepository(propietarioRepository);
        casoDeUso = new ConsultarMascotaService(mascotaRepository);
    }

    @Test
    void devuelveLaMascotaJuntoConSuPropietario() {
        Propietario ana = propietarioRepository.guardar(new Propietario(null, "Ana Perez", "ana@correo.com", null));
        Mascota rex = mascotaRepository.guardar(new Mascota(null, ana.id(), "Rex", "Perro", null, Sexo.M, null));

        MascotaConPropietario consultada = casoDeUso.consultarMascota(rex.id());

        assertThat(consultada).isEqualTo(new MascotaConPropietario(rex, ana));
    }

    @Test
    void lanzaMascotaNoEncontradaCuandoNoExiste() {
        assertThatThrownBy(() -> casoDeUso.consultarMascota(999L))
                .isInstanceOf(MascotaNoEncontradaException.class);
    }
}
