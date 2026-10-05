package com.iceibank.agencia.mensageria;

import com.iceibank.agencia.model.CreditoRemotoMensagem;
import com.iceibank.agencia.services.CreditoRemotoService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumer: recebe os créditos vindos de outras agências pela fila desta agência.
 * Só cuida do transporte; a regra de negócio fica no CreditoRemotoService.
 * O ack é automático: a mensagem sai da fila quando este método termina sem exceção.
 * Se o crédito não puder ser aplicado, a mensagem é rejeitada sem reenfileirar e o
 * RabbitMQ a move para a dead-letter queue (fila-agencia-{id}.dlq) em vez de descartá-la.
 */
@Component
@RequiredArgsConstructor
public class CreditoRemotoListener {

    private final CreditoRemotoService creditoRemotoService;

    @RabbitListener(queues = "#{filaCreditos.name}")
    public void aoReceber(CreditoRemotoMensagem mensagem) {
        if (!creditoRemotoService.aplicar(mensagem)) {
            throw new AmqpRejectAndDontRequeueException(
                    "Crédito não aplicado (conta " + mensagem.idConta() + " não encontrada); enviado para a DLQ.");
        }
    }
}
