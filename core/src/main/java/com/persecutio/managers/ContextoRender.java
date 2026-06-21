package com.persecutio.managers;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.persecutio.game.PersecutioGame;

// Contexto compartilhado de renderizacao
public class ContextoRender {

    public SpriteBatch batch;
    public BitmapFont  fonteMenu;
    public BitmapFont  fonteDialogos;
    public BitmapFont  fonteNomes;
    public BitmapFont  fonteIndicadores;

    public float vLargura;
    public float vAltura;

    public float centroX;
    public float centroY;

    public float cameraX;
    public float cameraY;

    public Camera   camera;
    public Viewport viewport;

    public ContextoRender() {}

// Atualiza camera e referencias de tela
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

// Ajusta a camera para um comodo estatico
    public void atualizar(PersecutioGame jogo, float jogadorMundoX, float jogadorMundoY,
                          GerenciadorComodos.Comodo comodo) {
        atualizar(jogo, jogadorMundoX, jogadorMundoY);

        if (comodo != null && comodo.cameraEstatica) {
            float comodoMeioX = comodo.area.x + comodo.area.width  / 2f;
            float comodoMeioY = comodo.area.y + comodo.area.height / 2f;
            this.cameraX = Math.round(centroX - comodoMeioX);
            this.cameraY = Math.round(centroY - comodoMeioY);
        }
    }

// Converte coordenadas do mundo para tela
    public float mundoParaTelaX(float mundoX) { return cameraX + mundoX; }
    public float mundoParaTelaY(float mundoY) { return cameraY + mundoY; }
}
