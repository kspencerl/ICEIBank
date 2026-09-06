package com.iceibank.agencia.model;

public record TransferenciaRequest(
        int idOrigem,
        int idDestino,
        double valor
) {}