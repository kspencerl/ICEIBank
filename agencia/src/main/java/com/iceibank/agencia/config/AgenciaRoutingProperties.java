package com.iceibank.agencia.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.List;

@ConfigurationProperties(prefix = "agencia-routing")
public record AgenciaRoutingProperties(
    String host,
    int basePort,
    List<AgenciaConfig> agencias
) {}
