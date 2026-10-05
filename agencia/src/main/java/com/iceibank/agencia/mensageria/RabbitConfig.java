package com.iceibank.agencia.mensageria;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topologia do RabbitMQ (Parte A). O Spring declara tudo isso no broker ao conectar:
 *
 *   exchange "iceibank.eventos" (topic, durável)
 *       └── routing key "agencia.{id}.creditar" ──> fila "fila-agencia-{id}" (durável)
 *
 * Cada agência declara apenas a SUA fila; quem publica só conhece a exchange e a routing key.
 */
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "iceibank.eventos";

    public static String nomeFila(int idAgencia) {
        return "fila-agencia-" + idAgencia;
    }

    public static String routingKeyCredito(int idAgencia) {
        return "agencia." + idAgencia + ".creditar";
    }

    @Bean
    public TopicExchange exchangeEventos() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue filaCreditos(@Value("${agencia.id:0}") int idAgencia) {
        return QueueBuilder.durable(nomeFila(idAgencia)).build();
    }

    @Bean
    public Binding bindingCreditos(Queue filaCreditos, TopicExchange exchangeEventos,
                                   @Value("${agencia.id:0}") int idAgencia) {
        return BindingBuilder.bind(filaCreditos).to(exchangeEventos).with(routingKeyCredito(idAgencia));
    }

    /** Envia/recebe as mensagens como JSON (em vez de objetos Java serializados). */
    @Bean
    public MessageConverter conversorJson() {
        return new JacksonJsonMessageConverter();
    }
}
