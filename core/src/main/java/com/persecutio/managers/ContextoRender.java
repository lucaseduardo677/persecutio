package com.persecutio.managers;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.persecutio.game.PersecutioGame;

// Contexto compartilhado para renderizacao
public class ContextoRender {

    // Batch para desenho
    public SpriteBatch batch;
    // Fonte do menu
    public BitmapFont  fonteMenu;
    // Fonte dos dialogos
    public BitmapFont  fonteDialogos;
    // Fonte dos nomes
    public BitmapFont  fonteNomes;
    // Fonte dos indicadores
    public BitmapFont  fonteIndicadores;

    // Largura da tela virtual
    public float vLargura;
    // Altura da tela virtual
    public float vAltura;

    // Centro X da tela virtual
    public float centroX;
    // Centro Y da tela virtual
    public float centroY;

    // Offset X da camera
    public float cameraX;
    // Offset Y da camera
    public float cameraY;

    // Camera atual
    public Camera   camera;
    // Viewport atual
    public Viewport viewport;

    // Construtor do contexto de renderizacao
    public ContextoRender() {}

    // Atualiza camera e referencias da tela
    public void atualizar(PersecutioGame jogo, float jogadorMundoX, float jogadorMundoY) {
        this.batch            = jogo.batch;
        this.fonteMenu        = jogo.fonteMenu;
        this.fonteDialogos    = jogo.fonteDialogos;
        this.fonteNomes       = jogo.fonteNomes;
        this.fonteIndicadores = jogo.fonteIndicadores;

        this.vLargura = jogo.viewport.getWorldWidth();
        this.vAltura  = jogo.viewport.getWorldHeight();
        this.camera   = jogo.viewport.getCamera();
        this.viewport = jogo.viewport;

        this.centroX = Math.round(vLargura / 2f);
        this.centroY = Math.round(vAltura  / 2f);
        this.cameraX = Math.round(centroX - jogadorMundoX);
        this.cameraY = Math.round(centroY - jogadorMundoY);
    }

    // Atualiza camera com comodo estatico
    public void atualizar(PersecutioGame jogo, float jogadorMundoX, float jogadorMundoY,
                          GerenciadorComodos.Comodo comodo) {
        atualizar(jogo, jogadorMundoX, jogadorMundoY);

        if (comodo != null && comodo.cameraEstatica) {
            float comodoMeioX = comodo.areaCamera.x + comodo.areaCamera.width  / 2f;
            float comodoMeioY = comodo.areaCamera.y + comodo.areaCamera.height / 2f;
            this.cameraX = Math.round(centroX - comodoMeioX);
            this.cameraY = Math.round(centroY - comodoMeioY);
        }
    }

    // Converte coordenada X do mundo para coordenada da tela
    public float mundoParaTelaX(float mundoX) { return cameraX + mundoX; }

    // Converte coordenada Y do mundo para coordenada da tela
    public float mundoParaTelaY(float mundoY) { return cameraY + mundoY; }
}