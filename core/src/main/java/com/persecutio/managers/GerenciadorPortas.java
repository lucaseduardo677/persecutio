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
import java.util.List;
import java.util.Map;

// Le e organiza as portas do mapa
public class GerenciadorPortas {

    private static final float FOLGA = 24f;

    private final List<Porta> portas = new ArrayList<>();
    private final float escala;

    private final Rectangle rectAlcance = new Rectangle();

// Carrega as portas definidas no Tiled
    public GerenciadorPortas(TiledMap mapa, float escala,
                             Map<String, Map<String, Object>> defaults) {
        this.escala = escala;
        CoordenadasTiled.setEscala(escala);

        MapLayer camada = mapa.getLayers().get("Portas");
        if (camada == null) return;

        for (MapObject obj : camada.getObjects()) {
            if (!(obj instanceof RectangleMapObject)) continue;

            MapProperties props = obj.getProperties();
            Rectangle     r     = ((RectangleMapObject) obj).getRectangle();

            String destino = lerProp(props, "destino");
            if (destino == null || destino.isEmpty()) continue;

            String  coordStr = lerProp(props, "coordenadas");
            Vector2 spawn;
            if (coordStr == null || coordStr.isEmpty()) {
                spawn = new Vector2((r.x + r.width / 2f) * escala, (r.y + r.height / 2f) * escala);
            } else {
                spawn = CoordenadasTiled.parseCoordenadasMundo(coordStr);
                if (spawn == null) continue;
            }

            String classe = props.get("type")  != null ? props.get("type").toString()  :
                            props.get("class") != null ? props.get("class").toString() : "";

            String  video        = lerProp(props, "video");
            boolean usarFade     = lerBool(props, defaults, classe, "fade",         true);
            boolean noUmbra      = lerBool(props, defaults, classe, "umbra",        false);
            boolean noReal       = lerBool(props, defaults, classe, "real",         true);
            boolean trancado     = lerBool(props, defaults, classe, "trancado",     false);
            boolean destrancavel = lerBool(props, defaults, classe, "destrancavel", false);

            String condicao = lerProp(props, "condicao");
            if (condicao == null) condicao = "";

            String nome = obj.getName();
            if (nome == null || nome.isEmpty()) nome = lerProp(props, "nome");
            if (nome == null || nome.isEmpty()) nome = destino;

            portas.add(new Porta(
                CoordenadasTiled.paraMundo(r), nome, destino, spawn,
                video, usarFade, noUmbra, noReal, trancado, destrancavel, condicao
            ));
        }
    }

// Busca uma porta no alcance do jogador
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

// Le uma propriedade do mapa
    private static String lerProp(MapProperties props, String chave) {
        if (props.containsKey(chave)) {
            Object val = props.get(chave);
            if (val != null) return val.toString().trim();
        }
        return null;
    }

// Le um valor booleano com fallback
    private static boolean lerBool(MapProperties props, Map<String, Map<String, Object>> defaults,
                                   String classe, String chave, boolean fallback) {
        String val = lerProp(props, chave);
        if (val != null) return Boolean.parseBoolean(val) || val.equals("1") || val.equalsIgnoreCase("yes");

        Map<String, Object> cd = defaults.get(classe.toLowerCase());
        if (cd != null && cd.containsKey(chave)) {
            Object v = cd.get(chave);
            if (v instanceof Boolean) return (Boolean) v;
        }
        return fallback;
    }

    public List<Porta> getPortas() { return portas; }

// Dados de uma porta do mapa
    public static class Porta {
        public final Rectangle area;
        public final String    nome;
        public final String    destino;
        public final Vector2   spawn;
        public final String    video;
        public final boolean   usarFade;

        public final boolean noUmbra;
        public final boolean noReal;

        public final boolean trancado;
        public final boolean destrancavel;
        public final String  condicao;

        Porta(Rectangle area, String nome, String destino, Vector2 spawn,
              String video, boolean usarFade, boolean noUmbra, boolean noReal,
              boolean trancado, boolean destrancavel, String condicao) {
            this.area         = area;
            this.nome         = nome;
            this.destino      = destino;
            this.spawn        = spawn;
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
