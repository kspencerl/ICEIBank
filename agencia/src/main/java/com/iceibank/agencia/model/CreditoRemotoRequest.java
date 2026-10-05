package com.iceibank.agencia.model;

public record CreditoRemotoRequest(
        Double valor,
        int[] vetorEnvio,
        int origemAgencia
) {}