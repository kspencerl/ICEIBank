package com.iceibank.agencia.model;

import java.time.Instant;

public record EventoLog(
        String agencia,
        String tipo,
        int[] timestampVetorial,
        String horaParede,
        Object detalhes
) {
    public static EventoLog criar(int agenciaId, int[] timestampVetorial, String tipo, Object detalhes) {
        return new EventoLog("agencia-" + agenciaId, tipo, timestampVetorial, Instant.now().toString(), detalhes);
    }
}
