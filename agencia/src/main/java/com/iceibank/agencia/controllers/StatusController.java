package com.iceibank.agencia.controllers;

import com.iceibank.agencia.model.StatusAgenciaResponse;
import com.iceibank.agencia.services.ContaRepository;
import com.iceibank.agencia.services.RelogioLamport;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class StatusController {

    @Value("${agencia.id:0}")
    private int idAgencia;

    private final ContaRepository contas;
    private final RelogioLamport relogio;

    @GetMapping("/status")
    public StatusAgenciaResponse consultarStatus() {
        return new StatusAgenciaResponse(
                idAgencia,
                relogio.valorAtual(),
                contas.quantidade());
    }
}
