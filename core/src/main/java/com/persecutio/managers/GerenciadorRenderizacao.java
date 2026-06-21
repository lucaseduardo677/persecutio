package com.persecutio.managers;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.persecutio.entities.EntidadeMapa;
import com.persecutio.entities.Jogador;

// concentra as operações de desenho do mapa, NPCs, efeitos e espelho
public class GerenciadorRenderizacao {

    // fator de escala aplicado sobre o tamanho original das texturas de mapa
    private static final int ESCALA = 2;

    // textura 1x1 preta usada para preencher regiões escuras com scissor
    private final Texture texPreto;

    public GerenciadorRenderizacao() {
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.BLACK);
        pm.fill();
        texPreto = new Texture(pm);
        pm.dispose();
    }

    // desenha a imagem base do mapa escalada para o mundo
    public void desenharMapa(ContextoRender ctx, Texture imagemMapa) {
        ctx.batch.draw(imagemMapa, ctx.cameraX, ctx.cameraY,
            imagemMapa.getWidth() * ESCALA, imagemMapa.getHeight() * ESCALA);
    }

    // desenha os NPCs do mundo atual usando a área definida no Tiled
    public void desenharNpcs(ContextoRender ctx, GerenciadorColisao sistemaColisao, boolean umbra) {
        for (EntidadeMapa npc : sistemaColisao.getNpcs(umbra).values()) {
            ctx.batch.draw(npc.textura,
                Math.round(npc.area.x + ctx.cameraX),
                Math.round(npc.area.y + ctx.cameraY),
                npc.area.width, npc.area.height);
        }
    }

    // aplica sobreposição vermelha semitransparente para indicar o mundo umbra
    public void desenharUmbra(ContextoRender ctx, Texture imagemMapa) {
        ctx.batch.setColor(0.59f, 0f, 0f, 0.27f);
        ctx.batch.draw(imagemMapa, ctx.cameraX, ctx.cameraY,
            imagemMapa.getWidth() * ESCALA, imagemMapa.getHeight() * ESCALA);
        ctx.batch.setColor(Color.WHITE);
    }

    // desenha a textura de luz e sombra sobre o mapa
    public void desenharLuz(ContextoRender ctx, Texture luzMapa) {
        ctx.batch.draw(luzMapa, ctx.cameraX, ctx.cameraY,
            luzMapa.getWidth() * ESCALA, luzMapa.getHeight() * ESCALA);
    }

    // escurece todos os cômodos exceto o atual usando scissor por região
    public void desenharComodos(ContextoRender ctx, GerenciadorComodos gerComodos, Jogador jogador) {
        if (gerComodos == null) return;

        float centroHbX = jogador.hitbox.x + jogador.hitbox.width  / 2f;
        float centroHbY = jogador.hitbox.y + jogador.hitbox.height / 2f;
        GerenciadorComodos.Comodo atual = gerComodos.achar(centroHbX, centroHbY);
        if (atual == null) return;

        for (GerenciadorComodos.Comodo c : gerComodos.getComodos()) {
            if (c.nome.equals(atual.nome)) continue;
            // corredores não se escurecem entre si
            if (atual.corredor && c.corredor) continue;
            escurecer(ctx, c.area);
        }
    }

    // preenche uma região do mapa com preto usando ScissorStack
    private void escurecer(ContextoRender ctx, Rectangle area) {
        Rectangle clip = new Rectangle(
            ctx.cameraX + area.x,
            ctx.cameraY + area.y,
            area.width,
            area.height
        );

        Rectangle scissors = new Rectangle();
        ctx.batch.flush();
        ctx.viewport.calculateScissors(ctx.batch.getTransformMatrix(), clip, scissors);

        if (ScissorStack.pushScissors(scissors)) {
            ctx.batch.setColor(0f, 0f, 0f, 1f);
            ctx.batch.draw(texPreto, clip.x, clip.y, clip.width, clip.height);
            ctx.batch.setColor(Color.WHITE);
            ctx.batch.flush();
            ScissorStack.popScissors();
        }
    }

    // desenha o reflexo do jogador dentro da área do espelho com scissor
    public void desenharEspelho(ContextoRender ctx, GerenciadorColisao sistemaColisao,
                                Jogador jogador, Texture spriteSheet) {
        Rectangle espelho = sistemaColisao.getArea("espelho", true);
        if (espelho == null) return;

        // reflexo se afasta conforme o jogador se distancia do espelho
        float dist = Math.max(0f, espelho.y - jogador.mundoY);
        float espelhoTelaY = ctx.cameraY + espelho.y;
        float reflexoTelaY = (espelhoTelaY + espelho.height) - (dist / 10f);

        // inverte a direção vertical do reflexo
        int dReflexo = jogador.getDirecao();
        if      (jogador.getDirecao() == 3) dReflexo = 0;
        else if (jogador.getDirecao() == 0) dReflexo = 3;
        else if (jogador.getDirecao() == 1) dReflexo = 2;
        else if (jogador.getDirecao() == 2) dReflexo = 1;

        TextureRegion reflexo = new TextureRegion(spriteSheet,
            jogador.getFrame() * jogador.getTamanho(),
            dReflexo           * jogador.getTamanho(),
            jogador.getTamanho(), jogador.getTamanho());

        // limita o desenho do reflexo à área do espelho
        Rectangle clipBounds = new Rectangle(
            ctx.cameraX + espelho.x, ctx.cameraY + espelho.y,
            espelho.width, espelho.height);
        Rectangle scissors = new Rectangle();

        ctx.batch.flush();
        ctx.viewport.calculateScissors(ctx.batch.getTransformMatrix(), clipBounds, scissors);

        if (ScissorStack.pushScissors(scissors)) {
            ctx.batch.setColor(1f, 1f, 1f, 0.45f);
            // largura negativa espelha o sprite horizontalmente
            ctx.batch.draw(reflexo,
                Math.round(ctx.centroX + 28),
                Math.round(reflexoTelaY - 56),
                -56, 56);
            ctx.batch.setColor(Color.WHITE);
            ctx.batch.flush();
            ScissorStack.popScissors();
        }
    }

    public void dispose() { texPreto.dispose(); }
}
