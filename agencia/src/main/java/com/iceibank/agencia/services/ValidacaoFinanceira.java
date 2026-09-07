package com.iceibank.agencia.services;

import org.springframework.stereotype.Service;

@Service
public class ValidacaoFinanceira {

    public boolean valorPositivo(Double valor) {
        return valor != null && Double.isFinite(valor) && valor > 0;
    }

    public boolean saldoInicialValido(Double saldoInicial) {
        return saldoInicial == null
                || (Double.isFinite(saldoInicial) && saldoInicial >= 0);
    }
}
