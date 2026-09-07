package com.iceibank.agencia.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidacaoFinanceiraTest {

    private final ValidacaoFinanceira validacao = new ValidacaoFinanceira();

    @Test
    void aceitaValoresMonetariosPositivosEFinitos() {
        assertTrue(validacao.valorPositivo(0.01));
        assertTrue(validacao.valorPositivo(100.0));
    }

    @Test
    void rejeitaValoresNulosZeroNegativosENaoFinitos() {
        assertFalse(validacao.valorPositivo(null));
        assertFalse(validacao.valorPositivo(0.0));
        assertFalse(validacao.valorPositivo(-1.0));
        assertFalse(validacao.valorPositivo(Double.NaN));
        assertFalse(validacao.valorPositivo(Double.POSITIVE_INFINITY));
    }

    @Test
    void aceitaSaldoInicialAusenteOuNaoNegativo() {
        assertTrue(validacao.saldoInicialValido(null));
        assertTrue(validacao.saldoInicialValido(0.0));
        assertTrue(validacao.saldoInicialValido(100.0));
    }

    @Test
    void rejeitaSaldoInicialNegativoOuNaoFinito() {
        assertFalse(validacao.saldoInicialValido(-0.01));
        assertFalse(validacao.saldoInicialValido(Double.NaN));
        assertFalse(validacao.saldoInicialValido(Double.NEGATIVE_INFINITY));
    }
}
