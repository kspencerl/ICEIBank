package com.iceibank.agencia.model;

import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class Contador {

    private final AtomicInteger valor = new AtomicInteger(0);

    public int incrementar() {
        return valor.incrementAndGet();
    }

    public int verificarMax(int timestampEstrangeiro) {
        return valor.updateAndGet(atual -> Math.max(atual, timestampEstrangeiro) + 1);
    }

    public int obterValorAtual() {
        return valor.get();
    }
}
