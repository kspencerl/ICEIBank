package com.iceibank.agencia.services;

import com.iceibank.agencia.model.Conta;
import com.iceibank.agencia.model.CreditoRemotoMensagem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/** Aplica na conta local um crédito que chegou de outra agência via mensageria. */
@Service
@RequiredArgsConstructor
public class CreditoRemotoService {

    private final ContaRepository contas;
    private final RelogioVetorial relogio;
    private final EventLogService registro;

    public void aplicar(CreditoRemotoMensagem mensagem) {
        // Receber a mensagem é um evento: aplica a regra 3 do relógio vetorial.
        int[] vetor = relogio.aoReceber(mensagem.vetorEnvio());

        Conta conta = contas.buscar(mensagem.idConta());
        if (conta == null) {
            // Ex.: a agência reiniciou e perdeu as contas em memória antes de consumir a mensagem.
            Map<String, Object> detalhes = detalhes(mensagem);
            detalhes.put("motivo", "conta nao encontrada");
            registro.registrarEvento(vetor, "CREDITO_REMOTO_FALHOU", detalhes);
            return;
        }

        conta.setSaldo(conta.getSaldo() + mensagem.valor());
        registro.registrarEvento(vetor, "TRANSFERENCIA_CREDITO_REMOTO", detalhes(mensagem));
    }

    private Map<String, Object> detalhes(CreditoRemotoMensagem mensagem) {
        Map<String, Object> detalhes = new LinkedHashMap<>();
        detalhes.put("idConta", mensagem.idConta());
        detalhes.put("valor", mensagem.valor());
        detalhes.put("origemAgencia", mensagem.origemAgencia());
        return detalhes;
    }
}
