package com.persecutio.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;

// gerencia música ambiente e efeitos sonoros do jogo
public class GerenciadorAudio {

    // volume máximo da música ambiente
    private static final float VOLUME_MAX  = 0.4f;
    // unidades de volume por segundo durante o fade
    private static final float FADE_SPEED  = 0.8f;

    private Music ambiente;
    private Sound somPorta;

    private boolean fazendoFadeOut = false;
    private boolean fazendoFadeIn  = false;
    private float   volumeAtual    = VOLUME_MAX;

    // carrega os arquivos de áudio ao entrar na tela de jogo
    public void carregar() {
        ambiente = Gdx.audio.newMusic(Gdx.files.internal("audio/ambiente.wav"));
        ambiente.setLooping(true);
        ambiente.setVolume(VOLUME_MAX);
        ambiente.play();

        if (Gdx.files.internal("audio/porta.wav").exists()) {
            somPorta = Gdx.audio.newSound(Gdx.files.internal("audio/porta.wav"));
        }
    }

    // deve ser chamado a cada frame para processar o fade gradual
    public void atualizar(float delta) {
        if (fazendoFadeOut) {
            volumeAtual = Math.max(0f, volumeAtual - FADE_SPEED * delta);
            ambiente.setVolume(volumeAtual);
            if (volumeAtual <= 0f) {
                fazendoFadeOut = false;
                ambiente.pause();
            }
        }
        if (fazendoFadeIn) {
            if (!ambiente.isPlaying()) ambiente.play();
            volumeAtual = Math.min(VOLUME_MAX, volumeAtual + FADE_SPEED * delta);
            ambiente.setVolume(volumeAtual);
            if (volumeAtual >= VOLUME_MAX) {
                fazendoFadeIn = false;
            }
        }
    }

    // inicia redução gradual do volume até silenciar
    public void iniciarFadeOut() {
        fazendoFadeOut = true;
        fazendoFadeIn  = false;
    }

    // inicia aumento gradual do volume até o máximo
    public void iniciarFadeIn() {
        fazendoFadeIn  = true;
        fazendoFadeOut = false;
    }

    // reservado para som de passos do personagem
    public void tocarPassos() {}
    public void pararPassos() {}

    public void tocarSomPorta() {
        if (somPorta != null) somPorta.play(0.7f);
    }

    public void dispose() {
        if (ambiente  != null) ambiente.dispose();
        if (somPorta  != null) somPorta.dispose();
    }
}
