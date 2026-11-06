package com.vetsync.notification.infrastructure.config;

import com.vetsync.notification.application.port.in.EnviarRecordatoriosUseCase;
import com.vetsync.notification.application.port.in.ListarNotificacionesUseCase;
import com.vetsync.notification.application.port.out.AgendaGateway;
import com.vetsync.notification.application.port.out.CorreoSender;
import com.vetsync.notification.application.port.out.NotificacionRepository;
import com.vetsync.notification.application.service.EnviarRecordatoriosService;
import com.vetsync.notification.application.service.ListarNotificacionesService;
import com.vetsync.notification.infrastructure.client.AgendaRestAdapter;
import com.vetsync.notification.infrastructure.mail.CorreoSimuladoAdapter;
import com.vetsync.notification.infrastructure.persistence.NotificacionJpaRepository;
import com.vetsync.notification.infrastructure.persistence.NotificacionRepositoryAdapter;
import com.vetsync.notification.infrastructure.scheduler.RecordatorioScheduler;
import java.time.Clock;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/** Único lugar donde se cablean puertos, adaptadores y casos de uso (AGENTS.md, principio D). */
@Configuration
public class BeansConfig {

    @Bean
    public NotificacionRepository notificacionRepository(NotificacionJpaRepository notificacionJpaRepository) {
        return new NotificacionRepositoryAdapter(notificacionJpaRepository);
    }

    @Bean
    public AgendaGateway agendaGateway(RestClient.Builder constructor, AppointmentServiceProperties propiedades) {
        return new AgendaRestAdapter(constructor.baseUrl(propiedades.baseUrl()).build());
    }

    @Bean
    public CorreoSender correoSender() {
        return new CorreoSimuladoAdapter();
    }

    @Bean
    public EnviarRecordatoriosUseCase enviarRecordatoriosUseCase(
            AgendaGateway agendaGateway,
            NotificacionRepository notificacionRepository,
            CorreoSender correoSender,
            Clock reloj,
            RecordatorioProperties propiedades) {
        return new EnviarRecordatoriosService(agendaGateway, notificacionRepository, correoSender, reloj,
                Duration.ofHours(propiedades.anticipacionHoras()));
    }

    @Bean
    public ListarNotificacionesUseCase listarNotificacionesUseCase(NotificacionRepository notificacionRepository) {
        return new ListarNotificacionesService(notificacionRepository);
    }

    @Bean
    public RecordatorioScheduler recordatorioScheduler(
            EnviarRecordatoriosUseCase enviarRecordatorios, RecordatorioProperties propiedades) {
        return new RecordatorioScheduler(enviarRecordatorios, propiedades.intervalo());
    }
}
