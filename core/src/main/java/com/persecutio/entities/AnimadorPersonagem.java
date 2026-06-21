package com.persecutio.entities;

// Animação frame personagem enquanto caminha
public class AnimadorPersonagem {

    // Duração cada frame segundos
    private static final float DURACAO_FRAME = 0.1f;
    private static final int   TOTAL_FRAMES  = 4;

    private int   frame  = 0;
    private float timer  = 0f;

    // Atualização do estado
    public void atualizar(float delta, boolean andando) {
        if (andando) {
            timer += delta;
            if (timer >= DURACAO_FRAME) {
                timer = 0f;
                frame = (frame + 1) % TOTAL_FRAMES;
            }
        } else {
            // Volta frame parado parar mover
            frame = 0;
            timer = 0f;
        }
    }

    // Consulta do estado
    public int getFrame() { return frame; }
}
