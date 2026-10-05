package com.iceibank.agencia.mensageria;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "iceibank.eventos";
    public static final String EXCHANGE_DLX = "iceibank.dlx";

    public static String nomeFila(int idAgencia) {
        return "fila-agencia-" + idAgencia;
    }

    public static String nomeFilaDlq(int idAgencia) {
        return nomeFila(idAgencia) + ".dlq";
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
        return QueueBuilder.durable(nomeFila(idAgencia))
                .deadLetterExchange(EXCHANGE_DLX)
                .deadLetterRoutingKey(nomeFilaDlq(idAgencia))
                .build();
    }

    @Bean
    public Binding bindingCreditos(Queue filaCreditos, TopicExchange exchangeEventos,
                                   @Value("${agencia.id:0}") int idAgencia) {
        return BindingBuilder.bind(filaCreditos).to(exchangeEventos).with(routingKeyCredito(idAgencia));
    }

    @Bean
    public DirectExchange exchangeDlx() {
        return new DirectExchange(EXCHANGE_DLX, true, false);
    }

    @Bean
    public Queue filaDlq(@Value("${agencia.id:0}") int idAgencia) {
        return QueueBuilder.durable(nomeFilaDlq(idAgencia)).build();
    }

    @Bean
    public Binding bindingDlq(Queue filaDlq, DirectExchange exchangeDlx,
                              @Value("${agencia.id:0}") int idAgencia) {
        return BindingBuilder.bind(filaDlq).to(exchangeDlx).with(nomeFilaDlq(idAgencia));
    }

    @Bean
    public MessageConverter conversorJson() {
        return new JacksonJsonMessageConverter();
    }
}
