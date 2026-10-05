package com.iceibank.agencia.mensageria;

import com.iceibank.agencia.model.CreditoRemotoMensagem;
import com.iceibank.agencia.services.CreditoRemotoService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumer: recebe os créditos vindos de outras agências pela fila desta agência.
 * Só cuida do transporte; a regra de negócio fica no CreditoRemotoService.
 * O ack é automático: a mensagem sai da fila quando este método termina sem exceção.
 */
@Component
@RequiredArgsConstructor
public class CreditoRemotoListener {

    private final CreditoRemotoService creditoRemotoService;

    @RabbitListener(queues = "#{filaCreditos.name}")
    public void aoReceber(CreditoRemotoMensagem mensagem) {
        creditoRemotoService.aplicar(mensagem);
    }
}
