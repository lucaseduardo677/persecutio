package com.persecutio.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.math.Rectangle;

import com.persecutio.game.PersecutioGame;
import com.persecutio.entities.Jogador;
import com.persecutio.managers.ContextoRender;
import com.persecutio.managers.GerenciadorAudio;
import com.persecutio.managers.GerenciadorColisao;
import com.persecutio.managers.GerenciadorComodos;
import com.persecutio.managers.GerenciadorDebug;
import com.persecutio.managers.GerenciadorPortas;
import com.persecutio.managers.GerenciadorProgresso;
import com.persecutio.managers.GerenciadorRenderizacao;
import com.persecutio.managers.GerenciadorUI;

public class TelaJogo implements Screen {

    private final PersecutioGame jogo;

    public  GerenciadorColisao      sistemaColisao;
    private GerenciadorDebug        sistemaDebug;
    private GerenciadorAudio        sistemaAudio;
    public  GerenciadorProgresso    progresso;
    private GerenciadorUI           interfaceJogo;
    private GerenciadorRenderizacao renderizador;
    private GerenciadorComodos      gerComodos;
    public  GerenciadorPortas       gerPortas;
    private TiledMap                mapaTiled;

    private Jogador jogador;

    public Rectangle hitboxJogador;

    public boolean mundoUmbra            = false;
    public boolean portaUmbraDestrancada = false;

    private boolean mostrarHitboxes = false;
    private boolean andando         = false;

    private GerenciadorComodos.Comodo comodoAtual = null;

    private Texture imagemMapa;
    private Texture spriteSheet;
    private Texture luzMapa;

    private Texture imgPorta0, imgPorta1, imgPorta2, imgPorta3;
    private Texture imgEspelho;

    private static final float DURACAO_FADE_IN_JOGO = 1.0f;
    private float timerFadeInJogo = 0f;
    private boolean fadeInJogoAtivo = true;
    private Texture texBranca;

    private final ContextoRender ctx = new ContextoRender();

    public TelaJogo(PersecutioGame jogo) {
        this.jogo = jogo;
    }

    @Override
    public void show() {
        imagemMapa  = new Texture(Gdx.files.internal("img/quarto.png"));
        spriteSheet = new Texture(Gdx.files.internal("img/personagem.png"));
        luzMapa     = new Texture(Gdx.files.internal("img/luz-sombra-temp.png"));
        imgPorta0   = new Texture(Gdx.files.internal("img/parte1.png"));
        imgPorta1   = new Texture(Gdx.files.internal("img/parte2.png"));
        imgPorta2   = new Texture(Gdx.files.internal("img/parte3.png"));
        imgPorta3   = new Texture(Gdx.files.internal("img/parte4.png"));
        imgEspelho  = new Texture(Gdx.files.internal("img/reflexo-espelho.png"));

        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.BLACK);
        pm.fill();
        texBranca = new Texture(pm);
        pm.dispose();

        for (Texture t : new Texture[]{imagemMapa, spriteSheet, luzMapa,
                                        imgPorta0, imgPorta1, imgPorta2, imgPorta3, imgEspelho})
            t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        sistemaAudio = jogo.audio;
        sistemaAudio.carregarJogo();

        TmxMapLoader.Parameters pmLoader = new TmxMapLoader.Parameters();
        pmLoader.textureMinFilter = Texture.TextureFilter.Nearest;
        pmLoader.textureMagFilter = Texture.TextureFilter.Nearest;

        mapaTiled      = new TmxMapLoader().load("map/tiles/mapa_quarto.tmx", pmLoader);
        int escala     = 2;

        sistemaColisao = new GerenciadorColisao(mapaTiled, escala, "map/mapa.tiled-project");
        gerComodos     = new GerenciadorComodos(mapaTiled, escala);
        gerPortas      = new GerenciadorPortas(mapaTiled, escala, sistemaColisao.getDefaults());
        sistemaDebug   = new GerenciadorDebug();
        progresso      = new GerenciadorProgresso(sistemaColisao, gerPortas);
        interfaceJogo  = new GerenciadorUI();
        interfaceJogo.inicializar(jogo.fonteDialogos, jogo.viewport, sistemaAudio);
        renderizador   = new GerenciadorRenderizacao();

        float inicialX = 75f;
        float inicialY = (320f * escala) - 12f - 20f;
        jogador        = new Jogador(inicialX, inicialY, spriteSheet);
        hitboxJogador  = jogador.hitbox;

        timerFadeInJogo = 0f;
        fadeInJogoAtivo = true;
    }

    @Override
    public void render(float delta) {
        if (fadeInJogoAtivo) {
            timerFadeInJogo += delta;
            if (timerFadeInJogo >= DURACAO_FADE_IN_JOGO) {
                timerFadeInJogo = DURACAO_FADE_IN_JOGO;
                fadeInJogoAtivo = false;
            }
        }

        interfaceJogo.atualizarTimers(delta);
        tratarInput(delta);

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        jogo.viewport.apply();
        SpriteBatch batch = jogo.batch;
        batch.setProjectionMatrix(jogo.viewport.getCamera().combined);

        boolean umbra       = progresso.isUmbra();
        boolean destrancada = progresso.isDestrancada();
        mundoUmbra            = umbra;
        portaUmbraDestrancada = destrancada;

        float hcX   = jogador.hitbox.x + jogador.hitbox.width  / 2f;
        float hcY   = jogador.hitbox.y + jogador.hitbox.height / 2f;
        comodoAtual = gerComodos.achar(hcX, hcY);

        if (comodoAtual != null && comodoAtual.cameraEstatica)
            ctx.atualizar(jogo, jogador.mundoX, jogador.mundoY, comodoAtual);
        else
            ctx.atualizar(jogo, jogador.mundoX, jogador.mundoY);

        batch.begin();

        renderizador.desenharMapa(ctx, imagemMapa);
        renderizador.desenharNpcs(ctx, sistemaColisao, umbra);

        if (umbra) {
            renderizador.desenharUmbra(ctx, imagemMapa);
        }

        renderizador.desenharComodos(ctx, gerComodos, jogador);

        jogador.desenhar(batch,
            Math.round(ctx.mundoParaTelaX(jogador.mundoX)),
            Math.round(ctx.mundoParaTelaY(jogador.mundoY)));

        // Clone do jogador espelhado
        // Renderiza apenas quando
        // Jogador no comodo quarto
        // Objeto reflexo ativo no mundo atual
        // Jogador visivel na area do reflexo
        // O espelho interativo continua na interface
        if (comodoAtual != null && "quarto".equals(comodoAtual.nomeGrupo)) {
            Rectangle areaReflexo = sistemaColisao.getReflexoArea(umbra);
            if (areaReflexo != null && jogador.hitbox.overlaps(areaReflexo)) {
                renderizador.desenharCloneEspelho(ctx, jogador, spriteSheet, areaReflexo);
            }
        }

        renderizador.desenharLuz(ctx, luzMapa);
        interfaceJogo.desenharTutorial(ctx);

        if (interfaceJogo.isNpc()) {
            interfaceJogo.desenharEscuro(ctx);
            interfaceJogo.desenharNpc(ctx, imgPorta3);
            batch.end();
            interfaceJogo.desenharFadeEVideo(ctx);
            desenharFadeInJogo(ctx);
            return;
        }
        if (interfaceJogo.isEspelho()) {
            interfaceJogo.desenharEspelho(ctx, imgEspelho);
            batch.end();
            interfaceJogo.desenharFadeEVideo(ctx);
            desenharFadeInJogo(ctx);
            return;
        }
        if (interfaceJogo.isPorta()) {
            interfaceJogo.desenharPorta(ctx,
                imgPorta0, imgPorta1, imgPorta2, imgPorta3, progresso.getPartes());
            batch.end();
            interfaceJogo.desenharFadeEVideo(ctx);
            desenharFadeInJogo(ctx);
            return;
        }
        if (interfaceJogo.isSenha()) {
            interfaceJogo.desenharEscuro(ctx);
            batch.end();
            interfaceJogo.desenharFadeEVideo(ctx);
            interfaceJogo.atualizarSenha(delta);
            processarSenha();
            desenharFadeInJogo(ctx);
            return;
        }

        interfaceJogo.desenharAvisos(ctx, sistemaColisao, jogador, umbra, destrancada, progresso.getAviso());
        interfaceJogo.desenharPromptPorta(ctx, gerPortas, sistemaColisao, jogador, umbra);
        interfaceJogo.desenharLiberada(ctx);
        if (interfaceJogo.isPausado()) interfaceJogo.desenharPausa(ctx);

        batch.end();

        interfaceJogo.desenharFadeEVideo(ctx);

        if (mostrarHitboxes) {
            sistemaDebug.desenharHitboxes(this, ctx.cameraX, ctx.cameraY);
            batch.begin();
            sistemaDebug.desenharInfo(this, ctx);
            batch.end();
        }

        desenharFadeInJogo(ctx);
    }

    private void desenharFadeInJogo(ContextoRender ctx) {
        if (!fadeInJogoAtivo) return;
        float alfa = 1f - (timerFadeInJogo / DURACAO_FADE_IN_JOGO);
        if (alfa <= 0.001f) return;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        ctx.batch.begin();
        ctx.batch.setColor(0f, 0f, 0f, alfa);
        ctx.batch.draw(texBranca, 0, 0, ctx.vLargura, ctx.vAltura);
        ctx.batch.setColor(Color.WHITE);
        ctx.batch.end();
    }

    private void processarSenha() {
        String senha = interfaceJogo.pegarSenha();
        if (senha == null) return;
        if (progresso.validarSenha(senha)) interfaceJogo.senhaSucesso();
        else                               interfaceJogo.senhaErro();
    }

    private void tratarInput(float delta) {
        sistemaAudio.tratarInputVolume();

        if (interfaceJogo.isSenha()) return;
        if (interfaceJogo.isFade())  return;

        boolean ctrl = Gdx.input.isKeyPressed(Keys.CONTROL_LEFT)
                    || Gdx.input.isKeyPressed(Keys.CONTROL_RIGHT);
        if (ctrl && Gdx.input.isKeyJustPressed(Keys.H)) {
            mostrarHitboxes = !mostrarHitboxes;
            return;
        }

        sistemaDebug.tratarAtalhos(progresso);

        if (interfaceJogo.puxarInput(jogo.viewport)) return;

        if (Gdx.input.isKeyJustPressed(Keys.E)) tratarInteracao();

        boolean estavaAndando = andando;
        jogador.atualizar(delta, sistemaColisao, mundoUmbra);
        andando = jogador.isAndando();

        progresso.verificarAfastamento(jogador);

        if  (andando && !estavaAndando) sistemaAudio.tocarPassos();
        else if (!andando && estavaAndando) sistemaAudio.pararPassos();

        interfaceJogo.atualizarTutorial(andando, delta);
    }

    private void tratarInteracao() {
        GerenciadorPortas.Porta porta = gerPortas.acharProxima(jogador, mundoUmbra);
        if (porta != null) {
            boolean estaDestrancada = !porta.trancado || sistemaColisao.isDestrancado(porta.nome);

            if (!estaDestrancada) {
                if (porta.destrancavel && progresso.podeDestrancar(porta)) {
                    sistemaColisao.destrancar(porta.nome);
                    sistemaAudio.tocarSomPorta();
                    String videoPath = porta.video;
                    interfaceJogo.iniciarFade(videoPath, () ->
                        jogador.teleportar(porta.spawn.x, porta.spawn.y)
                    );
                } else {
                    interfaceJogo.mudarEstado(GerenciadorUI.UI_PORTA);
                }
                return;
            }

            sistemaAudio.tocarSomPorta();
            if (porta.usarFade) {
                interfaceJogo.iniciarFade(porta.video, () ->
                    jogador.teleportar(porta.spawn.x, porta.spawn.y)
                );
            } else {
                jogador.teleportar(porta.spawn.x, porta.spawn.y);
            }
            return;
        }

        progresso.tratarInteracao(jogador);

        if (progresso.isCinematica()) interfaceJogo.iniciarCinematica();
        if (progresso.isEspelho())    interfaceJogo.mudarEstado(GerenciadorUI.UI_ESPELHO);
        if (progresso.isGaveta())     interfaceJogo.mudarEstado(GerenciadorUI.UI_SENHA);
    }

    @Override
    public void dispose() {
        imagemMapa.dispose();
        spriteSheet.dispose();
        luzMapa.dispose();
        imgPorta0.dispose();
        imgPorta1.dispose();
        imgPorta2.dispose();
        imgPorta3.dispose();
        imgEspelho.dispose();
        texBranca.dispose();
        sistemaAudio.dispose();
        sistemaDebug.dispose();
        interfaceJogo.dispose();
        renderizador.dispose();
        mapaTiled.dispose();
    }

    @Override
    public void resize(int width, int height) {
        jogo.viewport.update(width, height, true);
        interfaceJogo.redimensionar(width, height);
    }

    @Override public void pause()  {}
    @Override public void resume() {}
    @Override public void hide()   { dispose(); }
}
