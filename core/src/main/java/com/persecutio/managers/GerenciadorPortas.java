package com.persecutio.managers;

import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.persecutio.entities.Jogador;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GerenciadorPortas {

    private static final float FOLGA = 24f;

    private final List<Porta> portas = new ArrayList<>();
    private final float escala;

    private final Rectangle rectAlcance = new Rectangle();

    public GerenciadorPortas(TiledMap mapa, float escala,
                             Map<String, Map<String, Object>> defaults) {
        this.escala = escala;
        CoordenadasTiled.setEscala(escala);

        // Carrega objetos da camada "Destinos" (retângulo completo, não só centro)
        Map<String, Rectangle> destinos = new HashMap<>();
        MapLayer camadaDestinos = mapa.getLayers().get("Destinos");
        if (camadaDestinos != null) {
            for (MapObject obj : camadaDestinos.getObjects()) {
                if (!(obj instanceof RectangleMapObject)) continue;
                Rectangle r = ((RectangleMapObject) obj).getRectangle();
                String nome = obj.getName();
                if (nome != null && !nome.trim().isEmpty()) {
                    Rectangle rm = new Rectangle(
                        r.x * escala,
                        r.y * escala,
                        r.width * escala,
                        r.height * escala
                    );
                    destinos.put(nome.trim().toLowerCase(), rm);
                }
            }
        }

        MapLayer camada = mapa.getLayers().get("Portas");
        if (camada == null) return;

        for (MapObject obj : camada.getObjects()) {
            if (!(obj instanceof RectangleMapObject)) continue;

            MapProperties props = obj.getProperties();
            Rectangle     r     = ((RectangleMapObject) obj).getRectangle();

            // "destino" = nome do objeto spawn na camada "Destinos"
            String spawnNome = lerProp(props, "destino");
            if (spawnNome == null || spawnNome.isEmpty()) continue;

            // "area" = nome legível do próximo cômodo
            String label = lerProp(props, "area");
            if (label == null || label.isEmpty()) label = spawnNome;

            // Resolve spawn: procura na camada "Destinos", senão tenta x,y direto
            Rectangle areaDestino = null;
            Vector2 spawn;
            Rectangle destinoRect = destinos.get(spawnNome.trim().toLowerCase());
            if (destinoRect != null) {
                areaDestino = destinoRect;
                spawn = new Vector2(
                    areaDestino.x + areaDestino.width / 2f,
                    areaDestino.y + areaDestino.height / 2f
                );
            } else {
                spawn = CoordenadasTiled.parseCoordenadasMundoDireto(spawnNome);
                if (spawn == null) {
                    spawn = new Vector2(
                        (r.x + r.width / 2f) * escala,
                        (r.y + r.height / 2f) * escala
                    );
                }
            }

            String classOrig = props.get("type")  != null ? props.get("type").toString()  :
                            props.get("class") != null ? props.get("class").toString() : "";

            String  video        = lerProp(props, "video");
            boolean usarFade     = lerBool(props, defaults, classOrig, "fade",         true);
            boolean noUmbra      = lerBool(props, defaults, classOrig, "umbra",        false);
            boolean noReal       = lerBool(props, defaults, classOrig, "real",         true);
            boolean trancado     = lerBool(props, defaults, classOrig, "trancado",     false);
            boolean destrancavel = lerBool(props, defaults, classOrig, "destrancavel", false);

            String condicao = lerProp(props, "condicao");
            if (condicao == null) condicao = "";

            String nome = obj.getName();
            if (nome == null || nome.isEmpty()) nome = lerProp(props, "nome");
            if (nome == null || nome.isEmpty()) nome = label;

            portas.add(new Porta(
                CoordenadasTiled.paraMundo(r), nome, label, spawn, areaDestino,
                video, usarFade, noUmbra, noReal, trancado, destrancavel, condicao
            ));
        }
    }

    public Porta acharProxima(Jogador jogador, boolean umbra) {
        rectAlcance.set(
            jogador.hitbox.x - FOLGA,
            jogador.hitbox.y - FOLGA,
            jogador.hitbox.width  + FOLGA * 2f,
            jogador.hitbox.height + FOLGA * 2f
        );
        for (Porta p : portas) {
            if (!p.isAtivo(umbra)) continue;
            if (rectAlcance.overlaps(p.area)) return p;
        }
        return null;
    }

    private static String lerProp(MapProperties props, String chave) {
        if (props.containsKey(chave)) {
            Object val = props.get(chave);
            if (val != null) return val.toString().trim();
        }
        return null;
    }

    private static boolean lerBool(MapProperties props, Map<String, Map<String, Object>> defaults,
                                   String classOrig, String chave, boolean fallback) {
        String val = lerProp(props, chave);
        if (val != null) return Boolean.parseBoolean(val) || val.equals("1") || val.equalsIgnoreCase("yes");

        Map<String, Object> cd = defaults.get(classOrig.toLowerCase());
        if (cd != null && cd.containsKey(chave)) {
            Object v = cd.get(chave);
            if (v instanceof Boolean) return (Boolean) v;
        }
        return fallback;
    }

    public List<Porta> getPortas() { return portas; }

    public static class Porta {
        public final Rectangle area;
        public final String    nome;
        public final String    label;
        public final Vector2   spawn;
        public final Rectangle areaDestino; // retângulo do objeto na camada "Destinos"
        public final String    video;
        public final boolean   usarFade;

        public final boolean noUmbra;
        public final boolean noReal;

        public final boolean trancado;
        public final boolean destrancavel;
        public final String  condicao;

        Porta(Rectangle area, String nome, String label, Vector2 spawn, Rectangle areaDestino,
              String video, boolean usarFade, boolean noUmbra, boolean noReal,
              boolean trancado, boolean destrancavel, String condicao) {
            this.area         = area;
            this.nome         = nome;
            this.label        = label;
            this.spawn        = spawn;
            this.areaDestino  = areaDestino;
            this.video        = video;
            this.usarFade     = usarFade;
            this.noUmbra      = noUmbra;
            this.noReal       = noReal;
            this.trancado     = trancado;
            this.destrancavel = destrancavel;
            this.condicao     = condicao;
        }

        public boolean isAtivo(boolean umbra) {
            return umbra ? noUmbra : noReal;
        }
    }
}
