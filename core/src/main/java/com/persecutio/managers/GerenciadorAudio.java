package com.persecutio.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;

// Gerenciador de audio do jogo
public class GerenciadorAudio {

    // Volume maximo permitido
    private static final float VOLUME_MAXIMO       = 1.0f;
    // Incremento de volume por tecla
    private static final float INCREMENTO_VOLUME   = 0.1f;
    // Volume padrao da musica
    private static final float VOLUME_MUSICA_PADRAO  = 0.5f;
    // Volume padrao dos efeitos
    private static final float VOLUME_EFEITOS_PADRAO = 0.7f;

    // Musicas armazenadas
    private Music ambiente;
    private Music ambienteUmbra;
    private Music musicaDois;

    // Musica que esta tocando agora
    private Music musicaAtual;

    // Um booleano para trocar os sons ambientes
    private boolean tocandoUmbra = false;
    // Musica do menu
    private Music musicaMenu;
    // Som de abrir porta
    private Sound somPorta;
    // Som de abrir e fechar documentos gráficos
    private Sound somDocumento;
    // Som de selecao de opcao
    private Sound somSelecao;
    // Som de confirmacao
    private Sound somConfirmar;

    // Volume atual da musica
    private float volumeMusica  = VOLUME_MUSICA_PADRAO;
    // Volume atual dos efeitos
    private float volumeEfeitos = VOLUME_EFEITOS_PADRAO;

    // Controla fade out em andamento
    private boolean fazendoFadeOut = false;
    // Controla fade in em andamento
    private boolean fazendoFadeIn  = false;
    // Volume durante o fade
    private float   volumeFade     = 0f;
    // Duracao personalizada do fade out
    private float   tempoFadeOut   = 1.0f;
    // Duracao personalizada do fade in
    private float   tempoFadeIn    = 1.0f;

    // Som dos passos
    private Sound somPasso;
    // Identificador do som de passos em loop
    private long  idPasso = -1;
    // Indica se os passos estao tocando
    private boolean passosTocando = false;

    // Carrega sons do menu
    public void carregarMenu() {
        if (Gdx.files.internal("audio/musica_menu.ogg").exists()) {
            musicaMenu = Gdx.audio.newMusic(Gdx.files.internal("audio/musica_menu.ogg"));
            musicaMenu.setLooping(true);
            musicaMenu.setVolume(0f);
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
        }
        if (Gdx.files.internal("audio/ambienteUmbra.ogg").exists()) {
            ambienteUmbra = Gdx.audio.newMusic(Gdx.files.internal("audio/ambienteUmbra.ogg"));
            ambienteUmbra.setLooping(true);
        }
        if (Gdx.files.internal("audio/musicaUmbra.ogg").exists()) {
            musicaDois = Gdx.audio.newMusic(Gdx.files.internal("audio/musicaUmbra.ogg"));
            musicaDois.setLooping(true);
        }

        atualizarAmbiente(false, 1);

        if (Gdx.files.internal("audio/porta.ogg").exists()) {
            somPorta = Gdx.audio.newSound(Gdx.files.internal("audio/porta.ogg"));
        }
        if (Gdx.files.internal("audio/documento.ogg").exists()) {
            somDocumento = Gdx.audio.newSound(Gdx.files.internal("audio/documento.ogg"));
        }
        if (Gdx.files.internal("audio/passos.ogg").exists()) {
            somPasso = Gdx.audio.newSound(Gdx.files.internal("audio/passos.ogg"));
        }
    }

    // Atualiza o som ambiente de acordo com o mapa e a missao atual
    public void atualizarAmbiente(boolean mundoUmbra, int missaoAtual) {
        Music desejada = ambiente;

        if (mundoUmbra) {
            desejada = (missaoAtual == 2 && musicaDois != null) ? musicaDois : ambienteUmbra;
        }

        if (musicaAtual != desejada) {
            if (musicaAtual != null) musicaAtual.stop();
            musicaAtual = desejada;
            if (musicaAtual != null) {
                musicaAtual.setVolume(volumeMusica);
                musicaAtual.play();
            }
        }
        tocandoUmbra = mundoUmbra;
    }

    // Para a musica do menu
    public void pararMusicaMenu() {
        if (musicaMenu != null) musicaMenu.stop();
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

    // Toca som de folhear documento gráfico
    public void tocarDocumento() {
        if (somDocumento != null) somDocumento.play(volumeEfeitos);
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
            float velocidade = volumeMusica / tempoFadeOut;
            volumeFade = Math.max(0f, volumeFade - velocidade * delta);

            if (musicaAtual != null && musicaAtual.isPlaying()) {
                musicaAtual.setVolume(volumeFade);
            }
            if (musicaMenu != null && musicaMenu.isPlaying()) {
                musicaMenu.setVolume(volumeFade);
            }

            if (volumeFade <= 0f) {
                fazendoFadeOut = false;
                if (musicaAtual != null) musicaAtual.pause();
                if (musicaMenu != null) musicaMenu.stop();
            }
        }
        if (fazendoFadeIn) {
            float velocidade = volumeMusica / tempoFadeIn;
            volumeFade = Math.min(volumeMusica, volumeFade + velocidade * delta);

            if (musicaAtual != null && musicaAtual.isPlaying()) {
                musicaAtual.setVolume(volumeFade);
            }
            if (musicaMenu != null && musicaMenu.isPlaying()) {
                musicaMenu.setVolume(volumeFade);
            }

            if (volumeFade >= volumeMusica) {
                fazendoFadeIn = false;
            }
        }
    }

    public void iniciarFadeIn(float segundos) {
        fazendoFadeIn  = true;
        fazendoFadeOut = false;
        tempoFadeIn    = segundos > 0 ? segundos : 0.01f;
        volumeFade     = 0f;

        if (musicaMenu != null) {
            musicaMenu.setVolume(0f);
            if (!musicaMenu.isPlaying()) musicaMenu.play();
        }
        if (musicaAtual != null) {
            musicaAtual.setVolume(0f);
            if (!musicaAtual.isPlaying()) musicaAtual.play();
        }
    }

    public void iniciarFadeOut(float segundos) {
        fazendoFadeOut = true;
        fazendoFadeIn  = false;
        tempoFadeOut   = segundos > 0 ? segundos : 0.01f;

        if (musicaMenu != null && musicaMenu.isPlaying()) {
            volumeFade = musicaMenu.getVolume();
        } else if (musicaAtual != null && musicaAtual.isPlaying()) {
            volumeFade = musicaAtual.getVolume();
        } else {
            volumeFade = volumeMusica;
        }
    }

    // Aumenta volume geral
    public void aumentarVolume() {
        volumeMusica  = Math.min(VOLUME_MAXIMO, volumeMusica  + INCREMENTO_VOLUME);
        volumeEfeitos = Math.min(VOLUME_MAXIMO, volumeEfeitos + INCREMENTO_VOLUME);
        aplicarVolume();
    }

    // Diminui volume geral
    public void  diminuirVolume() {
        volumeMusica  = Math.max(0f, volumeMusica  - INCREMENTO_VOLUME);
        volumeEfeitos = Math.max(0f, volumeEfeitos - INCREMENTO_VOLUME);
        aplicarVolume();
    }

    // Aplica volumes atuais aos sons
    private void aplicarVolume() {
        if (musicaAtual != null && !fazendoFadeOut && !fazendoFadeIn) {
            musicaAtual.setVolume(volumeMusica);
        }
    }

    public float getVolumeMusica()  { return volumeMusica; }
    public float getVolumeEfeitos() { return volumeEfeitos; }

    // Processa teclas de ajuste de volume
    public void tratarInputVolume() {
        if (Gdx.input.isKeyPressed(Keys.PLUS) || Gdx.input.isKeyJustPressed(Keys.EQUALS)) {
            aumentarVolume();
        }
        if (Gdx.input.isKeyJustPressed(Keys.MINUS)) {
            diminuirVolume();
        }
    }

    // Libera todos os recursos de audio
    public void dispose() {
        if (ambiente    != null) ambiente.dispose();
        if (ambienteUmbra != null) ambienteUmbra.dispose();
        if (musicaDois  != null) musicaDois.dispose();
        if (musicaMenu  != null) musicaMenu.dispose();
        if (somPorta    != null) somPorta.dispose();
        if (somDocumento != null) somDocumento.dispose();
        if (somSelecao  != null) somSelecao.dispose();
        if (somConfirmar != null) somConfirmar.dispose();
        if (somPasso    != null) somPasso.dispose();
    }
}
