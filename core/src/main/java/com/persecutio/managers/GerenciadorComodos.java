package com.persecutio.managers;

import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.List;

// gerencia os cômodos definidos no Tiled para controle de câmera e escurecimento
public class GerenciadorComodos {

    private final List<Comodo> comodos = new ArrayList<>();

    public GerenciadorComodos(TiledMap mapa, float escala) {
        CoordenadasTiled.setEscala(escala);

        MapLayer camada = mapa.getLayers().get("Comodos");
        if (camada == null) return;

        for (MapObject obj : camada.getObjects()) {
            if (!(obj instanceof RectangleMapObject)) continue;

            Rectangle r    = ((RectangleMapObject) obj).getRectangle();
            String    nome = obj.getName() != null ? obj.getName() : "";

            // label exibido na UI, gerado a partir do nome se não definido
            String label = obj.getProperties().get("label", String.class);
            if (label == null || label.isEmpty())
                label = nome.isEmpty() ? "?" : Character.toUpperCase(nome.charAt(0)) + nome.substring(1);

            // cômodos com nome iniciando em "corredor" não se escurecem entre si
            boolean corredor = nome.toLowerCase().startsWith("corredor");

            // câmera estática faz o viewport se fixar no centro do cômodo
            boolean estatica = Boolean.TRUE.equals(obj.getProperties().get("cameraEstatica", Boolean.class));

            comodos.add(new Comodo(nome, label, CoordenadasTiled.paraMundo(r), corredor, estatica));
        }
    }

    // retorna o cômodo que contém o ponto fornecido, ou null se fora de todos
    public Comodo achar(float px, float py) {
        for (Comodo c : comodos) {
            if (c.area.contains(px, py)) return c;
        }
        return null;
    }

    // calcula o ponto de spawn dentro do destino baseado no lado mais próximo da porta
    public Vector2 spawnEntrada(Comodo destino, Rectangle porta) {
        Rectangle a = destino.area;

        float dBaixo = Math.abs(porta.y                - a.y);
        float dCima  = Math.abs(porta.y + porta.height - (a.y + a.height));
        float dEsq   = Math.abs(porta.x                - a.x);
        float dDir   = Math.abs(porta.x + porta.width  - (a.x + a.width));

        float menor  = Math.min(Math.min(dBaixo, dCima), Math.min(dEsq, dDir));
        float margem = 28f;

        // centro do cômodo usado como referência horizontal/vertical
        float cx = a.x + a.width  / 2f;
        float cy = a.y + a.height / 2f;

        if (menor == dBaixo) return new Vector2(cx, a.y              + margem);
        if (menor == dCima)  return new Vector2(cx, a.y + a.height   - margem);
        if (menor == dEsq)   return new Vector2(a.x              + margem, cy);
        return                      new Vector2(a.x + a.width    - margem, cy);
    }

    public List<Comodo> getComodos() { return comodos; }

    // dados de um cômodo carregado do Tiled
    public static class Comodo {
        public final String    nome;
        public final String    label;
        public final Rectangle area;
        public final boolean   corredor;
        public final boolean   estatica;

        Comodo(String nome, String label, Rectangle area, boolean corredor, boolean estatica) {
            this.nome     = nome;
            this.label    = label;
            this.area     = area;
            this.corredor = corredor;
            this.estatica = estatica;
        }
    }
}
