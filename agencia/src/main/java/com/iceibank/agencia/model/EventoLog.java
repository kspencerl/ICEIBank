package com.iceibank.agencia.model;

import java.time.Instant;

public record EventoLog(
        String agencia,
        String tipo,
        int timestampLamport,
        String horaParede,
        Object detalhes
) {
    public static EventoLog criar(int agenciaId, int timestampLamport, String tipo, Object detalhes) {
        return new EventoLog("agencia-" + agenciaId, tipo, timestampLamport, Instant.now().toString(), detalhes);
    }
}