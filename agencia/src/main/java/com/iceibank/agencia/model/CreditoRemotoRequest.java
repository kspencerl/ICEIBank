package com.iceibank.agencia.model;

public record CreditoRemotoRequest(
        double valor,
        int timestampLamport,
        int origemAgencia
) {}