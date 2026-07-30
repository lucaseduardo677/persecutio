package com.persecutio.managers;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class GerenciadorPontuacaoTest {

    @Test
    public void testFinalBomParaPontuacaoMaxima() {
        GerenciadorPontuacao pontuacao = new GerenciadorPontuacao();
        pontuacao.adicionarPontos(19);
        assertEquals(GerenciadorPontuacao.Final.BOM, pontuacao.obterFinal());
    }
}
