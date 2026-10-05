package com.iceibank.agencia.mensageria;

import com.iceibank.agencia.model.CreditoRemotoMensagem;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Producer: publica eventos na exchange. Não sabe (nem precisa saber) quem vai consumir.
 * O RabbitTemplate envia mensagens persistentes por padrão, então elas sobrevivem
 * no broker mesmo com a agência de destino fora do ar.
 */
@Component
@RequiredArgsConstructor
public class MensageriaPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publicarCredito(int idAgenciaDestino, CreditoRemotoMensagem mensagem) {
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.routingKeyCredito(idAgenciaDestino), mensagem);
    }
}
