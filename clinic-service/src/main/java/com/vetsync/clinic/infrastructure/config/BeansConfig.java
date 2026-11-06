package com.vetsync.clinic.infrastructure.config;

import com.vetsync.clinic.application.port.in.ConsultarHistorialMedicoUseCase;
import com.vetsync.clinic.application.port.in.ConsultarMascotaUseCase;
import com.vetsync.clinic.application.port.in.RegistrarEntradaMedicaUseCase;
import com.vetsync.clinic.application.port.in.RegistrarMascotaUseCase;
import com.vetsync.clinic.application.port.in.RegistrarPropietarioUseCase;
import com.vetsync.clinic.application.port.out.MascotaRepository;
import com.vetsync.clinic.application.port.out.PropietarioRepository;
import com.vetsync.clinic.application.port.out.RegistroMedicoRepository;
import com.vetsync.clinic.application.service.ConsultarHistorialMedicoService;
import com.vetsync.clinic.application.service.ConsultarMascotaService;
import com.vetsync.clinic.application.service.RegistrarEntradaMedicaService;
import com.vetsync.clinic.application.service.RegistrarMascotaService;
import com.vetsync.clinic.application.service.RegistrarPropietarioService;
import com.vetsync.clinic.infrastructure.persistence.MascotaJpaRepository;
import com.vetsync.clinic.infrastructure.persistence.MascotaRepositoryAdapter;
import com.vetsync.clinic.infrastructure.persistence.PropietarioJpaRepository;
import com.vetsync.clinic.infrastructure.persistence.PropietarioRepositoryAdapter;
import com.vetsync.clinic.infrastructure.persistence.RegistroMedicoJpaRepository;
import com.vetsync.clinic.infrastructure.persistence.RegistroMedicoRepositoryAdapter;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Único lugar donde se cablean puertos, adaptadores y casos de uso (AGENTS.md, principio D). */
@Configuration
public class BeansConfig {

    @Bean
    public PropietarioRepository propietarioRepository(PropietarioJpaRepository propietarioJpaRepository) {
        return new PropietarioRepositoryAdapter(propietarioJpaRepository);
    }

    @Bean
    public MascotaRepository mascotaRepository(MascotaJpaRepository mascotaJpaRepository) {
        return new MascotaRepositoryAdapter(mascotaJpaRepository);
    }

    @Bean
    public RegistroMedicoRepository registroMedicoRepository(RegistroMedicoJpaRepository registroMedicoJpaRepository) {
        return new RegistroMedicoRepositoryAdapter(registroMedicoJpaRepository);
    }

    @Bean
    public RegistrarPropietarioUseCase registrarPropietarioUseCase(PropietarioRepository propietarioRepository) {
        return new RegistrarPropietarioService(propietarioRepository);
    }

    @Bean
    public RegistrarMascotaUseCase registrarMascotaUseCase(
            PropietarioRepository propietarioRepository, MascotaRepository mascotaRepository) {
        return new RegistrarMascotaService(propietarioRepository, mascotaRepository);
    }

    @Bean
    public ConsultarMascotaUseCase consultarMascotaUseCase(MascotaRepository mascotaRepository) {
        return new ConsultarMascotaService(mascotaRepository);
    }

    @Bean
    public RegistrarEntradaMedicaUseCase registrarEntradaMedicaUseCase(
            MascotaRepository mascotaRepository, RegistroMedicoRepository registroMedicoRepository, Clock reloj) {
        return new RegistrarEntradaMedicaService(mascotaRepository, registroMedicoRepository, reloj);
    }

    @Bean
    public ConsultarHistorialMedicoUseCase consultarHistorialMedicoUseCase(
            MascotaRepository mascotaRepository, RegistroMedicoRepository registroMedicoRepository) {
        return new ConsultarHistorialMedicoService(mascotaRepository, registroMedicoRepository);
    }
}
