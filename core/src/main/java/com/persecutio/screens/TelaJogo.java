package com.persecutio.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import com.persecutio.game.PersecutioGame;
import com.persecutio.entities.HitboxConfig;
import com.persecutio.entities.Jogador;
import com.persecutio.entities.EntidadeMapa;
import com.persecutio.managers.ContextoRender;
import com.persecutio.managers.GerenciadorAudio;
import com.persecutio.managers.GerenciadorColisao;
import com.persecutio.managers.GerenciadorDialogo;
import com.persecutio.managers.GerenciadorComodos;
import com.persecutio.managers.GerenciadorDebug;
import com.persecutio.managers.GerenciadorLuzes;
import com.persecutio.managers.GerenciadorPortas;
import com.persecutio.managers.GerenciadorProgresso;
import com.persecutio.managers.GerenciadorRenderizacao;
import com.persecutio.managers.GerenciadorUI;
import com.persecutio.managers.GerenciadorVoz;

// Tela principal do jogo durante a gameplay
public class TelaJogo implements Screen {

    // Referencia para a classe principal do jogo
    private final PersecutioGame jogo;

    // Sistema de colisao do mapa
    public GerenciadorColisao sistemaColisao;
    // Ferramentas de debug
    private GerenciadorDebug sistemaDebug;
    // Gerenciador de audio
    private GerenciadorAudio sistemaAudio;
    // Progresso da historia
    public GerenciadorProgresso progresso;
    // Interface do usuario
    private GerenciadorUI interfaceJogo;
    // Dialogos do jogo
    private GerenciadorDialogo gerDialogo;
    // Voz animalese das falas
    private GerenciadorVoz gerVoz;
    // Renderizador do mapa
    private GerenciadorRenderizacao renderizador;
    // Gerenciador de comodos
    private GerenciadorComodos gerComodos;
    // Gerenciador de portas
    public GerenciadorPortas gerPortas;
    // Sistema de iluminacao
    private GerenciadorLuzes gerLuzes;
    // Mapa carregado do Tiled
    private TiledMap mapaTiled;
    // Mapas alternativos para os mundos real e umbra
    private TiledMap mapaTiledReal;
    private TiledMap mapaTiledUmbra;
    // Renderizador do mapa Tiled
    private OrthogonalTiledMapRenderer rendererTiled;

    // Estado do mapa atualmente renderizado
    private boolean mapaAtualUmbra = false;

    // Entidade do jogador
    public Jogador jogador;

    // Hitbox do jogador para referencia rapida
    public Rectangle hitboxJogador;

    // Indica se o jogador esta no mundo Umbra
    public boolean mundoUmbra = false;
    // Indica se a porta do Umbra foi destrancada
    public boolean portaUmbraDestrancada = false;

    // Flag para mostrar hitboxes de debug
    private boolean mostrarHitboxes = false;
    // Indica se o jogador esta andando
    private boolean andando = false;

    // Controla se o radio inicial de chamada ja foi ativado
    private boolean falanteTocado = false;

    // Comodo atual onde o jogador se encontra
    private GerenciadorComodos.Comodo comodoAtual = null;

    // Spritesheet do personagem
    private Texture spriteSheet;

    // Texturas da UI e overlays
    private Texture imgPorta0, imgEspelho, imgDocumento;
    private Texture spriteElimar;
    private TextureRegion[][] framesElimar;

    // Duracao do fade inicial em segundos
    private static final float DURACAO_FADE = 1.0f;
    // Timer do fade
    private float timerFade = 0f;
    // Indica se o fade esta ativo
    private boolean fadeAtivo = true;
    // Textura preta para o fade
    private Texture texBranca;

    // Coordenadas de spawn de cada mundo
    private float inicialXReal, inicialYReal;
    private float inicialXUmbra, inicialYUmbra;

    // Posicao independente do jogador em cada mundo
    private final Vector2 posReal = new Vector2();
    private final Vector2 posUmbra = new Vector2();
    // Indica se a posicao do mundo Umbra ja foi definida
    private boolean posUmbraDefinida = false;
    // Verdadeiro apenas ate a primeirissima visita ao Umbra
    private boolean primeiraVisitaUmbraGlobal = true;
    // Estado do mundo Umbra no frame anterior para detectar troca
    private boolean umbraAnterior = false;
    // Ultima missao conhecida para invalidar posicao Umbra de missoes anteriores
    private int ultimaMissaoConhecida = 1;
    // Flag para controlar se ja houve sincronizacao inicial no jardim
    private boolean sincronizacaoJardimFeita = false;
    // Cada mundo preserva tambem a ultima orientacao do jogador alem da posicao
    private int direcaoReal = Jogador.DIRECAO_BAIXO;
    private int direcaoUmbra = Jogador.DIRECAO_BAIXO;
    private boolean direcaoUmbraDefinida = false;

    // Chave do documento atualmente sob leitura de imagem
    private String docChave = null;

    // Porta aguardando confirmacao de senha no teclado
    private GerenciadorPortas.Porta portaSenhaPendente = null;

    // Flag para teleporte apos dialogo de revelacao do documento umbra terminar
    private boolean teleportePosRevelacao = false;
    // Controle da transicao visual apos fechar o documento 3
    private boolean exibindoSpriteElimar = false;
    private float timerSpriteElimar = 0f;
    private boolean iniciouDialogoElimar = false;
    private boolean aguardandoDialogoElimar = false;
    private boolean spriteElimarVisivel = false;
    private static final float DURACAO_EXIBICAO_ELIMAR = 1.2f;
    private static final float PAUSA_DIALOGO_ELIMAR = 0.45f;
    private static final float DURACAO_FADE_ELIMAR = 0.35f;

    // Contexto compartilhado de renderizacao
    private final ContextoRender ctx = new ContextoRender();

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
        imgPorta0 = carregarTextura("img/parte1.png");
        imgEspelho = carregarTextura("img/reflexo-espelho.png");
        imgDocumento = carregarTextura("img/documento1.jpg");
        spriteElimar = carregarTextura("img/elimarSprite.png");
        if (spriteElimar != null) {
            spriteElimar.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            framesElimar = TextureRegion.split(spriteElimar, spriteElimar.getWidth() / 2, spriteElimar.getHeight() / 2);
        }

        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.BLACK);
        pm.fill();
        texBranca = new Texture(pm);
        pm.dispose();

        for (Texture t : new Texture[]{ spriteSheet, imgPorta0, imgEspelho, imgDocumento })
            t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        sistemaAudio = jogo.audio;
        sistemaAudio.carregarJogo();

        TmxMapLoader.Parameters pmLoader = new TmxMapLoader.Parameters();
        pmLoader.textureMinFilter = Texture.TextureFilter.Nearest;
        pmLoader.textureMagFilter = Texture.TextureFilter.Nearest;

        mapaTiledReal = new TmxMapLoader().load("map/casaderepouso.tmx", pmLoader);
        mapaTiledUmbra = new TmxMapLoader().load("map/casaderepousoumbra.tmx", pmLoader);
        mapaTiled = mapaTiledReal;
        float escala = 1.375f;

        sistemaColisao = new GerenciadorColisao(mapaTiledReal, mapaTiledUmbra, escala, "map/mapa.tiled-project");
        gerComodos = new GerenciadorComodos(mapaTiled, escala);

        gerPortas = new GerenciadorPortas(mapaTiledReal, mapaTiledUmbra, escala,
                sistemaColisao.getDefaults());
        sistemaDebug = new GerenciadorDebug();
        progresso = new GerenciadorProgresso(sistemaColisao);

        sistemaColisao.setProgresso(progresso);
        gerPortas.setProgresso(progresso);

        interfaceJogo = new GerenciadorUI();
        interfaceJogo.iniciarAudio(jogo.fonteIndicadores, jogo.viewport, sistemaAudio);
        interfaceJogo.setProgresso(progresso);

        gerDialogo = new GerenciadorDialogo();
        interfaceJogo.setDialogo(gerDialogo);

        gerVoz = new GerenciadorVoz();
        gerDialogo.setVoz(gerVoz);

        renderizador = new GerenciadorRenderizacao(escala);
        rendererTiled = new OrthogonalTiledMapRenderer(mapaTiled, escala, jogo.batch);

        gerLuzes = new GerenciadorLuzes();
        gerLuzes.setGerenciadorComodos(gerComodos);
        gerLuzes.setDefaultsClasses(sistemaColisao.getDefaults());
        gerLuzes.carregarLuzesDoTiled(mapaTiledReal, false);
        gerLuzes.carregarLuzesDoTiled(mapaTiledUmbra, true);
        gerLuzes.criarParedes(sistemaColisao.paredesBox(), sistemaColisao.portasBox());

        // Posicao inicial padrao do jogador caso o Tiled nao tenha spawnpoint
        float padraoX = 75f * escala;
        float padraoY = (768f + 180f) * escala;
        this.inicialXReal = padraoX;
        this.inicialYReal = padraoY;
        this.inicialXUmbra = padraoX;
        this.inicialYUmbra = padraoY;

        HitboxConfig cfgSpawn = HitboxConfig.padrao();
        Vector2 spawnReal = lerSpawn(mapaTiledReal, escala, cfgSpawn);
        if (spawnReal != null) {
            this.inicialXReal = spawnReal.x;
            this.inicialYReal = spawnReal.y;
        }
        Vector2 spawnUmbra = lerSpawn(mapaTiledUmbra, escala, cfgSpawn);
        if (spawnUmbra != null) {
            this.inicialXUmbra = spawnUmbra.x;
            this.inicialYUmbra = spawnUmbra.y;
        }

        jogador = new Jogador(this.inicialXReal, this.inicialYReal, spriteSheet);
        hitboxJogador = jogador.hitbox;

        gerLuzes.inicializar(jogador.mundoX, jogador.mundoY);

        posReal.set(this.inicialXReal, this.inicialYReal);
        posUmbra.set(this.inicialXUmbra, this.inicialYUmbra);
        posUmbraDefinida = false;
        umbraAnterior = false;
        primeiraVisitaUmbraGlobal = true;
        ultimaMissaoConhecida = progresso.getMissao();
        direcaoReal = jogador.getDirecao();
        direcaoUmbra = jogador.getDirecao();
        direcaoUmbraDefinida = false;

        timerFade = 0f;
        fadeAtivo = true;
        falanteTocado = false;
        docChave = null;
        exibindoSpriteElimar = false;
        timerSpriteElimar = 0f;
        iniciouDialogoElimar = false;
    }

    // Troca a arte do documento de acordo com o conteudo sorteado do panfleto
    private void atualizarImagemDocumento(String chave) {
        String caminho = "img/documento1.jpg";
        if (chave != null) {
            switch (chave) {
                case "documento3":          caminho = "img/documento3.jpg"; break;
                case "panfleto_fisica":      caminho = "img/planfetoFisica.jpg"; break;
                case "panfleto_patrimonial": caminho = "img/planfetoPatrimonial.jpg"; break;
                case "panfleto_moral":       caminho = "img/planfetoMoral.jpg"; break;
                case "panfleto_psicologica": caminho = "img/planfetoPsicologica.jpg"; break;
                case "panfleto_sexual":      caminho = "img/planfetoSexual.jpg"; break;
                default: break;
            }
        }
        if (imgDocumento != null) imgDocumento.dispose();
        imgDocumento = carregarTextura(caminho);
        imgDocumento.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    }

    // Procura o spawnpoint em uma camada especifica do mapa
    private Vector2 procurarSpawnNaCamada(MapLayer camada, float escala, HitboxConfig cfg) {
        if (camada == null) return null;
        for (MapObject obj : camada.getObjects()) {
            if (!"spawnpoint".equalsIgnoreCase(obj.getName())) continue;
            if (!(obj instanceof RectangleMapObject)) continue;

            Rectangle r = ((RectangleMapObject) obj).getRectangle();
            float cx = r.x * escala + (r.width * escala) / 2f;
            float cy = r.y * escala + (r.height * escala) / 2f;

            return new Vector2(
                cx - cfg.offsetX() - cfg.larguraHitbox() / 2f,
                cy - cfg.offsetY() - cfg.alturaHitbox() / 2f
            );
        }
        return null;
    }

    // Le o spawnpoint de qualquer camada do mapa com busca ampla como fallback
    private Vector2 lerSpawn(TiledMap mapa, float escala, HitboxConfig cfg) {
        MapLayer camadaDestinos = mapa.getLayers().get("Destinos");
        Vector2 result = procurarSpawnNaCamada(camadaDestinos, escala, cfg);
        if (result != null) return result;

        // Busca em todas as outras camadas caso nao encontre na Destinos
        for (MapLayer camada : mapa.getLayers()) {
            if (camada == camadaDestinos) continue;
            result = procurarSpawnNaCamada(camada, escala, cfg);
            if (result != null) return result;
        }

        return null;
    }

    // Loop principal de atualizacao e desenho
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

        boolean umbra = progresso.isUmbra();
        boolean destrancada = progresso.isDestrancada();

        // Nova missao invalida a posicao Umbra salva da missao anterior
        if (progresso.getMissao() != ultimaMissaoConhecida) {
            ultimaMissaoConhecida = progresso.getMissao();
            posUmbraDefinida = false;
        }

        if (umbra != umbraAnterior) {
            umbraAnterior = umbra;
        }

        mundoUmbra = umbra;
        portaUmbraDestrancada = destrancada;

        atualizarMapa(umbra);
        gerLuzes.setAmbienteUmbra(mundoUmbra);

        sistemaAudio.atualizarAmbiente(mundoUmbra, progresso.getMissao());

        float hcX = jogador.hitbox.x + jogador.hitbox.width / 2f;
        float hcY = jogador.hitbox.y + jogador.hitbox.height / 2f;
        comodoAtual = gerComodos.achar(hcX, hcY);

        if (comodoAtual != null
                && ("jardimexterno".equals(comodoAtual.nomeGrupo)
                    || "jardiminterno".equals(comodoAtual.nomeGrupo))) {
            progresso.onSaveJardim(jogador.mundoX, jogador.mundoY);
            if (!mundoUmbra && progresso.getMissao() == 2) {
                // Depois da primeira entrada o remedio permanece liberado pelo resto da missao
                progresso.darFlag("entrou_jardim_m2");
            }
        }

        if (comodoAtual != null && comodoAtual.cameraEstatica)
            ctx.atualizar(jogo, jogador.mundoX, jogador.mundoY, comodoAtual);
        else
            ctx.atualizar(jogo, jogador.mundoX, jogador.mundoY);

        renderizador.renderizarMapa(ctx, rendererTiled, gerComodos, comodoAtual, sistemaColisao);

        batch.begin();

        renderizador.desenharObjetos(ctx, sistemaColisao, gerComodos, comodoAtual);
        renderizador.desenharNpcs(ctx, sistemaColisao, gerComodos, comodoAtual);

        jogador.desenhar(batch,
                Math.round(ctx.mundoParaTelaX(jogador.mundoX)),
                Math.round(ctx.mundoParaTelaY(jogador.mundoY)));

        // Desenha o reflexo do jogador no espelho do quarto
        if (comodoAtual != null && "quarto".equals(comodoAtual.nomeGrupo)) {
            Rectangle areaReflexo = sistemaColisao.areaReflexo();
            if (areaReflexo != null && jogador.hitbox.overlaps(areaReflexo)) {
                renderizador.desenharClone(ctx, jogador, spriteSheet, areaReflexo);
            }
        }

        batch.end();

        gerLuzes.atualizarPosicaoJogador(jogador.mundoX, jogador.mundoY);
        gerLuzes.render(ctx, gerComodos, comodoAtual);

        batch.begin();

        interfaceJogo.desenharTutorial(ctx);

        if (interfaceJogo.isEspelho()) {
            interfaceJogo.desenharEspelho(ctx, imgEspelho);
            batch.end();
            interfaceJogo.desenharVideo(ctx);
            desenharFade(ctx);
            return;
        }
        if (interfaceJogo.obterEstado() == GerenciadorUI.UI_DOCUMENTO) {
            interfaceJogo.desenharDocumento(ctx, imgDocumento);
            batch.end();
            interfaceJogo.desenharVideo(ctx);
            desenharFade(ctx);
            return;
        }
        if (interfaceJogo.isPorta()) {
            interfaceJogo.desenharPorta(ctx, imgPorta0);
            batch.end();
            interfaceJogo.desenharVideo(ctx);
            desenharFade(ctx);
            return;
        }
        if (interfaceJogo.isSenha()) {
            interfaceJogo.desenharEscuro(ctx);
            batch.end();
            interfaceJogo.desenharVideo(ctx);
            interfaceJogo.atualizarSenha(delta, ctx);
            processarSenha();
            if (!interfaceJogo.isSenha()) portaSenhaPendente = null;
            desenharFade(ctx);
            return;
        }

        interfaceJogo.desenharAvisos(ctx, sistemaColisao, jogador);
        interfaceJogo.desenharPrompt(ctx, gerPortas, sistemaColisao, jogador);
        interfaceJogo.desenharLiberada(ctx);
        interfaceJogo.desenharDialogo(ctx);
        if (interfaceJogo.isPausado()) interfaceJogo.desenharPausa(ctx);

        batch.end();

        interfaceJogo.desenharVideo(ctx);
        atualizarTransicaoElimar(delta);
        desenharTransicaoElimar(ctx);

        if (mostrarHitboxes) {
            shapesDebug(delta);
        }

        desenharFade(ctx);
    }

    // Desenha hitboxes e informacoes de debug na tela
    private void shapesDebug(float delta) {
        sistemaDebug.desenharHitboxes(this, ctx.cameraX, ctx.cameraY);
        jogo.batch.begin();
        sistemaDebug.desenharInfo(this, ctx);
        jogo.batch.end();
    }

    // Troca o mapa renderizado quando o mundo muda
    private void atualizarMapa(boolean umbra) {
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

        if (portaSenhaPendente != null) {
            if (senha == null) return;

            GerenciadorPortas.Porta porta = portaSenhaPendente;
            if (senha.equals(porta.senha)) {
                interfaceJogo.senhaSucesso();
                portaSenhaPendente = null;
                sistemaColisao.destrancar(porta.nome);
                sistemaAudio.tocarSomPorta();
                interfaceJogo.iniciarFade(porta.video, () ->
                    moverJogador(jogador, porta)
                );
            } else {
                interfaceJogo.senhaErro();
            }
            return;
        }

        if (senha == null) return;
        if (progresso.onPasswordEntered(senha)) interfaceJogo.senhaSucesso();
        else interfaceJogo.senhaErro();
    }

    // Processa entrada do jogador a cada frame
    private void tratarInput(float delta) {
        for (String efe : gerDialogo.pegarEfeitos()) processarEfeito(efe);

        sistemaAudio.tratarInputVolume();

        // Teleporte apos dialogo de revelacao do documento umbra terminar
        if (teleportePosRevelacao && !gerDialogo.estaAtivo() && interfaceJogo.obterEstado() != GerenciadorUI.UI_DIALOGO) {
            teleportePosRevelacao = false;
            interfaceJogo.fadeSimples(() -> {
                posReal.set(inicialXReal, inicialYReal);
                jogador.teleportar(inicialXReal, inicialYReal);
                jogador.virarParaBaixo();
                progresso.concluirPrimeira(inicialXReal, inicialYReal);
                mundoUmbra = false;
                gerDialogo.iniciar("maria_acorda_missao2");
                interfaceJogo.mudarEstado(GerenciadorUI.UI_DIALOGO);
            }, 3.0f);
        }

        if (interfaceJogo.isSenha()) return;

        if (exibindoSpriteElimar || aguardandoDialogoElimar) {
            andando = false;
            return;
        }

        if (fadeAtivo) {
            andando = false;
            interfaceJogo.atualizarTutorial(andando, delta);
            return;
        }

        atualizarVisibilidadeElimar();

        // Fechamento do documento
        if (interfaceJogo.consumirFechado()) {
            if (progresso.isTeleporteAposDocUmbra()) {
                progresso.consumirTeleporteAposDocUmbra();
                gerDialogo.iniciar("maria_doc1_revelacao");
                interfaceJogo.mudarEstado(GerenciadorUI.UI_DIALOGO);
                teleportePosRevelacao = true;
            } else if (docChave != null && !docChave.isEmpty()) {
                if (progresso.deveIniciarDialogoAposFecharDocumento(docChave)) {
                    gerDialogo.iniciar(docChave);
                    interfaceJogo.mudarEstado(GerenciadorUI.UI_DIALOGO);
                } else if (progresso.deveExibirSpriteElimarAposFecharDocumento(docChave)) {
                    interfaceJogo.fadeSimples(() -> {
                        exibindoSpriteElimar = true;
                        timerSpriteElimar = 0f;
                        iniciouDialogoElimar = false;
                        aguardandoDialogoElimar = false;
                        spriteElimarVisivel = true;
                    }, 0.35f);
                }
                docChave = null;
            }
        }

        // Alto falante inicial da Missao 1
        if (progresso.getMissao() == 1 && progresso.obterFase() == 0 && !interfaceJogo.isBloqueado() && !falanteTocado) {
            falanteTocado = true;
            gerDialogo.iniciar("alto_falante");
            interfaceJogo.mudarEstado(GerenciadorUI.UI_DIALOGO);
        }

        // Aguarda o jogador apertar E para acordar apos a Missao 1
        if (!interfaceJogo.isDialogo() && progresso.getMissao() == 1 && progresso.obterFase() == 6) {
            if (Gdx.input.isKeyJustPressed(Keys.E) || Gdx.input.isKeyJustPressed(Keys.ENTER)) {
                interfaceJogo.fadeSimples(() -> {
                    posReal.set(inicialXReal, inicialYReal);
                    jogador.teleportar(inicialXReal, inicialYReal);
                    jogador.virarParaBaixo();
                    progresso.concluirPrimeira(inicialXReal, inicialYReal);
                    mundoUmbra = false;
                    gerDialogo.iniciar("maria_acorda_missao2");
                    interfaceJogo.mudarEstado(GerenciadorUI.UI_DIALOGO);
                });
            }
            andando = false;
            interfaceJogo.atualizarTutorial(andando, delta);
            return;
        }

        if (interfaceJogo.puxarInput(jogo.viewport)) {
            andando = false;
            interfaceJogo.atualizarTutorial(andando, delta);
            return;
        }

        if (interfaceJogo.isFade()) return;

        if (interfaceJogo.isBloqueado()) {
            andando = false;
            progresso.checarLonge(jogador);
            interfaceJogo.atualizarTutorial(andando, delta);
            return;
        }

        boolean ctrl = Gdx.input.isKeyPressed(Keys.CONTROL_LEFT)
                || Gdx.input.isKeyPressed(Keys.CONTROL_RIGHT);
        if (ctrl && Gdx.input.isKeyJustPressed(Keys.H)) {
            mostrarHitboxes = !mostrarHitboxes;
            return;
        }
        if (ctrl && Gdx.input.isKeyJustPressed(Keys.G)) {
            progresso.completarPanfletosParaTeste();
            progresso.darAviso("Contador de panfletos completado para teste.");
            return;
        }

        sistemaDebug.tratarAtalhos(this);

        // Missao 2 em diante usa o remedio para alternar de mundo
        if (progresso.getMissao() >= 2 && progresso.temCartela() && Gdx.input.isKeyJustPressed(Keys.F)) {
            if (progresso.getMissao() == 2 && !progresso.temFlag("entrou_jardim_m2")) {
                progresso.darAviso("Aqui nao, preciso ir pra um lugar mais arejado...");
                return;
            }

            boolean indoProUmbra = !mundoUmbra;

            // Salva posicao e orientacao do mundo que esta sendo deixado
            if (indoProUmbra) {
                posReal.set(jogador.mundoX, jogador.mundoY);
                direcaoReal = jogador.getDirecao();
            } else {
                posUmbra.set(jogador.mundoX, jogador.mundoY);
                direcaoUmbra = jogador.getDirecao();
                direcaoUmbraDefinida = true;
            }

            // Ao ir para o Umbra a partir do jardim sincroniza posicoes na primeira vez
            if (indoProUmbra && !sincronizacaoJardimFeita) {
                float hcX = jogador.hitbox.x + jogador.hitbox.width / 2f;
                float hcY = jogador.hitbox.y + jogador.hitbox.height / 2f;
                GerenciadorComodos.Comodo comodoJogador = gerComodos.achar(hcX, hcY);
                if (comodoJogador != null && ("jardiminterno".equals(comodoJogador.nomeGrupo) || "jardimexterno".equals(comodoJogador.nomeGrupo))) {
                    posUmbra.set(jogador.mundoX, jogador.mundoY);
                    posUmbraDefinida = true;
                    if (!direcaoUmbraDefinida) {
                        direcaoUmbra = jogador.getDirecao();
                        direcaoUmbraDefinida = true;
                    }
                    sincronizacaoJardimFeita = true;
                }
            }

            float destinoX, destinoY;
            int destinoDirecao;
            if (indoProUmbra) {
                if (!posUmbraDefinida) {
                    if (primeiraVisitaUmbraGlobal) {
                        posUmbra.set(inicialXUmbra, inicialYUmbra);
                        primeiraVisitaUmbraGlobal = false;
                    }
                    posUmbraDefinida = true;
                }
                if (!direcaoUmbraDefinida) {
                    // Na primeira visita ainda nao ha estado anterior conserva a orientacao atual
                    direcaoUmbra = jogador.getDirecao();
                    direcaoUmbraDefinida = true;
                }
                destinoX = posUmbra.x;
                destinoY = posUmbra.y;
                destinoDirecao = direcaoUmbra;
            } else {
                destinoX = posReal.x;
                destinoY = posReal.y;
                destinoDirecao = direcaoReal;
            }

            interfaceJogo.fadeTrocaMundo(() -> {
                progresso.alternarUmbra();
                jogador.teleportar(destinoX, destinoY);
                jogador.setDirecao(destinoDirecao);
            }, true);
            return;
        }

        if (Gdx.input.isKeyJustPressed(Keys.E)) tratarInteracao();

        boolean estavaAndando = andando;
        jogador.atualizar(delta, sistemaColisao);
        andando = jogador.isAndando();

        progresso.checarLonge(jogador);

        if (andando && !estavaAndando) sistemaAudio.tocarPassos();
        else if (!andando && estavaAndando) sistemaAudio.pararPassos();

        interfaceJogo.atualizarTutorial(andando, delta);
    }

    // Teleporta o jogador centralizando a hitbox dentro do retangulo destino
    private void moverJogador(Jogador jogador, GerenciadorPortas.Porta porta) {
        if (porta.areaDestino != null) {
            float cx = porta.areaDestino.x + porta.areaDestino.width / 2f;
            float cy = porta.areaDestino.y + porta.areaDestino.height / 2f;
            float novoX = cx - jogador.obterOffsetX() - jogador.hitbox.width / 2f;
            float novoY = cy - jogador.obterOffsetY() - jogador.hitbox.height / 2f;
            jogador.teleportar(novoX, novoY);
        } else {
            jogador.teleportar(porta.spawn.x, porta.spawn.y);
        }
    }

    // Processa um efeito de jogo disparado por uma tag do dialogo
    private void processarEfeito(String nome) {
        switch (nome) {
            case "ler_prontuario_umbra":
                progresso.lerUmbra();
                break;
            case "tomar_pilula_missao2":
                // Apenas pega a cartela e libera alternancia de mundo sem teleportar
                progresso.darFlag("temcartela");
                progresso.mudarFase(1);
                break;
            default:
                Gdx.app.log("TelaJogo", "efeito desconhecido: " + nome);
        }
    }

    // Trata interacao do jogador com portas e objetos
    private void tratarInteracao() {
        // Missao 2 no Umbra bloqueia saida do jardim
        if (mundoUmbra && progresso.getMissao() == 2 && progresso.obterFase() < 3) {
            GerenciadorPortas.Porta portaSaida = gerPortas.acharPorta(jogador);
            if (portaSaida != null) {
                String destino = portaSaida.nome != null ? portaSaida.nome.toLowerCase() : "";
                if (!destino.contains("jardim")) {
                    progresso.darAviso("Nao posso sair do jardim agora...");
                    return;
                }
            }
        }

        // Empurrar pedras do puzzle na Missao 2 mundo real
        if (!mundoUmbra && progresso.getMissao() == 2 && progresso.obterFase() == 2) {
            GerenciadorColisao.ObjetoColisao pedra = sistemaColisao.acharPedra(jogador);
            if (pedra != null) {
                if (sistemaColisao.empurrarPedra(jogador, pedra)) {
                    sistemaAudio.tocarSomPorta();
                    String no = progresso.pegarDialogo();
                    if (no != null) {
                        gerDialogo.iniciar(no);
                        interfaceJogo.mudarEstado(GerenciadorUI.UI_DIALOGO);
                    }
                }
                return;
            }
        }

        GerenciadorPortas.Porta porta = gerPortas.acharPorta(jogador);
        if (porta != null) {
            // Sem a cartela na Missao 2 fase 0 mostra dialogo de lembrete ou curto
            if (!mundoUmbra && progresso.getMissao() == 2 && progresso.obterFase() == 0
                    && !progresso.temFlag("temcartela")) {
                if (!progresso.temFlag("lembrou_pilula")) {
                    progresso.darFlag("lembrou_pilula");
                    gerDialogo.iniciar("maria_pilula_lembrete");
                } else {
                    gerDialogo.iniciar("maria_precisa_pilulas");
                }
                interfaceJogo.mudarEstado(GerenciadorUI.UI_DIALOGO);
                return;
            }

            // Verifica se a porta exige comportamento especial do progresso
            GerenciadorProgresso.PortaResponse resp = progresso.onPortaInteract(porta);
            if (resp != null) {
                switch (resp.action) {
                    case DIALOG:
                        gerDialogo.iniciar(resp.dialogNode);
                        interfaceJogo.mudarEstado(GerenciadorUI.UI_DIALOGO);
                        return;
                    case FADE_MOVE_AND_CONCLUDE:
                        sistemaAudio.tocarSomPorta();
                        interfaceJogo.iniciarFade(resp.video, () -> {
                            moverJogador(jogador, porta);
                            progresso.concluirSegunda();
                        });
                        return;
                    case CONTINUE:
                        break;
                }
            }

            boolean estaDestrancada = !porta.trancado || sistemaColisao.isDestrancado(porta.nome);

            if (!estaDestrancada) {
                if (porta.temSenha()) {
                    portaSenhaPendente = porta;
                    interfaceJogo.mudarEstado(GerenciadorUI.UI_SENHA);
                } else if (porta.destrancavel && progresso.podeDestrancar(porta)) {
                    sistemaColisao.destrancar(porta.nome);
                    sistemaAudio.tocarSomPorta();
                    interfaceJogo.iniciarFade(porta.video, () ->
                        moverJogador(jogador, porta)
                    );
                } else {
                    interfaceJogo.mudarEstado(GerenciadorUI.UI_PORTA);
                }
                return;
            }

            sistemaAudio.tocarSomPorta();
            if (porta.usarFade) {
                interfaceJogo.iniciarFade(porta.video, () ->
                    moverJogador(jogador, porta)
                );
            } else {
                moverJogador(jogador, porta);
            }
            return;
        }

        // Detecta diretamente qual NPC ou objeto foi ativado
        Rectangle hitboxInteracao = progresso.hitboxFolga(jogador);

        // NPCs como a enfermeira
        EntidadeMapa enfermeira = sistemaColisao.getNpc("enfermeira");
        if (enfermeira != null && hitboxInteracao.overlaps(enfermeira.area)) {
            progresso.onNpcInteract("enfermeira");
        }

        // Dr Elimar na Missao 3 troca para a tela do questionario final
        EntidadeMapa elimar = sistemaColisao.getNpc("elimar");
        if (elimar != null && hitboxInteracao.overlaps(elimar.area) && progresso.getMissao() == 3) {
            jogo.setScreen(new TelaElimar(
                jogo, progresso.obterPanfletosLidos(), progresso.obterPontosPanfletos()));
            return;
        }

        // Interativos procura o primeiro objeto ativo sobreposto e dispara evento
        boolean interagiu = false;
        for (GerenciadorColisao.ObjetoColisao obj : sistemaColisao.todosInterativos().values()) {
            if (obj == null) continue;
            if (!sistemaColisao.checarAtivo(obj)) continue;
            if (!hitboxInteracao.overlaps(obj.area)) continue;

            String nomeObj = obj.nome != null ? obj.nome.toLowerCase() : "";
            // Panfletos usam um sorteio proprio e concedem conhecimento pontuacao
            if (progresso.ehPanfleto(nomeObj)) {
                progresso.onPanfletoFound(obj.nome);
                interagiu = true;
                break;
            }

            // Documentos comuns mantem o fluxo narrativo anterior
            if (nomeObj.startsWith("documento") || nomeObj.startsWith("objeto")) {
                progresso.onDocumentFound(obj.nome, obj.docId, mundoUmbra);
                interagiu = true;
                break;
            }

            progresso.onObjectInteract(obj.nome);
            interagiu = true;
            break;
        }

        if (interagiu) {
            if (progresso.consumirPilula()) {
                posReal.set(jogador.mundoX, jogador.mundoY);
                if (!posUmbraDefinida) {
                    if (primeiraVisitaUmbraGlobal) {
                        posUmbra.set(inicialXUmbra, inicialYUmbra);
                        primeiraVisitaUmbraGlobal = false;
                    }
                    posUmbraDefinida = true;
                }
                float destX = posUmbra.x;
                float destY = posUmbra.y;
                interfaceJogo.fadeSimples(() -> {
                    progresso.alternarUmbra();
                    progresso.mudarFase(1);
                    jogador.teleportar(destX, destY);
                    if (progresso.getMissao() == 1) {
                        gerDialogo.iniciar("maria_entra_umbra_m1");
                        interfaceJogo.mudarEstado(GerenciadorUI.UI_DIALOGO);
                    }
                });
                return;
            }

            if (progresso.consumirDocumento()) {
                progresso.lerUmbra();
                return;
            }

            if (progresso.consumirPendente()) {
                sistemaAudio.tocarDocumento();
                interfaceJogo.mudarEstado(GerenciadorUI.UI_DOCUMENTO);
                docChave = progresso.obterChave();
                atualizarImagemDocumento(docChave);
                return;
            }

            String no = progresso.pegarDialogo();
            if (no != null) {
                gerDialogo.iniciar(no);
                interfaceJogo.mudarEstado(GerenciadorUI.UI_DIALOGO);
                return;
            }

            if (progresso.consumirEspelho()) { interfaceJogo.mudarEstado(GerenciadorUI.UI_ESPELHO); return; }
            if (progresso.consumirGaveta()) { interfaceJogo.mudarEstado(GerenciadorUI.UI_SENHA); return; }
        }

        // Reage a eventos gerados pelo progresso fora da interacao direta
        if (progresso.consumirPilula()) {
            posReal.set(jogador.mundoX, jogador.mundoY);
            if (!posUmbraDefinida) {
                if (primeiraVisitaUmbraGlobal) {
                    posUmbra.set(inicialXUmbra, inicialYUmbra);
                    primeiraVisitaUmbraGlobal = false;
                }
                posUmbraDefinida = true;
            }
            float destX = posUmbra.x;
            float destY = posUmbra.y;
            interfaceJogo.fadeSimples(() -> {
                progresso.alternarUmbra();
                progresso.mudarFase(1);
                jogador.teleportar(destX, destY);
            });
            return;
        }

        if (progresso.consumirDocumento()) {
            progresso.lerUmbra();
            return;
        }

        if (progresso.consumirPendente()) {
            sistemaAudio.tocarDocumento();
            interfaceJogo.mudarEstado(GerenciadorUI.UI_DOCUMENTO);
            docChave = progresso.obterChave();
            atualizarImagemDocumento(docChave);
            return;
        }

        String no = progresso.pegarDialogo();
        if (no != null) {
            gerDialogo.iniciar(no);
            interfaceJogo.mudarEstado(GerenciadorUI.UI_DIALOGO);
        }
    }

    private void atualizarVisibilidadeElimar() {
        if (!spriteElimarVisivel) return;
        if (iniciouDialogoElimar && !gerDialogo.estaAtivo()) {
            finalizarTransicaoElimar();
        }
    }

    private void atualizarTransicaoElimar(float delta) {
        if (exibindoSpriteElimar) {
            timerSpriteElimar += delta;
            if (timerSpriteElimar >= DURACAO_EXIBICAO_ELIMAR) {
                exibindoSpriteElimar = false;
                timerSpriteElimar = 0f;
                if (!iniciouDialogoElimar) {
                    aguardandoDialogoElimar = true;
                    timerSpriteElimar = 0f;
                }
            }
        }

        if (aguardandoDialogoElimar) {
            timerSpriteElimar += delta;
            if (timerSpriteElimar >= PAUSA_DIALOGO_ELIMAR) {
                aguardandoDialogoElimar = false;
                timerSpriteElimar = 0f;
                iniciouDialogoElimar = true;
                gerDialogo.iniciar("elimar_intro");
                interfaceJogo.mudarEstado(GerenciadorUI.UI_DIALOGO);
            }
        }
    }

    private void desenharTransicaoElimar(ContextoRender ctx) {
        if (!spriteElimarVisivel) return;

        float progressoTransicao = exibindoSpriteElimar
            ? Math.min(1f, timerSpriteElimar / DURACAO_EXIBICAO_ELIMAR)
            : 1f;

        float alphaFade = 1f;
        if (exibindoSpriteElimar) {
            alphaFade = Math.min(1f, progressoTransicao / 0.2f);
        } else if (iniciouDialogoElimar) {
            alphaFade = Math.max(0.2f, 1f - Math.min(1f, timerSpriteElimar / DURACAO_FADE_ELIMAR));
        }

        float alphaOverlay = 0.72f * alphaFade;
        float alphaSprite = 0.96f * alphaFade;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        ctx.batch.begin();
        ctx.batch.setColor(0f, 0f, 0f, alphaOverlay);
        ctx.batch.draw(texBranca, 0f, 0f, ctx.vLargura, ctx.vAltura);

        if (framesElimar != null && framesElimar.length > 0 && framesElimar[0].length > 0) {
            TextureRegion frame = framesElimar[0][0];
            float largRetrato = ctx.vLargura * 0.28f;
            float altRetrato = ctx.vAltura * 0.45f;
            float posX = ctx.centroX - largRetrato / 2f;
            float posY = ctx.centroY - altRetrato / 2.1f;
            ctx.batch.setColor(1f, 1f, 1f, alphaSprite);
            ctx.batch.draw(frame, posX, posY, largRetrato, altRetrato);
        }

        ctx.batch.setColor(Color.WHITE);
        ctx.batch.end();
    }

    private void finalizarTransicaoElimar() {
        spriteElimarVisivel = false;
        exibindoSpriteElimar = false;
        aguardandoDialogoElimar = false;
        timerSpriteElimar = 0f;
    }

    // Libera todos os recursos da tela
    @Override
    public void dispose() {
        spriteSheet.dispose();
        imgPorta0.dispose();
        imgEspelho.dispose();
        imgDocumento.dispose();
        if (spriteElimar != null) spriteElimar.dispose();
        texBranca.dispose();
        sistemaAudio.dispose();
        if (gerVoz != null) gerVoz.dispose();
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

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() { dispose(); }
}
