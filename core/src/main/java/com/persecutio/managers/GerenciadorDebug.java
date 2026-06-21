package com.persecutio.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Rectangle;
import com.persecutio.screens.TelaJogo;

// desenha hitboxes e informações de estado em tela quando Ctrl+H está ativo
public class GerenciadorDebug {

    private final ShapeRenderer shapes;

    // cores de cada camada de hitbox
    private static final Color COR_PAREDE     = new Color(0.2f, 0.85f, 0.2f, 1f);
    private static final Color COR_INTERATIVO = new Color(0f,   0.9f,  0.9f, 1f);
    private static final Color COR_NPC        = new Color(0.8f, 0f,    0.9f, 1f);
    private static final Color COR_PORTA      = new Color(1f,   0.55f, 0f,   1f);
    private static final Color COR_ALCANCE    = new Color(1f,   1f,    0f,   1f);
    private static final Color COR_JOGADOR    = new Color(1f,   0.15f, 0.15f,1f);

    // deve ser igual à FOLGA definida no GerenciadorPortas
    private static final float FOLGA_PORTA = 24f;

    public GerenciadorDebug() {
        shapes = new ShapeRenderer();
    }

    // processa atalhos de teclado exclusivos do modo debug
    public void tratarAtalhos(GerenciadorProgresso progresso) {
        boolean ctrl = Gdx.input.isKeyPressed(Keys.CONTROL_LEFT)
                    || Gdx.input.isKeyPressed(Keys.CONTROL_RIGHT);
        if (!ctrl) return;

        // Ctrl+U alterna entre mundo real e umbra
        if (Gdx.input.isKeyJustPressed(Keys.U)) {
            progresso.alternarUmbra();
        }
        // Ctrl+P adiciona uma parte para testar o desbloqueio da porta umbra
        if (Gdx.input.isKeyJustPressed(Keys.P)) {
            progresso.adicionarParte();
            progresso.setAviso("[DEBUG] Partes: " + progresso.getPartes() + "/3");
        }
        // Ctrl+D força as 3 partes coletadas imediatamente
        if (Gdx.input.isKeyJustPressed(Keys.D)) {
            progresso.forcarPartes(3);
            progresso.setAviso("[DEBUG] Forcado 3/3 partes");
        }
    }

    // desenha os contornos de todas as hitboxes sobre o jogo
    public void desenharHitboxes(TelaJogo jogo, float cameraX, float cameraY) {
        shapes.begin(ShapeType.Line);

        // paredes e colisões do mundo atual
        shapes.setColor(COR_PAREDE);
        for (Rectangle r : jogo.sistemaColisao.getParedes(jogo.mundoUmbra)) {
            shapes.rect(r.x + cameraX, r.y + cameraY, r.width, r.height);
        }

        // áreas de objetos interativos
        shapes.setColor(COR_INTERATIVO);
        for (Rectangle r : jogo.sistemaColisao.getInterativos(jogo.mundoUmbra).values()) {
            shapes.rect(r.x + cameraX, r.y + cameraY, r.width, r.height);
        }

        // hitboxes de NPCs presentes no mundo atual
        shapes.setColor(COR_NPC);
        for (com.persecutio.entities.EntidadeMapa npc : jogo.sistemaColisao.getNpcs(jogo.mundoUmbra).values()) {
            shapes.rect(npc.area.x + cameraX, npc.area.y + cameraY, npc.area.width, npc.area.height);
        }

        // portas sempre visíveis pois bloqueiam em qualquer mundo
        shapes.setColor(COR_PORTA);
        for (GerenciadorColisao.ObjetoColisao p : jogo.sistemaColisao.getHitboxPortasCompletas()) {
            shapes.rect(p.area.x + cameraX, p.area.y + cameraY, p.area.width, p.area.height);
        }

        // retângulo expandido que representa o alcance de detecção de porta
        shapes.setColor(COR_ALCANCE);
        Rectangle hj = jogo.hitboxJogador;
        shapes.rect(
            hj.x - FOLGA_PORTA + cameraX,
            hj.y - FOLGA_PORTA + cameraY,
            hj.width  + FOLGA_PORTA * 2f,
            hj.height + FOLGA_PORTA * 2f
        );

        // hitbox real usada para colisão do jogador
        shapes.setColor(COR_JOGADOR);
        shapes.rect(hj.x + cameraX, hj.y + cameraY, hj.width, hj.height);

        shapes.end();
    }

    // desenha o painel de texto com estado do jogo no canto superior esquerdo
    public void desenharInfo(TelaJogo jogo, ContextoRender ctx) {
        float x  = 8f;
        float y  = ctx.vAltura - 10f;
        float dy = 18f;

        // cabeçalho com instrução para fechar o debug
        ctx.fonteIndicadores.setColor(Color.YELLOW);
        ctx.fonteIndicadores.draw(ctx.batch, "== DEBUG  Ctrl+H ==", x, y);
        y -= dy;

        // desempenho
        ctx.fonteIndicadores.setColor(Color.WHITE);
        ctx.fonteIndicadores.draw(ctx.batch, "FPS: " + Gdx.graphics.getFramesPerSecond(), x, y);
        y -= dy;

        // posição da hitbox do jogador no espaço de mundo
        Rectangle hj = jogo.hitboxJogador;
        ctx.fonteIndicadores.draw(ctx.batch, String.format("Pos: %.0f, %.0f", hj.x, hj.y), x, y);
        y -= dy;

        // mundo atual com cor diferente para cada estado
        ctx.fonteIndicadores.setColor(jogo.mundoUmbra ? Color.MAGENTA : Color.CYAN);
        ctx.fonteIndicadores.draw(ctx.batch, "Mundo: " + (jogo.mundoUmbra ? "UMBRA" : "REAL"), x, y);
        y -= dy;

        // contadores de progresso da sessão
        GerenciadorProgresso prog = jogo.progresso;
        ctx.fonteIndicadores.setColor(Color.WHITE);
        ctx.fonteIndicadores.draw(ctx.batch,
            "Partes: " + prog.getPartes() + "/3   Missao: " + prog.getMissao(), x, y);
        y -= dy;

        // estado de cada peça coletável
        ctx.fonteIndicadores.draw(ctx.batch,
            "Espelho:" + sim(prog.isPecaEspelho())
            + "  Gaveta:" + sim(prog.isPecaGaveta())
            + "  NPC:"    + sim(prog.isPecaNpc()), x, y);
        y -= dy;

        ctx.fonteIndicadores.draw(ctx.batch, "SabePalavra:" + sim(prog.isSabePalavra()), x, y);
        y -= dy;

        // porta mais próxima dentro do alcance de detecção
        GerenciadorPortas.Porta porta = portaNoAlcance(jogo);
        y -= 4f;
        if (porta != null) {
            boolean aberta = !porta.trancado || jogo.sistemaColisao.isDestrancado(porta.nome);
            ctx.fonteIndicadores.setColor(aberta ? Color.GREEN : Color.RED);
            ctx.fonteIndicadores.draw(ctx.batch, "Porta: " + porta.nome + " -> " + porta.destino, x, y);
            y -= dy;
            ctx.fonteIndicadores.setColor(Color.WHITE);
            String estado = aberta ? "ABERTA" : "TRANCADA";
            String cond   = porta.destrancavel ? "  cond:" + porta.condicao : "";
            ctx.fonteIndicadores.draw(ctx.batch, "  " + estado + cond, x, y);
            y -= dy;
        } else {
            ctx.fonteIndicadores.setColor(new Color(0.45f, 0.45f, 0.45f, 1f));
            ctx.fonteIndicadores.draw(ctx.batch, "Porta: nenhuma no alcance", x, y);
            y -= dy;
        }

        // legenda das cores das hitboxes
        y -= 4f;
        ctx.fonteIndicadores.setColor(COR_PAREDE);
        ctx.fonteIndicadores.draw(ctx.batch, "  parede", x + 60f, y);
        ctx.fonteIndicadores.setColor(COR_INTERATIVO);
        ctx.fonteIndicadores.draw(ctx.batch, "  interativo", x + 115f, y);
        ctx.fonteIndicadores.setColor(COR_NPC);
        ctx.fonteIndicadores.draw(ctx.batch, "  npc", x + 205f, y);
        ctx.fonteIndicadores.setColor(COR_PORTA);
        ctx.fonteIndicadores.draw(ctx.batch, "  porta", x + 245f, y);
        ctx.fonteIndicadores.setColor(COR_ALCANCE);
        ctx.fonteIndicadores.draw(ctx.batch, "  alcance", x + 295f, y);
        y -= dy;

        // lista de atalhos disponíveis em modo debug
        ctx.fonteIndicadores.setColor(new Color(0.55f, 0.55f, 0.55f, 1f));
        ctx.fonteIndicadores.draw(ctx.batch, "Ctrl+U umbra   Ctrl+P +parte   Ctrl+D 3partes", x, y);

        ctx.fonteIndicadores.setColor(Color.WHITE);
    }

    // verifica qual porta está dentro do alcance sem precisar de uma instância de Jogador
    private GerenciadorPortas.Porta portaNoAlcance(TelaJogo jogo) {
        Rectangle hj = jogo.hitboxJogador;
        Rectangle alcance = new Rectangle(
            hj.x - FOLGA_PORTA, hj.y - FOLGA_PORTA,
            hj.width + FOLGA_PORTA * 2f, hj.height + FOLGA_PORTA * 2f
        );
        for (GerenciadorPortas.Porta p : jogo.gerPortas.getPortas()) {
            if (!p.isAtivo(jogo.mundoUmbra)) continue;
            if (alcance.overlaps(p.area)) return p;
        }
        return null;
    }

    // formato de checkbox para exibir booleanos no painel de debug
    private static String sim(boolean v) { return v ? "[X]" : "[ ]"; }

    public void dispose() {
        shapes.dispose();
    }
}
