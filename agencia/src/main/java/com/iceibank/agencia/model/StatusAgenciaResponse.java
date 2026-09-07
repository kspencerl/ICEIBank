package com.iceibank.agencia.model;

public record StatusAgenciaResponse(
        int agencia,
        int timestampLamport,
        int contasLocais
) {}
