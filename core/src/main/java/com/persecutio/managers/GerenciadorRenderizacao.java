package com.persecutio.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.persecutio.entities.EntidadeMapa;
import com.persecutio.entities.Jogador;

import java.util.List;

// Renderizacao do jogo com cull de comodos por tile
public class GerenciadorRenderizacao {

    // Textura preta para tint de Umbra
    private final Texture texPreto;

    // Construtor do renderizador
    public GerenciadorRenderizacao(float escala) {
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.BLACK);
        pm.fill();
        texPreto = new Texture(pm);
        pm.dispose();
    }

    // Desenha NPCs visiveis aplicando o mesmo culling dos comodos ativos
    public void desenharNpcs(ContextoRender ctx, GerenciadorColisao sistemaColisao,
                             GerenciadorComodos gerComodos, GerenciadorComodos.Comodo comodoAtual, boolean umbra) {

        List<GerenciadorComodos.Comodo> cullAtivo = gerComodos.getCullAtivo(comodoAtual);

        for (EntidadeMapa npc : sistemaColisao.getNpcs(umbra).values()) {
            boolean desenhar = false;

            if (cullAtivo.isEmpty()) {
                // Se o jogador nao estiver em nenhum comodo, desenha todos por seguranca
                desenhar = true;
            } else {
                // Encontra qual comodo este NPC ocupa com base no centro de sua hitbox
                float cx = npc.area.x + npc.area.width / 2f;
                float cy = npc.area.y + npc.area.height / 2f;
                GerenciadorComodos.Comodo comodoNpc = gerComodos.achar(cx, cy);

                if (comodoNpc != null) {
                    // Desenha o NPC apenas se o comodo dele estiver na lista de comodos ativos
                    for (GerenciadorComodos.Comodo c : cullAtivo) {
                        if (c == comodoNpc) {
                            desenhar = true;
                            break;
                        }
                    }
                }
            }

            if (desenhar) {
                ctx.batch.draw(npc.textura,
                    Math.round(npc.area.x + ctx.cameraX),
                    Math.round(npc.area.y + ctx.cameraY),
                    npc.area.width, npc.area.height);
            }
        }
    }

    // Desenha reflexo espelhado do jogador
    public void desenharCloneEspelho(ContextoRender ctx, Jogador jogador, Texture spriteSheet,
                                     Rectangle areaReflexo) {
        if (areaReflexo == null || spriteSheet == null || jogador == null) return;

        float centroReflexoX = areaReflexo.x + areaReflexo.width / 2f;
        float cloneMundoX    = 2f * centroReflexoX - jogador.mundoX;
        float cloneMundoY    = jogador.mundoY;

        float telaX = ctx.mundoParaTelaX(cloneMundoX);
        float telaY = ctx.mundoParaTelaY(cloneMundoY);

        int frame    = jogador.getFrame();
        int dir      = jogador.getDirecao();
        int tam      = jogador.getTamanho();

        int dirClone = dir;
        if      (dir == Jogador.DIRECAO_DIREITA)  dirClone = Jogador.DIRECAO_ESQUERDA;
        else if (dir == Jogador.DIRECAO_ESQUERDA) dirClone = Jogador.DIRECAO_DIREITA;

        TextureRegion region = new TextureRegion(spriteSheet, frame * tam, dirClone * tam, tam, tam);
        region.flip(true, false);

        ctx.batch.draw(region, Math.round(telaX) - 28, Math.round(telaY) - 28, 56, 56);
    }

    // Renderiza o mapa Tiled com cull por comodo
    public void renderizarMapa(ContextoRender ctx,
                               OrthogonalTiledMapRenderer rendererTiled,
                               GerenciadorComodos gerComodos,
                               GerenciadorComodos.Comodo comodoJogador,
                               boolean umbra) {

        SpriteBatch batch = ctx.batch;
        List<GerenciadorComodos.Comodo> cullAtivo = gerComodos.getCullAtivo(comodoJogador);

        // Salva estado original do batch
        Matrix4 projOriginal   = batch.getProjectionMatrix().cpy();
        Matrix4 matrizOriginal = batch.getTransformMatrix().cpy();

        batch.setProjectionMatrix(ctx.camera.combined);
        batch.getTransformMatrix().translate(ctx.cameraX, ctx.cameraY, 0);
        batch.setTransformMatrix(batch.getTransformMatrix());

        if (cullAtivo.isEmpty()) {
            // Jogador fora de qualquer comodo renderiza tudo
            renderCamadas(rendererTiled, ctx, umbra);
        } else {
            // Renderiza apenas tiles que passam no filtro
            TiledMap mapa = rendererTiled.getMap();
            float escala = CoordenadasTiled.getEscala();

            batch.begin();

            for (MapLayer layer : mapa.getLayers()) {
                if (layer instanceof TiledMapTileLayer) {
                    renderTileLayerComCull(batch, (TiledMapTileLayer) layer, cullAtivo, escala);
                }
            }

            if (umbra) {
                batch.setColor(0.59f, 0f, 0f, 0.27f);
                for (MapLayer layer : mapa.getLayers()) {
                    if (layer instanceof TiledMapTileLayer) {
                        renderTileLayerComCull(batch, (TiledMapTileLayer) layer, cullAtivo, escala);
                    }
                }
                batch.setColor(Color.WHITE);
            }

            batch.end();
        }

        // Restaura estado original do batch
        batch.setProjectionMatrix(projOriginal);
        batch.setTransformMatrix(matrizOriginal);
    }

    // Renderiza camada de tiles aplicando filtro de area
    private void renderTileLayerComCull(SpriteBatch batch, TiledMapTileLayer layer,
                                        List<GerenciadorComodos.Comodo> cullAtivo, float escala) {
        final float tileWidth  = layer.getTileWidth()  * escala;
        final float tileHeight = layer.getTileHeight() * escala;
        final int layerWidth   = layer.getWidth();
        final int layerHeight  = layer.getHeight();

        // Calcula bounds em tiles dos comodos ativos
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE, maxY = Float.MIN_VALUE;
        for (GerenciadorComodos.Comodo c : cullAtivo) {
            minX = Math.min(minX, c.area.x);
            minY = Math.min(minY, c.area.y);
            maxX = Math.max(maxX, c.area.x + c.area.width);
            maxY = Math.max(maxY, c.area.y + c.area.height);
        }

        int startX = Math.max(0, (int) (minX / tileWidth));
        int startY = Math.max(0, (int) (minY / tileHeight));
        int endX   = Math.min(layerWidth,  (int) Math.ceil(maxX / tileWidth));
        int endY   = Math.min(layerHeight, (int) Math.ceil(maxY / tileHeight));

        for (int y = startY; y < endY; y++) {
            for (int x = startX; x < endX; x++) {
                TiledMapTileLayer.Cell cell = layer.getCell(x, y);
                if (cell == null) continue;

                float tileX = x * tileWidth;
                float tileY = y * tileHeight;
                Rectangle tileRect = new Rectangle(tileX, tileY, tileWidth, tileHeight);

                boolean passa = false;
                for (GerenciadorComodos.Comodo c : cullAtivo) {
                    if (GerenciadorComodos.passaFiltro(tileRect, c.area)) {
                        passa = true;
                        break;
                    }
                }
                if (!passa) continue;

                TiledMapTile tile = cell.getTile();
                if (tile == null) continue;
                TextureRegion region = tile.getTextureRegion();
                if (region == null) continue;

                batch.draw(region, tileX, tileY, tileWidth, tileHeight);
            }
        }
    }

    // Renderiza camadas do mapa normal e tint de Umbra
    private void renderCamadas(OrthogonalTiledMapRenderer rendererTiled,
                               ContextoRender ctx, boolean umbra) {
        rendererTiled.getBatch().setColor(Color.WHITE);
        rendererTiled.setView(ctx.camera.combined,
            -ctx.cameraX, -ctx.cameraY,
            ctx.vLargura, ctx.vAltura);
        rendererTiled.render();

        if (umbra) {
            rendererTiled.getBatch().setColor(0.59f, 0f, 0f, 0.27f);
            rendererTiled.setView(ctx.camera.combined,
                -ctx.cameraX, -ctx.cameraY,
                ctx.vLargura, ctx.vAltura);
            rendererTiled.render();
            rendererTiled.getBatch().setColor(Color.WHITE);
        }
    }

    // Libera recursos do renderizador
    public void dispose() { texPreto.dispose(); }
}
