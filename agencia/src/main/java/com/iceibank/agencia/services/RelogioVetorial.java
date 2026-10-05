package com.iceibank.agencia.services;

import com.iceibank.agencia.config.AgenciaRoutingProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Relógio vetorial da agência: um contador por agência (posição = id da agência).
 *
 * Regras:
 * 1. Evento local: incrementa a própria posição.
 * 2. Envio: incrementa a própria posição e o vetor inteiro vai junto na mensagem.
 * 3. Recebimento: vetor[i] = max(vetor[i], recebido[i]) para todo i, depois incrementa a própria posição.
 *
 * Os métodos são synchronized porque requisições HTTP e o consumidor do RabbitMQ
 * rodam em threads diferentes. 
 */
@Component
public class RelogioVetorial {

    private final int idAgencia;
    private final int[] vetor;

    @Autowired
    public RelogioVetorial(@Value("${agencia.id:0}") int idAgencia, AgenciaRoutingProperties routing) {
        this(idAgencia, routing.agencias().size());
    }

    public RelogioVetorial(int idAgencia, int numeroAgencias) {
        this.idAgencia = idAgencia;
        this.vetor = new int[numeroAgencias];
    }

    public synchronized int[] eventoLocal() {
        vetor[idAgencia]++;
        return vetor.clone();
    }

    public synchronized int[] aoEnviar() {
        vetor[idAgencia]++;
        return vetor.clone();
    }

    public synchronized int[] aoReceber(int[] vetorRecebido) {
        for (int i = 0; i < vetor.length; i++) {
            vetor[i] = Math.max(vetor[i], vetorRecebido[i]);
        }
        vetor[idAgencia]++;
        return vetor.clone();
    }

    public synchronized int[] valorAtual() {
        return vetor.clone();
    }
}
