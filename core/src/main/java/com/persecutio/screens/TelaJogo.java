package com.persecutio.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Rectangle;

import com.persecutio.game.PersecutioGame;
import com.persecutio.entities.HitboxConfig;
import com.persecutio.entities.Jogador;
import com.persecutio.managers.ContextoRender;
import com.persecutio.managers.GerenciadorAudio;
import com.persecutio.managers.GerenciadorColisao;
import com.persecutio.managers.GerenciadorComodos;
import com.persecutio.managers.GerenciadorDebug;
import com.persecutio.managers.GerenciadorLuzes;
import com.persecutio.managers.GerenciadorPortas;
import com.persecutio.managers.GerenciadorProgresso;
import com.persecutio.managers.GerenciadorRenderizacao;
import com.persecutio.managers.GerenciadorUI;

// Tela principal do jogo durante a gameplay
public class TelaJogo implements Screen {

    // Referencia para a classe principal do jogo
    private final PersecutioGame jogo;

    // Sistema de colisao do mapa
    public  GerenciadorColisao         sistemaColisao;
    // Ferramentas de debug
    private GerenciadorDebug           sistemaDebug;
    // Gerenciador de audio
    private GerenciadorAudio           sistemaAudio;
    // Progresso da historia
    public  GerenciadorProgresso       progresso;
    // Interface do usuario
    private GerenciadorUI              interfaceJogo;
    // Renderizador do mapa
    private GerenciadorRenderizacao    renderizador;
    // Gerenciador de comodos
    private GerenciadorComodos         gerComodos;
    // Gerenciador de portas
    public  GerenciadorPortas          gerPortas;
    // Sistema de iluminacao
    private GerenciadorLuzes           gerLuzes;
    // Mapa carregado do Tiled
    private TiledMap                   mapaTiled;
    // Mapas alternativos para os mundos real e umbra
    private TiledMap                   mapaTiledReal;
    private TiledMap                   mapaTiledUmbra;
    // Renderizador do mapa Tiled
    private OrthogonalTiledMapRenderer rendererTiled;

    // Estado do mapa atualmente renderizado
    private boolean mapaAtualUmbra = false;

    // Entidade do jogador
    public  Jogador jogador;

    // Hitbox do jogador para referencia rapida
    public Rectangle hitboxJogador;

    // Indica se o jogador esta no mundo Umbra
    public boolean mundoUmbra            = false;
    // Indica se a porta do Umbra foi destrancada
    public boolean portaUmbraDestrancada = false;

    // Flag para mostrar hitboxes de debug
    private boolean mostrarHitboxes = false;
    // Flag se o jogador esta andando
    private boolean andando         = false;

    // Comodo atual onde o jogador se encontra
    private GerenciadorComodos.Comodo comodoAtual = null;

    // Spritesheet do personagem
    private Texture spriteSheet;

    // Texturas das partes da porta
    private Texture imgPorta0, imgPorta1, imgPorta2, imgPorta3;
    // Textura do reflexo do espelho
    private Texture imgEspelho;

    // Duracao do fade inicial em segundos
    private static final float DURACAO_FADE = 1.0f;
    // Timer do fade
    private float timerFade  = 0f;
    // Flag se o fade esta ativo
    private boolean fadeAtivo = true;
    // Textura branca para o fade
    private Texture texBranca;

    // Contexto compartilhado de renderizacao
    private final ContextoRender ctx = new ContextoRender();

    // Construtor da tela do jogo
    public TelaJogo(PersecutioGame jogo) {
        this.jogo = jogo;
    }

    // Carrega uma textura com fallback para branco se nao existir
    private Texture carregarTextura(String caminho) {
        if (Gdx.files.internal(caminho).exists()) {
            try { return new Texture(Gdx.files.internal(caminho)); }
            catch (Exception ignored) {}
        }
        if (caminho.endsWith(".png")) {
            for (String ext : new String[]{ ".jpeg", ".jpg" }) {
                String alt = caminho.substring(0, caminho.length() - 4) + ext;
                if (Gdx.files.internal(alt).exists()) {
                    try { return new Texture(Gdx.files.internal(alt)); }
                    catch (Exception ignored) {}
                }
            }
        } else if (caminho.endsWith(".jpeg") || caminho.endsWith(".jpg")) {
            String alt = caminho.substring(0, caminho.lastIndexOf('.')) + ".png";
            if (Gdx.files.internal(alt).exists()) {
                try { return new Texture(Gdx.files.internal(alt)); }
                catch (Exception ignored) {}
            }
        }
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fill();
        Texture fallback = new Texture(pm);
        pm.dispose();
        return fallback;
    }

    // Inicializa recursos ao entrar na tela
    @Override
    public void show() {
        spriteSheet = carregarTextura("img/personagem.png");
        imgPorta0   = carregarTextura("img/parte1.png");
        imgPorta1   = carregarTextura("img/parte2.png");
        imgPorta2   = carregarTextura("img/parte3.png");
        imgPorta3   = carregarTextura("img/parte4.png");
        imgEspelho  = carregarTextura("img/reflexo-espelho.png");

        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.BLACK);
        pm.fill();
        texBranca = new Texture(pm);
        pm.dispose();

        for (Texture t : new Texture[]{ spriteSheet, imgPorta0, imgPorta1, imgPorta2, imgPorta3, imgEspelho })
            t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        sistemaAudio = jogo.audio;
        sistemaAudio.carregarJogo();

        TmxMapLoader.Parameters pmLoader = new TmxMapLoader.Parameters();
        pmLoader.textureMinFilter = Texture.TextureFilter.Nearest;
        pmLoader.textureMagFilter = Texture.TextureFilter.Nearest;

        mapaTiledReal  = new TmxMapLoader().load("map/casaderepouso.tmx", pmLoader);
        mapaTiledUmbra = new TmxMapLoader().load("map/casaderepousoumbra.tmx", pmLoader);
        mapaTiled      = mapaTiledReal;
        mapaAtualUmbra = false;
        float escala   = 1.375f;

        sistemaColisao = new GerenciadorColisao(mapaTiled, escala, "map/mapa.tiled-project");
        gerComodos     = new GerenciadorComodos(mapaTiled, escala);
        gerPortas      = new GerenciadorPortas(mapaTiled, escala, sistemaColisao.getDefaults());
        sistemaDebug   = new GerenciadorDebug();
        progresso      = new GerenciadorProgresso(sistemaColisao, gerPortas);
        interfaceJogo  = new GerenciadorUI();
        interfaceJogo.inicializar(jogo.fonteDialogos, jogo.viewport, sistemaAudio);
        renderizador   = new GerenciadorRenderizacao(escala);
        rendererTiled  = new OrthogonalTiledMapRenderer(mapaTiled, escala, jogo.batch);

        gerLuzes = new GerenciadorLuzes();
        gerLuzes.setGerenciadorComodos(gerComodos);
        gerLuzes.carregarLuzesDoTiled(mapaTiled);
        gerLuzes.criarParedes(sistemaColisao.getParedesBox2D(), sistemaColisao.getPortasBox2D());

        // Posicao inicial do jogador caso nao encontre spawnpoint
        float inicialX = 75f * escala;
        float inicialY = (768f + 180f) * escala;

        // Procura spawnpoint na camada Destinos
        Rectangle spawnRect = null;
        MapLayer camadaDestinos = mapaTiled.getLayers().get("Destinos");
        if (camadaDestinos != null) {
            for (MapObject obj : camadaDestinos.getObjects()) {
                if ("spawnpoint".equalsIgnoreCase(obj.getName())) {
                    if (obj instanceof RectangleMapObject) {
                        Rectangle r = ((RectangleMapObject) obj).getRectangle();
                        spawnRect = new Rectangle(
                            r.x * escala,
                            r.y * escala,
                            r.width * escala,
                            r.height * escala
                        );
                        break;
                    }
                }
            }
        }

        if (spawnRect != null) {
            // Centraliza a hitbox do jogador dentro do retangulo do spawnpoint
            HitboxConfig cfg = HitboxConfig.padrao();
            float cx = spawnRect.x + spawnRect.width / 2f;
            float cy = spawnRect.y + spawnRect.height / 2f;
            inicialX = cx - cfg.offsetX() - cfg.larguraHitbox() / 2f;
            inicialY = cy - cfg.offsetY() - cfg.alturaHitbox() / 2f;
        }

        jogador        = new Jogador(inicialX, inicialY, spriteSheet);
        hitboxJogador  = jogador.hitbox;

        gerLuzes.inicializar(jogador.mundoX, jogador.mundoY);

        timerFade  = 0f;
        fadeAtivo  = true;
    }

    // Loop principal de renderizacao
    @Override
    public void render(float delta) {
        if (fadeAtivo) {
            timerFade += delta;
            if (timerFade >= DURACAO_FADE) {
                timerFade = DURACAO_FADE;
                fadeAtivo = false;
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

        atualizarMapaParaMundo(umbra);
        gerLuzes.setAmbienteUmbra(mundoUmbra);

        float hcX   = jogador.hitbox.x + jogador.hitbox.width  / 2f;
        float hcY   = jogador.hitbox.y + jogador.hitbox.height / 2f;
        comodoAtual = gerComodos.achar(hcX, hcY);

        if (comodoAtual != null && comodoAtual.cameraEstatica)
            ctx.atualizar(jogo, jogador.mundoX, jogador.mundoY, comodoAtual);
        else
            ctx.atualizar(jogo, jogador.mundoX, jogador.mundoY);

        renderizador.renderizarMapa(ctx, rendererTiled, gerComodos, comodoAtual, umbra);

        batch.begin();

        renderizador.desenharNpcs(ctx, sistemaColisao, umbra);

        jogador.desenhar(batch,
            Math.round(ctx.mundoParaTelaX(jogador.mundoX)),
            Math.round(ctx.mundoParaTelaY(jogador.mundoY)));

        if (comodoAtual != null && "quarto".equals(comodoAtual.nomeGrupo)) {
            Rectangle areaReflexo = sistemaColisao.getReflexoArea(umbra);
            if (areaReflexo != null && jogador.hitbox.overlaps(areaReflexo)) {
                renderizador.desenharCloneEspelho(ctx, jogador, spriteSheet, areaReflexo);
            }
        }

        batch.end();

        gerLuzes.atualizarPosicaoJogador(jogador.mundoX, jogador.mundoY);
        gerLuzes.render(ctx, gerComodos, comodoAtual);

        batch.begin();

        interfaceJogo.desenharTutorial(ctx);

        if (interfaceJogo.isNpc()) {
            interfaceJogo.desenharEscuro(ctx);
            interfaceJogo.desenharNpc(ctx, imgPorta3);
            batch.end();
            interfaceJogo.desenharFadeEVideo(ctx);
            desenharFade(ctx);
            return;
        }
        if (interfaceJogo.isEspelho()) {
            interfaceJogo.desenharEspelho(ctx, imgEspelho);
            batch.end();
            interfaceJogo.desenharFadeEVideo(ctx);
            desenharFade(ctx);
            return;
        }
        if (interfaceJogo.isPorta()) {
            interfaceJogo.desenharPorta(ctx, imgPorta0, imgPorta1, imgPorta2, imgPorta3, progresso.getPartes());
            batch.end();
            interfaceJogo.desenharFadeEVideo(ctx);
            desenharFade(ctx);
            return;
        }
        if (interfaceJogo.isSenha()) {
            interfaceJogo.desenharEscuro(ctx);
            batch.end();
            interfaceJogo.desenharFadeEVideo(ctx);
            interfaceJogo.atualizarSenha(delta);
            processarSenha();
            desenharFade(ctx);
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

        desenharFade(ctx);
    }

    private void atualizarMapaParaMundo(boolean umbra) {
        if (mapaAtualUmbra == umbra) return;
        mapaAtualUmbra = umbra;
        mapaTiled = umbra ? mapaTiledUmbra : mapaTiledReal;
        if (rendererTiled != null && mapaTiled != null) {
            rendererTiled.setMap(mapaTiled);
        }
    }

    // Desenha overlay preto para fade inicial
    private void desenharFade(ContextoRender ctx) {
        if (!fadeAtivo) return;
        float alfa = 1f - (timerFade / DURACAO_FADE);
        if (alfa <= 0.001f) return;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        ctx.batch.begin();
        ctx.batch.setColor(0f, 0f, 0f, alfa);
        ctx.batch.draw(texBranca, 0, 0, ctx.vLargura, ctx.vAltura);
        ctx.batch.setColor(Color.WHITE);
        ctx.batch.end();
    }

    // Valida a senha digitada pelo jogador
    private void processarSenha() {
        String senha = interfaceJogo.pegarSenha();
        if (senha == null) return;
        if (progresso.validarSenha(senha)) interfaceJogo.senhaSucesso();
        else                               interfaceJogo.senhaErro();
    }

    // Processa entrada do jogador a cada frame
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

        sistemaDebug.tratarAtalhos(this); // --------- ctrl + U

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

    // Teleporta o jogador centralizando a hitbox dentro do retangulo destino
    private void teleportarJogadorParaDestino(Jogador jogador, GerenciadorPortas.Porta porta) {
        if (porta.areaDestino != null) {
            float cx = porta.areaDestino.x + porta.areaDestino.width / 2f;
            float cy = porta.areaDestino.y + porta.areaDestino.height / 2f;
            float novoX = cx - jogador.hitboxOffsetX() - jogador.hitbox.width / 2f;
            float novoY = cy - jogador.hitboxOffsetY() - jogador.hitbox.height / 2f;
            jogador.teleportar(novoX, novoY);
        } else {
            jogador.teleportar(porta.spawn.x, porta.spawn.y);
        }
    }

    // Trata interacao do jogador com portas e objetos
    private void tratarInteracao() {
        GerenciadorPortas.Porta porta = gerPortas.acharProxima(jogador, mundoUmbra);
        if (porta != null) {
            boolean estaDestrancada = !porta.trancado || sistemaColisao.isDestrancado(porta.nome);

            if (!estaDestrancada) {
                if (porta.destrancavel && progresso.podeDestrancar(porta)) {
                    sistemaColisao.destrancar(porta.nome);
                    sistemaAudio.tocarSomPorta();
                    interfaceJogo.iniciarFade(porta.video, () ->
                        teleportarJogadorParaDestino(jogador, porta)
                    );
                } else {
                    interfaceJogo.mudarEstado(GerenciadorUI.UI_PORTA);
                }
                return;
            }

            sistemaAudio.tocarSomPorta();
            if (porta.usarFade) {
                interfaceJogo.iniciarFade(porta.video, () ->
                    teleportarJogadorParaDestino(jogador, porta)
                );
            } else {
                teleportarJogadorParaDestino(jogador, porta);
            }
            return;
        }

        progresso.tratarInteracao(jogador);

        if (progresso.isCinematica()) interfaceJogo.iniciarCinematica();
        if (progresso.isEspelho())    interfaceJogo.mudarEstado(GerenciadorUI.UI_ESPELHO);
        if (progresso.isGaveta())     interfaceJogo.mudarEstado(GerenciadorUI.UI_SENHA);
    }

    // Libera todos os recursos da tela
    @Override
    public void dispose() {
        spriteSheet.dispose();
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
        if (gerLuzes != null) gerLuzes.dispose();
        if (mapaTiledReal != null) mapaTiledReal.dispose();
        if (mapaTiledUmbra != null) mapaTiledUmbra.dispose();
        if (rendererTiled != null) rendererTiled.dispose();
    }

    // Ajusta a tela quando a janela e redimensionada
    @Override
    public void resize(int width, int height) {
        jogo.viewport.update(width, height, true);
        interfaceJogo.redimensionar(width, height);
    }

    @Override public void pause()  {}
    @Override public void resume() {}
    @Override public void hide()   { dispose(); }
}