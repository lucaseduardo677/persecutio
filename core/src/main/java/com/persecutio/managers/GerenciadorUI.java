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
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.persecutio.entities.Jogador;

// gerencia todos os estados de interface: jogo, porta, espelho, NPC, senha e fade
public class GerenciadorUI {

    // estados possíveis da interface
    public static final int UI_JOGO    = 0;
    public static final int UI_PORTA   = 1;
    public static final int UI_ESPELHO = 2;
    public static final int UI_NPC     = 3;
    public static final int UI_SENHA   = 4;
    public static final int UI_FADE    = 5;

    // fases do fade de transição de porta
    private enum FaseFade { INATIVO, ESCURECENDO, ESCURO, VIDEO, AGUARDANDO, CLAREANDO }

    private int     estadoUi       = UI_JOGO;
    private boolean mostrarLiberada = false;

    // timers para cinematic NPC e mensagem verde de área liberada
    private float timerNpc   = -1f;
    private float timerVerde = -1f;

    private float opacidade = 1.0f;

    private boolean pausado    = false;
    private int     opcaoPausa = 0;

    private PuzzleSenha puzzle;

    // duração do fade e do tempo de espera entre fases
    private static final float T_FADE   = 0.6f;
    private static final float T_ESPERA = 0.3f;

    private FaseFade faseFade  = FaseFade.INATIVO;
    private float    timerFade = 0f;
    private float    alfaFade  = 0f;

    // callback executado quando a tela atinge o ponto mais escuro
    private Runnable aoEscurecer;

    // textura branca 1x1 usada para o retângulo de fade
    private Texture         texBranca;
    private GerenciadorVideo video;
    private GerenciadorAudio audio;

    private final Vector2     mouse   = new Vector2();
    private final GlyphLayout medidor = new GlyphLayout();

    // guarda o último prompt de interativo para não sobrepor com o prompt de porta
    private String ultimoPromptInterativo = null;

    public void inicializar(BitmapFont fonte, ExtendViewport viewport) {
        inicializar(fonte, viewport, null);
    }

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

    public void setAudio(GerenciadorAudio audioRef) {
        audio = audioRef;
    }

    // atualiza timers e a máquina de estados do fade a cada frame
    public void atualizarTimers(float delta) {
        if (audio != null) audio.atualizar(delta);

        // timer da cena de NPC antes de voltar ao jogo
        if (timerNpc > 0) {
            timerNpc -= delta;
            if (timerNpc <= 0) {
                timerNpc        = -1;
                estadoUi        = UI_JOGO;
                mostrarLiberada = true;
                timerVerde      = 4f;
            }
        }

        // timer para esconder a mensagem de área liberada
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

                    // executa o teleporte no momento mais escuro
                    if (aoEscurecer != null) {
                        aoEscurecer.run();
                        aoEscurecer = null;
                    }

                    if (video.isPreparado()) {
                        video.iniciar();
                        faseFade = FaseFade.VIDEO;
                        if (audio != null) audio.iniciarFadeOut();
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
                    if (audio != null) audio.iniciarFadeIn();
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
                    estadoUi = UI_JOGO;
                }
                break;

            case INATIVO:
                break;
        }
    }

    // inicia o fade escurecendo a tela, executando o callback e clarendo depois
    public void iniciarFade(String caminhoVideo, Runnable aoEscurecer) {
        this.aoEscurecer = aoEscurecer;
        estadoUi         = UI_FADE;
        faseFade         = FaseFade.ESCURECENDO;
        timerFade        = 0f;
        alfaFade         = 0f;
        video.preparar(caminhoVideo);
    }

    public void iniciarFadeSimples(Runnable aoEscurecer) {
        iniciarFade(null, aoEscurecer);
    }

    public boolean isFadeAtivo() { return faseFade != FaseFade.INATIVO; }

    // processa input de UI, retorna true para bloquear input do jogo
    public boolean puxarInput(ExtendViewport viewport) {
        if (estadoUi == UI_FADE) return true;

        if (Gdx.input.isKeyJustPressed(Keys.ESCAPE)) {
            if (estadoUi == UI_PORTA || estadoUi == UI_ESPELHO) {
                estadoUi = UI_JOGO;
                return true;
            }
            pausado = !pausado;
            return true;
        }

        if (pausado) {
            // navegação por teclado no menu de pausa
            if (Gdx.input.isKeyJustPressed(Keys.UP)   || Gdx.input.isKeyJustPressed(Keys.W)) opcaoPausa = 0;
            if (Gdx.input.isKeyJustPressed(Keys.DOWN)  || Gdx.input.isKeyJustPressed(Keys.S)) opcaoPausa = 1;
            if (Gdx.input.isKeyJustPressed(Keys.ENTER)) {
                if (opcaoPausa == 0) pausado = false;
                if (opcaoPausa == 1) Gdx.app.exit();
            }

            // navegação por mouse no menu de pausa
            float vL = viewport.getWorldWidth(), vA = viewport.getWorldHeight();
            float cx = vL / 2f,               cy = vA / 2f;
            mouse.set(Gdx.input.getX(), Gdx.input.getY());
            viewport.unproject(mouse);
            if (mouse.x >= cx-100 && mouse.x <= cx+150 && mouse.y >= cy+45 && mouse.y <= cy+85) {
                opcaoPausa = 0;
                if (Gdx.input.isButtonJustPressed(Buttons.LEFT)) pausado = false;
            }
            if (mouse.x >= cx-100 && mouse.x <= cx+150 && mouse.y >= cy-15 && mouse.y <= cy+25) {
                opcaoPausa = 1;
                if (Gdx.input.isButtonJustPressed(Buttons.LEFT)) Gdx.app.exit();
            }
            return true;
        }

        // E fecha a tela de porta ou espelho sem disparar interação novamente
        if ((estadoUi == UI_PORTA || estadoUi == UI_ESPELHO) && Gdx.input.isKeyJustPressed(Keys.E)) {
            estadoUi = UI_JOGO;
            return true;
        }

        if (estadoUi != UI_JOGO) return true;
        return false;
    }

    public void mudarEstado(int novoEstado) {
        estadoUi = novoEstado;
        if (novoEstado == UI_SENHA) abrirSenha();
    }

    public int getEstado() { return estadoUi; }

    // inicia a sequência de cinemática de NPC com timer de encerramento
    public void iniciarCinematica() {
        estadoUi = UI_NPC;
        timerNpc = 3f;
    }

    // reduz a opacidade do tutorial conforme o jogador começa a andar
    public void atualizarTutorial(boolean andando, float delta) {
        if (andando && opacidade > 0f)
            opacidade = Math.max(0f, opacidade - 1.5f * delta);
    }

    public void redimensionar(int w, int h) { if (puzzle != null) puzzle.redimensionar(w, h); }

    public void   abrirSenha()    { estadoUi = UI_SENHA; if (puzzle != null) puzzle.abrir(); }
    public boolean isSenha()      { return estadoUi == UI_SENHA; }
    public void   atualizarSenha(float delta) {
        if (puzzle != null) { puzzle.atualizar(delta); if (!puzzle.isAberto()) estadoUi = UI_JOGO; }
    }
    public String  pegarSenha()   { return puzzle != null ? puzzle.pegarSenha() : null; }
    public void    senhaSucesso()  { if (puzzle != null) puzzle.fecharSucesso(); }
    public void    senhaErro()     { if (puzzle != null) puzzle.mostrarErro(); }

    // preenche a tela com retângulo preto semitransparente para sobreposições de UI
    public void desenharEscuro(ContextoRender ctx, Texture imagemMapa) {
        ctx.batch.setColor(0f, 0f, 0f, 0.86f);
        ctx.batch.draw(imagemMapa, 0, 0, ctx.vLargura, ctx.vAltura);
        ctx.batch.setColor(Color.WHITE);
    }

    // desenha um texto centralizado horizontalmente em relação ao centro da tela
    private void desenharCentralizado(ContextoRender ctx, BitmapFont fonte, String texto, float offsetY) {
        medidor.setText(fonte, texto);
        fonte.draw(ctx.batch, texto, ctx.centroX - medidor.width / 2f, ctx.centroY + offsetY);
    }

    // exibe as dicas de controle que desaparecem ao começar a andar
    public void desenharTutorial(ContextoRender ctx) {
        if (opacidade <= 0f) return;
        ctx.fonteIndicadores.setColor(0.78f, 0.78f, 0.78f, opacidade);
        desenharCentralizado(ctx, ctx.fonteIndicadores, "Use as setas ou W A S D para andar...", 120);
        desenharCentralizado(ctx, ctx.fonteIndicadores, "Pressione [E] para investigar...", 150);
        ctx.fonteIndicadores.setColor(Color.WHITE);
    }

    // exibe a imagem do NPC sobre fundo escuro durante a cinemática
    public void desenharNpc(ContextoRender ctx, Texture imgPorta3) {
        float popupL = Math.min(300, ctx.vLargura - 40f);
        float popupA = (popupL / imgPorta3.getWidth()) * imgPorta3.getHeight();
        ctx.batch.draw(imgPorta3, ctx.centroX - popupL/2, ctx.centroY - popupA/2, popupL, popupA);
    }

    // exibe a tela de espelho com o horário e instrução de fechar
    public void desenharEspelho(ContextoRender ctx, Texture imagemMapa) {
        desenharEscuro(ctx, imagemMapa);
        ctx.fonteDialogos.setColor(Color.WHITE);
        desenharCentralizado(ctx, ctx.fonteDialogos, "[IMAGEM DO RELOGIO NO ESPELHO]", 50);
        ctx.fonteDialogos.setColor(Color.RED);
        desenharCentralizado(ctx, ctx.fonteDialogos, "O relogio marca 04:10", 0);
        ctx.fonteDialogos.setColor(Color.WHITE);
        desenharCentralizado(ctx, ctx.fonteDialogos, "Pressione [ESC] ou [E] para fechar", -80);
    }

    // exibe a imagem de estado da porta umbra conforme o número de partes coletadas
    public void desenharPorta(ContextoRender ctx, Texture imagemMapa,
                              Texture p0, Texture p1, Texture p2, Texture p3, int partes) {
        desenharEscuro(ctx, imagemMapa);

        // seleciona a imagem correspondente ao progresso atual
        Texture img = partes == 3 ? p3 : partes == 2 ? p2 : partes == 1 ? p1 : p0;
        ctx.batch.draw(img, ctx.centroX - 200, ctx.centroY - 200, 400, 400);

        if (partes == 3) {
            ctx.fonteDialogos.setColor(Color.GREEN);
            desenharCentralizado(ctx, ctx.fonteDialogos, "AREA LIBERADA NO MUNDO UMBRA", -220);
        } else {
            ctx.fonteDialogos.setColor(Color.WHITE);
            desenharCentralizado(ctx, ctx.fonteDialogos, "Faltam pecas (" + partes + "/3)", -220);
        }
        ctx.fonteDialogos.setColor(Color.WHITE);
        desenharCentralizado(ctx, ctx.fonteDialogos, "Pressione [ESC] ou [E] para fechar", -260);
    }

    // desenha a sobreposição de fade e o vídeo de transição quando ativos
    public void desenharFadeEVideo(ContextoRender ctx) {
        if (faseFade == FaseFade.INATIVO) return;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        ctx.batch.begin();

        // retângulo preto com alpha controlado pelo progresso do fade
        ctx.batch.setColor(0f, 0f, 0f, alfaFade);
        ctx.batch.draw(texBranca, 0, 0, ctx.vLargura, ctx.vAltura);

        // vídeo exibido sobre o preto enquanto a tela está escura
        if (faseFade == FaseFade.VIDEO || faseFade == FaseFade.ESCURO) {
            ctx.batch.setColor(Color.WHITE);
            video.desenhar(ctx.batch, 0, 0, ctx.vLargura, ctx.vAltura);
        }

        ctx.batch.setColor(Color.WHITE);
        ctx.batch.end();
    }

    // exibe os prompts de interação e avisos de diálogo sobre o jogo
    public void desenharAvisos(ContextoRender ctx, GerenciadorColisao sistemaColisao,
                               Jogador jogador, boolean mundoUmbra, boolean destrancada, String aviso) {
        ctx.fonteIndicadores.setColor(Color.WHITE);

        // área levemente expandida para detectar proximidade com objetos
        Rectangle hi = new Rectangle(
            jogador.hitbox.x - 8f, jogador.hitbox.y - 8f,
            jogador.hitbox.width + 16f, jogador.hitbox.height + 16f);

        String prompt = null;

        // verifica interativos do mundo real
        if (!mundoUmbra) {
            if (sobreArea(hi, sistemaColisao.getArea("pilula",      false)))
                prompt = "Aperte [E] para tomar a Pilula";
            else if (sobreArea(hi, sistemaColisao.getArea("paciente",  false)))
                prompt = "Aperte [E] para falar com o Paciente";
            else if (sobreArea(hi, sistemaColisao.getArea("enfermeira",false)))
                prompt = "Aperte [E] para falar com a Enfermeira";
            else if (sobreArea(hi, sistemaColisao.getArea("documento", false)))
                prompt = "Aperte [E] para ler o Papel";
        } else {
            // verifica interativos do mundo umbra
            if (sobreArea(hi, sistemaColisao.getArea("cama",    true)))
                prompt = "Aperte [E] para Acordar";
            else if (sobreArea(hi, sistemaColisao.getArea("pilula",  true)))
                prompt = "Aperte [E] para tomar a Pilula";
            else if (sobreArea(hi, sistemaColisao.getArea("espelho", true)))
                prompt = "Aperte [E] para olhar no Espelho";
            else if (sobreArea(hi, sistemaColisao.getArea("gaveta",  true)))
                prompt = "Aperte [E] para abrir a Gaveta";
        }

        // guarda o prompt para que o de porta não sobreponha
        if (prompt != null) {
            ultimoPromptInterativo = prompt;
            desenharCentralizado(ctx, ctx.fonteIndicadores, prompt, -40);
        } else {
            ultimoPromptInterativo = null;
        }

        // exibe o aviso de diálogo ou narrativa, com nome do falante em laranja
        if (!aviso.isEmpty()) {
            if (aviso.contains(": ")) {
                String[] p = aviso.split(": ", 2);
                ctx.fonteNomes.setColor(Color.ORANGE);
                desenharCentralizado(ctx, ctx.fonteNomes, p[0], 80);
                ctx.fonteDialogos.setColor(Color.YELLOW);
                desenharCentralizado(ctx, ctx.fonteDialogos, p[1], 50);
                ctx.fonteNomes.setColor(Color.WHITE);
            } else {
                ctx.fonteDialogos.setColor(Color.WHITE);
                desenharCentralizado(ctx, ctx.fonteDialogos, aviso, 60);
            }
            ctx.fonteDialogos.setColor(Color.WHITE);
        }
    }

    // exibe o prompt de interação com a porta mais próxima, sem sobrepor o de interativo
    public void desenharPromptPorta(ContextoRender ctx, GerenciadorPortas gerPortas,
                                    GerenciadorColisao colisao, Jogador jogador, boolean umbra) {
        if (gerPortas == null) return;
        // outro prompt já está visível, não sobrepõe
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
            label = "Aperte [E] para ir para " + proxima.destino;
        }

        desenharCentralizado(ctx, ctx.fonteIndicadores, label, -40);
    }

    // exibe a mensagem verde de área liberada após a cinemática de NPC
    public void desenharLiberada(ContextoRender ctx) {
        if (!mostrarLiberada) return;
        ctx.fonteIndicadores.setColor(Color.GREEN);
        desenharCentralizado(ctx, ctx.fonteIndicadores, "AREA LIBERADA NO MUNDO UMBRA", -150);
        ctx.fonteIndicadores.setColor(Color.WHITE);
    }

    // desenha o menu de pausa com as opções Voltar e Sair
    public void desenharPausa(ContextoRender ctx) {
        ctx.fonteMenu.setColor(Color.WHITE);
        desenharCentralizado(ctx, ctx.fonteMenu, opcaoPausa == 0 ? "> VOLTAR" : "  VOLTAR", 60);
        desenharCentralizado(ctx, ctx.fonteMenu, opcaoPausa == 1 ? "> SAIR"   : "  SAIR",    0);
    }

    private boolean sobreArea(Rectangle hi, Rectangle area) {
        return area != null && hi.overlaps(area);
    }

    public boolean isPorta()   { return estadoUi == UI_PORTA; }
    public boolean isEspelho() { return estadoUi == UI_ESPELHO; }
    public boolean isNpc()     { return estadoUi == UI_NPC; }
    public boolean isPausado() { return pausado; }
    public boolean isFade()    { return estadoUi == UI_FADE; }
    public boolean isVideo()   { return faseFade == FaseFade.VIDEO; }
    public GerenciadorVideo getVideo() { return video; }

    public void dispose() {
        if (puzzle    != null) puzzle.dispose();
        if (texBranca != null) texBranca.dispose();
        if (video     != null) video.dispose();
        medidor.reset();
    }

    // puzzle de senha com campo de texto e feedback de erro
    private static class PuzzleSenha {
        private static final int TAMANHO_SENHA = 4;

        private Stage     stage;
        private TextField campoSenha;
        private Label     labelFeedback;
        private boolean   aberto        = false;
        private boolean   fecharProximo = false;
        private String    senhaSubmetida = null;

        // texturas geradas programaticamente para o campo de texto
        private Texture cursorTex, selecaoTex, backTex;

        public void inicializar(BitmapFont fonte, ExtendViewport vp) {
            stage = new Stage(new ExtendViewport(
                Math.round(vp.getWorldWidth()), Math.round(vp.getWorldHeight())));

            // cursor de texto
            Pixmap cp = new Pixmap(2, 20, Pixmap.Format.RGBA8888);
            cp.setColor(Color.WHITE); cp.fill();
            cursorTex = new Texture(cp); cp.dispose();

            // seleção de texto
            Pixmap sp = new Pixmap(2, 20, Pixmap.Format.RGBA8888);
            sp.setColor(Color.RED); sp.fill();
            selecaoTex = new Texture(sp); sp.dispose();

            // fundo do campo de senha
            Pixmap bp = new Pixmap(120, 30, Pixmap.Format.RGBA8888);
            bp.setColor(new Color(0.12f, 0.12f, 0.12f, 0.85f)); bp.fill();
            backTex = new Texture(bp); bp.dispose();

            TextFieldStyle ts = new TextFieldStyle();
            ts.font       = fonte;
            ts.fontColor  = Color.WHITE;
            ts.cursor     = new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(new TextureRegion(cursorTex));
            ts.selection  = new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(new TextureRegion(selecaoTex));
            ts.background = new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(new TextureRegion(backTex));

            LabelStyle ls = new LabelStyle(); ls.font = fonte; ls.fontColor = Color.WHITE;

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

        public void abrir() {
            aberto        = true;
            senhaSubmetida = null;
            campoSenha.setText("");
            labelFeedback.setText("Cadeado de 4 digitos:");
            stage.setKeyboardFocus(campoSenha);
            Gdx.input.setOnscreenKeyboardVisible(true);
            Gdx.input.setInputProcessor(stage);
        }

        public boolean isAberto() { return aberto; }

        public void atualizar(float delta) {
            if (!aberto) return;
            if (fecharProximo) {
                aberto        = false;
                fecharProximo = false;
                if (Gdx.input.getInputProcessor() == stage) Gdx.input.setInputProcessor(null);
                return;
            }
            // submete a senha ao pressionar Enter
            if (Gdx.input.isKeyJustPressed(Keys.ENTER) || Gdx.input.isKeyJustPressed(Keys.NUMPAD_ENTER))
                senhaSubmetida = campoSenha.getText();
            if (Gdx.input.isKeyJustPressed(Keys.ESCAPE)) fecharCancelar();
            stage.act(delta);
            stage.draw();
        }

        public void mostrarErro() {
            labelFeedback.setText("Senha incorreta. Tente de novo:");
            campoSenha.setText("");
            stage.setKeyboardFocus(campoSenha);
        }

        public void fecharCancelar() {
            senhaSubmetida = null;
            fecharProximo  = true;
            Gdx.input.setOnscreenKeyboardVisible(false);
            if (Gdx.input.getInputProcessor() == stage) Gdx.input.setInputProcessor(null);
        }

        public void fecharSucesso() {
            fecharProximo = true;
            Gdx.input.setOnscreenKeyboardVisible(false);
            if (Gdx.input.getInputProcessor() == stage) Gdx.input.setInputProcessor(null);
        }

        // retorna a senha digitada e reseta o campo, ou null se nenhuma foi submetida
        public String pegarSenha() {
            String r       = senhaSubmetida;
            senhaSubmetida = null;
            return r;
        }

        public void redimensionar(int w, int h) {
            if (stage != null) stage.getViewport().update(w, h, true);
        }

        public void dispose() {
            if (stage      != null) stage.dispose();
            if (cursorTex  != null) cursorTex.dispose();
            if (selecaoTex != null) selecaoTex.dispose();
            if (backTex    != null) backTex.dispose();
            if (Gdx.input.getInputProcessor() == stage) Gdx.input.setInputProcessor(null);
        }
    }
}
