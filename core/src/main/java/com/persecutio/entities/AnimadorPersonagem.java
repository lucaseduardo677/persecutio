package com.persecutio.entities;

// controla a animação de frame do personagem enquanto caminha
public class AnimadorPersonagem {

    // duração de cada frame em segundos
    private static final float DURACAO_FRAME = 0.1f;
    private static final int   TOTAL_FRAMES  = 4;

    private int   frame  = 0;
    private float timer  = 0f;

    public void atualizar(float delta, boolean andando) {
        if (andando) {
            timer += delta;
            if (timer >= DURACAO_FRAME) {
                timer = 0f;
                frame = (frame + 1) % TOTAL_FRAMES;
            }
        } else {
            // volta para o frame parado ao parar de mover
            frame = 0;
            timer = 0f;
        }
    }

    public int getFrame() { return frame; }
}
