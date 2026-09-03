package com.iceibank.agencia.model;

public record CriarContaRequest(
        int id,
        String nomeAluno,
        Double saldoInicial
) {}
