package com.iceibank.agencia.routing;

import org.springframework.stereotype.Component;

import com.iceibank.agencia.config.AgenciaConfig;
import com.iceibank.agencia.config.AgenciaRoutingProperties;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AppRouting {

    private final AgenciaRoutingProperties routingProperties;


    public AgenciaConfig obterAgenciaResponsavel(int idConta) {
        var agencias = routingProperties.agencias();

        int agenciaIdAlvo = idConta % agencias.size();
        
        return agencias.stream()
                .filter(a -> a.id() == agenciaIdAlvo)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Agência " + agenciaIdAlvo + " não mapeada no YAML."));
    }

    public String resolverUrl(AgenciaConfig agencia) {
        return String.format("http://%s:%d", 
            routingProperties.host(), 
            routingProperties.basePort() + agencia.id()
        );
    }
}
