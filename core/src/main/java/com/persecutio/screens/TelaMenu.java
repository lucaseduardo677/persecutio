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

// Tela inicial animacao VHS
public class TelaMenu implements Screen {

    private final PersecutioGame jogo;

    private Texture imagemFundo;
    private Texture imagemLogo;

    // Spritesheet animação efeito VHS sobreposto fundo
    private Texture                    vhsSheet;
    private Animation<TextureRegion>   animVhs;
    private float                      tempoAnim = 0f;

    private final String[] opcoes = {"NOVO JOGO", "SAIR"};
    private int opcaoSelecionada  = 0;
    // Detectar mudança tocar som
    private int opcaoAnterior     = 0;

    // Opacidade sobreposição VHS não esconder conteúdo
    private static final float OPACIDADE_VHS = 0.15f;

    // Margem esquerda alinhar logo botões
    private static final float MARGEM_ESQUERDA = 40f;

    // Largura máxima logo
    private static final float LOGO_LARGURA_MAX = 380f;

    // Fade in menu
    private static final float DURACAO_FADE_IN = 1.5f;
    private float timerFadeIn = 0f;
    private boolean fadeInAtivo = true;

    // Fade out rapido confirmar
    private static final float DURACAO_FADE_OUT = 0.5f;
    private float timerFadeOut = 0f;
    private boolean fadeOutAtivo = false;
    // Novo jogo sair
    private boolean confirmouNovoJogo = false;

    // Textura branca 1x1 fade
    private Texture texBranca;

    // Reprodução vídeo introdução
    private VideoPlayer playerVideo;
    private boolean     videoTocando = false;
    private boolean     videoPreparado = false;
    private boolean     fadePreVideoAtivo = false;
    private boolean     fadePosVideoAtivo = false;
    private float       timerFadeVideo = 0f;
    private static final float DURACAO_FADE_VIDEO = 0.6f;
    private static final String CAMINHO_VIDEO = "video/intro.webm";

    // Coordenadas mouse espaço virtual hover opções
    private final Vector2 coordenadasMouse = new Vector2();

    // Criação da tela inicial do menu
    public TelaMenu(PersecutioGame jogo) {
        this.jogo = jogo;
    }

    @Override
    // Carregamento dos recursos
    public void show() {
        imagemFundo = new Texture(Gdx.files.internal("img/fundo_menu.jpg"));
        imagemLogo  = new Texture(Gdx.files.internal("img/titulo_logo.png"));

        // Textura branca fades
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.BLACK);
        pm.fill();
        texBranca = new Texture(pm);
        pm.dispose();

        // Monta array frames partir grid 5x6
        vhsSheet = new Texture(Gdx.files.internal("img/vhs_sheet.png"));
        TextureRegion[][] frames2d = TextureRegion.split(vhsSheet, 120, 96);
        TextureRegion[]   frames   = new TextureRegion[30];
        for (int r = 0; r < 5; r++)
            for (int c = 0; c < 6; c++)
                frames[r * 6 + c] = frames2d[r][c];
        animVhs = new Animation<>(0.02f, frames);
        animVhs.setPlayMode(Animation.PlayMode.LOOP);

        // Áudio centralizado
        jogo.audio.carregarMenu();

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

    @Override
    // Atualização e desenho
    public void render(float delta) {
        // Audio
        jogo.audio.atualizar(delta);
        jogo.audio.tratarInputVolume();

        // Fase video introducao
        if (videoTocando) {
            renderVideoIntro(delta);
            return;
        }

        // Fase fade in menu
        if (fadeInAtivo) {
            timerFadeIn += delta;
            if (timerFadeIn >= DURACAO_FADE_IN) {
                timerFadeIn = DURACAO_FADE_IN;
                fadeInAtivo = false;
            }
        }

        // Fase fade out confirmar
        if (fadeOutAtivo) {
            timerFadeOut += delta;
            if (timerFadeOut >= DURACAO_FADE_OUT) {
                timerFadeOut = DURACAO_FADE_OUT;
                fadeOutAtivo = false;
                // Video introducao confirmar novo jogo
                if (confirmouNovoJogo) {
                    iniciarVideoIntro();
                    return;
                } else {
                    Gdx.app.exit();
                    return;
                }
            }
        }

        // Alpha fade in
        float alphaFadeIn = fadeInAtivo ? (timerFadeIn / DURACAO_FADE_IN) : 1f;

        // Alpha fade out
        float alphaFadeOut = fadeOutAtivo ? (1f - timerFadeOut / DURACAO_FADE_OUT) : 1f;

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

        // Fundo esticado cobrir toda tela virtual
        batch.setColor(1f, 1f, 1f, alphaFinal);
        batch.draw(imagemFundo, 0, 0, larguraMundo, alturaMundo);

        // Logo alinhada esquerda margem
        float logoLargura = Math.min(LOGO_LARGURA_MAX, larguraMundo - MARGEM_ESQUERDA * 2f);
        float logoAltura  = logoLargura * (imagemLogo.getHeight() / (float) imagemLogo.getWidth());
        float logoX       = MARGEM_ESQUERDA;
        float logoY       = alturaMundo - 40f - logoAltura;
        batch.draw(imagemLogo, logoX, logoY, logoLargura, logoAltura);

        // Menu alinhado esquerda mesma margem logo
        float menuX = MARGEM_ESQUERDA;
        // 60px abaixo logo
        float menuYBase = logoY - 60f;

        for (int i = 0; i < opcoes.length; i++) {
            String texto = (i == opcaoSelecionada) ? "> " + opcoes[i] : "  " + opcoes[i];
            // Opcao selecionada branco
            jogo.fonteMenu.setColor(
                i == opcaoSelecionada ? 1f : 0.5f,
                i == opcaoSelecionada ? 1f : 0.5f,
                i == opcaoSelecionada ? 1f : 0.5f,
                alphaFinal);
            jogo.fonteMenu.draw(batch, texto, menuX, menuYBase - i * 45f);
        }

        // Sobreposição VHS semitransparente todo conteúdo
        tempoAnim += delta;
        TextureRegion frameVhs = animVhs.getKeyFrame(tempoAnim);
        batch.setColor(1f, 1f, 1f, OPACIDADE_VHS * alphaFinal);
        batch.draw(frameVhs, 0, 0, larguraMundo, alturaMundo);
        batch.setColor(1f, 1f, 1f, 1f);

        batch.end();

        // Overlay preto fade out
        if (fadeOutAtivo) {
            float alfaFadePreto = timerFadeOut / DURACAO_FADE_OUT;
            batch.begin();
            batch.setColor(0f, 0f, 0f, alfaFadePreto);
            batch.draw(texBranca, 0, 0, larguraMundo, alturaMundo);
            batch.setColor(Color.WHITE);
            batch.end();
        }

        // Input fora fade out
        if (!fadeOutAtivo) {
            tratarInput(menuYBase);
        }
    }

    // Reproducao video introducao
    private void iniciarVideoIntro() {
        jogo.audio.pararMusicaMenu();

        if (!Gdx.files.internal(CAMINHO_VIDEO).exists()) {
            // Video nao existe vai direto pro
            jogo.setScreen(new TelaJogo(jogo));
            return;
        }

        try {
            playerVideo = VideoPlayerCreator.createVideoPlayer();
            playerVideo.setOnCompletionListener(file -> {
                // Video terminar fade pos video
                fadePosVideoAtivo = true;
                timerFadeVideo = 0f;
            });
            playerVideo.play(Gdx.files.internal(CAMINHO_VIDEO));
            videoPreparado = true;
            fadePreVideoAtivo = true;
            timerFadeVideo = 0f;
            videoTocando = true;
        } catch (Exception e) {
            // Falha carregar video vai direto pro
            jogo.setScreen(new TelaJogo(jogo));
        }
    }

    // Processamento interno
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

            // Frame vídeo disponível
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

            // Overlay preto some gradualmente
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
                // Vai pro jogo
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

            // Último frame vídeo
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

            // Overlay preto aumenta gradualmente
            batch.begin();
            batch.setColor(0f, 0f, 0f, alfa);
            batch.draw(texBranca, 0, 0, larguraMundo, alturaMundo);
            batch.setColor(Color.WHITE);
            batch.end();
            return;
        }

        // Vídeo tocando normalmente
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
                // Erro video vai pro jogo
                fadePosVideoAtivo = true;
                timerFadeVideo = 0f;
            }
        }
    }

    @Override
    // Ajuste de tela
    public void resize(int width, int height) {
        jogo.viewport.update(width, height, true);
    }

    // Som seleção opção
    private void tocarSomSelecao() {
        jogo.audio.tocarSelecao();
    }

    // Som confirmar
    private void iniciarConfirmacao(boolean novoJogo) {
        jogo.audio.tocarConfirmar();
        confirmouNovoJogo = novoJogo;
        fadeOutAtivo = true;
        timerFadeOut = 0f;
    }

    // Teclado mouse navegar confirmar opções menu
    private void tratarInput(float menuYBase) {
        float larguraMundo = jogo.viewport.getWorldWidth();
        float alturaMundo  = jogo.viewport.getWorldHeight();
        float menuX        = MARGEM_ESQUERDA;

        // Navegação teclado opções
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

        // Som seleção mudou via teclado
        if (opcaoSelecionada != opcaoAnterior) {
            tocarSomSelecao();
            opcaoAnterior = opcaoSelecionada;
        }

        // Hover clique mouse opções
        coordenadasMouse.set(Gdx.input.getX(), Gdx.input.getY());
        jogo.viewport.unproject(coordenadasMouse);

        for (int i = 0; i < opcoes.length; i++) {
            float textoY = menuYBase - i * 45f;

            // Área clique redor texto cada opção
            float minX = menuX - 10f, maxX = menuX + 220f;
            float minY = textoY - 10f, maxY = textoY + 25f;

            if (coordenadasMouse.x >= minX && coordenadasMouse.x <= maxX &&
                coordenadasMouse.y >= minY && coordenadasMouse.y <= maxY) {
                // Som hover mudou seleção
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

    @Override
    // Liberação dos recursos
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
