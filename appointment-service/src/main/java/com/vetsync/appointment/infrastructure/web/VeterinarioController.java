package com.vetsync.appointment.infrastructure.web;

import com.vetsync.appointment.application.port.in.ListarVeterinariosUseCase;
import com.vetsync.appointment.application.port.in.RegistrarVeterinarioUseCase;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Adaptador de entrada HTTP para veterinarios. */
@RestController
@RequestMapping("/veterinarios")
public class VeterinarioController {

    private final RegistrarVeterinarioUseCase registrarVeterinario;
    private final ListarVeterinariosUseCase listarVeterinarios;

    public VeterinarioController(
            RegistrarVeterinarioUseCase registrarVeterinario, ListarVeterinariosUseCase listarVeterinarios) {
        this.registrarVeterinario = registrarVeterinario;
        this.listarVeterinarios = listarVeterinarios;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VeterinarioResponse registrar(@RequestBody RegistrarVeterinarioRequest solicitud) {
        return VeterinarioResponse.desde(registrarVeterinario.registrarVeterinario(solicitud.aComando()));
    }

    @GetMapping
    public List<VeterinarioResponse> listar() {
        return listarVeterinarios.listarVeterinarios().stream()
                .map(VeterinarioResponse::desde)
                .toList();
    }
}
