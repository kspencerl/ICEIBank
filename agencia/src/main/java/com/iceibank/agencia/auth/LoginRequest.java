package com.iceibank.agencia.auth;

public record LoginRequest(
        String usuario,
        String senha
) {}
