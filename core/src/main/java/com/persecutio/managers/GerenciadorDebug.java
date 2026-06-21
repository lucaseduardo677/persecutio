package com.persecutio.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Rectangle;
import com.persecutio.screens.TelaJogo;

// Ferramentas depuracao jogo
public class GerenciadorDebug {

    private final ShapeRenderer shapes;

    private static final Color COR_PAREDE     = new Color(0.2f, 0.85f, 0.2f, 1f);
    private static final Color COR_INTERATIVO = new Color(0f,   0.9f,  0.9f, 1f);
    private static final Color COR_NPC        = new Color(0.8f, 0f,    0.9f, 1f);
    private static final Color COR_PORTA      = new Color(1f,   0.55f, 0f,   1f);
    private static final Color COR_ALCANCE    = new Color(1f,   1f,    0f,   1f);
    private static final Color COR_JOGADOR    = new Color(1f,   0.15f, 0.15f,1f);

    private static final float FOLGA_PORTA = 24f;

    private final Rectangle rectAlcance = new Rectangle();

    // Criação da ferramentas de depuração
    public GerenciadorDebug() {
        shapes = new ShapeRenderer();
    }

// Atalhos depuracao
    public void tratarAtalhos(GerenciadorProgresso progresso) {
        boolean ctrl = Gdx.input.isKeyPressed(Keys.CONTROL_LEFT)
                    || Gdx.input.isKeyPressed(Keys.CONTROL_RIGHT);
        if (!ctrl) return;

        if (Gdx.input.isKeyJustPressed(Keys.U)) progresso.alternarUmbra();

        if (Gdx.input.isKeyJustPressed(Keys.P)) {
            progresso.adicionarParte();
            progresso.setAviso("[DEBUG] Partes: " + progresso.getPartes() + "/3");
        }

        if (Gdx.input.isKeyJustPressed(Keys.D)) {
            progresso.forcarPartes(3);
            progresso.setAviso("[DEBUG] Forcado 3/3 partes");
        }
    }

// Hitboxes tela
    public void desenharHitboxes(TelaJogo jogo, float cameraX, float cameraY) {
        shapes.begin(ShapeType.Line);

        shapes.setColor(COR_PAREDE);
        for (Rectangle r : jogo.sistemaColisao.getParedes(jogo.mundoUmbra))
            shapes.rect(r.x + cameraX, r.y + cameraY, r.width, r.height);

        shapes.setColor(COR_INTERATIVO);
        for (Rectangle r : jogo.sistemaColisao.getInterativos(jogo.mundoUmbra).values())
            shapes.rect(r.x + cameraX, r.y + cameraY, r.width, r.height);

        shapes.setColor(COR_NPC);
        for (com.persecutio.entities.EntidadeMapa npc : jogo.sistemaColisao.getNpcs(jogo.mundoUmbra).values())
            shapes.rect(npc.area.x + cameraX, npc.area.y + cameraY, npc.area.width, npc.area.height);

        shapes.setColor(COR_PORTA);
        for (GerenciadorColisao.ObjetoColisao p : jogo.sistemaColisao.getHitboxPortasCompletas())
            shapes.rect(p.area.x + cameraX, p.area.y + cameraY, p.area.width, p.area.height);

        shapes.setColor(COR_ALCANCE);
        Rectangle hj = jogo.hitboxJogador;
        shapes.rect(hj.x - FOLGA_PORTA + cameraX, hj.y - FOLGA_PORTA + cameraY,
                    hj.width + FOLGA_PORTA * 2f, hj.height + FOLGA_PORTA * 2f);

        shapes.setColor(COR_JOGADOR);
        shapes.rect(hj.x + cameraX, hj.y + cameraY, hj.width, hj.height);

        shapes.end();
    }

// Informacoes depuracao
    public void desenharInfo(TelaJogo jogo, ContextoRender ctx) {
        float x  = 8f;
        float y  = ctx.vAltura - 10f;
        float dy = 18f;

        ctx.fonteIndicadores.setColor(Color.YELLOW);
        ctx.fonteIndicadores.draw(ctx.batch, "== DEBUG  Ctrl+H ==", x, y); y -= dy;

        ctx.fonteIndicadores.setColor(Color.WHITE);
        ctx.fonteIndicadores.draw(ctx.batch, "FPS: " + Gdx.graphics.getFramesPerSecond(), x, y); y -= dy;

        Rectangle hj = jogo.hitboxJogador;
        ctx.fonteIndicadores.draw(ctx.batch, "Pos: " + (int)hj.x + ", " + (int)hj.y, x, y); y -= dy;

        ctx.fonteIndicadores.setColor(jogo.mundoUmbra ? Color.MAGENTA : Color.CYAN);
        ctx.fonteIndicadores.draw(ctx.batch, "Mundo: " + (jogo.mundoUmbra ? "UMBRA" : "REAL"), x, y); y -= dy;

        GerenciadorProgresso prog = jogo.progresso;
        ctx.fonteIndicadores.setColor(Color.WHITE);
        ctx.fonteIndicadores.draw(ctx.batch,
            "Partes: " + prog.getPartes() + "/3   Missao: " + prog.getMissao(), x, y); y -= dy;

        GerenciadorPortas.Porta porta = portaNoAlcance(jogo);
        y -= 4f;
        if (porta != null) {
            boolean aberta = !porta.trancado || jogo.sistemaColisao.isDestrancado(porta.nome);
            ctx.fonteIndicadores.setColor(aberta ? Color.GREEN : Color.RED);
            String cond = porta.destrancavel ? " [" + porta.condicao + "]" : "";
            ctx.fonteIndicadores.draw(ctx.batch,
                porta.nome + " -> " + porta.destino + "  " + (aberta ? "ABERTA" : "TRANCADA") + cond,
                x, y); y -= dy;
        } else {
            ctx.fonteIndicadores.setColor(new Color(0.45f, 0.45f, 0.45f, 1f));
            ctx.fonteIndicadores.draw(ctx.batch, "Porta: nenhuma", x, y); y -= dy;
        }

        ctx.fonteIndicadores.setColor(new Color(0.55f, 0.55f, 0.55f, 1f));
        ctx.fonteIndicadores.draw(ctx.batch, "Ctrl+U umbra   Ctrl+P +parte   Ctrl+D 3partes", x, y);

        ctx.fonteIndicadores.setColor(Color.WHITE);
    }

// Procura porta mais proxima
    private GerenciadorPortas.Porta portaNoAlcance(TelaJogo jogo) {
        Rectangle hj = jogo.hitboxJogador;
        rectAlcance.set(
            hj.x - FOLGA_PORTA, hj.y - FOLGA_PORTA,
            hj.width + FOLGA_PORTA * 2f, hj.height + FOLGA_PORTA * 2f
        );
        for (GerenciadorPortas.Porta p : jogo.gerPortas.getPortas()) {
            if (!p.isAtivo(jogo.mundoUmbra)) continue;
            if (rectAlcance.overlaps(p.area)) return p;
        }
        return null;
    }

    // Liberação dos recursos
    public void dispose() { shapes.dispose(); }
}
