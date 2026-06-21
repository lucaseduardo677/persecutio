package com.persecutio.managers;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.persecutio.game.PersecutioGame;

// agrupa referências de renderização e os offsets de câmera calculados para o frame
public class ContextoRender {

    public final SpriteBatch batch;
    public final BitmapFont  fonteMenu;
    public final BitmapFont  fonteDialogos;
    public final BitmapFont  fonteNomes;
    public final BitmapFont  fonteIndicadores;

    // dimensões do viewport virtual
    public final float vLargura;
    public final float vAltura;

    // centro da tela em coordenadas virtuais
    public final float centroX;
    public final float centroY;

    // offset para converter coordenada de mundo em coordenada de tela
    public final float cameraX;
    public final float cameraY;

    public final Camera   camera;
    public final Viewport viewport;

    // câmera segue o jogador
    public ContextoRender(PersecutioGame jogo, float jogadorMundoX, float jogadorMundoY) {
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

    // câmera fixa no centro do cômodo quando a propriedade cameraEstatica estiver ativa
    public ContextoRender(PersecutioGame jogo, float jogadorMundoX, float jogadorMundoY,
                          GerenciadorComodos.Comodo comodo) {
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

        if (comodo != null && comodo.estatica) {
            // ancora a câmera no centro geométrico do cômodo
            float comodoMeioX = comodo.area.x + comodo.area.width  / 2f;
            float comodoMeioY = comodo.area.y + comodo.area.height / 2f;
            this.cameraX = Math.round(centroX - comodoMeioX);
            this.cameraY = Math.round(centroY - comodoMeioY);
        } else {
            this.cameraX = Math.round(centroX - jogadorMundoX);
            this.cameraY = Math.round(centroY - jogadorMundoY);
        }
    }

    // converte coordenada de mundo para coordenada de tela
    public float mundoParaTelaX(float mundoX) { return cameraX + mundoX; }
    public float mundoParaTelaY(float mundoY) { return cameraY + mundoY; }
}
