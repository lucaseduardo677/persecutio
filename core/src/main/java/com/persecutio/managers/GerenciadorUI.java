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

// Interface do jogo
public class GerenciadorUI {

    public static final int UI_JOGO    = 0;
    public static final int UI_PORTA   = 1;
    public static final int UI_ESPELHO = 2;
    public static final int UI_NPC     = 3;
    public static final int UI_SENHA   = 4;
    public static final int UI_FADE    = 5;

    private enum FaseFade { INATIVO, ESCURECENDO, ESCURO, VIDEO, AGUARDANDO, CLAREANDO }

    private int     estadoUi       = UI_JOGO;
    private boolean mostrarLiberada = false;

    private float timerNpc   = -1f;
    private float timerVerde = -1f;

    private float opacidade = 1.0f;

    private boolean pausado    = false;
    private int     opcaoPausa = 0;
    private int     opcaoPausaAnterior = 0;

    private PuzzleSenha puzzle;

    private static final float T_FADE   = 0.6f;
    private static final float T_ESPERA = 0.3f;

    private FaseFade faseFade  = FaseFade.INATIVO;
    private float    timerFade = 0f;
    private float    alfaFade  = 0f;

    private Runnable aoEscurecer;

    private Texture         texBranca;
    private GerenciadorVideo video;
    private GerenciadorAudio audio;

    private final Vector2     mouse   = new Vector2();
    private final GlyphLayout medidor = new GlyphLayout();

    private String ultimoPromptInterativo = null;

    private final Rectangle rectTemp = new Rectangle();

    // Inicialização dos recursos
    public void inicializar(BitmapFont fonte, ExtendViewport viewport) {
        inicializar(fonte, viewport, null);
    }

    // Inicialização dos recursos
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

    // Definição do valor
    public void setAudio(GerenciadorAudio audioRef) {
        audio = audioRef;
    }

    // Reprodução do som
    private void tocarSomSelecao() {
        if (audio != null) {
            audio.tocarSelecao();
        }
    }

    // Atualização do estado
    public void atualizarTimers(float delta) {
        if (audio != null) audio.atualizar(delta);

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

    // Início do processo
    public void iniciarFade(String caminhoVideo, Runnable aoEscurecer) {
        this.aoEscurecer = aoEscurecer;
        estadoUi         = UI_FADE;
        faseFade         = FaseFade.ESCURECENDO;
        timerFade        = 0f;
        alfaFade         = 0f;
        video.preparar(caminhoVideo);
    }

    // Início do processo
    public void iniciarFadeSimples(Runnable aoEscurecer) {
        iniciarFade(null, aoEscurecer);
    }

    // Consulta do estado
    public boolean isFadeAtivo() { return faseFade != FaseFade.INATIVO; }

    // Leitura do input
    public boolean puxarInput(ExtendViewport viewport) {
        if (estadoUi == UI_FADE) return true;

        if (Gdx.input.isKeyJustPressed(Keys.ESCAPE)) {
            if (estadoUi == UI_PORTA) {
                estadoUi = UI_JOGO;
                return true;
            }
            if (estadoUi == UI_ESPELHO) {
                estadoUi = UI_JOGO;
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

        if (estadoUi != UI_JOGO) return true;
        return false;
    }

    // Troca de estado
    public void mudarEstado(int novoEstado) {
        estadoUi = novoEstado;
        if (novoEstado == UI_SENHA) abrirSenha();
    }

    // Consulta do estado
    public int getEstado() { return estadoUi; }

    // Início do processo
    public void iniciarCinematica() {
        estadoUi = UI_NPC;
        timerNpc = 3f;
    }

    // Atualização do estado
    public void atualizarTutorial(boolean andando, float delta) {
        if (andando && opacidade > 0f)
            opacidade = Math.max(0f, opacidade - 1.5f * delta);
    }

    // Ajuste da interface
    public void redimensionar(int w, int h) { if (puzzle != null) puzzle.redimensionar(w, h); }

    // Abertura da tela
    public void   abrirSenha()    { estadoUi = UI_SENHA; if (puzzle != null) puzzle.abrir(); }
    // Consulta do estado
    public boolean isSenha()      { return estadoUi == UI_SENHA; }
    // Atualização do estado
    public void   atualizarSenha(float delta) {
        if (puzzle != null) { puzzle.atualizar(delta); if (!puzzle.isAberto()) estadoUi = UI_JOGO; }
    }
    // Leitura da senha
    public String  pegarSenha()   { return puzzle != null ? puzzle.pegarSenha() : null; }
    // Processamento interno
    public void    senhaSucesso()  { if (puzzle != null) puzzle.fecharSucesso(); }
    // Processamento interno
    public void    senhaErro()     { if (puzzle != null) puzzle.mostrarErro(); }

    // Desenho dos elementos
    public void desenharEscuro(ContextoRender ctx) {
        desenharEscuro(ctx, 0.86f);
    }

    // Desenho dos elementos
    public void desenharEscuro(ContextoRender ctx, float alpha) {
        ctx.batch.setColor(0f, 0f, 0f, alpha);
        ctx.batch.draw(texBranca, 0, 0, ctx.vLargura, ctx.vAltura);
        ctx.batch.setColor(Color.WHITE);
    }

    // Desenho dos elementos
    private void desenharCentralizado(ContextoRender ctx, BitmapFont fonte, String texto, float offsetY) {
        medidor.setText(fonte, texto);
        fonte.draw(ctx.batch, texto, ctx.centroX - medidor.width / 2f, ctx.centroY + offsetY);
    }

    // Desenho dos elementos
    public void desenharTutorial(ContextoRender ctx) {
        if (opacidade <= 0f) return;
        ctx.fonteIndicadores.setColor(0.78f, 0.78f, 0.78f, opacidade);
        desenharCentralizado(ctx, ctx.fonteIndicadores, "Use as setas ou W A S D para andar...", 120);
        desenharCentralizado(ctx, ctx.fonteIndicadores, "Pressione [E] para investigar...", 150);
        ctx.fonteIndicadores.setColor(Color.WHITE);
    }

    // Desenho dos elementos
    public void desenharNpc(ContextoRender ctx, Texture imgPorta3) {
        float popupL = Math.min(300, ctx.vLargura - 40f);
        float popupA = (popupL / imgPorta3.getWidth()) * imgPorta3.getHeight();
        ctx.batch.draw(imgPorta3, ctx.centroX - popupL/2, ctx.centroY - popupA/2, popupL, popupA);
    }

    // Desenho dos elementos
    public void desenharEspelho(ContextoRender ctx, Texture imgEspelho) {
        // Fundo escuro
        desenharEscuro(ctx, 0.86f);

        // Imagem espelho tela cheia
        ctx.batch.draw(imgEspelho, 0, 0, ctx.vLargura, ctx.vAltura);

        // Texto
        ctx.fonteDialogos.setColor(Color.WHITE);
        desenharCentralizado(ctx, ctx.fonteDialogos, "Pressione [ESC] ou [E] para fechar", -80);
    }

    public void desenharPorta(ContextoRender ctx,
                              Texture p0, Texture p1, Texture p2, Texture p3, int partes) {
        desenharEscuro(ctx);

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

    // Desenho dos elementos
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

    // Desenho dos elementos
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

    public void desenharAvisos(ContextoRender ctx, GerenciadorColisao sistemaColisao,
                               Jogador jogador, boolean mundoUmbra, boolean destrancada, String aviso) {
        ctx.fonteIndicadores.setColor(Color.WHITE);

        rectTemp.set(
            jogador.hitbox.x - 8f, jogador.hitbox.y - 8f,
            jogador.hitbox.width + 16f, jogador.hitbox.height + 16f);

        String prompt = null;

        if (!mundoUmbra) {
            if (sobreArea(rectTemp, sistemaColisao.getArea("pilula",      false)))
                prompt = "Aperte [E] para tomar a Pilula";
            else if (sobreArea(rectTemp, sistemaColisao.getArea("paciente",  false)))
                prompt = "Aperte [E] para falar com o Paciente";
            else if (sobreArea(rectTemp, sistemaColisao.getArea("enfermeira",false)))
                prompt = "Aperte [E] para falar com a Enfermeira";
            else if (sobreArea(rectTemp, sistemaColisao.getArea("documento", false)))
                prompt = "Aperte [E] para ler o Papel";
        } else {
            if (sobreArea(rectTemp, sistemaColisao.getArea("cama",    true)))
                prompt = "Aperte [E] para Acordar";
            else if (sobreArea(rectTemp, sistemaColisao.getArea("pilula",  true)))
                prompt = "Aperte [E] para tomar a Pilula";
            else if (sobreArea(rectTemp, sistemaColisao.getArea("espelho", true)))
                prompt = "Aperte [E] para olhar no Espelho";
            else if (sobreArea(rectTemp, sistemaColisao.getArea("gaveta",  true)))
                prompt = "Aperte [E] para abrir a Gaveta";
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
            label = "Aperte [E] para ir para " + proxima.destino;
        }

        desenharCentralizado(ctx, ctx.fonteIndicadores, label, -40);
    }

    // Desenho dos elementos
    public void desenharLiberada(ContextoRender ctx) {
        if (!mostrarLiberada) return;
        ctx.fonteIndicadores.setColor(Color.GREEN);
        desenharCentralizado(ctx, ctx.fonteIndicadores, "AREA LIBERADA NO MUNDO UMBRA", -150);
        ctx.fonteIndicadores.setColor(Color.WHITE);
    }

    // Desenho dos elementos
    public void desenharPausa(ContextoRender ctx) {
        ctx.fonteMenu.setColor(Color.WHITE);
        desenharCentralizado(ctx, ctx.fonteMenu, opcaoPausa == 0 ? "> VOLTAR" : "  VOLTAR", 60);
        desenharCentralizado(ctx, ctx.fonteMenu, opcaoPausa == 1 ? "> SAIR"   : "  SAIR",    0);
    }

    // Processamento interno
    private boolean sobreArea(Rectangle hi, Rectangle area) {
        return area != null && hi.overlaps(area);
    }

    // Consulta do estado
    public boolean isPorta()   { return estadoUi == UI_PORTA; }
    // Consulta do estado
    public boolean isEspelho() { return estadoUi == UI_ESPELHO; }
    // Consulta do estado
    public boolean isNpc()     { return estadoUi == UI_NPC; }
    // Consulta do estado
    public boolean isPausado() { return pausado; }
    // Consulta do estado
    public boolean isFade()    { return estadoUi == UI_FADE; }
    // Consulta do estado
    public boolean isVideo()   { return faseFade == FaseFade.VIDEO; }
    // Consulta do estado
    public GerenciadorVideo getVideo() { return video; }

    // Liberação dos recursos
    public void dispose() {
        if (puzzle    != null) puzzle.dispose();
        if (texBranca != null) texBranca.dispose();
        if (video     != null) video.dispose();
        medidor.reset();
    }

    private static class PuzzleSenha {
        private static final int TAMANHO_SENHA = 4;

        private Stage     stage;
        private TextField campoSenha;
        private Label     labelFeedback;
        private boolean   aberto        = false;
        private boolean   fecharProximo = false;
        private String    senhaSubmetida = null;

        private Texture cursorTex, selecaoTex, backTex;

        // Inicialização dos recursos
        public void inicializar(BitmapFont fonte, ExtendViewport vp) {
            stage = new Stage(new ExtendViewport(
                Math.round(vp.getWorldWidth()), Math.round(vp.getWorldHeight())));

            Pixmap cp = new Pixmap(2, 20, Pixmap.Format.RGBA8888);
            cp.setColor(Color.WHITE); cp.fill();
            cursorTex = new Texture(cp); cp.dispose();

            Pixmap sp = new Pixmap(2, 20, Pixmap.Format.RGBA8888);
            sp.setColor(Color.RED); sp.fill();
            selecaoTex = new Texture(sp); sp.dispose();

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

        // Abertura da tela
        public void abrir() {
            aberto        = true;
            senhaSubmetida = null;
            campoSenha.setText("");
            labelFeedback.setText("Cadeado de 4 digitos:");
            stage.setKeyboardFocus(campoSenha);
            Gdx.input.setOnscreenKeyboardVisible(true);
            Gdx.input.setInputProcessor(stage);
        }

        // Consulta do estado
        public boolean isAberto() { return aberto; }

        // Atualização do estado
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

        // Exibição do aviso
        public void mostrarErro() {
            labelFeedback.setText("Senha incorreta. Tente de novo:");
            campoSenha.setText("");
            stage.setKeyboardFocus(campoSenha);
        }

        // Abertura da tela
        public void fecharCancelar() {
            senhaSubmetida = null;
            fecharProximo  = true;
            Gdx.input.setOnscreenKeyboardVisible(false);
            if (Gdx.input.getInputProcessor() == stage) Gdx.input.setInputProcessor(null);
        }

        // Abertura da tela
        public void fecharSucesso() {
            fecharProximo = true;
            Gdx.input.setOnscreenKeyboardVisible(false);
            if (Gdx.input.getInputProcessor() == stage) Gdx.input.setInputProcessor(null);
        }

        // Leitura da senha
        public String pegarSenha() {
            String r       = senhaSubmetida;
            senhaSubmetida = null;
            return r;
        }

        // Ajuste da interface
        public void redimensionar(int w, int h) {
            if (stage != null) stage.getViewport().update(w, h, true);
        }

        // Liberação dos recursos
        public void dispose() {
            if (stage      != null) stage.dispose();
            if (cursorTex  != null) cursorTex.dispose();
            if (selecaoTex != null) selecaoTex.dispose();
            if (backTex    != null) backTex.dispose();
            if (Gdx.input.getInputProcessor() == stage) Gdx.input.setInputProcessor(null);
        }
    }
}