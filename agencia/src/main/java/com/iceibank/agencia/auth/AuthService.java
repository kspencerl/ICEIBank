package com.iceibank.agencia.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
public class AuthService {

    @Value("${security.auth.username}")
    private String usuarioConfigurado;

    @Value("${security.auth.password}")
    private String senhaConfigurada;

    private final JwtService jwtService;

    public AuthService(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    public LoginResponse autenticar(LoginRequest request) {
        if (request == null || !igual(request.usuario(), usuarioConfigurado)
                || !igual(request.senha(), senhaConfigurada)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas.");
        }
        return new LoginResponse(
                jwtService.gerarToken(request.usuario()),
                "Bearer",
                jwtService.getExpirationSeconds());
    }

    private boolean igual(String recebido, String esperado) {
        if (recebido == null) {
            return false;
        }
        return MessageDigest.isEqual(
                recebido.getBytes(StandardCharsets.UTF_8),
                esperado.getBytes(StandardCharsets.UTF_8));
    }
}
