package com.iceibank.agencia.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Conta {
    private int id;
    private String nomeAluno;
    private double saldo;
}