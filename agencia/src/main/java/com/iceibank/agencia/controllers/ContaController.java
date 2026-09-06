package com.iceibank.agencia.controllers;

import com.iceibank.agencia.model.Conta;
import com.iceibank.agencia.model.CriarContaRequest;
import com.iceibank.agencia.model.TransacaoRequest;
import com.iceibank.agencia.routing.AppRouting;
import com.iceibank.agencia.services.EventLogService;
import com.iceibank.agencia.services.RelogioLamport;
import com.iceibank.agencia.services.ContaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/contas")
@RequiredArgsConstructor
public class ContaController {

    @Value("${agencia.id:0}")
    private int idAgenciaLocal;

    private final AppRouting appRouting;
    private final RelogioLamport relogio;
    private final EventLogService registro;
    private final ContaRepository contas;

    @PostMapping
    public ResponseEntity<?> criarConta(@RequestBody CriarContaRequest req) {
        if (appRouting.obterAgenciaResponsavel(req.id()).id() != idAgenciaLocal) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("erro", "Conta " + req.id() + " não pertence a esta agência."));
        }
        if (contas.existe(req.id())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("erro", "Conta já existe."));
        }

        int ts = relogio.eventoLocal();
        double saldoInicial = req.saldoInicial() != null ? req.saldoInicial() : 0.0;
        Conta novaConta = new Conta(req.id(), req.nomeAluno(), saldoInicial);

        contas.salvar(novaConta);

        registro.registrarEvento(ts, "CRIAR_CONTA", "id: " + req.id() + ", nome: " + req.nomeAluno() + ", saldoInicial: " + saldoInicial);

        return ResponseEntity.status(HttpStatus.CREATED).body(novaConta);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> consultarSaldo(@PathVariable int id) {
        Conta conta = contas.buscar(id);
        if (conta == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("erro", "Conta não encontrada nesta agência."));
        }
        return ResponseEntity.ok(conta);
    }

    @PostMapping("/{id}/depositar")
    public ResponseEntity<?> depositar(@PathVariable int id, @RequestBody TransacaoRequest req) {
        Conta conta = contas.buscar(id);
        if (conta == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("erro", "Conta não encontrada nesta agência."));
        }

        int ts = relogio.eventoLocal();
        conta.setSaldo(conta.getSaldo() + req.valor());

        registro.registrarEvento(ts, "DEPOSITO", "id: " + id + ", valor: " + req.valor() + ", novoSaldo: " + conta.getSaldo());

        return ResponseEntity.ok(conta);
    }

    @PostMapping("/{id}/sacar")
    public ResponseEntity<?> sacar(@PathVariable int id, @RequestBody TransacaoRequest req) {
        Conta conta = contas.buscar(id);
        if (conta == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("erro", "Conta não encontrada nesta agência."));
        }
        if (conta.getSaldo() < req.valor()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("erro", "Saldo insuficiente."));
        }

        int ts = relogio.eventoLocal();
        conta.setSaldo(conta.getSaldo() - req.valor());

        registro.registrarEvento(ts, "SAQUE", "id: " + id + ", valor: " + req.valor() + ", novoSaldo: " + conta.getSaldo());

        return ResponseEntity.ok(conta);
    }
}
