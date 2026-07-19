package com.persecutio.entities;

// Animacao de frames do personagem enquanto caminha
public class AnimadorPersonagem {

    // Duracao de cada frame em segundos
    private static final float DURACAO_FRAME = 0.1f;
    // Total de frames na animacao
    private static final int   TOTAL_FRAMES  = 4;

    // Frame atual
    private int   frame  = 0;
    // Timer para troca de frame
    private float timer  = 0f;

    // Atualiza animacao
    public void atualizar(float delta, boolean andando) {
        if (andando) {
            timer += delta;
            if (timer >= DURACAO_FRAME) {
                timer = 0f;
                frame = (frame + 1) % TOTAL_FRAMES;
            }
        } else {
            frame = 0;
            timer = 0f;
        }
    }

    public int getFrame() { return frame; }
}
