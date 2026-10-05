package com.iceibank.agencia.controllers;

import com.iceibank.agencia.config.AgenciaConfig;
import com.iceibank.agencia.mensageria.MensageriaPublisher;
import com.iceibank.agencia.model.Conta;
import com.iceibank.agencia.model.CreditoRemotoMensagem;
import com.iceibank.agencia.model.TransferenciaRequest;
import com.iceibank.agencia.routing.AppRouting;
import com.iceibank.agencia.services.ContaRepository;
import com.iceibank.agencia.services.EventLogService;
import com.iceibank.agencia.services.RelogioVetorial;
import com.iceibank.agencia.services.ValidacaoFinanceira;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.AmqpException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class TransferenciaController {

    @Value("${agencia.id:0}")
    private int idAgenciaLocal;

    private final ContaRepository contas;
    private final AppRouting appRouting;
    private final RelogioVetorial relogio;
    private final EventLogService registro;
    private final MensageriaPublisher mensageria;
    private final ValidacaoFinanceira validacaoFinanceira;

    @PostMapping("/transferencias")
    public ResponseEntity<?> transferir(@RequestBody TransferenciaRequest req) {
        if (!validacaoFinanceira.valorPositivo(req.valor())) {
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
        int[] vetorDebito = relogio.eventoLocal();
        contaOrigem.setSaldo(contaOrigem.getSaldo() - req.valor());
        registrar(vetorDebito, "TRANSFERENCIA_DEBITO", detalhesTransferencia(req));

        if (agenciaDestino.id() == idAgenciaLocal) {
            Conta contaDestino = contas.buscar(req.idDestino());
            if (contaDestino == null) {
                contaOrigem.setSaldo(contaOrigem.getSaldo() + req.valor());
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("erro", "Conta de destino não encontrada."));
            }

            int[] vetorCredito = relogio.eventoLocal();
            contaDestino.setSaldo(contaDestino.getSaldo() + req.valor());
            registrar(vetorCredito, "TRANSFERENCIA_CREDITO", detalhesTransferencia(req));
            return ResponseEntity.ok(Map.of("mensagem", "Transferência concluída (mesma agência)."));
        }

        // Sprint 2: em vez de chamar a outra agência por REST (Sprint 1), publicamos um evento
        // no RabbitMQ. A agência de destino consome quando puder; se estiver fora do ar,
        // a mensagem fica retida na fila durável e é entregue quando ela voltar.
        int[] vetorEnvio = relogio.aoEnviar();
        try {
            mensageria.publicarCredito(agenciaDestino.id(),
                    new CreditoRemotoMensagem(req.idDestino(), req.valor(), vetorEnvio, idAgenciaLocal));
        } catch (AmqpException erro) {
            // Broker inacessível: nada foi publicado, então desfazemos o débito.
            contaOrigem.setSaldo(contaOrigem.getSaldo() + req.valor());
            Map<String, Object> detalhes = detalhesTransferencia(req);
            detalhes.put("erro", erro.getMessage());
            registrar(relogio.eventoLocal(), "TRANSFERENCIA_FALHOU", detalhes);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("erro", "Mensageria indisponível. A transferência não foi realizada."));
        }
        registrar(vetorEnvio, "TRANSFERENCIA_PUBLICADA", detalhesTransferencia(req));

        // 200 aqui significa "mensagem publicada", não "crédito já aplicado" (entrega assíncrona).
        return ResponseEntity.ok(Map.of("mensagem", "Transferência publicada para a agência de destino (entrega assíncrona)."));
    }

    private Map<String, Object> detalhesTransferencia(TransferenciaRequest req) {
        Map<String, Object> detalhes = new LinkedHashMap<>();
        detalhes.put("idOrigem", req.idOrigem());
        detalhes.put("idDestino", req.idDestino());
        detalhes.put("valor", req.valor());
        return detalhes;
    }

    private void registrar(int[] timestampVetorial, String tipo, Object detalhes) {
        registro.registrarEvento(timestampVetorial, tipo, detalhes);
    }
}
