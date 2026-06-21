package com.persecutio.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.persecutio.game.PersecutioGame;

// tela inicial com animação VHS, logo e menu de opções
public class TelaMenu implements Screen {

    private final PersecutioGame jogo;

    private Texture imagemFundo;
    private Texture imagemLogo;

    // spritesheet e animação do efeito VHS sobreposto ao fundo
    private Texture                    vhsSheet;
    private Animation<TextureRegion>   animVhs;
    private float                      tempoAnim = 0f;

    private Music musicaFundo;

    private final String[] opcoes = {"NOVO JOGO", "SAIR"};
    private int opcaoSelecionada  = 0;

    // opacidade da sobreposição VHS para não esconder o conteúdo abaixo
    private static final float OPACIDADE_VHS = 0.3f;

    // coordenadas do mouse em espaço virtual para hover nas opções
    private final Vector2 coordenadasMouse = new Vector2();

    public TelaMenu(PersecutioGame jogo) {
        this.jogo = jogo;
    }

    @Override
    public void show() {
        imagemFundo = new Texture(Gdx.files.internal("img/fundo_menu.jpg"));
        imagemLogo  = new Texture(Gdx.files.internal("img/titulo_logo.png"));

        // monta o array de frames a partir do grid 5x6 do spritesheet
        vhsSheet = new Texture(Gdx.files.internal("img/vhs_sheet.png"));
        TextureRegion[][] frames2d = TextureRegion.split(vhsSheet, 120, 96);
        TextureRegion[]   frames   = new TextureRegion[30];
        for (int r = 0; r < 5; r++)
            for (int c = 0; c < 6; c++)
                frames[r * 6 + c] = frames2d[r][c];
        animVhs = new Animation<>(0.02f, frames);
        animVhs.setPlayMode(Animation.PlayMode.LOOP);

        musicaFundo = Gdx.audio.newMusic(Gdx.files.internal("audio/musica_menu.wav"));
        musicaFundo.setLooping(true);
        musicaFundo.play();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        jogo.viewport.apply();

        float larguraMundo = jogo.viewport.getWorldWidth();
        float alturaMundo  = jogo.viewport.getWorldHeight();

        SpriteBatch batch = jogo.batch;
        batch.setProjectionMatrix(jogo.viewport.getCamera().combined);
        batch.begin();

        // fundo esticado para cobrir toda a tela virtual
        batch.draw(imagemFundo, 0, 0, larguraMundo, alturaMundo);

        // logo centralizado horizontalmente no topo
        float logoLargura = Math.min(640f, larguraMundo);
        float logoAltura  = logoLargura * (imagemLogo.getHeight() / (float) imagemLogo.getWidth());
        float logoX       = (larguraMundo - logoLargura) / 2f;
        float logoY       = alturaMundo - 30 - logoAltura;
        batch.draw(imagemLogo, logoX, logoY, logoLargura, logoAltura);

        // posição X do menu alinhada com a área esquerda da tela
        float menuX = (larguraMundo - 640f) / 2f + 100f;

        for (int i = 0; i < opcoes.length; i++) {
            String texto = (i == opcaoSelecionada) ? "-> " + opcoes[i] : "   " + opcoes[i];
            // opção selecionada em branco, demais em cinza
            jogo.fonteMenu.setColor(
                i == opcaoSelecionada ? 1f : 0.5f,
                i == opcaoSelecionada ? 1f : 0.5f,
                i == opcaoSelecionada ? 1f : 0.5f,
                1f);
            jogo.fonteMenu.draw(batch, texto, menuX, alturaMundo - 320 - i * 50f);
        }

        // sobreposição VHS semitransparente sobre todo o conteúdo
        tempoAnim += delta;
        TextureRegion frameVhs = animVhs.getKeyFrame(tempoAnim);
        batch.setColor(1f, 1f, 1f, OPACIDADE_VHS);
        batch.draw(frameVhs, 0, 0, larguraMundo, alturaMundo);
        batch.setColor(1f, 1f, 1f, 1f);

        batch.end();

        tratarInput();
    }

    @Override
    public void resize(int width, int height) {
        jogo.viewport.update(width, height, true);
    }

    // processa teclado e mouse para navegar e confirmar opções do menu
    private void tratarInput() {
        float larguraMundo = jogo.viewport.getWorldWidth();
        float alturaMundo  = jogo.viewport.getWorldHeight();
        float menuX        = (larguraMundo - 640f) / 2f + 100f;

        // navegação por teclado entre as opções
        if (Gdx.input.isKeyJustPressed(Keys.UP)   || Gdx.input.isKeyJustPressed(Keys.W)) {
            opcaoSelecionada--;
            if (opcaoSelecionada < 0) opcaoSelecionada = opcoes.length - 1;
        }
        if (Gdx.input.isKeyJustPressed(Keys.DOWN)  || Gdx.input.isKeyJustPressed(Keys.S)) {
            opcaoSelecionada++;
            if (opcaoSelecionada >= opcoes.length) opcaoSelecionada = 0;
        }
        if (Gdx.input.isKeyJustPressed(Keys.ENTER)) {
            confirmarOpcao();
        }

        // hover e clique do mouse nas opções
        coordenadasMouse.set(Gdx.input.getX(), Gdx.input.getY());
        jogo.viewport.unproject(coordenadasMouse);

        for (int i = 0; i < opcoes.length; i++) {
            float textoY = alturaMundo - 320 - i * 50f;

            // área de clique ao redor do texto de cada opção
            float minX = menuX - 20f, maxX = menuX + 250f;
            float minY = textoY - 10f, maxY = textoY + 25f;

            if (coordenadasMouse.x >= minX && coordenadasMouse.x <= maxX &&
                coordenadasMouse.y >= minY && coordenadasMouse.y <= maxY) {
                opcaoSelecionada = i;
                if (Gdx.input.isButtonJustPressed(Buttons.LEFT)) confirmarOpcao();
            }
        }
    }

    // executa a ação da opção selecionada
    private void confirmarOpcao() {
        if (opcaoSelecionada == 0) {
            musicaFundo.stop();
            jogo.setScreen(new TelaJogo(jogo));
        } else if (opcaoSelecionada == 1) {
            Gdx.app.exit();
        }
    }

    @Override
    public void dispose() {
        imagemFundo.dispose();
        imagemLogo.dispose();
        vhsSheet.dispose();
        musicaFundo.dispose();
    }

    @Override public void pause()  {}
    @Override public void resume() {}
    @Override public void hide()   { dispose(); }
}
