package com.iceibank.agencia.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iceibank.agencia.config.AgenciaConfig;
import com.iceibank.agencia.model.Conta;
import com.iceibank.agencia.model.CreditoRemotoRequest;
import com.iceibank.agencia.model.TransferenciaRequest;
import com.iceibank.agencia.routing.AppRouting;
import com.iceibank.agencia.services.ContaRepository;
import com.iceibank.agencia.services.EventLogService;
import com.iceibank.agencia.services.RelogioLamport;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class TransferenciaController {

    @Value("${agencia.id:0}")
    private int idAgenciaLocal;

    private final ContaRepository contas;
    private final AppRouting appRouting;
    private final RelogioLamport relogio;
    private final EventLogService registro;
    private final ObjectMapper objectMapper;

    @PostMapping("/transferencias")
    public ResponseEntity<?> transferir(@RequestBody TransferenciaRequest req) {
        if (req.valor() <= 0) {
            return ResponseEntity.badRequest().body(Map.of("erro", "O valor deve ser positivo."));
        }

        Conta contaOrigem = contas.buscar(req.idOrigem());
        if (contaOrigem == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("erro", "Conta de origem não encontrada nesta agência."));
        }
        if (contaOrigem.getSaldo() < req.valor()) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Saldo insuficiente."));
        }

        AgenciaConfig agenciaDestino = appRouting.obterAgenciaResponsavel(req.idDestino());
        int tsDebito = relogio.eventoLocal();
        contaOrigem.setSaldo(contaOrigem.getSaldo() - req.valor());
        registrar(tsDebito, "TRANSFERENCIA_DEBITO", req);

        if (agenciaDestino.id() == idAgenciaLocal) {
            Conta contaDestino = contas.buscar(req.idDestino());
            if (contaDestino == null) {
                contaOrigem.setSaldo(contaOrigem.getSaldo() + req.valor());
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("erro", "Conta de destino não encontrada."));
            }

            int tsCredito = relogio.eventoLocal();
            contaDestino.setSaldo(contaDestino.getSaldo() + req.valor());
            registrar(tsCredito, "TRANSFERENCIA_CREDITO", req);
            return ResponseEntity.ok(Map.of("mensagem", "Transferência concluída (mesma agência)."));
        }

        int tsEnvio = relogio.aoEnviar();
        try {
            enviarCreditoRemoto(agenciaDestino, req, tsEnvio);
            return ResponseEntity.ok(Map.of("mensagem", "Transferência concluída (entre agências)."));
        } catch (Exception erro) {
            registrar(relogio.eventoLocal(), "TRANSFERENCIA_FALHOU",
                    req + ", erro: " + erro.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                    "erro", "Falha ao contatar agência de destino. Débito já aplicado - inconsistência conhecida (ver Sprint 4)."));
        }
    }

    @PostMapping("/contas/{id}/creditar-remoto")
    public ResponseEntity<?> creditarRemoto(@PathVariable int id,
                                             @RequestBody CreditoRemotoRequest req) {
        Conta conta = contas.buscar(id);
        if (conta == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("erro", "Conta não encontrada nesta agência."));
        }

        int ts = relogio.aoReceber(req.timestampLamport());
        conta.setSaldo(conta.getSaldo() + req.valor());
        registrar(ts, "TRANSFERENCIA_CREDITO_REMOTO",
                "idConta: " + id + ", valor: " + req.valor() + ", origemAgencia: " + req.origemAgencia());

        return ResponseEntity.ok(Map.of("mensagem", "Crédito remoto aplicado.", "saldoAtual", conta.getSaldo()));
    }

    private void enviarCreditoRemoto(AgenciaConfig agenciaDestino,
                                      TransferenciaRequest transferencia,
                                      int timestampLamport) throws Exception {
        String corpo = objectMapper.writeValueAsString(new CreditoRemotoRequest(
                transferencia.valor(), timestampLamport, idAgenciaLocal));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(appRouting.resolverUrl(agenciaDestino)
                        + "/contas/" + transferencia.idDestino() + "/creditar-remoto"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(corpo))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Agência de destino respondeu HTTP " + response.statusCode());
        }
    }

    private void registrar(int timestamp, String operacao, Object detalhes) {
        registro.registrarEvento(timestamp, operacao, detalhes.toString());
    }
}