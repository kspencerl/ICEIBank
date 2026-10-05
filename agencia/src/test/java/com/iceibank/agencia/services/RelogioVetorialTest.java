package com.iceibank.agencia.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class RelogioVetorialTest {

    @Test
    void eventoLocalIncrementaSomenteAPropriaPosicao() {
        RelogioVetorial relogio = new RelogioVetorial(1, 3);

        assertArrayEquals(new int[]{0, 1, 0}, relogio.eventoLocal());
        assertArrayEquals(new int[]{0, 2, 0}, relogio.eventoLocal());
    }

    @Test
    void aoEnviarIncrementaAPropriaPosicao() {
        RelogioVetorial relogio = new RelogioVetorial(0, 3);

        assertArrayEquals(new int[]{1, 0, 0}, relogio.aoEnviar());
    }

    @Test
    void aoReceberFazMaximoPosicaoAPosicaoEDepoisIncrementa() {
        RelogioVetorial relogio = new RelogioVetorial(2, 3);
        relogio.eventoLocal(); // [0, 0, 1]

        int[] vetor = relogio.aoReceber(new int[]{3, 1, 0});

        assertArrayEquals(new int[]{3, 1, 2}, vetor);
    }

    @Test
    void retornaCopiaParaProtegerOVetorInterno() {
        RelogioVetorial relogio = new RelogioVetorial(0, 3);
        int[] copia = relogio.eventoLocal();

        copia[0] = 99;

        assertArrayEquals(new int[]{1, 0, 0}, relogio.valorAtual());
    }
}
