package com.vetsync.appointment.infrastructure.config;

import com.vetsync.appointment.application.port.in.AgendarCitaUseCase;
import com.vetsync.appointment.application.port.in.ListarCitasPorRecordarUseCase;
import com.vetsync.appointment.application.port.in.ListarVeterinariosUseCase;
import com.vetsync.appointment.application.port.in.RegistrarVeterinarioUseCase;
import com.vetsync.appointment.application.port.out.CitaRepository;
import com.vetsync.appointment.application.port.out.ClinicaGateway;
import com.vetsync.appointment.application.port.out.VeterinarioRepository;
import com.vetsync.appointment.application.service.AgendarCitaService;
import com.vetsync.appointment.application.service.ListarCitasPorRecordarService;
import com.vetsync.appointment.application.service.ListarVeterinariosService;
import com.vetsync.appointment.application.service.RegistrarVeterinarioService;
import com.vetsync.appointment.domain.HorarioDeAtencion;
import com.vetsync.appointment.infrastructure.client.ClinicaRestAdapter;
import com.vetsync.appointment.infrastructure.persistence.CitaJpaRepository;
import com.vetsync.appointment.infrastructure.persistence.CitaRepositoryAdapter;
import com.vetsync.appointment.infrastructure.persistence.VeterinarioJpaRepository;
import com.vetsync.appointment.infrastructure.persistence.VeterinarioRepositoryAdapter;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/** Único lugar donde se cablean puertos, adaptadores y casos de uso (AGENTS.md, principio D). */
@Configuration
public class BeansConfig {

    @Bean
    public CitaRepository citaRepository(CitaJpaRepository citaJpaRepository) {
        return new CitaRepositoryAdapter(citaJpaRepository);
    }

    @Bean
    public VeterinarioRepository veterinarioRepository(VeterinarioJpaRepository veterinarioJpaRepository) {
        return new VeterinarioRepositoryAdapter(veterinarioJpaRepository);
    }

    @Bean
    public ClinicaGateway clinicaGateway(RestClient.Builder constructor, ClinicServiceProperties propiedades) {
        return new ClinicaRestAdapter(constructor.baseUrl(propiedades.baseUrl()).build());
    }

    @Bean
    public HorarioDeAtencion horarioDeAtencion(AgendaProperties propiedades) {
        return new HorarioDeAtencion(propiedades.horaApertura(), propiedades.horaCierre());
    }

    @Bean
    public AgendarCitaUseCase agendarCitaUseCase(
            CitaRepository citaRepository,
            VeterinarioRepository veterinarioRepository,
            ClinicaGateway clinicaGateway,
            Clock reloj,
            HorarioDeAtencion horarioDeAtencion) {
        return new AgendarCitaService(
                citaRepository, veterinarioRepository, clinicaGateway, reloj, horarioDeAtencion);
    }

    @Bean
    public ListarCitasPorRecordarUseCase listarCitasPorRecordarUseCase(CitaRepository citaRepository) {
        return new ListarCitasPorRecordarService(citaRepository);
    }

    @Bean
    public RegistrarVeterinarioUseCase registrarVeterinarioUseCase(VeterinarioRepository veterinarioRepository) {
        return new RegistrarVeterinarioService(veterinarioRepository);
    }

    @Bean
    public ListarVeterinariosUseCase listarVeterinariosUseCase(VeterinarioRepository veterinarioRepository) {
        return new ListarVeterinariosService(veterinarioRepository);
    }
}
