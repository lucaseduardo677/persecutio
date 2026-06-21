package com.persecutio.managers;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.persecutio.entities.EntidadeMapa;
import com.persecutio.entities.Jogador;

import java.util.List;

// Renderização do jogo
public class GerenciadorRenderizacao {

    private static final int ESCALA = 2;
    private final Texture texPreto;
    private final Rectangle rectTemp = new Rectangle();

    // Criação da renderização do jogo
    public GerenciadorRenderizacao() {
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.BLACK);
        pm.fill();
        texPreto = new Texture(pm);
        pm.dispose();
    }

    // Desenho dos elementos
    public void desenharMapa(ContextoRender ctx, Texture imagemMapa) {
        ctx.batch.draw(imagemMapa, ctx.cameraX, ctx.cameraY,
            imagemMapa.getWidth() * ESCALA, imagemMapa.getHeight() * ESCALA);
    }

    // Desenho dos elementos
    public void desenharNpcs(ContextoRender ctx, GerenciadorColisao sistemaColisao, boolean umbra) {
        for (EntidadeMapa npc : sistemaColisao.getNpcs(umbra).values()) {
            ctx.batch.draw(npc.textura,
                Math.round(npc.area.x + ctx.cameraX),
                Math.round(npc.area.y + ctx.cameraY),
                npc.area.width, npc.area.height);
        }
    }

    // Desenho dos elementos
    public void desenharUmbra(ContextoRender ctx, Texture imagemMapa) {
        ctx.batch.setColor(0.59f, 0f, 0f, 0.27f);
        ctx.batch.draw(imagemMapa, ctx.cameraX, ctx.cameraY,
            imagemMapa.getWidth() * ESCALA, imagemMapa.getHeight() * ESCALA);
        ctx.batch.setColor(Color.WHITE);
    }

    // Desenho dos elementos
    public void desenharLuz(ContextoRender ctx, Texture luzMapa) {
        ctx.batch.draw(luzMapa, ctx.cameraX, ctx.cameraY,
            luzMapa.getWidth() * ESCALA, luzMapa.getHeight() * ESCALA);
    }

    // Desenho dos elementos
    public void desenharComodos(ContextoRender ctx, GerenciadorComodos gerComodos, Jogador jogador) {
        if (gerComodos == null) return;

        float centroHbX = jogador.hitbox.x + jogador.hitbox.width  / 2f;
        float centroHbY = jogador.hitbox.y + jogador.hitbox.height / 2f;
        GerenciadorComodos.Comodo atual = gerComodos.achar(centroHbX, centroHbY);
        if (atual == null) return;

        List<GerenciadorComodos.Comodo> grupoAtual = gerComodos.getComodosDoMesmoGrupo(atual);

        ctx.batch.setColor(0f, 0f, 0f, 1f);
        for (GerenciadorComodos.Comodo c : gerComodos.getComodos()) {
            if (c == atual) continue;
            if (grupoAtual.contains(c)) continue;
            ctx.batch.draw(texPreto,
                ctx.cameraX + c.area.x,
                ctx.cameraY + c.area.y,
                c.area.width,
                c.area.height);
        }
        ctx.batch.setColor(Color.WHITE);
    }

    // Reflexo jogador
    public void desenharCloneEspelho(ContextoRender ctx, Jogador jogador, Texture spriteSheet,
                                     Rectangle areaReflexo) {
        if (areaReflexo == null || spriteSheet == null || jogador == null) return;

        // Centro espelho eixo X
        float centroReflexoX = areaReflexo.x + areaReflexo.width / 2f;

        // Posicao espelhada
        float cloneMundoX = 2f * centroReflexoX - jogador.mundoX;
        float cloneMundoY = jogador.mundoY;

        // Coordenadas tela
        float telaX = ctx.mundoParaTelaX(cloneMundoX);
        float telaY = ctx.mundoParaTelaY(cloneMundoY);

        int frame = jogador.getFrame();
        int dir   = jogador.getDirecao();
        int tam   = jogador.getTamanho();

        // Inverte direcao horizontal
        int dirClone = dir;
        if (dir == Jogador.DIRECAO_DIREITA) {
            dirClone = Jogador.DIRECAO_ESQUERDA;
        } else if (dir == Jogador.DIRECAO_ESQUERDA) {
            dirClone = Jogador.DIRECAO_DIREITA;
        }

        // Regiao temporaria frame correto
        TextureRegion region = new TextureRegion(spriteSheet, frame * tam, dirClone * tam, tam, tam);

        // Espelha horizontalmente
        region.flip(true, false);

        // Clone espelhado jogador
        ctx.batch.draw(region, Math.round(telaX) - 28, Math.round(telaY) - 28, 56, 56);
    }

    // Liberação dos recursos
    public void dispose() { texPreto.dispose(); }
}
