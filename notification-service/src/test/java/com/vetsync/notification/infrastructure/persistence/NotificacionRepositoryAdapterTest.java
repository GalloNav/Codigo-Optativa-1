package com.vetsync.notification.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsync.notification.application.port.out.AccesoADatosException;
import com.vetsync.notification.application.port.out.NotificacionRepository;
import com.vetsync.notification.domain.Notificacion;
import com.vetsync.notification.domain.RecordatorioYaEnviadoException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.LongStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Limit;

/**
 * Comportamiento de {@link NotificacionRepositoryAdapter} que no necesita base de datos: la partición en lotes de
 * la consulta, el caso vacío y la traducción de fallos. El resto del contrato está en PersistenciaNotificacionesIT.
 */
class NotificacionRepositoryAdapterTest {

    private static final int LIMITE_DE_IDS_POR_CONSULTA = 1_000;
    private static final int ERROR_INDICE_UNICO = 2601;
    private static final int ERROR_RESTRICCION_UNICA = 2627;
    private static final Notificacion NOTIFICACION = new Notificacion(
            null, 5L, "ana@correo.com", "Recordatorio de cita: Rex", "Hola Ana", LocalDateTime.of(2030, 6, 10, 10, 0));

    private NotificacionJpaRepository notificacionJpaRepository;
    private NotificacionRepository notificacionRepository;

    @BeforeEach
    void prepararElAdaptador() {
        notificacionJpaRepository = mock(NotificacionJpaRepository.class);
        notificacionRepository = new NotificacionRepositoryAdapter(notificacionJpaRepository);
    }

    @Test
    void citasYaNotificadasNoConsultaLaBaseDeDatosSiNoSeIndicanCitas() {
        Set<Long> notificadas = notificacionRepository.citasYaNotificadas(List.of());

        assertThat(notificadas).isEmpty();
        verifyNoInteractions(notificacionJpaRepository);
    }

    @Test
    void citasYaNotificadasPartePorLotesLasConsultasMuyLargasYUneLosResultados() {
        List<Long> citaIds = LongStream.rangeClosed(1, 2_500).boxed().toList();
        when(notificacionJpaRepository.buscarCitaIdsNotificadas(anyCollection()))
                .thenReturn(List.of(5L))
                .thenReturn(List.of(1_500L))
                .thenReturn(List.of(2_400L));

        Set<Long> notificadas = notificacionRepository.citasYaNotificadas(citaIds);

        assertThat(notificadas).containsExactlyInAnyOrder(5L, 1_500L, 2_400L);
        ArgumentCaptor<Collection<Long>> lotes = ArgumentCaptor.captor();
        verify(notificacionJpaRepository, times(3)).buscarCitaIdsNotificadas(lotes.capture());
        assertThat(lotes.getAllValues()).extracting(Collection::size)
                .containsExactly(LIMITE_DE_IDS_POR_CONSULTA, LIMITE_DE_IDS_POR_CONSULTA, 500);
    }

    @ParameterizedTest
    @ValueSource(ints = {ERROR_INDICE_UNICO, ERROR_RESTRICCION_UNICA})
    void guardarTraduceLaViolacionDeUnicidadARecordatorioYaEnviado(int codigoDeError) {
        DuplicateKeyException duplicado = new DuplicateKeyException(
                "clave duplicada", new SQLException("clave duplicada", "23000", codigoDeError));
        when(notificacionJpaRepository.saveAndFlush(any(NotificacionEntity.class))).thenThrow(duplicado);

        assertThatThrownBy(() -> notificacionRepository.guardar(NOTIFICACION))
                .isInstanceOf(RecordatorioYaEnviadoException.class)
                .hasCause(duplicado);
    }

    @Test
    void guardarTraduceLosDemasFallosDeLaBaseDeDatosAAccesoADatos() {
        DataAccessResourceFailureException fallo = new DataAccessResourceFailureException("base caida");
        when(notificacionJpaRepository.saveAndFlush(any(NotificacionEntity.class))).thenThrow(fallo);

        assertThatThrownBy(() -> notificacionRepository.guardar(NOTIFICACION))
                .isInstanceOf(AccesoADatosException.class)
                .hasCause(fallo);
    }

    @Test
    void buscarRecientesPideALaBaseDeDatosSoloElLimiteIndicado() {
        when(notificacionJpaRepository.findAllByOrderByEnviadaEnDescIdDesc(any(Limit.class))).thenReturn(List.of());

        notificacionRepository.buscarRecientes(25);

        verify(notificacionJpaRepository).findAllByOrderByEnviadaEnDescIdDesc(Limit.of(25));
    }

    @Test
    void buscarRecientesTraduceLosFallosDeLaBaseDeDatos() {
        DataAccessResourceFailureException fallo = new DataAccessResourceFailureException("base caida");
        when(notificacionJpaRepository.findAllByOrderByEnviadaEnDescIdDesc(any(Limit.class))).thenThrow(fallo);

        assertThatThrownBy(() -> notificacionRepository.buscarRecientes(25))
                .isInstanceOf(AccesoADatosException.class)
                .hasCause(fallo);
    }

    @Test
    void citasYaNotificadasTraduceLosFallosDeLaBaseDeDatos() {
        DataAccessResourceFailureException fallo = new DataAccessResourceFailureException("base caida");
        when(notificacionJpaRepository.buscarCitaIdsNotificadas(anyCollection())).thenThrow(fallo);

        assertThatThrownBy(() -> notificacionRepository.citasYaNotificadas(List.of(5L)))
                .isInstanceOf(AccesoADatosException.class)
                .hasCause(fallo);
    }
}
