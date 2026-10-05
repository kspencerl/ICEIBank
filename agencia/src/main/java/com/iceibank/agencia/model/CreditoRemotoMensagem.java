package com.iceibank.agencia.model;

/**
 * Mensagem publicada no RabbitMQ quando uma transferência credita uma conta de outra agência.
 * Leva o vetor do relógio no momento do envio para o destino aplicar a regra de recebimento.
 */
public record CreditoRemotoMensagem(
        int idConta,
        double valor,
        int[] vetorEnvio,
        int origemAgencia
) {}
