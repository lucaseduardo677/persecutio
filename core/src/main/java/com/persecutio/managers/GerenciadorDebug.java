package com.persecutio.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.persecutio.screens.TelaJogo;

// Ferramentas de debug do jogo
public class GerenciadorDebug {

    private final ShapeRenderer shapes;

    // Cor das paredes no debug
    private static final Color COR_PAREDE     = Color.valueOf("#33d833");
    // Cor dos interativos no debug
    private static final Color COR_INTERATIVO = Color.valueOf("#00e5e5");
    // Cor dos NPCs no debug
    private static final Color COR_NPC        = Color.valueOf("#cc00e5");
    // Cor dos objetos de colisao no debug
    private static final Color COR_OBJETO     = Color.valueOf("#3366ff");
    // Cor das portas no debug
    private static final Color COR_PORTA      = Color.valueOf("#ff8c00");
    // Cor do alcance de interacao no debug
    private static final Color COR_ALCANCE    = Color.valueOf("#ffff00");
    // Cor do jogador no debug
    private static final Color COR_JOGADOR    = Color.valueOf("#ff2626");

    // Novas cores para o puzzle da Missão 2
    private static final Color COR_PEDRA      = Color.ORANGE;
    private static final Color COR_OBJETIVO   = Color.PINK;

    // Folga do alcance de interacao
    private static final float FOLGA_PORTA = 24f;

    // Retangulo temporario para alcance
    private final Rectangle rectAlcance = new Rectangle();

    // Construtor das ferramentas de debug
    public GerenciadorDebug() {
        shapes = new ShapeRenderer();
    }

    // Processa atalhos de debug usando apenas o pool de flags
    public void tratarAtalhos(TelaJogo jogo) {
        boolean ctrl = Gdx.input.isKeyPressed(Keys.CONTROL_LEFT)
                    || Gdx.input.isKeyPressed(Keys.CONTROL_RIGHT);
        if (!ctrl) return;

        GerenciadorProgresso progresso = jogo.progresso;

        if (Gdx.input.isKeyJustPressed(Keys.U)) progresso.alternarUmbra();

        // Removed debug shortcuts that directly set progression flags to avoid
        // accidental game-state manipulation during development.

        if (Gdx.input.isKeyJustPressed(Keys.I)) {
            jogo.sistemaColisao.alternarColisoes();
        }
    }

    // Desenha hitboxes na tela, incluindo as pedras e objetivos da Missao 2
    public void desenharHitboxes(TelaJogo jogo, float cameraX, float cameraY) {
        shapes.begin(ShapeType.Line);

        shapes.setColor(COR_PAREDE);
        for (Rectangle r : jogo.sistemaColisao.getParedes())
            shapes.rect(r.x + cameraX, r.y + cameraY, r.width, r.height);

        shapes.setColor(COR_INTERATIVO);
        // Group interativos by base key and merge only overlapping rectangles into clusters
        java.util.Map<String, java.util.List<Rectangle>> clustersByBase = new java.util.HashMap<>();
        for (java.util.Map.Entry<String, Rectangle> e : jogo.sistemaColisao.getInterativos().entrySet()) {
            String key = e.getKey();
            Rectangle r = e.getValue();
            if (key == null || r == null) continue;
            String base = key.split("_")[0];
            java.util.List<Rectangle> clusters = clustersByBase.computeIfAbsent(base, k -> new java.util.ArrayList<>());

            boolean added = false;
            for (Rectangle c : clusters) {
                if (c.overlaps(r)) {
                    float minX = Math.min(c.x, r.x);
                    float minY = Math.min(c.y, r.y);
                    float maxX = Math.max(c.x + c.width, r.x + r.width);
                    float maxY = Math.max(c.y + c.height, r.y + r.height);
                    c.set(minX, minY, maxX - minX, maxY - minY);
                    added = true;
                    break;
                }
            }
            if (!added) {
                clusters.add(new Rectangle(r));
            }
        }
        for (java.util.List<Rectangle> clusters : clustersByBase.values()) {
            for (Rectangle c : clusters) shapes.rect(c.x + cameraX, c.y + cameraY, c.width, c.height);
        }

        shapes.setColor(COR_NPC);
        for (com.persecutio.entities.EntidadeMapa npc : jogo.sistemaColisao.getNpcs().values())
            shapes.rect(npc.area.x + cameraX, npc.area.y + cameraY, npc.area.width, npc.area.height);

        shapes.setColor(COR_PORTA);
        for (GerenciadorPortas.Porta p : jogo.gerPortas.getPortas())
            shapes.rect(p.area.x + cameraX, p.area.y + cameraY, p.area.width, p.area.height);

        shapes.setColor(COR_OBJETO);
        for (GerenciadorColisao.ObjetoColisao obj : jogo.sistemaColisao.obterObjetos()) {
            if (jogo.sistemaColisao.checarAtivo(obj))
                shapes.rect(obj.area.x + cameraX, obj.area.y + cameraY, obj.area.width, obj.area.height);
        }

        // Desenha as pedras empurráveis da Missão 2 no debug
        shapes.setColor(COR_PEDRA);
        for (GerenciadorColisao.ObjetoColisao pedra : jogo.sistemaColisao.mapaPedras().values()) {
            shapes.rect(pedra.area.x + cameraX, pedra.area.y + cameraY, pedra.area.width, pedra.area.height);
        }

        // Desenha as marcas de objetivos das pedras no debug
        shapes.setColor(COR_OBJETIVO);
        for (Rectangle obj : jogo.sistemaColisao.mapObjetivos().values()) {
            shapes.rect(obj.x + cameraX, obj.y + cameraY, obj.width, obj.height);
        }

        shapes.setColor(COR_ALCANCE);
        Rectangle hj = jogo.hitboxJogador;
        shapes.rect(hj.x - FOLGA_PORTA + cameraX, hj.y - FOLGA_PORTA + cameraY,
                    hj.width + FOLGA_PORTA * 2f, hj.height + FOLGA_PORTA * 2f);

        shapes.setColor(COR_JOGADOR);
        shapes.rect(hj.x + cameraX, hj.y + cameraY, hj.width, hj.height);

        shapes.end();
    }

    // Desenha informacoes de debug na tela
    public void desenharInfo(TelaJogo jogo, ContextoRender ctx) {
        float x  = 8f;
        float y  = ctx.vAltura - 10f;
        float dy = 18f;

        ctx.fonteIndicadores.setColor(Color.YELLOW);
        ctx.fonteIndicadores.draw(ctx.batch, "== DEBUG  Ctrl+H ==", x, y); y -= dy;

        ctx.fonteIndicadores.setColor(Color.WHITE);
        ctx.fonteIndicadores.draw(ctx.batch, "FPS: " + Gdx.graphics.getFramesPerSecond(), x, y); y -= dy;

        ctx.fonteIndicadores.draw(ctx.batch, "Pos: " + (int)jogo.jogador.mundoX + ", " + (int)jogo.jogador.mundoY, x, y); y -= dy;

        ctx.fonteIndicadores.setColor(jogo.mundoUmbra ? Color.MAGENTA : Color.CYAN);
        ctx.fonteIndicadores.draw(ctx.batch, "Mundo: " + (jogo.mundoUmbra ? "UMBRA" : "REAL"), x, y); y -= dy;

        GerenciadorProgresso prog = jogo.progresso;
        ctx.fonteIndicadores.setColor(Color.WHITE);

        String txtFlags = String.join(", ", prog.obterFlags());
        ctx.fonteIndicadores.draw(ctx.batch,
            "Ativas: [" + (txtFlags.isEmpty() ? "nenhuma" : txtFlags) + "]", x, y); y -= dy;

        ctx.fonteIndicadores.draw(ctx.batch,
            "Missao: " + prog.getMissao() + " | Fase: " + prog.obterFase(), x, y); y -= dy;

        GerenciadorPortas.Porta porta = acharPorta(jogo);
        y -= 4f;
        if (porta != null) {
            boolean aberta = !porta.trancado || jogo.sistemaColisao.isDestrancado(porta.nome);
            ctx.fonteIndicadores.setColor(aberta ? Color.GREEN : Color.RED);
            String cond = porta.destrancavel ? " [" + porta.condicao + "]" : "";
            ctx.fonteIndicadores.draw(ctx.batch,
            porta.nome + " -> " + porta.label + "  " + (aberta ? "ABERTA" : "TRANCADA") + cond,
            x, y); y -= dy;
        } else {
            ctx.fonteIndicadores.setColor(Color.valueOf("#737373"));
            ctx.fonteIndicadores.draw(ctx.batch, "Porta: nenhuma", x, y); y -= dy;
        }

        ctx.fonteIndicadores.setColor(Color.valueOf("#8c8c8c"));
        ctx.fonteIndicadores.draw(ctx.batch, "Ctrl+U umbra  Ctrl+P alternar  Ctrl+D forcar  Ctrl+I noclip", x, y);

        ctx.fonteIndicadores.setColor(Color.WHITE);
    }

    // Procura porta mais proxima do jogador
    private GerenciadorPortas.Porta acharPorta(TelaJogo jogo) {
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

    // Libera recursos do debug
    public void dispose() { shapes.dispose(); }
}
