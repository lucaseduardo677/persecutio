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

// Gerencia os cômodos definidos no Tiled para controle de câmera e escurecimento
public class GerenciadorComodos {

    private final List<Comodo> comodos = new ArrayList<>();
    // Agrupa cômodos pelo nome para não escurecer entre si
    private final Map<String, List<Comodo>> comodosPorNome = new HashMap<>();

    public GerenciadorComodos(TiledMap mapa, float escala) {
        CoordenadasTiled.setEscala(escala);

        MapLayer camada = mapa.getLayers().get("Comodos");
        if (camada == null) return;

        for (MapObject obj : camada.getObjects()) {
            if (!(obj instanceof RectangleMapObject)) continue;

            Rectangle r = ((RectangleMapObject) obj).getRectangle();

            // Câmera estática faz o viewport se fixar no centro do cômodo
            boolean cameraEstatica = Boolean.TRUE.equals(obj.getProperties().get("cameraEstatica", Boolean.class));

            // Le o nome do objeto no Tiled
            String nome = obj.getName();
            if (nome == null || nome.isEmpty()) nome = "";
            // Remove numeros do final para agrupar
            String nomeGrupo = normalizarNome(nome);

            Comodo c = new Comodo(CoordenadasTiled.paraMundo(r), cameraEstatica, nome, nomeGrupo);
            comodos.add(c);
            comodosPorNome.computeIfAbsent(nomeGrupo, k -> new ArrayList<>()).add(c);
        }
    }

    // Remove sufixo numerico para agrupar
    private static String normalizarNome(String nome) {
        if (nome == null || nome.isEmpty()) return "";
        // Remove dígitos do final
        return nome.replaceAll("\\d+$", "").toLowerCase().trim();
    }

    // Retorna o comodo que contem o ponto
    public Comodo achar(float px, float py) {
        for (Comodo c : comodos) {
            if (c.area.contains(px, py)) return c;
        }
        return null;
    }

    // Retorna os comodos do mesmo grupo
    public List<Comodo> getComodosDoMesmoGrupo(Comodo atual) {
        if (atual == null || atual.nomeGrupo.isEmpty()) return new ArrayList<>();
        List<Comodo> grupo = comodosPorNome.get(atual.nomeGrupo);
        return grupo != null ? new ArrayList<>(grupo) : new ArrayList<>();
    }

    // Calcula o ponto de spawn dentro do destino baseado no lado mais próximo da porta
    public Vector2 spawnEntrada(Comodo destino, Rectangle porta) {
        Rectangle a = destino.area;

        float dBaixo = Math.abs(porta.y                - a.y);
        float dCima  = Math.abs(porta.y + porta.height - (a.y + a.height));
        float dEsq   = Math.abs(porta.x                - a.x);
        float dDir   = Math.abs(porta.x + porta.width  - (a.x + a.width));

        float menor  = Math.min(Math.min(dBaixo, dCima), Math.min(dEsq, dDir));
        float margem = 28f;

        // Centro do comodo
        float cx = a.x + a.width  / 2f;
        float cy = a.y + a.height / 2f;

        if (menor == dBaixo) return new Vector2(cx, a.y              + margem);
        if (menor == dCima)  return new Vector2(cx, a.y + a.height   - margem);
        if (menor == dEsq)   return new Vector2(a.x              + margem, cy);
        return                      new Vector2(a.x + a.width    - margem, cy);
    }

    public List<Comodo> getComodos() { return comodos; }

    // Dados do comodo carregado do Tiled
    public static class Comodo {
        public final Rectangle area;
        public final boolean   cameraEstatica;
        // Nome original do objeto
        public final String    nome;
        // Nome sem numero
        public final String    nomeGrupo;

        Comodo(Rectangle area, boolean cameraEstatica, String nome, String nomeGrupo) {
            this.area           = area;
            this.cameraEstatica = cameraEstatica;
            this.nome           = nome;
            this.nomeGrupo      = nomeGrupo;
        }
    }
}
