package com.iceibank.agencia.model;

public record StatusAgenciaResponse(
        int agencia,
        int[] timestampVetorial,
        int contasLocais
) {}
