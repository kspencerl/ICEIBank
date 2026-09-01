package com.iceibank.agencia.services;

import com.iceibank.agencia.model.Contador;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RelogioLamport {

    private final Contador contador;

    public int eventoLocal() {
        return contador.incrementar();
    }

    public int aoEnviar() {
        return contador.incrementar();
    }

    public int aoReceber(int timestamp) {
        return contador.verificarMax(timestamp);
    }
}
