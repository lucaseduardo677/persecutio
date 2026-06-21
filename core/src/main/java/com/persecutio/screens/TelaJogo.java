package com.persecutio.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
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

// tela principal do jogo, orquestra todos os managers a cada frame
public class TelaJogo implements Screen {

    private final PersecutioGame jogo;

    // managers públicos para que o GerenciadorDebug possa acessar o estado
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

    // hitbox exposta para que o debug acesse a posição sem precisar do Jogador
    public Rectangle hitboxJogador;

    // estado de mundo e porta espelhado para o debug
    public boolean mundoUmbra            = false;
    public boolean portaUmbraDestrancada = false;

    // flag que ativa a sobreposição de hitboxes via Ctrl+H
    private boolean mostrarHitboxes = false;
    private boolean andando         = false;

    // cômodo atual usado para controle de câmera estática
    private GerenciadorComodos.Comodo comodoAtual = null;

    // texturas do mapa, personagem e iluminação
    private Texture imagemMapa;
    private Texture spriteSheet;
    private Texture luzMapa;

    // imagens do puzzle de porta umbra, uma por estado de partes coletadas
    private Texture imgPorta0, imgPorta1, imgPorta2, imgPorta3;

    // imagem exibida na tela do espelho
    private Texture imgEspelho;

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

        // nearest para preservar o visual pixel art
        for (Texture t : new Texture[]{imagemMapa, spriteSheet, luzMapa,
                                        imgPorta0, imgPorta1, imgPorta2, imgPorta3, imgEspelho})
            t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        sistemaAudio = new GerenciadorAudio();
        sistemaAudio.carregar();

        // configuração de filtro aplicada ao loader para as texturas do Tiled
        TmxMapLoader.Parameters pm = new TmxMapLoader.Parameters();
        pm.textureMinFilter = Texture.TextureFilter.Nearest;
        pm.textureMagFilter = Texture.TextureFilter.Nearest;

        mapaTiled      = new TmxMapLoader().load("map/tiles/mapa_quarto.tmx", pm);
        int escala     = 2;

        sistemaColisao = new GerenciadorColisao(mapaTiled, escala);
        gerComodos     = new GerenciadorComodos(mapaTiled, escala);
        gerPortas      = new GerenciadorPortas(mapaTiled, escala);
        sistemaDebug   = new GerenciadorDebug();
        progresso      = new GerenciadorProgresso(sistemaColisao, gerPortas);
        interfaceJogo  = new GerenciadorUI();
        interfaceJogo.inicializar(jogo.fonteDialogos, jogo.viewport, sistemaAudio);
        renderizador   = new GerenciadorRenderizacao();

        // posição inicial do jogador no mapa
        float inicialX = 75f;
        float inicialY = (320f * escala) - 12f - 20f;
        jogador        = new Jogador(inicialX, inicialY, spriteSheet);
        hitboxJogador  = jogador.hitbox;
    }

    @Override
    public void render(float delta) {
        interfaceJogo.atualizarTimers(delta);
        tratarInput(delta);

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        jogo.viewport.apply();
        SpriteBatch batch = jogo.batch;
        batch.setProjectionMatrix(jogo.viewport.getCamera().combined);

        // sincroniza o estado de mundo com o progresso
        boolean umbra       = progresso.isUmbra();
        boolean destrancada = progresso.isDestrancada();
        mundoUmbra            = umbra;
        portaUmbraDestrancada = destrancada;

        // descobre o cômodo atual usando o centro da hitbox
        float hcX   = jogador.hitbox.x + jogador.hitbox.width  / 2f;
        float hcY   = jogador.hitbox.y + jogador.hitbox.height / 2f;
        comodoAtual = gerComodos.achar(hcX, hcY);

        // contexto com câmera estática se o cômodo exigir
        ContextoRender ctx = (comodoAtual != null && comodoAtual.estatica)
            ? new ContextoRender(jogo, jogador.mundoX, jogador.mundoY, comodoAtual)
            : new ContextoRender(jogo, jogador.mundoX, jogador.mundoY);

        batch.begin();

        renderizador.desenharMapa(ctx, imagemMapa);
        renderizador.desenharNpcs(ctx, sistemaColisao, umbra);

        if (umbra) {
            renderizador.desenharUmbra(ctx, imagemMapa);
            renderizador.desenharEspelho(ctx, sistemaColisao, jogador, spriteSheet);
        }

        renderizador.desenharComodos(ctx, gerComodos, jogador);

        jogador.desenhar(batch,
            Math.round(ctx.mundoParaTelaX(jogador.mundoX)),
            Math.round(ctx.mundoParaTelaY(jogador.mundoY)));

        renderizador.desenharLuz(ctx, luzMapa);
        interfaceJogo.desenharTutorial(ctx);

        // telas de sobreposição encerram o batch e retornam antes do resto
        if (interfaceJogo.isNpc()) {
            interfaceJogo.desenharEscuro(ctx);
            interfaceJogo.desenharNpc(ctx, imgPorta3);
            batch.end();
            interfaceJogo.desenharFadeEVideo(ctx);
            return;
        }
        if (interfaceJogo.isEspelho()) {
            interfaceJogo.desenharEspelho(ctx, imgEspelho);
            batch.end();
            interfaceJogo.desenharFadeEVideo(ctx);
            // desenha o fade do espelho por cima de tudo
            interfaceJogo.desenharFadeEspelho(ctx);
            return;
        }
        if (interfaceJogo.isPorta()) {
            interfaceJogo.desenharPorta(ctx,
                imgPorta0, imgPorta1, imgPorta2, imgPorta3, progresso.getPartes());
            batch.end();
            interfaceJogo.desenharFadeEVideo(ctx);
            return;
        }
        if (interfaceJogo.isSenha()) {
            interfaceJogo.desenharEscuro(ctx);
            batch.end();
            interfaceJogo.desenharFadeEVideo(ctx);
            interfaceJogo.atualizarSenha(delta);
            processarSenha();
            return;
        }

        // prompts e avisos visíveis durante o jogo normal
        interfaceJogo.desenharAvisos(ctx, sistemaColisao, jogador, umbra, destrancada, progresso.getAviso());
        interfaceJogo.desenharPromptPorta(ctx, gerPortas, sistemaColisao, jogador, umbra);
        interfaceJogo.desenharLiberada(ctx);
        if (interfaceJogo.isPausado()) interfaceJogo.desenharPausa(ctx);

        batch.end();

        interfaceJogo.desenharFadeEVideo(ctx);

        // overlay de debug desenhado por último para ficar sobre tudo
        if (mostrarHitboxes) {
            sistemaDebug.desenharHitboxes(this, ctx.cameraX, ctx.cameraY);
            batch.begin();
            sistemaDebug.desenharInfo(this, ctx);
            batch.end();
        }

        // desenha o fade do espelho por cima de tudo quando ativo
        interfaceJogo.desenharFadeEspelho(ctx);
    }

    // valida a senha digitada e repassa o resultado para a UI
    private void processarSenha() {
        String senha = interfaceJogo.pegarSenha();
        if (senha == null) return;
        if (progresso.validarSenha(senha)) interfaceJogo.senhaSucesso();
        else                               interfaceJogo.senhaErro();
    }

    // processa todo o input de jogo e debug a cada frame
    private void tratarInput(float delta) {
        // nenhum input de movimento enquanto senha ou fade estão ativos
        if (interfaceJogo.isSenha()) return;
        if (interfaceJogo.isFade())  return;

        // Ctrl+H alterna o overlay de hitboxes
        boolean ctrl = Gdx.input.isKeyPressed(Keys.CONTROL_LEFT)
                    || Gdx.input.isKeyPressed(Keys.CONTROL_RIGHT);
        if (ctrl && Gdx.input.isKeyJustPressed(Keys.H)) {
            mostrarHitboxes = !mostrarHitboxes;
            return;
        }

        // atalhos extras de debug como Ctrl+U, Ctrl+P e Ctrl+D
        sistemaDebug.tratarAtalhos(progresso);

        // UI consome o input quando está em estado diferente de jogo
        if (interfaceJogo.puxarInput(jogo.viewport)) return;

        if (Gdx.input.isKeyJustPressed(Keys.E)) tratarInteracao();

        boolean estavaAndando = andando;
        jogador.atualizar(delta, sistemaColisao, mundoUmbra);
        andando = jogador.isAndando();

        // limpa aviso quando o jogador se afasta do interativo
        progresso.verificarAfastamento(jogador);

        // dispara e para os sons de passo na transição de estado
        if  (andando && !estavaAndando) sistemaAudio.tocarPassos();
        else if (!andando && estavaAndando) sistemaAudio.pararPassos();

        interfaceJogo.atualizarTutorial(andando, delta);
    }

    // trata o pressionamento de E dependendo do que está na frente do jogador
    private void tratarInteracao() {
        GerenciadorPortas.Porta porta = gerPortas.acharProxima(jogador, mundoUmbra);
        if (porta != null) {
            boolean estaDestrancada = !porta.trancado || sistemaColisao.isDestrancado(porta.nome);

            if (!estaDestrancada) {
                if (porta.destrancavel && progresso.podeDestrancar(porta)) {
                    // condição cumprida: destrava e teleporta com animação
                    sistemaColisao.destrancar(porta.nome);
                    sistemaAudio.tocarSomPorta();
                    String videoPath = porta.video;
                    interfaceJogo.iniciarFade(videoPath, () ->
                        jogador.teleportar(porta.spawn.x, porta.spawn.y)
                    );
                } else {
                    // condição não cumprida: exibe a tela de partes da porta
                    interfaceJogo.mudarEstado(GerenciadorUI.UI_PORTA);
                }
                return;
            }

            // porta destrancada: teleporta com fade ou diretamente
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

        // sem porta por perto, tenta interação com objetos do mapa
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
