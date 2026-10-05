package com.iceibank.agencia.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    @Value("${security.jwt.secret}")
    private String secret;

    @Value("${security.jwt.expiration-seconds:900}")
    private long expirationSeconds;

    private SecretKey signingKey;

    @PostConstruct
    void initialize() {
        signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    public String gerarToken(String sujeito) {
        Date agora = new Date();
        Date expiracao = new Date(agora.getTime() + expirationSeconds * 1000);
        return Jwts.builder()
                .subject(sujeito)
                .issuedAt(agora)
                .expiration(expiracao)
                .signWith(signingKey)
                .compact();
    }

    public boolean ehValido(String token) {
        try {
            extrairClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException erro) {
            return false;
        }
    }

    public String extrairSujeito(String token) {
        return extrairClaims(token).getSubject();
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    private Claims extrairClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
