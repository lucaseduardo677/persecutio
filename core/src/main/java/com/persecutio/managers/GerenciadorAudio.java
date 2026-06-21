package com.persecutio.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;

public class GerenciadorAudio {

    private static final float VOLUME_MAXIMO       = 1.0f;
    private static final float FADE_SPEED          = 0.8f;
    private static final float INCREMENTO_VOLUME   = 0.1f;
    private static final float VOLUME_MUSICA_PADRAO  = 0.5f;
    private static final float VOLUME_EFEITOS_PADRAO = 0.7f;

    private Music ambiente;
    private Music musicaMenu;
    private Sound somPorta;
    private Sound somSelecao;
    private Sound somConfirmar;

    private float volumeMusica  = VOLUME_MUSICA_PADRAO;
    private float volumeEfeitos = VOLUME_EFEITOS_PADRAO;

    private boolean fazendoFadeOut = false;
    private boolean fazendoFadeIn  = false;
    private float   volumeFade     = 0f;

    // Passos
    private Sound somPasso;
    private long  idPasso = -1;
    private boolean passosTocando = false;

    public void carregarMenu() {
        if (Gdx.files.internal("audio/musica_menu.wav").exists()) {
            musicaMenu = Gdx.audio.newMusic(Gdx.files.internal("audio/musica_menu.wav"));
            musicaMenu.setLooping(true);
            musicaMenu.setVolume(volumeMusica);
            musicaMenu.play();
        }
        if (Gdx.files.internal("audio/selecao.wav").exists()) {
            somSelecao = Gdx.audio.newSound(Gdx.files.internal("audio/selecao.wav"));
        }
        if (Gdx.files.internal("audio/confirmar.wav").exists()) {
            somConfirmar = Gdx.audio.newSound(Gdx.files.internal("audio/confirmar.wav"));
        }
    }

    public void carregarJogo() {
        if (Gdx.files.internal("audio/ambiente.wav").exists()) {
            ambiente = Gdx.audio.newMusic(Gdx.files.internal("audio/ambiente.wav"));
            ambiente.setLooping(true);
            ambiente.setVolume(volumeMusica);
            ambiente.play();
        }
        if (Gdx.files.internal("audio/porta.wav").exists()) {
            somPorta = Gdx.audio.newSound(Gdx.files.internal("audio/porta.wav"));
        }
        if (Gdx.files.internal("audio/passos.wav").exists()) {
            somPasso = Gdx.audio.newSound(Gdx.files.internal("audio/passos.wav"));
        }
    }

    public void pararMusicaMenu() {
        if (musicaMenu != null) {
            musicaMenu.stop();
        }
    }

    public void tocarSelecao() {
        if (somSelecao != null) somSelecao.play(volumeEfeitos);
    }

    public void tocarConfirmar() {
        if (somConfirmar != null) somConfirmar.play(volumeEfeitos);
    }

    public void tocarSomPorta() {
        if (somPorta != null) somPorta.play(volumeEfeitos);
    }

    public void tocarPassos() {
        if (somPasso != null && !passosTocando) {
            idPasso = somPasso.loop(volumeEfeitos * 0.5f);
            passosTocando = true;
        }
    }

    public void pararPassos() {
        if (somPasso != null && passosTocando) {
            somPasso.stop(idPasso);
            passosTocando = false;
            idPasso = -1;
        }
    }

    public void atualizar(float delta) {
        if (fazendoFadeOut) {
            volumeFade = Math.max(0f, volumeFade - FADE_SPEED * delta);
            if (ambiente != null) ambiente.setVolume(volumeFade);
            if (volumeFade <= 0f) {
                fazendoFadeOut = false;
                if (ambiente != null) ambiente.pause();
            }
        }
        if (fazendoFadeIn) {
            if (ambiente != null && !ambiente.isPlaying()) ambiente.play();
            volumeFade = Math.min(volumeMusica, volumeFade + FADE_SPEED * delta);
            if (ambiente != null) ambiente.setVolume(volumeFade);
            if (volumeFade >= volumeMusica) {
                fazendoFadeIn = false;
            }
        }
    }

    public void iniciarFadeOut() {
        fazendoFadeOut = true;
        fazendoFadeIn  = false;
        volumeFade = (ambiente != null) ? ambiente.getVolume() : volumeMusica;
    }

    public void iniciarFadeIn() {
        fazendoFadeIn  = true;
        fazendoFadeOut = false;
        volumeFade = 0f;
    }

    public void aumentarVolume() {
        volumeMusica  = Math.min(VOLUME_MAXIMO, volumeMusica  + INCREMENTO_VOLUME);
        volumeEfeitos = Math.min(VOLUME_MAXIMO, volumeEfeitos + INCREMENTO_VOLUME);
        aplicarVolume();
    }

    public void diminuirVolume() {
        volumeMusica  = Math.max(0f, volumeMusica  - INCREMENTO_VOLUME);
        volumeEfeitos = Math.max(0f, volumeEfeitos - INCREMENTO_VOLUME);
        aplicarVolume();
    }

    private void aplicarVolume() {
        if (musicaMenu != null) musicaMenu.setVolume(volumeMusica);
        if (ambiente != null && !fazendoFadeOut && !fazendoFadeIn) {
            ambiente.setVolume(volumeMusica);
        }
        if (somPasso != null && passosTocando) {
            somPasso.setVolume(idPasso, volumeEfeitos * 0.5f);
        }
    }

    public float getVolumeMusica()  { return volumeMusica; }
    public float getVolumeEfeitos() { return volumeEfeitos; }

    public void tratarInputVolume() {
        if (Gdx.input.isKeyJustPressed(Keys.PLUS) || Gdx.input.isKeyJustPressed(Keys.EQUALS)) {
            aumentarVolume();
        }
        if (Gdx.input.isKeyJustPressed(Keys.MINUS)) {
            diminuirVolume();
        }
    }

    public void dispose() {
        if (ambiente    != null) ambiente.dispose();
        if (musicaMenu  != null) musicaMenu.dispose();
        if (somPorta    != null) somPorta.dispose();
        if (somSelecao  != null) somSelecao.dispose();
        if (somConfirmar != null) somConfirmar.dispose();
        if (somPasso    != null) somPasso.dispose();
    }
}
