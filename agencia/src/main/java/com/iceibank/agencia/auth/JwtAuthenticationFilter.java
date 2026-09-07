package com.iceibank.agencia.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7).trim();
            if (jwtService.ehValido(token)) {
                String sujeito = jwtService.extrairSujeito(token);
                String tipo = jwtService.extrairTipo(token);
                var authorities = "AGENCIA".equals(tipo)
                    ? AuthorityUtils.createAuthorityList("ROLE_AGENCIA")
                    : AuthorityUtils.NO_AUTHORITIES;
                var autenticacao = new UsernamePasswordAuthenticationToken(
                    sujeito, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(autenticacao);
            }
        }
        filterChain.doFilter(request, response);
    }
}
