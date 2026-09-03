package com.iceibank.agencia.model;

import java.time.Instant;

public record EventoLog(
        int agenciaId,
        int lamportTime,
        String operacao,
        String detalhes,
        String relogioParede
) {
    public static EventoLog criar(int agenciaId, int lamportTime, String operacao, String detalhes) {
        return new EventoLog(agenciaId, lamportTime, operacao, detalhes, Instant.now().toString());
    }
}