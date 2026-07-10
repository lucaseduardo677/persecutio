package com.persecutio.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.video.VideoPlayer;
import com.badlogic.gdx.video.VideoPlayerCreator;
import com.persecutio.game.PersecutioGame;

// Tela inicial do menu com animacao VHS
public class TelaMenu implements Screen {

    // Referencia para o jogo principal
    private final PersecutioGame jogo;

    // Imagem de fundo do menu
    private Texture imagemFundo;
    // Logo do jogo
    private Texture imagemLogo;

    // Spritesheet da animacao VHS
    private Texture                    vhsSheet;
    private Animation<TextureRegion>   animVhs;
    private float                      tempoAnim = 0f;

    // Opcoes do menu
    private final String[] opcoes = {"iniciar", "sair"};
    private int opcaoSelecionada  = 0;
    // Opcao anterior para detectar mudanca e tocar som
    private int opcaoAnterior     = 0;

    // Opacidade da sobreposicao VHS
    private static final float OPACIDADE_VHS = 0.15f;

    // Margem esquerda para alinhar logo e botoes
    private static final float MARGEM_ESQUERDA = 40f;

    // Largura maxima do logo
    private static final float LOGO_LARGURA_MAX = 380f;

    // Tempo de duracao do fade in em segundos
    private float tempoEntrada = 1.5f;
    // Timer do fade in do menu
    private float timerFadeIn = 0f;
    // Flag se o fade in esta ativo
    private boolean fadeInAtivo = true;

    // Tempo de duracao do fade out em segundos
    private float tempoFade = 0.8f;
    // Timer do fade out
    private float timerFadeOut = 0f;
    // Flag se o fade out esta ativo
    private boolean fadeOutAtivo = false;
    // Flag se confirmou novo jogo
    private boolean confirmouNovoJogo = false;

    // Textura branca para fades
    private Texture texBranca;

    // Reproducao do video de introducao
    private VideoPlayer playerVideo;
    private boolean     videoTocando = false;
    private boolean     videoPreparado = false;
    private boolean     fadePreVideoAtivo = false;
    private boolean     fadePosVideoAtivo = false;
    private float       timerFadeVideo = 0f;
    private static final float DURACAO_FADE_VIDEO = 0.6f;
    private static final String CAMINHO_VIDEO = "video/intro.webm";

    // Coordenadas do mouse para hover nas opcoes
    private final Vector2 coordenadasMouse = new Vector2();

    // Construtor da tela do menu
    public TelaMenu(PersecutioGame jogo) {
        this.jogo = jogo;
    }

    // Carrega recursos ao entrar na tela
    @Override
    public void show() {
        imagemFundo = new Texture(Gdx.files.internal("img/fundo_menu.jpg"));
        imagemLogo  = new Texture(Gdx.files.internal("img/titulo_logo.png"));

        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.BLACK);
        pm.fill();
        texBranca = new Texture(pm);
        pm.dispose();

        // Monta array de frames a partir de grid 5x6
        vhsSheet = new Texture(Gdx.files.internal("img/vhs_sheet.png"));
        TextureRegion[][] frames2d = TextureRegion.split(vhsSheet, 120, 96);
        TextureRegion[]   frames   = new TextureRegion[30];
        for (int r = 0; r < 5; r++)
            for (int c = 0; c < 6; c++)
                frames[r * 6 + c] = frames2d[r][c];
        animVhs = new Animation<>(0.02f, frames);
        animVhs.setPlayMode(Animation.PlayMode.LOOP);

        jogo.audio.carregarMenu();
        // Ativa o fade in gradual da musica junto com o visual usando o tempo configurado
        jogo.audio.iniciarFadeIn(tempoEntrada);

        // Reseta estados
        timerFadeIn = 0f;
        fadeInAtivo = true;
        timerFadeOut = 0f;
        fadeOutAtivo = false;
        confirmouNovoJogo = false;
        videoTocando = false;
        videoPreparado = false;
        fadePreVideoAtivo = false;
        fadePosVideoAtivo = false;
        timerFadeVideo = 0f;
        opcaoAnterior = 0;
    }

    // Loop principal de atualizacao e desenho
    @Override
    public void render(float delta) {
        jogo.audio.atualizar(delta);
        jogo.audio.tratarInputVolume();

        // Fase do video de introducao
        if (videoTocando) {
            renderVideoIntro(delta);
            return;
        }

        // Fase do fade in do menu
        if (fadeInAtivo) {
            timerFadeIn += delta;
            if (timerFadeIn >= tempoEntrada) {
                timerFadeIn = tempoEntrada;
                fadeInAtivo = false;
            }
        }

        // Fase do fade out ao confirmar
        if (fadeOutAtivo) {
            timerFadeOut += delta;
            if (timerFadeOut >= tempoFade) {
                timerFadeOut = tempoFade;
                fadeOutAtivo = false;
                if (confirmouNovoJogo) {
                    iniciarVideoIntro();
                    return;
                } else {
                    Gdx.app.exit();
                    return;
                }
            }
        }

        // Alpha do fade in
        float alphaFadeIn = fadeInAtivo ? (timerFadeIn / tempoEntrada) : 1f;

        // Alpha do fade out
        float alphaFadeOut = fadeOutAtivo ? (1f - timerFadeOut / tempoFade) : 1f;

        // Alpha final combinado
        float alphaFinal = alphaFadeIn * alphaFadeOut;

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        jogo.viewport.apply();

        float larguraMundo = jogo.viewport.getWorldWidth();
        float alturaMundo  = jogo.viewport.getWorldHeight();

        SpriteBatch batch = jogo.batch;
        batch.setProjectionMatrix(jogo.viewport.getCamera().combined);
        batch.begin();

        // Fundo esticado cobrindo toda a tela virtual
        batch.setColor(1f, 1f, 1f, alphaFinal);
        batch.draw(imagemFundo, 0, 0, larguraMundo, alturaMundo);

        // Logo alinhada a esquerda com margem
        float logoLargura = Math.min(LOGO_LARGURA_MAX, larguraMundo - MARGEM_ESQUERDA * 2f);
        float logoAltura  = logoLargura * (imagemLogo.getHeight() / (float) imagemLogo.getWidth());
        float logoX       = MARGEM_ESQUERDA;
        float logoY       = alturaMundo - 40f - logoAltura;
        batch.draw(imagemLogo, logoX, logoY, logoLargura, logoAltura);

        // Menu alinhado a esquerda na mesma margem da logo
        float menuX = MARGEM_ESQUERDA;
        float menuYBase = logoY - 60f;

        for (int i = 0; i < opcoes.length; i++) {
            String texto = (i == opcaoSelecionada) ? "> " + opcoes[i] : "  " + opcoes[i];
            jogo.fonteMenu.setColor(
                i == opcaoSelecionada ? 1f : 0.5f,
                i == opcaoSelecionada ? 1f : 0.5f,
                i == opcaoSelecionada ? 1f : 0.5f,
                alphaFinal);
            jogo.fonteMenu.draw(batch, texto, menuX, menuYBase - i * 45f);
        }

        // Sobreposicao VHS semitransparente sobre todo conteudo
        tempoAnim += delta;
        TextureRegion frameVhs = animVhs.getKeyFrame(tempoAnim);
        batch.setColor(1f, 1f, 1f, OPACIDADE_VHS * alphaFinal);
        batch.draw(frameVhs, 0, 0, larguraMundo, alturaMundo);
        batch.setColor(1f, 1f, 1f, 1f);

        batch.end();

        // Overlay preto do fade out
        if (fadeOutAtivo) {
            float alfaFadePreto = timerFadeOut / tempoFade;
            batch.begin();
            batch.setColor(0f, 0f, 0f, alfaFadePreto);
            batch.draw(texBranca, 0, 0, larguraMundo, alturaMundo);
            batch.setColor(Color.WHITE);
            batch.end();
        }

        // Input fora do fade out
        if (!fadeOutAtivo) {
            tratarInput(menuYBase);
        }
    }

    // Inicia reproducao do video de introducao
    private void iniciarVideoIntro() {
        jogo.audio.pararMusicaMenu();

        if (!Gdx.files.internal(CAMINHO_VIDEO).exists()) {
            jogo.setScreen(new TelaJogo(jogo));
            return;
        }

        try {
            playerVideo = VideoPlayerCreator.createVideoPlayer();
            playerVideo.setOnCompletionListener(file -> {
                fadePosVideoAtivo = true;
                timerFadeVideo = 0f;
            });
            playerVideo.play(Gdx.files.internal(CAMINHO_VIDEO));
            videoPreparado = true;
            fadePreVideoAtivo = true;
            timerFadeVideo = 0f;
            videoTocando = true;
        } catch (Exception e) {
            jogo.setScreen(new TelaJogo(jogo));
        }
    }

    // Renderiza o video de introducao com fades
    private void renderVideoIntro(float delta) {
        float larguraMundo = jogo.viewport.getWorldWidth();
        float alturaMundo  = jogo.viewport.getWorldHeight();
        SpriteBatch batch = jogo.batch;

        // Fade pre video
        if (fadePreVideoAtivo) {
            timerFadeVideo += delta;
            if (timerFadeVideo >= DURACAO_FADE_VIDEO) {
                timerFadeVideo = DURACAO_FADE_VIDEO;
                fadePreVideoAtivo = false;
            }
            float alfa = timerFadeVideo / DURACAO_FADE_VIDEO;

            Gdx.gl.glClearColor(0, 0, 0, 1);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

            if (playerVideo != null) {
                try {
                    playerVideo.update();
                    com.badlogic.gdx.graphics.Texture tex = playerVideo.getTexture();
                    if (tex != null) {
                        batch.begin();
                        batch.draw(tex, 0, 0, larguraMundo, alturaMundo);
                        batch.end();
                    }
                } catch (Exception ignored) {}
            }

            // Overlay preto que some gradualmente
            batch.begin();
            batch.setColor(0f, 0f, 0f, 1f - alfa);
            batch.draw(texBranca, 0, 0, larguraMundo, alturaMundo);
            batch.setColor(Color.WHITE);
            batch.end();
            return;
        }

        // Fade pos video
        if (fadePosVideoAtivo) {
            timerFadeVideo += delta;
            if (timerFadeVideo >= DURACAO_FADE_VIDEO) {
                timerFadeVideo = DURACAO_FADE_VIDEO;
                fadePosVideoAtivo = false;
                if (playerVideo != null) {
                    try { playerVideo.dispose(); } catch (Exception ignored) {}
                    playerVideo = null;
                }
                jogo.setScreen(new TelaJogo(jogo));
                return;
            }
            float alfa = timerFadeVideo / DURACAO_FADE_VIDEO;

            Gdx.gl.glClearColor(0, 0, 0, 1);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

            if (playerVideo != null) {
                try {
                    playerVideo.update();
                    com.badlogic.gdx.graphics.Texture tex = playerVideo.getTexture();
                    if (tex != null) {
                        batch.begin();
                        batch.draw(tex, 0, 0, larguraMundo, alturaMundo);
                        batch.end();
                    }
                } catch (Exception ignored) {}
            }

            // Overlay preto que aumenta gradualmente
            batch.begin();
            batch.setColor(0f, 0f, 0f, alfa);
            batch.draw(texBranca, 0, 0, larguraMundo, alturaMundo);
            batch.setColor(Color.WHITE);
            batch.end();
            return;
        }

        // Video tocando normalmente
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (playerVideo != null) {
            try {
                playerVideo.update();
                com.badlogic.gdx.graphics.Texture tex = playerVideo.getTexture();
                if (tex != null) {
                    batch.begin();
                    batch.draw(tex, 0, 0, larguraMundo, alturaMundo);
                    batch.end();
                }
            } catch (Exception e) {
                fadePosVideoAtivo = true;
                timerFadeVideo = 0f;
            }
        }
    }

    // Ajusta a tela quando a janela e redimensionada
    @Override
    public void resize(int width, int height) {
        jogo.viewport.update(width, height, true);
    }

    // Toca som de selecao de opcao
    private void tocarSomSelecao() {
        jogo.audio.tocarSelecao();
    }

    // Inicia confirmacao com fade e som
    private void iniciarConfirmacao(boolean novoJogo) {
        jogo.audio.tocarConfirmar();
        // Inicia o fade out da musica com a mesma duracao do fade visual
        jogo.audio.iniciarFadeOut(tempoFade);
        confirmouNovoJogo = novoJogo;
        fadeOutAtivo = true;
        timerFadeOut = 0f;
    }

    // Processa teclado e mouse para navegar e confirmar opcoes
    private void tratarInput(float menuYBase) {
        float larguraMundo = jogo.viewport.getWorldWidth();
        float alturaMundo  = jogo.viewport.getWorldHeight();
        float menuX        = MARGEM_ESQUERDA;

        // Navegacao pelo teclado
        if (Gdx.input.isKeyJustPressed(Keys.UP)   || Gdx.input.isKeyJustPressed(Keys.W)) {
            opcaoSelecionada--;
            if (opcaoSelecionada < 0) opcaoSelecionada = opcoes.length - 1;
        }
        if (Gdx.input.isKeyJustPressed(Keys.DOWN)  || Gdx.input.isKeyJustPressed(Keys.S)) {
            opcaoSelecionada++;
            if (opcaoSelecionada >= opcoes.length) opcaoSelecionada = 0;
        }
        if (Gdx.input.isKeyJustPressed(Keys.ENTER)) {
            iniciarConfirmacao(opcaoSelecionada == 0);
            return;
        }

        // Som quando muda selecao via teclado
        if (opcaoSelecionada != opcaoAnterior) {
            tocarSomSelecao();
            opcaoAnterior = opcaoSelecionada;
        }

        // Hover e clique do mouse nas opcoes
        coordenadasMouse.set(Gdx.input.getX(), Gdx.input.getY());
        jogo.viewport.unproject(coordenadasMouse);

        for (int i = 0; i < opcoes.length; i++) {
            float textoY = menuYBase - i * 45f;

            float minX = menuX - 10f, maxX = menuX + 220f;
            float minY = textoY - 10f, maxY = textoY + 25f;

            if (coordenadasMouse.x >= minX && coordenadasMouse.x <= maxX &&
                coordenadasMouse.y >= minY && coordenadasMouse.y <= maxY) {
                if (opcaoSelecionada != i) {
                    opcaoSelecionada = i;
                    tocarSomSelecao();
                    opcaoAnterior = opcaoSelecionada;
                }
                if (Gdx.input.isButtonJustPressed(Buttons.LEFT)) {
                    iniciarConfirmacao(i == 0);
                    return;
                }
            }
        }
    }

    // Libera todos os recursos da tela
    @Override
    public void dispose() {
        imagemFundo.dispose();
        imagemLogo.dispose();
        vhsSheet.dispose();
        texBranca.dispose();
        if (playerVideo != null) {
            try { playerVideo.dispose(); } catch (Exception ignored) {}
        }
    }

    @Override public void pause()  {}
    @Override public void resume() {}
    @Override public void hide()   { dispose(); }
}
