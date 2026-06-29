package com.persecutio.managers;

import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

// Gerencia comodos definidos no tiled e controla cull de tiles
public class GerenciadorComodos {

    private final List<Comodo> comodos = new ArrayList<>();
    // Agrupa comodos pelo nome base sem sufixo numerico
    private final Map<String, List<Comodo>> comodosPorNome = new HashMap<>();
    // Agrupa comodos pelo nome EXATO incluindo numeros
    private final Map<String, List<Comodo>> comodosPorNomeExato = new HashMap<>();

    // Lista reutilizavel para evitar alocacao por frame
    private final List<Comodo> cacheCull = new ArrayList<>();

    // Criacao do gerenciador de comodos
    public GerenciadorComodos(TiledMap mapa, float escala) {
        CoordenadasTiled.setEscala(escala);

        MapLayer camada = mapa.getLayers().get("Comodos");
        if (camada == null) return;

        for (MapObject obj : camada.getObjects()) {
            if (!(obj instanceof RectangleMapObject)) continue;

            Rectangle r = ((RectangleMapObject) obj).getRectangle();

            // Camera estatica faz viewport fixar no centro do comodo
            boolean cameraEstatica = Boolean.TRUE.equals(obj.getProperties().get("cameraEstatica", Boolean.class));

            String nome = obj.getName();
            if (nome == null || nome.isEmpty()) nome = "";
            // Sufixo numerico removido para agrupar variantes do mesmo comodo
            String nomeGrupo = normalizarNome(nome);

            Comodo c = new Comodo(CoordenadasTiled.paraMundo(r), cameraEstatica, nome, nomeGrupo);
            comodos.add(c);
            comodosPorNome.computeIfAbsent(nomeGrupo, k -> new ArrayList<>()).add(c);
            comodosPorNomeExato.computeIfAbsent(nome.toLowerCase().trim(), k -> new ArrayList<>()).add(c);
        }

        // Combina comodos com EXATAMENTE O MESMO NOME que se sobrepõem e
        // Tem pelo menos um com camera estatica em um unico comodo virtual
        for (Map.Entry<String, List<Comodo>> entry : comodosPorNomeExato.entrySet()) {
            List<Comodo> grupo = entry.getValue();
            if (grupo.size() <= 1) continue;

            List<Comodo> naoVisitados = new ArrayList<>(grupo);

            while (!naoVisitados.isEmpty()) {
                Comodo seed = naoVisitados.remove(0);
                List<Comodo> componente = new ArrayList<>();
                componente.add(seed);

                Queue<Comodo> fila = new LinkedList<>();
                fila.add(seed);

                while (!fila.isEmpty()) {
                    Comodo atual = fila.poll();
                    Iterator<Comodo> it = naoVisitados.iterator();
                    while (it.hasNext()) {
                        Comodo outro = it.next();
                        if (atual.area.overlaps(outro.area)) {
                            fila.add(outro);
                            componente.add(outro);
                            it.remove();
                        }
                    }
                }

                boolean algumEstatico = false;
                for (Comodo c : componente) {
                    if (c.cameraEstatica) { algumEstatico = true; break; }
                }

                if (algumEstatico) {
                    float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
                    float maxX = Float.MIN_VALUE, maxY = Float.MIN_VALUE;
                    for (Comodo c : componente) {
                        minX = Math.min(minX, c.area.x);
                        minY = Math.min(minY, c.area.y);
                        maxX = Math.max(maxX, c.area.x + c.area.width);
                        maxY = Math.max(maxY, c.area.y + c.area.height);
                    }
                    Rectangle combinado = new Rectangle(minX, minY, maxX - minX, maxY - minY);
                    for (Comodo c : componente) {
                        c.areaCamera = combinado;
                        c.cameraEstatica = true;
                    }
                }
            }
        }
    }

    // Remove sufixo numerico do nome para agrupar comodos
    private static String normalizarNome(String nome) {
        if (nome == null || nome.isEmpty()) return "";
        return nome.replaceAll("\\d+$", "").toLowerCase().trim();
    }

    // Retorna o comodo que contem o ponto dado
    public Comodo achar(float px, float py) {
        for (Comodo c : comodos) {
            if (c.area.contains(px, py)) return c;
        }
        return null;
    }

    // Retorna comodos do mesmo grupo base que o atual
    public List<Comodo> getComodosDoMesmoGrupo(Comodo atual) {
        if (atual == null || atual.nomeGrupo.isEmpty()) return new ArrayList<>();
        List<Comodo> grupo = comodosPorNome.get(atual.nomeGrupo);
        return grupo != null ? new ArrayList<>(grupo) : new ArrayList<>();
    }

    // Retorna os comodos que devem receber cull de tiles neste frame
    // Regra mesmo nome EXATO que o atual renderiza todos com aquele nome
    // Nomes diferentes renderiza apenas o comodo onde o player esta
    public List<Comodo> getCullAtivo(Comodo comodoJogador) {
        cacheCull.clear();

        if (comodoJogador == null) return cacheCull;

        String nome = comodoJogador.nome;
        if (nome != null && !nome.trim().isEmpty()) {
            List<Comodo> mesmoNome = comodosPorNomeExato.get(nome.toLowerCase().trim());
            if (mesmoNome != null && mesmoNome.size() > 1) {
                // Mesmo nome exato em multiplos comodos cull nao ocorre entre eles
                cacheCull.addAll(mesmoNome);
                return cacheCull;
            }
        }

        // Nome unico vazio ou diferente dos demais renderiza apenas o atual
        cacheCull.add(comodoJogador);
        return cacheCull;
    }

    // Verifica se um tile com posicao e tamanho dados passa no filtro de 50
    // O tile precisa ter pelo menos 50 de sua area dentro do retangulo de cull
    public static boolean passaFiltro(Rectangle tileRect, Rectangle cullRect) {
        // Intersecao dos dois retangulos
        float ix = Math.max(tileRect.x, cullRect.x);
        float iy = Math.max(tileRect.y, cullRect.y);
        float iw = Math.min(tileRect.x + tileRect.width,  cullRect.x + cullRect.width)  - ix;
        float ih = Math.min(tileRect.y + tileRect.height, cullRect.y + cullRect.height) - iy;

        if (iw <= 0 || ih <= 0) return false;

        float areaTile = tileRect.width * tileRect.height;
        if (areaTile <= 0) return false;

        float areaIntersecao = iw * ih;
        // Pelo menos 50 do tile deve estar dentro do comodo
        return (areaIntersecao / areaTile) >= 0.5f;
    }

    // Calcula ponto de spawn dentro do destino baseado na borda mais proxima da porta
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

    // Consulta da lista completa de comodos
    public List<Comodo> getComodos() { return comodos; }

    // Dados de um comodo carregado do tiled
    public static class Comodo {
        public final Rectangle area;
        public boolean   cameraEstatica;
        // Nome original do objeto no tiled
        public final String    nome;
        // Nome sem sufixo numerico para agrupamento
        public final String    nomeGrupo;
        // Area combinada para camera estatica de comodos sobrepostos do mesmo nome EXATO
        public Rectangle areaCamera;

        Comodo(Rectangle area, boolean cameraEstatica, String nome, String nomeGrupo) {
            this.area          = area;
            this.cameraEstatica = cameraEstatica;
            this.nome          = nome;
            this.nomeGrupo     = nomeGrupo;
            this.areaCamera    = new Rectangle(area);
        }
    }
}