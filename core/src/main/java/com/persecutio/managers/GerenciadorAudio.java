package com.persecutio.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;

// Gerenciador de audio do jogo
public class GerenciadorAudio {

    // Volume maximo permitido
    private static final float VOLUME_MAXIMO       = 1.0f;
    // Velocidade do fade
    private static final float FADE_SPEED          = 0.8f;
    // Incremento de volume por tecla
    private static final float INCREMENTO_VOLUME   = 0.1f;
    // Volume padrao da musica
    private static final float VOLUME_MUSICA_PADRAO  = 0.5f;
    // Volume padrao dos efeitos
    private static final float VOLUME_EFEITOS_PADRAO = 0.7f;

    // Musica ambiente do jogo
    private Music ambiente;
    // Musica do menu
    private Music musicaMenu;
    // Som de abrir porta
    private Sound somPorta;
    // Som de selecao de opcao
    private Sound somSelecao;
    // Som de confirmacao
    private Sound somConfirmar;

    // Volume atual da musica
    private float volumeMusica  = VOLUME_MUSICA_PADRAO;
    // Volume atual dos efeitos
    private float volumeEfeitos = VOLUME_EFEITOS_PADRAO;

    // Flag de fade out em andamento
    private boolean fazendoFadeOut = false;
    // Flag de fade in em andamento
    private boolean fazendoFadeIn  = false;
    // Volume durante o fade
    private float   volumeFade     = 0f;

    // Som dos passos
    private Sound somPasso;
    // ID do som de passos em loop
    private long  idPasso = -1;
    // Flag se os passos estao tocando
    private boolean passosTocando = false;

    // Carrega sons do menu
    public void carregarMenu() {
        if (Gdx.files.internal("audio/musica_menu.ogg").exists()) {
            musicaMenu = Gdx.audio.newMusic(Gdx.files.internal("audio/musica_menu.ogg"));
            musicaMenu.setLooping(true);
            musicaMenu.setVolume(volumeMusica);
            musicaMenu.play();
        }
        if (Gdx.files.internal("audio/selecao.ogg").exists()) {
            somSelecao = Gdx.audio.newSound(Gdx.files.internal("audio/selecao.ogg"));
        }
        if (Gdx.files.internal("audio/confirmar.ogg").exists()) {
            somConfirmar = Gdx.audio.newSound(Gdx.files.internal("audio/confirmar.ogg"));
        }
    }

    // Carrega sons da gameplay
    public void carregarJogo() {
        if (Gdx.files.internal("audio/ambiente.ogg").exists()) {
            ambiente = Gdx.audio.newMusic(Gdx.files.internal("audio/ambiente.ogg"));
            ambiente.setLooping(true);
            ambiente.setVolume(volumeMusica);
            ambiente.play();
        }
        if (Gdx.files.internal("audio/porta.ogg").exists()) {
            somPorta = Gdx.audio.newSound(Gdx.files.internal("audio/porta.ogg"));
        }
        if (Gdx.files.internal("audio/passos.ogg").exists()) {
            somPasso = Gdx.audio.newSound(Gdx.files.internal("audio/passos.ogg"));
        }
    }

    // Para a musica do menu
    public void pararMusicaMenu() {
        if (musicaMenu != null) {
            musicaMenu.stop();
        }
    }

    // Toca som de selecao
    public void tocarSelecao() {
        if (somSelecao != null) somSelecao.play(volumeEfeitos);
    }

    // Toca som de confirmacao
    public void tocarConfirmar() {
        if (somConfirmar != null) somConfirmar.play(volumeEfeitos);
    }

    // Toca som de abrir porta
    public void tocarSomPorta() {
        if (somPorta != null) somPorta.play(volumeEfeitos);
    }

    // Inicia loop de passos
    public void tocarPassos() {
        if (somPasso != null && !passosTocando) {
            idPasso = somPasso.loop(volumeEfeitos * 0.5f);
            passosTocando = true;
        }
    }

    // Para o loop de passos
    public void pararPassos() {
        if (somPasso != null && passosTocando) {
            somPasso.stop(idPasso);
            passosTocando = false;
            idPasso = -1;
        }
    }

    // Atualiza fades de volume
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

    // Inicia fade out da musica
    public void iniciarFadeOut() {
        fazendoFadeOut = true;
        fazendoFadeIn  = false;
        volumeFade = (ambiente != null) ? ambiente.getVolume() : volumeMusica;
    }

    // Inicia fade in da musica
    public void iniciarFadeIn() {
        fazendoFadeIn  = true;
        fazendoFadeOut = false;
        volumeFade = 0f;
    }

    // Aumenta volume geral
    public void aumentarVolume() {
        volumeMusica  = Math.min(VOLUME_MAXIMO, volumeMusica  + INCREMENTO_VOLUME);
        volumeEfeitos = Math.min(VOLUME_MAXIMO, volumeEfeitos + INCREMENTO_VOLUME);
        aplicarVolume();
    }

    // Diminui volume geral
    public void diminuirVolume() {
        volumeMusica  = Math.max(0f, volumeMusica  - INCREMENTO_VOLUME);
        volumeEfeitos = Math.max(0f, volumeEfeitos - INCREMENTO_VOLUME);
        aplicarVolume();
    }

    // Aplica volumes atuais aos sons
    private void aplicarVolume() {
        if (musicaMenu != null) musicaMenu.setVolume(volumeMusica);
        if (ambiente != null && !fazendoFadeOut && !fazendoFadeIn) {
            ambiente.setVolume(volumeMusica);
        }
        if (somPasso != null && passosTocando) {
            somPasso.setVolume(idPasso, volumeEfeitos * 0.5f);
        }
    }

    // Retorna volume da musica
    public float getVolumeMusica()  { return volumeMusica; }

    // Retorna volume dos efeitos
    public float getVolumeEfeitos() { return volumeEfeitos; }

    // Processa teclas de ajuste de volume
    public void tratarInputVolume() {
        if (Gdx.input.isKeyJustPressed(Keys.PLUS) || Gdx.input.isKeyJustPressed(Keys.EQUALS)) {
            aumentarVolume();
        }
        if (Gdx.input.isKeyJustPressed(Keys.MINUS)) {
            diminuirVolume();
        }
    }

    // Libera todos os recursos de audio
    public void dispose() {
        if (ambiente    != null) ambiente.dispose();
        if (musicaMenu  != null) musicaMenu.dispose();
        if (somPorta    != null) somPorta.dispose();
        if (somSelecao  != null) somSelecao.dispose();
        if (somConfirmar != null) somConfirmar.dispose();
        if (somPasso    != null) somPasso.dispose();
    }
}