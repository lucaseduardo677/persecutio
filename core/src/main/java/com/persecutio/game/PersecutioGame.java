package com.persecutio.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.persecutio.managers.GerenciadorAudio;
import com.persecutio.screens.TelaMenu;

// Ponto de entrada do jogo
public class PersecutioGame extends Game {

    // Resolução virtual base do viewport
    public static final int V_LARGURA = 640;
    public static final int V_ALTURA  = 480;

    public SpriteBatch   batch;
    public AssetManager  assets;
    public ExtendViewport viewport;
    public GerenciadorAudio audio;

    // Fontes compartilhadas entre telas
    public BitmapFont fonteMenu;
    public BitmapFont fonteDialogos;
    public BitmapFont fonteNomes;
    public BitmapFont fonteIndicadores;

    @Override
    public void create() {
        batch    = new SpriteBatch();
        assets   = new AssetManager();
        viewport = new ExtendViewport(V_LARGURA, V_ALTURA);
        audio    = new GerenciadorAudio();

        atualizarFontes(Gdx.graphics.getHeight());

        setScreen(new TelaMenu(this));
    }

    // Recria as fontes escaladas para a altura física atual da janela
    public void atualizarFontes(int alturaFisica) {
        if (fonteMenu        != null) fonteMenu.dispose();
        if (fonteDialogos    != null) fonteDialogos.dispose();
        if (fonteNomes       != null) fonteNomes.dispose();
        if (fonteIndicadores != null) fonteIndicadores.dispose();

        float proporcaoY = (float) alturaFisica / V_ALTURA;

        fonteMenu        = carregarFonte("fonts/fonte_titulo.ttf",      28, proporcaoY, Color.WHITE, false);
        fonteDialogos    = carregarFonte("fonts/fonte_dialogos.ttf",    14, proporcaoY, Color.WHITE, true);
        fonteNomes       = carregarFonte("fonts/fonte_nomes.ttf",       16, proporcaoY, Color.WHITE, true);
        fonteIndicadores = carregarFonte("fonts/fonte_indicadores.ttf", 12, proporcaoY, Color.WHITE, true);
    }

    // Carrega a fonte true type com borda opcional
    private BitmapFont carregarFonte(String caminho, int tamanhoVirtual,
                                     float proporcaoY, Color cor, boolean comBorda) {
        int tamanhoFisico = Math.max(1, Math.round(tamanhoVirtual * proporcaoY));

        if (Gdx.files.internal(caminho).exists()) {
            try {
                FreeTypeFontGenerator gerador   = new FreeTypeFontGenerator(Gdx.files.internal(caminho));
                FreeTypeFontParameter parametro = new FreeTypeFontParameter();
                parametro.size        = tamanhoFisico;
                parametro.color       = cor;
                parametro.minFilter   = Texture.TextureFilter.Nearest;
                parametro.magFilter   = Texture.TextureFilter.Nearest;

                if (comBorda) {
                    parametro.borderWidth = Math.max(0.5f, 1f * (tamanhoFisico / 14f));
                    parametro.borderColor = Color.BLACK;
                }

                BitmapFont fonte = gerador.generateFont(parametro);
                gerador.dispose();

                // Escala inversa para compensar a geração em tamanho físico
                fonte.getData().setScale(1f / proporcaoY);
                return fonte;
            } catch (Exception e) {
                // Fallback para a fonte padrão do LibGDX se o arquivo falhar
                BitmapFont padrao = new BitmapFont();
                padrao.getData().setScale(tamanhoVirtual / 15f);
                return padrao;
            }
        }

        // Fonte padrão quando o arquivo não existe
        BitmapFont padrao = new BitmapFont();
        padrao.getData().setScale(tamanhoVirtual / 15f);
        return padrao;
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        atualizarFontes(height);
        super.resize(width, height);
    }

    @Override
    public void dispose() {
        batch.dispose();
        assets.dispose();
        if (audio != null) audio.dispose();

        if (fonteMenu        != null) fonteMenu.dispose();
        if (fonteDialogos    != null) fonteDialogos.dispose();
        if (fonteNomes       != null) fonteNomes.dispose();
        if (fonteIndicadores != null) fonteIndicadores.dispose();
    }
}
