package com.persecutio.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.Vector2;

// Lê o teclado e produz direção de movimento e animação
public class ControladorInput {

    // Índices de direção usados para selecionar a linha do spritesheet
    public static final int DIRECAO_BAIXO    = 0;
    public static final int DIRECAO_DIREITA  = 1;
    public static final int DIRECAO_ESQUERDA = 2;
    public static final int DIRECAO_CIMA     = 3;

    private final Vector2 direcaoMovimento = new Vector2();
    private int     direcaoAnimacao = DIRECAO_BAIXO;
    private boolean movendo         = false;

    public void atualizar() {
        direcaoMovimento.setZero();
        movendo = false;

        if (Gdx.input.isKeyPressed(Keys.RIGHT) || Gdx.input.isKeyPressed(Keys.D)) {
            direcaoMovimento.x += 1f;
            direcaoAnimacao = DIRECAO_DIREITA;
            movendo = true;
        }
        if (Gdx.input.isKeyPressed(Keys.LEFT) || Gdx.input.isKeyPressed(Keys.A)) {
            direcaoMovimento.x -= 1f;
            direcaoAnimacao = DIRECAO_ESQUERDA;
            movendo = true;
        }
        if (Gdx.input.isKeyPressed(Keys.UP) || Gdx.input.isKeyPressed(Keys.W)) {
            direcaoMovimento.y += 1f;
            direcaoAnimacao = DIRECAO_CIMA;
            movendo = true;
        }
        if (Gdx.input.isKeyPressed(Keys.DOWN) || Gdx.input.isKeyPressed(Keys.S)) {
            direcaoMovimento.y -= 1f;
            direcaoAnimacao = DIRECAO_BAIXO;
            movendo = true;
        }
    }

    public Vector2 getDirecaoMovimento() { return direcaoMovimento; }
    public int     getDirecaoAnimacao()  { return direcaoAnimacao; }
    public boolean isMovendo()           { return movendo; }
}
