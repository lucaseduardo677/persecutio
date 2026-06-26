package com.persecutio.managers;

import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// gerencia comodos definidos no tiled e controla cull de tiles
public class GerenciadorComodos {

    private final List<Comodo> comodos = new ArrayList<>();
    // agrupa comodos pelo nome base sem sufixo numerico
    private final Map<String, List<Comodo>> comodosPorNome = new HashMap<>();
    // agrupa comodos pelo nome EXATO (incluindo numeros)
    private final Map<String, List<Comodo>> comodosPorNomeExato = new HashMap<>();

    // lista reutilizavel para evitar alocacao por frame
    private final List<Comodo> cacheCull = new ArrayList<>();

    // criacao do gerenciador de comodos
    public GerenciadorComodos(TiledMap mapa, float escala) {
        CoordenadasTiled.setEscala(escala);

        MapLayer camada = mapa.getLayers().get("Comodos");
        if (camada == null) return;

        for (MapObject obj : camada.getObjects()) {
            if (!(obj instanceof RectangleMapObject)) continue;

            Rectangle r = ((RectangleMapObject) obj).getRectangle();

            // camera estatica faz viewport fixar no centro do comodo
            boolean cameraEstatica = Boolean.TRUE.equals(obj.getProperties().get("cameraEstatica", Boolean.class));

            String nome = obj.getName();
            if (nome == null || nome.isEmpty()) nome = "";
            // sufixo numerico removido para agrupar variantes do mesmo comodo
            String nomeGrupo = normalizarNome(nome);

            Comodo c = new Comodo(CoordenadasTiled.paraMundo(r), cameraEstatica, nome, nomeGrupo);
            comodos.add(c);
            comodosPorNome.computeIfAbsent(nomeGrupo, k -> new ArrayList<>()).add(c);
            comodosPorNomeExato.computeIfAbsent(nome.toLowerCase().trim(), k -> new ArrayList<>()).add(c);
        }
    }

    // remove sufixo numerico do nome para agrupar comodos
    private static String normalizarNome(String nome) {
        if (nome == null || nome.isEmpty()) return "";
        return nome.replaceAll("\\d+$", "").toLowerCase().trim();
    }

    // retorna o comodo que contem o ponto dado
    public Comodo achar(float px, float py) {
        for (Comodo c : comodos) {
            if (c.area.contains(px, py)) return c;
        }
        return null;
    }

    // retorna comodos do mesmo grupo base que o atual
    public List<Comodo> getComodosDoMesmoGrupo(Comodo atual) {
        if (atual == null || atual.nomeGrupo.isEmpty()) return new ArrayList<>();
        List<Comodo> grupo = comodosPorNome.get(atual.nomeGrupo);
        return grupo != null ? new ArrayList<>(grupo) : new ArrayList<>();
    }

    // retorna os comodos que devem receber cull de tiles neste frame
    // regra: mesmo nome EXATO que o atual -> renderiza todos com aquele nome
    //        nomes diferentes -> renderiza apenas o comodo onde o player esta
    public List<Comodo> getCullAtivo(Comodo comodoJogador) {
        cacheCull.clear();

        if (comodoJogador == null) return cacheCull;

        String nome = comodoJogador.nome;
        if (nome != null && !nome.trim().isEmpty()) {
            List<Comodo> mesmoNome = comodosPorNomeExato.get(nome.toLowerCase().trim());
            if (mesmoNome != null && mesmoNome.size() > 1) {
                // mesmo nome exato em multiplos comodos: cull nao ocorre entre eles
                cacheCull.addAll(mesmoNome);
                return cacheCull;
            }
        }

        // nome unico, vazio, ou diferente dos demais: renderiza apenas o atual
        cacheCull.add(comodoJogador);
        return cacheCull;
    }

    // verifica se um tile com posicao e tamanho dados passa no filtro de 50%
    // o tile precisa ter pelo menos 50% de sua area dentro do retangulo de cull
    public static boolean passaFiltro(Rectangle tileRect, Rectangle cullRect) {
        // intersecao dos dois retangulos
        float ix = Math.max(tileRect.x, cullRect.x);
        float iy = Math.max(tileRect.y, cullRect.y);
        float iw = Math.min(tileRect.x + tileRect.width,  cullRect.x + cullRect.width)  - ix;
        float ih = Math.min(tileRect.y + tileRect.height, cullRect.y + cullRect.height) - iy;

        if (iw <= 0 || ih <= 0) return false;

        float areaTile = tileRect.width * tileRect.height;
        if (areaTile <= 0) return false;

        float areaIntersecao = iw * ih;
        // pelo menos 50% do tile deve estar dentro do comodo
        return (areaIntersecao / areaTile) >= 0.5f;
    }

    // calcula ponto de spawn dentro do destino baseado na borda mais proxima da porta
    public Vector2 spawnEntrada(Comodo destino, Rectangle porta) {
        Rectangle a = destino.area;

        float dBaixo = Math.abs(porta.y               - a.y);
        float dCima  = Math.abs(porta.y + porta.height - (a.y + a.height));
        float dEsq   = Math.abs(porta.x               - a.x);
        float dDir   = Math.abs(porta.x + porta.width  - (a.x + a.width));

        float menor  = Math.min(Math.min(dBaixo, dCima), Math.min(dEsq, dDir));
        float margem = 28f;

        float cx = a.x + a.width  / 2f;
        float cy = a.y + a.height / 2f;

        if (menor == dBaixo) return new Vector2(cx, a.y             + margem);
        if (menor == dCima)  return new Vector2(cx, a.y + a.height  - margem);
        if (menor == dEsq)   return new Vector2(a.x             + margem, cy);
        return                      new Vector2(a.x + a.width   - margem, cy);
    }

    // consulta da lista completa de comodos
    public List<Comodo> getComodos() { return comodos; }

    // dados de um comodo carregado do tiled
    public static class Comodo {
        public final Rectangle area;
        public final boolean   cameraEstatica;
        // nome original do objeto no tiled
        public final String    nome;
        // nome sem sufixo numerico para agrupamento
        public final String    nomeGrupo;

        Comodo(Rectangle area, boolean cameraEstatica, String nome, String nomeGrupo) {
            this.area          = area;
            this.cameraEstatica = cameraEstatica;
            this.nome          = nome;
            this.nomeGrupo     = nomeGrupo;
        }
    }
}
