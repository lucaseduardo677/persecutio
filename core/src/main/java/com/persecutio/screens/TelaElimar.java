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

import com.persecutio.game.PersecutioGame;
import com.persecutio.managers.GerenciadorDialogo;
import com.persecutio.managers.GerenciadorPontuacao;
import com.persecutio.managers.GerenciadorVoz;

// Tela do questionario final com o Dr. Elimar (GDD paginas 4 e 7-9)
// Substitui a tela de jogo assim que a Missao 3 inicia o questionario
public class TelaElimar implements Screen {

    // Referencia para o jogo principal
    private final PersecutioGame jogo;

    // Fundo do escritorio do Elimar
    private Texture imagemFundo;
    // Retrato falante do Elimar (sprite 2x2)
    private Texture spriteElimar;
    private TextureRegion[][] framesElimar;

    // Sobreposicao VHS reaproveitada do menu
    private Texture                  vhsSheet;
    private Animation<TextureRegion> animVhs;
    private float                    tempoAnim = 0f;

    // Textura branca para caixas e fades
    private Texture texBranca;

    // Sistemas de dialogo, voz e pontuacao desta sessao
    private GerenciadorDialogo   dialogo;
    private GerenciadorVoz       voz;
    private final GerenciadorPontuacao pontuacao = new GerenciadorPontuacao();

    // Selecao atual entre as escolhas exibidas
    private int opcaoSelecionada = 0;
    // Flag se o no do final ja foi iniciado
    private boolean resultadoIniciado = false;

    // Duracao do fade de entrada
    private static final float TEMPO_FADE = 0.6f;
    private float timerFade  = 0f;
    private boolean fadeAtivo = true;

    private final Vector2 coordenadasMouse = new Vector2();

    public TelaElimar(PersecutioGame jogo) {
        this.jogo = jogo;
    }

    @Override
    public void show() {
        imagemFundo  = new Texture(Gdx.files.internal("img/fundoElimar.png"));
        spriteElimar = new Texture(Gdx.files.internal("img/elimarSprite.png"));
        spriteElimar.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        framesElimar = TextureRegion.split(spriteElimar, spriteElimar.getWidth() / 2, spriteElimar.getHeight() / 2);

        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fill();
        texBranca = new Texture(pm);
        pm.dispose();

        if (Gdx.files.internal("img/vhs_sheet.png").exists()) {
            vhsSheet = new Texture(Gdx.files.internal("img/vhs_sheet.png"));
            TextureRegion[][] frames2d = TextureRegion.split(vhsSheet, 120, 96);
            TextureRegion[]   frames   = new TextureRegion[30];
            for (int r = 0; r < 5; r++)
                for (int c = 0; c < 6; c++)
                    frames[r * 6 + c] = frames2d[r][c];
            animVhs = new Animation<>(0.03f, frames);
            animVhs.setPlayMode(Animation.PlayMode.LOOP);
        }

        voz     = new GerenciadorVoz();
        dialogo = new GerenciadorDialogo();
        dialogo.setVoz(voz);
        dialogo.iniciar("elimar_intro");

        pontuacao.reiniciar();
        opcaoSelecionada  = 0;
        resultadoIniciado = false;
        timerFade  = 0f;
        fadeAtivo  = true;
    }

    @Override
    public void render(float delta) {
        verificarFimDialogo();

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        jogo.viewport.apply();
        float larguraMundo = jogo.viewport.getWorldWidth();
        float alturaMundo  = jogo.viewport.getWorldHeight();

        SpriteBatch batch = jogo.batch;
        batch.setProjectionMatrix(jogo.viewport.getCamera().combined);

        if (fadeAtivo) {
            timerFade += delta;
            if (timerFade >= TEMPO_FADE) {
                timerFade = TEMPO_FADE;
                fadeAtivo = false;
            }
        }
        float alphaFade = fadeAtivo ? (timerFade / TEMPO_FADE) : 1f;

        batch.begin();

        // Fundo do escritorio
        batch.setColor(1f, 1f, 1f, alphaFade);
        batch.draw(imagemFundo, 0, 0, larguraMundo, alturaMundo);

        // Sobreposicao VHS sutil
        if (animVhs != null) {
            tempoAnim += delta;
            TextureRegion frameVhs = animVhs.getKeyFrame(tempoAnim);
            batch.setColor(1f, 1f, 1f, 0.20f * alphaFade);
            batch.draw(frameVhs, 0, 0, larguraMundo, alturaMundo);
        }
        batch.setColor(1f, 1f, 1f, alphaFade);

        desenharRetrato(batch, larguraMundo, alturaMundo);
        desenharCaixaTexto(batch, larguraMundo, alturaMundo);

        batch.end();

        if (!fadeAtivo) {
            tratarInput(alturaMundo);
        }
    }

    // Desenha o retrato falante do Elimar, com boca sincronizada a fala
    private void desenharRetrato(SpriteBatch batch, float larguraMundo, float alturaMundo) {
        if (dialogo.getRetrato() == null) return;

        String textoOriginal = dialogo.getTexto();
        String textoVisivel  = dialogo.getTextoVisivel();
        boolean falandoAgora = textoVisivel.length() < textoOriginal.length();

        int coluna = falandoAgora ? (textoVisivel.length() % 2) : 0;
        TextureRegion frame = framesElimar[0][coluna];

        float largRetrato = 200f;
        float altRetrato  = 200f;
        float posX = larguraMundo / 2f - largRetrato / 2f;
        float posY = alturaMundo * 0.42f;

        batch.draw(frame, posX, posY, largRetrato, altRetrato);
    }

    // Desenha a caixa de fala e as escolhas na parte inferior da tela
    private void desenharCaixaTexto(SpriteBatch batch, float larguraMundo, float alturaMundo) {
        float alturaCaixa = alturaMundo * 0.34f;

        batch.setColor(0f, 0f, 0f, 0.75f);
        batch.draw(texBranca, 0, 0, larguraMundo, alturaCaixa);
        batch.setColor(Color.WHITE);

        float margem = 20f;
        float y = alturaCaixa - margem;

        if (dialogo.getFalante() != null && !dialogo.getFalante().isEmpty()) {
            jogo.fonteNomes.setColor(Color.SALMON);
            jogo.fonteNomes.draw(batch, dialogo.getFalante(), margem, y);
            y -= 22f;
        }

        jogo.fonteDialogos.setColor(Color.WHITE);
        jogo.fonteDialogos.draw(batch, dialogo.getTextoVisivel(), margem, y, larguraMundo - margem * 2f, -1, true);

        if (dialogo.temEscolhas()) {
            float yEscolha = alturaCaixa * 0.42f;
            for (int i = 0; i < dialogo.getEscolhas().size(); i++) {
                boolean selecionada = (i == opcaoSelecionada);
                jogo.fonteIndicadores.setColor(selecionada ? Color.GOLD : Color.LIGHT_GRAY);
                String prefixo = selecionada ? "> " : "  ";
                jogo.fonteIndicadores.draw(batch, prefixo + dialogo.getEscolhas().get(i), margem, yEscolha - i * 20f);
            }
        } else {
            jogo.fonteIndicadores.setColor(Color.GRAY);
            jogo.fonteIndicadores.draw(batch, "[ESPACO / ENTER] Continuar", margem, 14f);
        }
    }

    // Processa teclado e mouse para avancar falas ou escolher respostas
    private void tratarInput(float alturaMundo) {
        if (dialogo.temEscolhas()) {
            int total = dialogo.getEscolhas().size();

            if (Gdx.input.isKeyJustPressed(Keys.UP) || Gdx.input.isKeyJustPressed(Keys.W)) {
                opcaoSelecionada = (opcaoSelecionada - 1 + total) % total;
            }
            if (Gdx.input.isKeyJustPressed(Keys.DOWN) || Gdx.input.isKeyJustPressed(Keys.S)) {
                opcaoSelecionada = (opcaoSelecionada + 1) % total;
            }

            coordenadasMouse.set(Gdx.input.getX(), Gdx.input.getY());
            jogo.viewport.unproject(coordenadasMouse);
            float alturaCaixa = alturaMundo * 0.34f;
            float yEscolha    = alturaCaixa * 0.42f;
            for (int i = 0; i < total; i++) {
                float minY = yEscolha - i * 20f - 5f;
                float maxY = yEscolha - i * 20f + 15f;
                if (coordenadasMouse.y >= minY && coordenadasMouse.y <= maxY) {
                    opcaoSelecionada = i;
                    if (Gdx.input.isButtonJustPressed(Buttons.LEFT)) {
                        confirmarEscolha();
                        return;
                    }
                }
            }

            if (Gdx.input.isKeyJustPressed(Keys.ENTER) || Gdx.input.isKeyJustPressed(Keys.SPACE)) {
                confirmarEscolha();
            }
        } else {
            if (Gdx.input.isKeyJustPressed(Keys.ENTER) || Gdx.input.isKeyJustPressed(Keys.SPACE)
                || Gdx.input.justTouched()) {
                dialogo.avancar();
            }
        }
    }

    // Aplica a pontuacao da escolha selecionada e avanca o dialogo
    private void confirmarEscolha() {
        pontuacao.adicionarPontos(dialogo.obterPontos(opcaoSelecionada));
        dialogo.escolher(opcaoSelecionada);
        opcaoSelecionada = 0;
    }

    // Detecta o fim do questionario e do encerramento para decidir o proximo passo
    private void verificarFimDialogo() {
        if (dialogo.estaAtivo()) return;

        if (!resultadoIniciado) {
            resultadoIniciado = true;

            String noFinal;
            switch (pontuacao.obterFinal()) {
                case BOM:  noFinal = "final_bom";  break;
                case RUIM: noFinal = "final_ruim"; break;
                default:   noFinal = "final_normal";
            }
            dialogo.iniciar(noFinal);
        } else {
            // O dialogo do final tambem terminou: volta ao menu principal
            jogo.setScreen(new TelaMenu(jogo));
        }
    }

    @Override
    public void resize(int width, int height) {
        jogo.viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        imagemFundo.dispose();
        spriteElimar.dispose();
        texBranca.dispose();
        if (vhsSheet != null) vhsSheet.dispose();
        if (voz != null) voz.dispose();
    }

    @Override public void pause()  {}
    @Override public void resume() {}
    @Override public void hide()   { dispose(); }
}
