package com.iceibank.agencia.auth;

public record LoginResponse(
        String token,
        String tipo,
        long expiraEmSegundos
) {}
