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

// renderizacao do jogo com cull de comodos por tile (filtro 50% de area)
public class GerenciadorRenderizacao {

    private final Texture texPreto;

    // criacao do renderizador
    public GerenciadorRenderizacao(float escala) {
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.BLACK);
        pm.fill();
        texPreto = new Texture(pm);
        pm.dispose();
    }

    // desenha npcs visiveis no mundo atual
    public void desenharNpcs(ContextoRender ctx, GerenciadorColisao sistemaColisao, boolean umbra) {
        for (EntidadeMapa npc : sistemaColisao.getNpcs(umbra).values()) {
            ctx.batch.draw(npc.textura,
                Math.round(npc.area.x + ctx.cameraX),
                Math.round(npc.area.y + ctx.cameraY),
                npc.area.width, npc.area.height);
        }
    }

    // desenha reflexo espelhado do jogador
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

    // renderiza o mapa tiled com cull por comodo via filtro de tiles 50%
    public void renderizarMapa(ContextoRender ctx,
                               OrthogonalTiledMapRenderer rendererTiled,
                               GerenciadorComodos gerComodos,
                               GerenciadorComodos.Comodo comodoJogador,
                               boolean umbra) {

        SpriteBatch batch = ctx.batch;
        List<GerenciadorComodos.Comodo> cullAtivo = gerComodos.getCullAtivo(comodoJogador);

        // salva estado original do batch
        Matrix4 projOriginal   = batch.getProjectionMatrix().cpy();
        Matrix4 matrizOriginal = batch.getTransformMatrix().cpy();

        batch.setProjectionMatrix(ctx.camera.combined);
        batch.getTransformMatrix().translate(ctx.cameraX, ctx.cameraY, 0);
        batch.setTransformMatrix(batch.getTransformMatrix());

        if (cullAtivo.isEmpty()) {
            // jogador fora de qualquer comodo: renderiza tudo sem corte
            renderCamadas(rendererTiled, ctx, umbra);
        } else {
            // renderiza apenas tiles que passam no filtro 50% dos comodos ativos
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

        // restaura estado original do batch
        batch.setProjectionMatrix(projOriginal);
        batch.setTransformMatrix(matrizOriginal);
    }

    // renderiza uma camada de tiles aplicando o filtro de 50% de area dentro dos comodos ativos
    private void renderTileLayerComCull(SpriteBatch batch, TiledMapTileLayer layer,
                                        List<GerenciadorComodos.Comodo> cullAtivo, float escala) {
        final float tileWidth  = layer.getTileWidth()  * escala;
        final float tileHeight = layer.getTileHeight() * escala;
        final int layerWidth   = layer.getWidth();
        final int layerHeight  = layer.getHeight();

        // calcula bounds em tiles dos comodos ativos para otimizar iteracao
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

    // renderiza as camadas do mapa (normal e tint de umbra se ativo)
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

    // liberacao dos recursos
    public void dispose() { texPreto.dispose(); }
}
