package com.vetsync.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsync.notification.application.fakes.InMemoryNotificacionRepository;
import com.vetsync.notification.application.port.in.ListarNotificacionesUseCase;
import com.vetsync.notification.application.service.ListarNotificacionesService;
import com.vetsync.notification.domain.LimiteDeListadoInvalidoException;
import com.vetsync.notification.domain.Notificacion;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Especificación de {@code listarNotificaciones}, sin Spring ni base de datos. */
class ListarNotificacionesServiceTest {

    private static final LocalDateTime FECHA = LocalDateTime.of(2030, 6, 10, 10, 0);
    private static final int LIMITE_MAXIMO = 500;

    private InMemoryNotificacionRepository notificacionRepository;
    private ListarNotificacionesUseCase casoDeUso;

    @BeforeEach
    void prepararEscenario() {
        notificacionRepository = new InMemoryNotificacionRepository();
        casoDeUso = new ListarNotificacionesService(notificacionRepository);
    }

    @Test
    void devuelveLasNotificacionesDeLaMasRecienteALaMasAntigua() {
        Notificacion primera = guardar(5L, FECHA);
        Notificacion segunda = guardar(6L, FECHA.plusHours(1));

        assertThat(casoDeUso.listarNotificaciones(100)).containsExactly(segunda, primera);
    }

    @Test
    void devuelveComoMuchoElLimiteIndicadoQuedandoseConLasMasRecientes() {
        guardar(5L, FECHA);
        Notificacion segunda = guardar(6L, FECHA.plusHours(1));
        Notificacion tercera = guardar(7L, FECHA.plusHours(2));

        assertThat(casoDeUso.listarNotificaciones(2)).containsExactly(tercera, segunda);
    }

    @Test
    void devuelveUnaListaVaciaCuandoNoSeHaEnviadoNada() {
        assertThat(casoDeUso.listarNotificaciones(100)).isEmpty();
    }

    @Test
    void aceptaElLimiteMaximoPermitido() {
        assertThatCode(() -> casoDeUso.listarNotificaciones(LIMITE_MAXIMO)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(ints = {Integer.MIN_VALUE, -1, 0, LIMITE_MAXIMO + 1, Integer.MAX_VALUE})
    void rechazaUnLimiteFueraDeRango(int limite) {
        assertThatThrownBy(() -> casoDeUso.listarNotificaciones(limite))
                .isInstanceOf(LimiteDeListadoInvalidoException.class)
                .hasMessageContaining(String.valueOf(LIMITE_MAXIMO));
    }

    private Notificacion guardar(long citaId, LocalDateTime enviadaEn) {
        return notificacionRepository.guardar(
                new Notificacion(null, citaId, "ana@correo.com", "Recordatorio", "Hola", enviadaEn));
    }
}
