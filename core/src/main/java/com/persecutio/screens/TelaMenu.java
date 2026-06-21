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

// Tela inicial com animacao VHS
public class TelaMenu implements Screen {

    private final PersecutioGame jogo;

    private Texture imagemFundo;
    private Texture imagemLogo;

    // Spritesheet e animação do efeito VHS sobreposto ao fundo
    private Texture                    vhsSheet;
    private Animation<TextureRegion>   animVhs;
    private float                      tempoAnim = 0f;

    private final String[] opcoes = {"NOVO JOGO", "SAIR"};
    private int opcaoSelecionada  = 0;
    // Para detectar mudança e tocar som
    private int opcaoAnterior     = 0;

    // Opacidade da sobreposição VHS para não esconder o conteúdo abaixo
    private static final float OPACIDADE_VHS = 0.15f;

    // Margem esquerda para alinhar logo e botões
    private static final float MARGEM_ESQUERDA = 40f;

    // Largura máxima da logo
    private static final float LOGO_LARGURA_MAX = 380f;

    // Fade in do menu
    private static final float DURACAO_FADE_IN = 1.5f;
    private float timerFadeIn = 0f;
    private boolean fadeInAtivo = true;

    // Fade out rapido ao confirmar
    private static final float DURACAO_FADE_OUT = 0.5f;
    private float timerFadeOut = 0f;
    private boolean fadeOutAtivo = false;
    // Novo jogo ou sair
    private boolean confirmouNovoJogo = false;

    // Textura branca 1x1 para o fade
    private Texture texBranca;

    // Reprodução de vídeo de introdução
    private VideoPlayer playerVideo;
    private boolean     videoTocando = false;
    private boolean     videoPreparado = false;
    private boolean     fadePreVideoAtivo = false;
    private boolean     fadePosVideoAtivo = false;
    private float       timerFadeVideo = 0f;
    private static final float DURACAO_FADE_VIDEO = 0.6f;
    private static final String CAMINHO_VIDEO = "video/intro.webm";

    // Coordenadas do mouse em espaço virtual para hover nas opções
    private final Vector2 coordenadasMouse = new Vector2();

    public TelaMenu(PersecutioGame jogo) {
        this.jogo = jogo;
    }

    @Override
    public void show() {
        imagemFundo = new Texture(Gdx.files.internal("img/fundo_menu.jpg"));
        imagemLogo  = new Texture(Gdx.files.internal("img/titulo_logo.png"));

        // Textura branca para fades
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.BLACK);
        pm.fill();
        texBranca = new Texture(pm);
        pm.dispose();

        // Monta o array de frames a partir do grid 5x6 do spritesheet
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
    public void render(float delta) {
        // Atualiza audio
        jogo.audio.atualizar(delta);
        jogo.audio.tratarInputVolume();

        // Fase de video de introducao
        if (videoTocando) {
            renderVideoIntro(delta);
            return;
        }

        // Fase de fade in do menu
        if (fadeInAtivo) {
            timerFadeIn += delta;
            if (timerFadeIn >= DURACAO_FADE_IN) {
                timerFadeIn = DURACAO_FADE_IN;
                fadeInAtivo = false;
            }
        }

        // Fase de fade out ao confirmar
        if (fadeOutAtivo) {
            timerFadeOut += delta;
            if (timerFadeOut >= DURACAO_FADE_OUT) {
                timerFadeOut = DURACAO_FADE_OUT;
                fadeOutAtivo = false;
                // Inicia o video de introducao ao confirmar novo jogo
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
        float alphaFadeIn = fadeInAtivo ? (timerFadeIn / DURACAO_FADE_IN) : 1f;

        // Alpha do fade out
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

        // Fundo esticado para cobrir toda a tela virtual
        batch.setColor(1f, 1f, 1f, alphaFinal);
        batch.draw(imagemFundo, 0, 0, larguraMundo, alturaMundo);

        // Logo alinhada a esquerda com margem
        float logoLargura = Math.min(LOGO_LARGURA_MAX, larguraMundo - MARGEM_ESQUERDA * 2f);
        float logoAltura  = logoLargura * (imagemLogo.getHeight() / (float) imagemLogo.getWidth());
        float logoX       = MARGEM_ESQUERDA;
        float logoY       = alturaMundo - 40f - logoAltura;
        batch.draw(imagemLogo, logoX, logoY, logoLargura, logoAltura);

        // Menu alinhado à esquerda com a mesma margem da logo
        float menuX = MARGEM_ESQUERDA;
        // 60px abaixo da logo
        float menuYBase = logoY - 60f;

        for (int i = 0; i < opcoes.length; i++) {
            String texto = (i == opcaoSelecionada) ? "> " + opcoes[i] : "  " + opcoes[i];
            // Opcao selecionada em branco
            jogo.fonteMenu.setColor(
                i == opcaoSelecionada ? 1f : 0.5f,
                i == opcaoSelecionada ? 1f : 0.5f,
                i == opcaoSelecionada ? 1f : 0.5f,
                alphaFinal);
            jogo.fonteMenu.draw(batch, texto, menuX, menuYBase - i * 45f);
        }

        // Sobreposição VHS semitransparente sobre todo o conteúdo
        tempoAnim += delta;
        TextureRegion frameVhs = animVhs.getKeyFrame(tempoAnim);
        batch.setColor(1f, 1f, 1f, OPACIDADE_VHS * alphaFinal);
        batch.draw(frameVhs, 0, 0, larguraMundo, alturaMundo);
        batch.setColor(1f, 1f, 1f, 1f);

        batch.end();

        // Overlay preto do fade out
        if (fadeOutAtivo) {
            float alfaFadePreto = timerFadeOut / DURACAO_FADE_OUT;
            batch.begin();
            batch.setColor(0f, 0f, 0f, alfaFadePreto);
            batch.draw(texBranca, 0, 0, larguraMundo, alturaMundo);
            batch.setColor(Color.WHITE);
            batch.end();
        }

        // Processa input fora do fade out
        if (!fadeOutAtivo) {
            tratarInput(menuYBase);
        }
    }

    // Reproducao do video de introducao
    private void iniciarVideoIntro() {
        jogo.audio.pararMusicaMenu();

        if (!Gdx.files.internal(CAMINHO_VIDEO).exists()) {
            // Video nao existe vai direto pro jogo
            jogo.setScreen(new TelaJogo(jogo));
            return;
        }

        try {
            playerVideo = VideoPlayerCreator.createVideoPlayer();
            playerVideo.setOnCompletionListener(file -> {
                // Quando o video terminar inicia o fade pos video
                fadePosVideoAtivo = true;
                timerFadeVideo = 0f;
            });
            playerVideo.play(Gdx.files.internal(CAMINHO_VIDEO));
            videoPreparado = true;
            fadePreVideoAtivo = true;
            timerFadeVideo = 0f;
            videoTocando = true;
        } catch (Exception e) {
            // Falha ao carregar video vai direto pro jogo
            jogo.setScreen(new TelaJogo(jogo));
        }
    }

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

            // Desenha o frame do vídeo se disponível
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
                // Limpa e vai pro jogo
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

            // Desenha o último frame do vídeo
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
                // Erro no video vai pro jogo
                fadePosVideoAtivo = true;
                timerFadeVideo = 0f;
            }
        }
    }

    @Override
    public void resize(int width, int height) {
        jogo.viewport.update(width, height, true);
    }

    // Toca o som de seleção quando a opção muda
    private void tocarSomSelecao() {
        jogo.audio.tocarSelecao();
    }

    // Toca o som de confirmar
    private void iniciarConfirmacao(boolean novoJogo) {
        jogo.audio.tocarConfirmar();
        confirmouNovoJogo = novoJogo;
        fadeOutAtivo = true;
        timerFadeOut = 0f;
    }

    // Processa teclado e mouse para navegar e confirmar opções do menu
    private void tratarInput(float menuYBase) {
        float larguraMundo = jogo.viewport.getWorldWidth();
        float alturaMundo  = jogo.viewport.getWorldHeight();
        float menuX        = MARGEM_ESQUERDA;

        // Navegação por teclado entre as opções
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

        // Toca som se a seleção mudou via teclado
        if (opcaoSelecionada != opcaoAnterior) {
            tocarSomSelecao();
            opcaoAnterior = opcaoSelecionada;
        }

        // Hover e clique do mouse nas opções
        coordenadasMouse.set(Gdx.input.getX(), Gdx.input.getY());
        jogo.viewport.unproject(coordenadasMouse);

        for (int i = 0; i < opcoes.length; i++) {
            float textoY = menuYBase - i * 45f;

            // Área de clique ao redor do texto de cada opção
            float minX = menuX - 10f, maxX = menuX + 220f;
            float minY = textoY - 10f, maxY = textoY + 25f;

            if (coordenadasMouse.x >= minX && coordenadasMouse.x <= maxX &&
                coordenadasMouse.y >= minY && coordenadasMouse.y <= maxY) {
                // Toca som se o hover mudou a seleção
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
