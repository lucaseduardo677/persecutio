package com.persecutio.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldStyle;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.persecutio.entities.Jogador;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Interface do usuario do jogo
public class GerenciadorUI {

    // Estados da interface
    public static final int UI_JOGO    = 0;
    public static final int UI_PORTA   = 1;
    public static final int UI_ESPELHO = 2;
    public static final int UI_NPC     = 3;
    public static final int UI_SENHA   = 4;
    public static final int UI_FADE    = 5;
    public static final int UI_DIALOGO = 6;

    // Fases do fade de tela
    private enum FaseFade { INATIVO, ESCURECENDO, ESCURO, VIDEO, AGUARDANDO, CLAREANDO }

    // Estados de exibição da mensagem de missão centralizada
    private enum EstadoMissao { ENTRADA, VISIVEL, SAIDA, CONCLUIDA }

    // State atual da UI
    private int     estadoUi       = UI_JOGO;
    // Flag para mostrar mensagem de area liberada
    private boolean mostrarLiberada = false;

    // Timer da cinematica de NPC
    private float timerNpc   = -1f;
    // Timer da mensagem verde
    private float timerVerde = -1f;

    // Estados e variáveis para controle de fade da missão
    private EstadoMissao estadoMissao = EstadoMissao.ENTRADA;
    private float timerMissao = 0f;
    private float alphaMissao = 0f;
    private static final float TEMPO_FADE_MISSAO = 1.5f;
    private static final float TEMPO_VISIVEL_MISSAO = 3.0f;

    // Referencia ao progresso para acompanhar mudancas de missao
    private GerenciadorProgresso progresso;
    // Missao atualmente exibida na UI
    private int missaoExibida = 1;
    // Texto da missao e objetivo em exibicao
    private String tituloMissaoAtual = "Primeiros Passos";
    private String objetivoMissaoAtual = "Va ate a recepcao.";

    // Flag se esta pausado
    private boolean pausado    = false;
    // Opcao selecionada no menu de pausa
    private int     opcaoPausa = 0;
    // Opcao anterior para som
    private int     opcaoPausaAnterior = 0;

    // Puzzle de senha
    private PuzzleSenha puzzle;

    // Referencia ao dialogo ativo
    private GerenciadorDialogo dialogo;
    // Escolha selecionada no dialogo
    private int escolhaDialogo = 0;

    // Cache de texturas carregadas dinamicamente
    private final Map<String, Texture> cacheTexturas = new HashMap<>();

    // Duracao do fade (curto para transição rápida)
    private static final float T_FADE   = 0.3f;
    // Tempo de espera entre fades
    private static final float T_ESPERA = 0.3f;

    // Fase atual do fade
    private FaseFade faseFade  = FaseFade.INATIVO;
    // Timer do fade
    private float    timerFade = 0f;
    // Alpha do fade
    private float    alfaFade  = 0f;

    // Acao a executar ao escurecer
    private Runnable aoEscurecer;

    // Timer para ignorar inputs logo apos a transicao
    private float timerInput = 0f;

    // Textura branca para overlays
    private Texture         texBranca;
    // Gerenciador de video
    private GerenciadorVideo video;
    // Referencia ao audio
    private GerenciadorAudio audio;

    // Coordenadas do mouse
    private final Vector2     mouse   = new Vector2();
    // Medidor de text
    private final GlyphLayout medidor = new GlyphLayout();

    // Ultimo prompt interativo exibido
    private String ultimoPromptInterativo = null;

    // Retangulo temporario
    private final Rectangle rectTemp = new Rectangle();

    // Inicializa recursos da UI
    public void inicializar(BitmapFont fonte, ExtendViewport viewport) {
        inicializar(fonte, viewport, null);
    }

    // Inicializa recursos da UI com audio
    public void inicializar(BitmapFont fonte, ExtendViewport viewport, GerenciadorAudio audioRef) {
        audio  = audioRef;
        puzzle = new PuzzleSenha();
        puzzle.inicializar(fonte, viewport);

        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fill();
        texBranca = new Texture(pm);
        pm.dispose();

        video = new GerenciadorVideo();
    }

    // Define o gerenciador de audio
    public void setAudio(GerenciadorAudio audioRef) {
        audio = audioRef;
    }

    // Define o gerenciador de dialogo
    public void setDialogo(GerenciadorDialogo dialogoRef) {
        dialogo = dialogoRef;
    }

    // Define o gerenciador de progresso para sincronizar a missao exibida
    public void setProgresso(GerenciadorProgresso progressoRef) {
        progresso = progressoRef;
        if (progresso != null) {
            missaoExibida = Math.max(1, progresso.getMissao());
            atualizarTextoMissao();
        }
    }

    // Atualiza os textos de missao conforme o estado atual (sem prefixo numerico)
    private void atualizarTextoMissao() {
        if (progresso == null) return;

        int missaoAtual = Math.max(1, progresso.getMissao());
        int fase = progresso.getFaseMissao();
        switch (missaoAtual) {
            case 1:
                tituloMissaoAtual = "Primeiros Passos";
                if (fase == 0) {
                    objetivoMissaoAtual = "Va ate a recepcao.";
                } else if (fase == 1) {
                    objetivoMissaoAtual = "Fale com a recepcionista.";
                } else if (fase == 2) {
                    objectiveMudaParaPílula(); // objetivo: Volte ao seu quarto e tome o remedio.
                } else if (fase == 3) {
                    objetivoMissaoAtual = "Saia do quarto.";
                } else if (fase == 4) {
                    objetivoMissaoAtual = "Descubra como abrir a porta.";
                } else {
                    objetivoMissaoAtual = "Leia o documento na recepcao.";
                }
                break;
            case 2:
                tituloMissaoAtual = "Ecos do Jardim";
                if (fase == 0) {
                    objetivoMissaoAtual = "Tome seu remedio.";
                } else if (fase == 1) {
                    objetivoMissaoAtual = "Investigue a origem do som.";
                } else if (fase == 2) {
                    objetivoMissaoAtual = "Descubra como abrir a porta que leva aos escritorios.";
                } else if (fase == 3) {
                    objetivoMissaoAtual = "Verifique a porta do Jardim.";
                } else {
                    objetivoMissaoAtual = "Atravesse a porta do Jardim.";
                }
                break;
            default:
                tituloMissaoAtual = "Objetivo Atual";
                objetivoMissaoAtual = "Continue explorando.";
                break;
        }

        missaoExibida = missaoAtual;
    }

    private void objectiveMudaParaPílula() {
        objetivoMissaoAtual = "Volte ao seu quarto e tome o remedio.";
    }

    // Toca som de selecao
    private void tocarSomSelecao() {
        if (audio != null) {
            audio.tocarSelecao();
        }
    }

    // Retorna se o jogador esta bloqueado de se mover ou interagir
    public boolean isBloqueado() {
        return estadoUi == UI_FADE || estadoUi == UI_NPC || estadoUi == UI_SENHA || estadoUi == UI_DIALOGO || estadoMissao != EstadoMissao.CONCLUIDA || timerInput > 0f || (progresso != null && progresso.getFaseMissao() == 6);
    }

    // Atualiza timers da UI e faz o controle de fluxo da exibicao de missao
    public void atualizarTimers(float delta) {
        if (audio != null) audio.atualizar(delta);

        // Deduz tempo do bloqueio de input pos-transicao
        if (timerInput > 0f) {
            timerInput -= delta;
            if (timerInput < 0f) {
                timerInput = 0f;
            }
        }

        if (timerNpc > 0) {
            timerNpc -= delta;
            if (timerNpc <= 0) {
                timerNpc        = -1;
                estadoUi        = UI_JOGO;
                mostrarLiberada = true;
                timerVerde      = 4f;
            }
        }

        if (timerVerde > 0) {
            timerVerde -= delta;
            if (timerVerde <= 0) { timerVerde = -1; mostrarLiberada = false; }
        }

        if (faseFade == FaseFade.INATIVO) return;
        timerFade += delta;

        switch (faseFade) {
            case ESCURECENDO:
                alfaFade = Math.min(1f, timerFade / T_FADE);
                if (timerFade >= T_FADE) {
                    alfaFade  = 1f;
                    faseFade  = FaseFade.ESCURO;
                    timerFade = 0f;

                    if (aoEscurecer != null) {
                        aoEscurecer.run();
                        aoEscurecer = null;
                    }

                    if (video.isPreparado()) {
                        video.iniciar();
                        faseFade = FaseFade.VIDEO;
                        if (audio != null) audio.iniciarFadeOut(T_FADE);
                    } else {
                        faseFade  = FaseFade.AGUARDANDO;
                        timerFade = 0f;
                    }
                }
                break;

            case ESCURO:
                break;

            case VIDEO:
                boolean videoAtivo = video.atualizar(delta);
                if (!videoAtivo) {
                    faseFade  = FaseFade.AGUARDANDO;
                    timerFade = 0f;
                    if (audio != null) audio.iniciarFadeIn(T_FADE);
                }
                break;

            case AGUARDANDO:
                if (timerFade >= T_ESPERA) {
                    faseFade  = FaseFade.CLAREANDO;
                    timerFade = 0f;
                }
                break;

            case CLAREANDO:
                alfaFade = Math.max(0f, 1f - timerFade / T_FADE);
                if (timerFade >= T_FADE) {
                    alfaFade = 0f;
                    faseFade = FaseFade.INATIVO;

                    // Mantem estado de dialogo se estiver ativo
                    if (dialogo != null && dialogo.estaAtivo()) {
                        estadoUi = UI_DIALOGO;
                    } else {
                        estadoUi = UI_JOGO;
                    }

                    // Inicia bloqueio total de 0.5 segundos logo apos o fade terminar
                    timerInput = 0.5f;
                }
                break;

            case INATIVO:
                break;
        }
    }

    // Inicia fade com video opcional
    public void iniciarFade(String caminhoVideo, Runnable aoEscurecer) {
        this.aoEscurecer = aoEscurecer;
        estadoUi         = UI_FADE;
        faseFade         = FaseFade.ESCURECENDO;
        timerFade        = 0f;
        alfaFade         = 0f;
        video.preparar(caminhoVideo);
    }

    // Inicia fade simples sem video
    public void iniciarFadeSimples(Runnable aoEscurecer) {
        iniciarFade(null, aoEscurecer);
    }

    // Retorna se o fade esta ativo
    public boolean isFadeAtivo() { return faseFade != FaseFade.INATIVO; }

    // Processa input do jogador
    public boolean pusrInput(ExtendViewport viewport) {
        return puxarInput(viewport);
    }

    public boolean puxarInput(ExtendViewport viewport) {
        // Ignora totalmente se estiver em transicao ou sob bloqueio pos-transicao
        if (estadoUi == UI_FADE || timerInput > 0f) return true;

        // Se estiver aguardando acordar no final da Missao 1, consome input de movimento de forma estrita
        if (progresso != null && progresso.getFaseMissao() == 6) {
            if (Gdx.input.isKeyJustPressed(Keys.E) || Gdx.input.isKeyJustPressed(Keys.ENTER)) {
                return false; // Permite que a TelaJogo leia a interacao para acordar
            }
            return true;
        }

        if (Gdx.input.isKeyJustPressed(Keys.ESCAPE)) {
            if (estadoUi == UI_PORTA) {
                estadoUi = UI_JOGO;
                return true;
            }
            if (estadoUi == UI_ESPELHO) {
                estadoUi = UI_JOGO;
                return true;
            }
            if (estadoUi == UI_DIALOGO) {
                return true;
            }
            pausado = !pausado;
            if (!pausado) {
                opcaoPausa = 0;
                opcaoPausaAnterior = 0;
            }
            return true;
        }

        if (pausado) {
            boolean mudou = false;
            if (Gdx.input.isKeyJustPressed(Keys.UP)   || Gdx.input.isKeyJustPressed(Keys.W)) {
                opcaoPausa = 0;
                mudou = true;
            }
            if (Gdx.input.isKeyJustPressed(Keys.DOWN)  || Gdx.input.isKeyJustPressed(Keys.S)) {
                opcaoPausa = 1;
                mudou = true;
            }
            if (mudou && opcaoPausa != opcaoPausaAnterior) {
                tocarSomSelecao();
                opcaoPausaAnterior = opcaoPausa;
            }
            if (Gdx.input.isKeyJustPressed(Keys.ENTER)) {
                if (opcaoPausa == 0) pausado = false;
                if (opcaoPausa == 1) Gdx.app.exit();
            }

            float vL = viewport.getWorldWidth(), vA = viewport.getWorldHeight();
            float cx = vL / 2f,               cy = vA / 2f;
            mouse.set(Gdx.input.getX(), Gdx.input.getY());
            viewport.unproject(mouse);

            boolean hoverVoltar = mouse.x >= cx-100 && mouse.x <= cx+150 && mouse.y >= cy+45 && mouse.y <= cy+85;
            boolean hoverSair   = mouse.x >= cx-100 && mouse.x <= cx+150 && mouse.y >= cy-15 && mouse.y <= cy+25;

            if (hoverVoltar) {
                if (opcaoPausa != 0) {
                    opcaoPausa = 0;
                    tocarSomSelecao();
                    opcaoPausaAnterior = 0;
                }
                if (Gdx.input.isButtonJustPressed(Buttons.LEFT)) pausado = false;
            }
            if (hoverSair) {
                if (opcaoPausa != 1) {
                    opcaoPausa = 1;
                    tocarSomSelecao();
                    opcaoPausaAnterior = 1;
                }
                if (Gdx.input.isButtonJustPressed(Buttons.LEFT)) Gdx.app.exit();
            }
            return true;
        }

        if (estadoUi == UI_PORTA && Gdx.input.isKeyJustPressed(Keys.E)) {
            estadoUi = UI_JOGO;
            return true;
        }
        if (estadoUi == UI_ESPELHO) {
            if (Gdx.input.isKeyJustPressed(Keys.E)) {
                estadoUi = UI_JOGO;
            }
            return true;
        }

        if (estadoUi == UI_DIALOGO) {
            puxarInputDialogo();
            return true;
        }

        if (estadoUi != UI_JOGO) return true;
        return false;
    }

    // Processa input do dialogo ativo
    private void puxarInputDialogo() {
        if (dialogo == null) { estadoUi = UI_JOGO; return; }

        if (dialogo.temEscolhas()) {
            List<String> escolhas = dialogo.getEscolhas();

            if (Gdx.input.isKeyJustPressed(Keys.UP) || Gdx.input.isKeyJustPressed(Keys.W)) {
                if (escolhaDialogo > 0) { escolhaDialogo--; tocarSomSelecao(); }
            }
            if (Gdx.input.isKeyJustPressed(Keys.DOWN) || Gdx.input.isKeyJustPressed(Keys.S)) {
                if (escolhaDialogo < escolhas.size() - 1) { escolhaDialogo++; tocarSomSelecao(); }
            }
            if (Gdx.input.isKeyJustPressed(Keys.ENTER) || Gdx.input.isKeyJustPressed(Keys.E)) {
                dialogo.escolher(escolhaDialogo);
                escolhaDialogo = 0;
            }
        } else {
            if (Gdx.input.isKeyJustPressed(Keys.ENTER) || Gdx.input.isKeyJustPressed(Keys.E)) {
                dialogo.avancar();
            }
        }

        if (!dialogo.estaAtivo()) estadoUi = UI_JOGO;
    }

    // Muda o estado da UI
    public void mudarEstado(int novoEstado) {
        estadoUi = novoEstado;
        if (novoEstado == UI_SENHA)   abrirSenha();
        if (novoEstado == UI_DIALOGO) escolhaDialogo = 0;
    }

    // Retorna estado atual da UI
    public int getEstado() { return estadoUi; }

    // Inicia cinematica de NPC
    public void iniciarCinematica() {
        estadoUi = UI_NPC;
        timerNpc = 3f;
    }

    // Gerencia o fluxo da exibicao central da missao por fatias de tempo
    public void atualizarTutorial(boolean andando, float delta) {
        if (progresso != null && missaoExibida != progresso.getMissao()) {
            atualizarTextoMissao();
            // Reseta a maquina de estados para rodar o fade da nova missao
            estadoMissao = EstadoMissao.ENTRADA;
            timerMissao = 0f;
            alphaMissao = 0f;
        }

        if (estadoMissao != EstadoMissao.CONCLUIDA) {
            timerMissao += delta;
            switch (estadoMissao) {
                case ENTRADA:
                    alphaMissao = Math.min(1f, timerMissao / TEMPO_FADE_MISSAO);
                    if (timerMissao >= TEMPO_FADE_MISSAO) {
                        alphaMissao = 1f;
                        estadoMissao = EstadoMissao.VISIVEL;
                        timerMissao = 0f;
                    }
                    break;
                case VISIVEL:
                    alphaMissao = 1f;
                    if (timerMissao >= TEMPO_VISIVEL_MISSAO) {
                        estadoMissao = EstadoMissao.SAIDA;
                        timerMissao = 0f;
                    }
                    break;
                case SAIDA:
                    alphaMissao = Math.max(0f, 1f - (timerMissao / TEMPO_FADE_MISSAO));
                    if (timerMissao >= TEMPO_FADE_MISSAO) {
                        alphaMissao = 0f;
                        estadoMissao = EstadoMissao.CONCLUIDA;
                    }
                    break;
            }
        }
    }

    // Ajusta interface ao redimensionar
    public void redimensionar(int w, int h) { if (puzzle != null) puzzle.redimensionar(w, h); }

    // Abre tela de senha
    public void   abrirSenha()    { estadoUi = UI_SENHA; if (puzzle != null) puzzle.abrir(); }

    // Retorna se esta na tela de senha
    public boolean isSenha()      { return estadoUi == UI_SENHA; }

    // slot para atualizar senha
    public void   atualizarSenha(float delta) {
        if (puzzle != null) { puzzle.atualizar(delta); if (!puzzle.isAberto()) estadoUi = UI_JOGO; }
    }

    // Retorna senha digitada
    public String  pegarSenha()   { return puzzle != null ? puzzle.pegarSenha() : null; }

    // Processa sucesso na senha
    public void    senhaSucesso()  { if (puzzle != null) puzzle.fecharSucesso(); }

    // Processa erro na senha
    public void    senhaErro()     { if (puzzle != null) puzzle.mostrarErro(); }

    // Desenha overlay escuro
    public void desenharEscuro(ContextoRender ctx) {
        desenharEscuro(ctx, 0.86f);
    }

    // Desenha overlay escuro com alpha customizado
    public void desenharEscuro(ContextoRender ctx, float alpha) {
        ctx.batch.setColor(0f, 0f, 0f, alpha);
        ctx.batch.draw(texBranca, 0, 0, ctx.vLargura, ctx.vAltura);
        ctx.batch.setColor(Color.WHITE);
    }

    // Desenha text centralizado na tela
    private void desenharCentralizado(ContextoRender ctx, BitmapFont fonte, String texto, float offsetY) {
        medidor.setText(fonte, texto);
        fonte.draw(ctx.batch, texto, ctx.centroX - medidor.width / 2f, ctx.centroY + offsetY);
    }

    // Desenha tutoriais e o HUD de missao usando estritamente a fonte_indicadores.ttf
    public void desenharTutorial(ContextoRender ctx) {
        // Se a missao ainda nao concluiu a transicao centralizada, desenha com fade e tooltip de controles refinada
        if (estadoMissao != EstadoMissao.CONCLUIDA) {
            ctx.fonteIndicadores.setColor(1f, 1f, 1f, alphaMissao);
            desenharCentralizado(ctx, ctx.fonteIndicadores, tituloMissaoAtual, 40f);
            desenharCentralizado(ctx, ctx.fonteIndicadores, objetivoMissaoAtual, 15f);

            // Exibe a tooltip de controles logo abaixo de forma polida e refinada apenas se for o inicio do jogo (Missao 1)
            if (missaoExibida == 1) {
                ctx.fonteIndicadores.setColor(0.72f, 0.72f, 0.72f, alphaMissao);
                desenharCentralizado(ctx, ctx.fonteIndicadores, "[WASD / Setas] Mover  •  [E] Interagir  •  [TAB] Ver Objetivos", -20f);
            }
            ctx.fonteIndicadores.setColor(Color.WHITE);
            return;
        }

        // Exibe o HUD no canto superior esquerdo de forma temporaria apenas ao pressionar TAB
        if (Gdx.input.isKeyPressed(Keys.TAB)) {
            ctx.fonteIndicadores.setColor(Color.WHITE);
            ctx.fonteIndicadores.draw(ctx.batch, tituloMissaoAtual, 10f, ctx.vAltura - 18f);
            ctx.fonteIndicadores.setColor(0.72f, 0.72f, 0.72f, 1f);
            ctx.fonteIndicadores.draw(ctx.batch, objetivoMissaoAtual, 10f, ctx.vAltura - 40f);
            ctx.fonteIndicadores.setColor(Color.WHITE);
        }

        // Se o jogador possuir a cartela de pilulas no mundo real, desenha a tooltip meio apagada no canto inferior esquerdo
        if (progresso != null && !progresso.isUmbra() && progresso.hasCartela()) {
            ctx.fonteIndicadores.setColor(0.5f, 0.5f, 0.5f, 0.6f);
            ctx.fonteIndicadores.draw(ctx.batch, "Aperte [P] para tomar a pilula", 15f, 25f);
            ctx.fonteIndicadores.setColor(Color.WHITE);
        }
    }

    // Desenha tela de NPC
    public void desenharNpc(ContextoRender ctx, Texture imgPorta3) {
        float popupL = Math.min(300, ctx.vLargura - 40f);
        float popupA = (popupL / imgPorta3.getWidth()) * imgPorta3.getHeight();
        ctx.batch.draw(imgPorta3, ctx.centroX - popupL/2, ctx.centroY - popupA/2, popupL, popupA);
    }

    // Desenha tela do espelho
    public void desenharEspelho(ContextoRender ctx, Texture imgEspelho) {
        desenharEscuro(ctx, 0.86f);

        ctx.batch.draw(imgEspelho, 0, 0, ctx.vLargura, ctx.vAltura);

        ctx.fonteIndicadores.setColor(Color.WHITE);
        desenharCentralizado(ctx, ctx.fonteIndicadores, "Pressione [ESC] ou [E] para fechar", -80);
    }

    // Desenha tela da porta trancada por senha (requisito de pecas removido)
    public void desenharPorta(ContextoRender ctx, Texture p0) {
        desenharEscuro(ctx);

        ctx.batch.draw(p0, ctx.centroX - 200, ctx.centroY - 200, 400, 400);

        ctx.fonteIndicadores.setColor(Color.RED);
        desenharCentralizado(ctx, ctx.fonteIndicadores, "PORTA TRANCADA", -220);

        ctx.fonteIndicadores.setColor(Color.WHITE);
        desenharCentralizado(ctx, ctx.fonteIndicadores, "Encontre a senha de 4 digitos na gaveta do quarto.", -240);
        desenharCentralizado(ctx, ctx.fonteIndicadores, "Pressione [ESC] ou [E] para fechar", -270);
    }

    // Desenha gradiente vertical no rodape para os dialogos sem foto
    private void desenharGradiente(ContextoRender ctx, float altura) {
        float[] vertices = new float[20];
        Color rCor = Color.valueOf("#0D0D0D");

        // Cores empacotadas para o gradiente vertical (baixo opaco para cima transparente)
        float corBaixo = new Color(rCor.r, rCor.g, rCor.b, 1.0f).toFloatBits();
        float corCima  = new Color(rCor.r, rCor.g, rCor.b, 0.0f).toFloatBits();

        // Bottom-left (baixo esquerda)
        vertices[0] = 0f;
        vertices[1] = 0f;
        vertices[2] = corBaixo;
        vertices[3] = 0f;
        vertices[4] = 0f;

        // Top-left (cima esquerda)
        vertices[5] = 0f;
        vertices[6] = altura;
        vertices[7] = corCima;
        vertices[8] = 0f;
        vertices[9] = 1f;

        // Top-right (cima direita)
        vertices[10] = ctx.vLargura;
        vertices[11] = altura;
        vertices[12] = corCima;
        vertices[13] = 1f;
        vertices[14] = 1f;

        // Bottom-right (baixo direita)
        vertices[15] = ctx.vLargura;
        vertices[16] = 0f;
        vertices[17] = corBaixo;
        vertices[18] = 1f;
        vertices[19] = 0f;

        ctx.batch.draw(texBranca, vertices, 0, 20);
    }

    // Desenha caixa de dialogo aplicando as predefinicoes de fundo e retrato automaticamente
    public void desenharDialogo(ContextoRender ctx) {
        if (dialogo == null || !dialogo.estaAtivo()) return;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        String path = dialogo.getRetrato();
        boolean temFoto = path != null && !path.isEmpty();

        if (temFoto) {
            // Predefinicao A: Fundo com a cor #0D0D0D solida na tela inteira
            ctx.batch.setColor(Color.valueOf("#0D0D0D"));
            ctx.batch.draw(texBranca, 0, 0, ctx.vLargura, ctx.vAltura);
            ctx.batch.setColor(Color.WHITE);

            // Resolve textura usando o cache dinâmico de caminhos
            Texture texRetrato = cacheTexturas.get(path);
            if (texRetrato == null) {
                if (Gdx.files.internal(path).exists()) {
                    texRetrato = new Texture(Gdx.files.internal(path));
                    texRetrato.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
                    cacheTexturas.put(path, texRetrato);
                }
            }

            if (texRetrato != null) {
                // Limites maximos da caixa de retrato
                float maxLarg = 220f;
                float maxAlt  = 220f;

                // Calcula proporcao para nao distorcer a imagem
                float proporcao = (float) texRetrato.getWidth() / texRetrato.getHeight();
                float largRetrato = maxLarg;
                float altRetrato  = maxAlt;

                if (proporcao > 1f) {
                    altRetrato = maxLarg / proporcao;
                } else {
                    largRetrato = maxAlt * proporcao;
                }

                // Calcula posicoes de desenho centralizadas
                float posXRetrato = ctx.centroX - (largRetrato / 2f);
                float posYRetrato = ctx.centroY - 30f;

                ctx.batch.draw(texRetrato, posXRetrato, posYRetrato, largRetrato, altRetrato);
            }
        } else {
            // Predefinicao B: Fundo com apenas um gradiente de #0D0D0D ate transparente englobando apenas a area do dialogo
            float alturaDialogo = ctx.vAltura * 0.42f; // cobre de forma suave a area do texto e opcoes
            desenharGradiente(ctx, alturaDialogo);
        }

        // Posicao base do texto colada na borda inferior da tela, deslocada 25px para cima
        float baseX = ctx.centroX;
        float baseY = ctx.vAltura  * 0.25f;
        float nomeY = baseY + 30f;

        String falante = dialogo.getFalante();
        String texto   = dialogo.getTexto();

        // Oculta o nome de Maria (a protagonista) para dar efeito de reflexão/pensamento interno
        if (!falante.isEmpty() && !"maria".equalsIgnoreCase(falante)) {
            String textoNome = falante + ": ";
            ctx.fonteNomes.setColor(Color.ORANGE);
            medidor.setText(ctx.fonteNomes, textoNome);
            float nomeX = baseX - (medidor.width / 2f);
            ctx.fonteNomes.draw(ctx.batch, textoNome, nomeX, nomeY);
            ctx.fonteNomes.setColor(Color.WHITE);
        }

        ctx.fonteDialogos.setColor(Color.WHITE);

        // Divide e quebra as linhas de dialogo de forma automatica para caber em qualquer resolucao de tela de forma centrada
        float largTexto = ctx.vLargura - 100f;
        medidor.setText(ctx.fonteDialogos, texto, Color.WHITE, largTexto, Align.center, true);
        float textoX = baseX - (largTexto / 2f);
        ctx.fonteDialogos.draw(ctx.batch, medidor, textoX, baseY);

        // Lista de escolhas
        List<String> escolhas = dialogo.getEscolhas();
        float javaY = baseY - 35f;

        if (escolhas.isEmpty()) {
            ctx.fonteIndicadores.setColor(Color.GRAY);
            medidor.setText(ctx.fonteIndicadores, "> [E] Continuar");
            ctx.fonteIndicadores.draw(ctx.batch, "> [E] Continuar", baseX - (medidor.width / 2f), javaY);
            ctx.fonteIndicadores.setColor(Color.WHITE);
        } else {
            for (int i = 0; i < escolhas.size(); i++) {
                String prefixo = (i == escolhaDialogo) ? "> " : "  ";
                ctx.fonteDialogos.setColor((i == escolhaDialogo) ? Color.YELLOW : Color.WHITE);
                medidor.setText(ctx.fonteDialogos, prefixo + escolhas.get(i));
                ctx.fonteDialogos.draw(ctx.batch, prefixo + escolhas.get(i), baseX - (medidor.width / 2f), javaY);
                javaY -= 25f;
            }
            ctx.fonteDialogos.setColor(Color.WHITE);
        }
    }

    // Desenha fade e video de transicao
    public void desenharFadeEVideo(ContextoRender ctx) {
        if (faseFade == FaseFade.INATIVO) return;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        ctx.batch.begin();

        ctx.batch.setColor(0f, 0f, 0f, alfaFade);
        ctx.batch.draw(texBranca, 0, 0, ctx.vLargura, ctx.vAltura);

        if (faseFade == FaseFade.VIDEO || faseFade == FaseFade.ESCURO) {
            ctx.batch.setColor(Color.WHITE);
            video.desenhar(ctx.batch, 0, 0, ctx.vLargura, ctx.vAltura);
        }

        ctx.batch.setColor(Color.WHITE);
        ctx.batch.end();
    }

    // Desenha fade do espelho
    public void desenharFadeEspelho(ContextoRender ctx) {
        if (faseFade == FaseFade.INATIVO) return;
        if (alfaFade <= 0.001f) return;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        ctx.batch.begin();
        ctx.batch.setColor(0f, 0f, 0f, alfaFade);
        ctx.batch.draw(texBranca, 0, 0, ctx.vLargura, ctx.vAltura);
        ctx.batch.setColor(Color.WHITE);
        ctx.batch.end();
    }

    // Desenha avisos e prompts interativos utilizando fonte_indicadores.ttf
    public void desenharAvisos(ContextoRender ctx, GerenciadorColisao sistemaColisao,
                               Jogador jogador, boolean mundoUmbra, boolean destrancada,
                               String aviso, boolean falouComEnfermeira) {
        ctx.fonteIndicadores.setColor(Color.WHITE);

        rectTemp.set(
            jogador.hitbox.x - 8f, jogador.hitbox.y - 8f,
            jogador.hitbox.width + 16f, jogador.hitbox.height + 16f);

        String prompt = null;

        if (!mundoUmbra) {
            // Verifica se esta encarando alguma pedra para empurrar (Missão 2, Fase 2) no mundo real
            if (progresso.getMissao() == 2 && progresso.getFaseMissao() == 2) {
                GerenciadorColisao.ObjetoColisao pedra = sistemaColisao.acharPedraEncarada(jogador, mundoUmbra);
                if (pedra != null) {
                    prompt = "Aperte [E] para Empurrar";
                }
            }

            if (prompt == null) {
                if (sobreArea(rectTemp, sistemaColisao.getArea("pilula",      false))) {
                    // A prompt da pílula só aparece na tela após falar com a enfermeira
                    if (falouComEnfermeira) {
                        prompt = "Aperte [E] para tomar a Pilula";
                    }
                } else if (sobreArea(rectTemp, sistemaColisao.getArea("enfermeira",false))
                      || sobreArea(rectTemp, sistemaColisao.getArea("npcRecepcao", false))) {
                    // A enfermeira e a recepcionista sao tratadas de forma unificada como Enfermeira
                    prompt = "Aperte [E] para falar com a Enfermeira";
                } else if (sobreArea(rectTemp, sistemaColisao.getArea("documento", false))
                      || sobreArea(rectTemp, sistemaColisao.getArea("documento1", false))) {
                    prompt = "Aperte [E] para ler o Papel";
                }
            }
        } else {
            if (sobreArea(rectTemp, sistemaColisao.getArea("cama",    true)))
                prompt = "Aperte [E] para Acordar";
            else if (sobreArea(rectTemp, sistemaColisao.getArea("pilula",  true)))
                prompt = "Aperte [E] para tomar a Pilula";
            else if (sobreArea(rectTemp, sistemaColisao.getArea("espelho", true)))
                prompt = "Aperte [E] para olhar no Espelho";
            else if (sobreArea(rectTemp, sistemaColisao.getArea("gaveta",  true)))
                prompt = "Aperte [E] para abrir a Gaveta";
            else if (sobreArea(rectTemp, sistemaColisao.getArea("documento",  true))
                  || sobreArea(rectTemp, sistemaColisao.getArea("documento1",  true)))
                prompt = "Aperte [E] para ler o Documento";

            // Prompt do documento opcional de jardim no Umbra (Fase 3 ou superior)
            if (prompt == null && progresso.getFaseMissao() >= 3) {
                Rectangle bancoArea = sistemaColisao.getArea("banco", true);
                if (bancoArea == null) {
                    bancoArea = sistemaColisao.getArea("banco_jardim", true);
                }
                if (sobreArea(rectTemp, bancoArea)) {
                    prompt = "Aperte [E] para ler o Documento Opcional";
                }
            }
        }

        if (prompt != null) {
            ultimoPromptInterativo = prompt;
            desenharCentralizado(ctx, ctx.fonteIndicadores, prompt, -40);
        } else {
            ultimoPromptInterativo = null;
        }

        if (!aviso.isEmpty()) {
            if (aviso.contains(": ")) {
                String[] p = aviso.split(": ", 2);
                ctx.fonteIndicadores.setColor(Color.ORANGE);
                desenharCentralizado(ctx, ctx.fonteIndicadores, p[0], 80);
                ctx.fonteIndicadores.setColor(Color.YELLOW);
                desenharCentralizado(ctx, ctx.fonteIndicadores, p[1], 50);
                ctx.fonteIndicadores.setColor(Color.WHITE);
            } else {
                ctx.fonteIndicadores.setColor(Color.WHITE);
                desenharCentralizado(ctx, ctx.fonteIndicadores, aviso, 60);
            }
        }
    }

    // Desenha prompt de porta proxima
    public void desenharPromptPorta(ContextoRender ctx, GerenciadorPortas gerPortas,
                                    GerenciadorColisao colisao, Jogador jogador, boolean umbra) {
        if (gerPortas == null) return;
        if (ultimoPromptInterativo != null) return;

        GerenciadorPortas.Porta proxima = gerPortas.acharProxima(jogador, umbra);
        if (proxima == null) return;

        boolean estaDestrancada = !proxima.trancado || colisao.isDestrancado(proxima.nome);

        String label;
        if (!estaDestrancada) {
            label = proxima.destrancavel
                ? "Aperte [E] para inspecionar a Porta"
                : "Porta trancada para sempre";
        } else {
            label = "Aperte [E] para ir para " + proxima.label;
        }

        desenharCentralizado(ctx, ctx.fonteIndicadores, label, -40);
    }

    // Desenha mensagem de area liberada
    public void desenharLiberada(ContextoRender ctx) {
        if (!mostrarLiberada) return;
        ctx.fonteIndicadores.setColor(Color.GREEN);
        desenharCentralizado(ctx, ctx.fonteIndicadores, "AREA LIBERADA NO MUNDO UMBRA", -150);
        ctx.fonteIndicadores.setColor(Color.WHITE);
    }

    // Desenha menu de pausa usando fonte_indicadores.ttf
    public void desenharPausa(ContextoRender ctx) {
        ctx.fonteIndicadores.setColor(Color.WHITE);
        desenharCentralizado(ctx, ctx.fonteIndicadores, opcaoPausa == 0 ? "> VOLTAR" : "  VOLTAR", 40);
        desenharCentralizado(ctx, ctx.fonteIndicadores, opcaoPausa == 1 ? "> SAIR"   : "  SAIR",    0);
    }

    // Verifica se hitbox esta sobre uma area
    private boolean sobreArea(Rectangle hi, Rectangle area) {
        return area != null && hi.overlaps(area);
    }

    // Retorna se esta na tela de porta
    public boolean isPorta()   { return estadoUi == UI_PORTA; }

    // Retorna se esta na tela de espelho
    public boolean isEspelho() { return estadoUi == UI_ESPELHO; }

    // Retorna se esta em cinematica de NPC
    public boolean isNpc()     { return estadoUi == UI_NPC; }

    // Retorna se esta em dialogo
    public boolean isDialogo() { return estadoUi == UI_DIALOGO; }

    // Retorna se esta pausado
    public boolean isPausado() { return pausado; }

    // Retorna se esta em fade
    public boolean isFade()    { return estadoUi == UI_FADE; }

    // Retorna se esta em video
    public boolean isVideo()   { return faseFade == FaseFade.VIDEO; }

    // Retorna gerenciador de video
    public GerenciadorVideo getVideo() { return video; }

    // Libera recursos da UI
    public void dispose() {
        if (puzzle    != null) puzzle.dispose();
        if (texBranca != null) texBranca.dispose();
        if (video     != null) video.dispose();

        // Libera texturas dos retratos guardadas no cache
        for (Texture tex : cacheTexturas.values()) {
            if (tex != null) tex.dispose();
        }
        cacheTexturas.clear();

        medidor.reset();
    }

    // Puzzle de senha interno
    private static class PuzzleSenha {
        // Tamanho maximo da senha
        private static final int TAMANHO_SENHA = 4;

        // Stage do puzzle
        private Stage     stage;
        // Campo de texto da senha
        private TextField campoSenha;
        // Label de feedback
        private Label     labelFeedback;
        // Flag se esta aberto
        private boolean   aberto        = false;
        // Flag para fechar no proximo frame
        private boolean   fecharProximo = false;
        // Senha submetida
        private String    senhaSubmetida = null;

        // Texturas do cursor selecao e fundo
        private Texture cursorTex, selecaoTex, backTex;

        // Inicializa recursos do puzzle
        public void inicializar(BitmapFont font, ExtendViewport vp) {
            stage = new Stage(new ExtendViewport(
                Math.round(vp.getWorldWidth()), Math.round(vp.getWorldHeight())));

            Pixmap cp = new Pixmap(2, 20, Pixmap.Format.RGBA8888);
            cp.setColor(Color.WHITE); cp.fill();
            cursorTex = new Texture(cp); cp.dispose();

            Pixmap sp = new Pixmap(2, 20, Pixmap.Format.RGBA8888);
            sp.setColor(Color.RED); sp.fill();
            selecaoTex = new Texture(sp); sp.dispose();

            Pixmap bp = new Pixmap(120, 30, Pixmap.Format.RGBA8888);
            bp.setColor(Color.valueOf("#1e1e1ed8")); bp.fill();
            backTex = new Texture(bp); bp.dispose();

            TextFieldStyle ts = new TextFieldStyle();
            ts.font       = font;
            ts.fontColor  = Color.WHITE;
            ts.cursor     = new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(new TextureRegion(cursorTex));
            ts.selection  = new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(new TextureRegion(selecaoTex));
            ts.background = new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(new TextureRegion(backTex));

            LabelStyle ls = new LabelStyle(); ls.font = font; ls.fontColor = Color.WHITE;

            campoSenha    = new TextField("", ts);
            campoSenha.setMaxLength(TAMANHO_SENHA);
            campoSenha.setTextFieldFilter((f, c) -> Character.isDigit(c));
            campoSenha.setAlignment(com.badlogic.gdx.utils.Align.center);

            labelFeedback = new Label("Cadeado de 4 digitos:", ls);
            labelFeedback.setAlignment(com.badlogic.gdx.utils.Align.center);

            Table t = new Table(); t.setFillParent(true); t.center();
            t.add(labelFeedback).padBottom(12f).row();
            t.add(campoSenha).width(120f).height(30f).row();
            stage.addActor(t);
        }

        // Abre o puzzle de senha
        public void abrir() {
            aberto        = true;
            senhaSubmetida = null;
            campoSenha.setText("");
            labelFeedback.setText("Cadeado de 4 digitos:");
            stage.setKeyboardFocus(campoSenha);
            Gdx.input.setOnscreenKeyboardVisible(true);
            Gdx.input.setInputProcessor(stage);
        }

        // Retorna se esta aberto
        public boolean isAberto() { return aberto; }

        // Atualiza estado do puzzle
        public void atualizar(float delta) {
            if (!aberto) return;
            if (fecharProximo) {
                aberto        = false;
                fecharProximo = false;
                if (Gdx.input.getInputProcessor() == stage) Gdx.input.setInputProcessor(null);
                return;
            }
            if (Gdx.input.isKeyJustPressed(Keys.ENTER) || Gdx.input.isKeyJustPressed(Keys.NUMPAD_ENTER))
                senhaSubmetida = campoSenha.getText();
            if (Gdx.input.isKeyJustPressed(Keys.ESCAPE)) fecharCancelar();
            stage.act(delta);
            stage.draw();
        }

        // Mostra erro na senha
        public void mostrarErro() {
            labelFeedback.setText("Senha incorreta. Tente de novo:");
            campoSenha.setText("");
            stage.setKeyboardFocus(campoSenha);
        }

        // Fecha puzzle cancelando
        public void fecharCancelar() {
            senhaSubmetida = null;
            fecharProximo  = true;
            Gdx.input.setOnscreenKeyboardVisible(false);
            if (Gdx.input.getInputProcessor() == stage) Gdx.input.setInputProcessor(null);
        }

        // Fecha puzzle com sucesso
        public void fecharSucesso() {
            fecharProximo = true;
            Gdx.input.setOnscreenKeyboardVisible(false);
            if (Gdx.input.getInputProcessor() == stage) Gdx.input.setInputProcessor(null);
        }

        // Retorna senha digitada
        public String pegarSenha() {
            String r       = senhaSubmetida;
            senhaSubmetida = null;
            return r;
        }

        // Ajusta tamanho do puzzle
        public void redimensionar(int w, int h) {
            if (stage != null) stage.getViewport().update(w, h, true);
        }

        // Libera recursos do puzzle
        public void dispose() {
            if (stage      != null) stage.dispose();
            if (cursorTex  != null) cursorTex.dispose();
            if (selecaoTex != null) selecaoTex.dispose();
            if (backTex    != null) backTex.dispose();
            if (Gdx.input.getInputProcessor() == stage) Gdx.input.setInputProcessor(null);
        }
    }

    // Configuração de retrato
    public static class ConfigRetrato {
        // Cor do fundo
        public final Color   corFundo;
        // Opacidade do fundo
        public final float   opacidade;
        // Texture do retrato
        public final Texture imagem;

        // Construtor da configuracao
        public ConfigRetrato(Color corFundo, float opacidade, Texture imagem) {
            this.corFundo  = corFundo;
            this.opacidade = opacidade;
            this.imagem    = imagem;
        }
    }
}
