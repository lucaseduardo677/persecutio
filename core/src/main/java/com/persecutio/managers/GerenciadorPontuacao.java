package com.persecutio.managers;

// Controla a pontuacao do questionario final do Dr Elimar e decide o final do jogo
public class GerenciadorPontuacao {

    // Soma do maior valor possivel em cada uma das 7 perguntas GDD item 5
    private static final int PONTOS_MAXIMO = 19;

    // Abaixo deste percentual o jogador recebe o Final Ruim
    private static final float LIMIAR_RUIM = 20f;
    // Acima deste percentual o jogador recebe o Final Bom
    private static final float LIMIAR_BOM  = 80f;

    // Finais possiveis do jogo GDD item 13
    public enum Final { RUIM, NORMAL, BOM }

    // Pontos acumulados nas respostas do questionario
    private int pontos = 0;

    // Soma os pontos de uma resposta escolhida pelo jogador
    public void adicionarPontos(int valor) {
        pontos += valor;
    }

    public int obterTotal() {
        return pontos;
    }

    public float obterPercentual() {
        return PONTOS_MAXIMO > 0 ? (pontos / (float) PONTOS_MAXIMO) * 100f : 0f;
    }

    // Calcula qual final o jogador atingiu com base no percentual atual
    public Final obterFinal() {
        float percentual = obterPercentual();
        if (percentual < LIMIAR_RUIM) return Final.RUIM;
        if (percentual > LIMIAR_BOM)  return Final.BOM;
        return Final.NORMAL;
    }

    public String obterNomeImagemFinal() {
        switch (obterFinal()) {
            case BOM:
                return "finalBom.webp";
            case RUIM:
                return "finalRuim.webp";
            default:
                return "finalMedio.webp";
        }
    }

    // Reinicia a pontuacao usado ao comecar uma nova partida
    public void reiniciar() {
        pontos = 0;
    }
}
