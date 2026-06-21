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

// carrega e consulta as portas definidas na camada Portas do Tiled
public class GerenciadorPortas {

    // distância em pixels de mundo ao redor da hitbox para detectar proximidade
    private static final float FOLGA = 24f;

    private final List<Porta> portas = new ArrayList<>();
    private final float escala;

    public GerenciadorPortas(TiledMap mapa, float escala) {
        this.escala = escala;
        CoordenadasTiled.setEscala(escala);

        MapLayer camada = mapa.getLayers().get("Portas");
        if (camada == null) return;

        for (MapObject obj : camada.getObjects()) {
            if (!(obj instanceof RectangleMapObject)) continue;

            MapProperties props = obj.getProperties();
            Rectangle     r     = ((RectangleMapObject) obj).getRectangle();

            // porta sem destino é ignorada
            String destino = lerPropriedade(props, "destino");
            if (destino == null || destino.isEmpty()) continue;

            // spawn usa coordenadas da propriedade ou o centro da área da porta
            String  coordStr = lerPropriedade(props, "coordenadas");
            Vector2 spawn;
            if (coordStr == null || coordStr.isEmpty()) {
                spawn = new Vector2((r.x + r.width / 2f) * escala, (r.y + r.height / 2f) * escala);
            } else {
                spawn = CoordenadasTiled.parseCoordenadasMundo(coordStr);
                if (spawn == null) continue;
            }

            String  video        = lerPropriedade(props, "video");
            boolean usarFade     = parseBoolean(props, "fade",         true);
            boolean noUmbra      = parseBoolean(props, "umbra",        false);
            boolean noReal       = parseBoolean(props, "real",         true);
            boolean trancado     = parseBoolean(props, "trancado",     false);
            boolean destrancavel = parseBoolean(props, "destrancavel", false);

            String condicao = lerPropriedade(props, "condicao");
            if (condicao == null) condicao = "";

            // o nome vem do campo name do objeto no Tiled, não de uma propriedade
            String nome = obj.getName();
            if (nome == null || nome.isEmpty()) nome = lerPropriedade(props, "nome");
            if (nome == null || nome.isEmpty()) nome = destino;

            portas.add(new Porta(
                CoordenadasTiled.paraMundo(r), nome, destino, spawn,
                video, usarFade, noUmbra, noReal, trancado, destrancavel, condicao
            ));
        }
    }

    // retorna a primeira porta ativa no alcance de detecção do jogador, ou null
    public Porta acharProxima(Jogador jogador, boolean umbra) {
        Rectangle alcance = new Rectangle(
            jogador.hitbox.x - FOLGA,
            jogador.hitbox.y - FOLGA,
            jogador.hitbox.width  + FOLGA * 2f,
            jogador.hitbox.height + FOLGA * 2f
        );
        for (Porta p : portas) {
            if (!p.isAtivo(umbra)) continue;
            if (alcance.overlaps(p.area)) return p;
        }
        return null;
    }

    // tenta ler uma propriedade pelo nome exato e por variações comuns
    private static String lerPropriedade(MapProperties props, String chave) {
        if (props.containsKey(chave)) {
            Object val = props.get(chave);
            if (val != null) return val.toString().trim();
        }
        String[] alternativas = {
            chave.toLowerCase(), chave.toUpperCase(),
            "property_" + chave, "Property_" + chave
        };
        for (String alt : alternativas) {
            if (props.containsKey(alt)) {
                Object val = props.get(alt);
                if (val != null) return val.toString().trim();
            }
        }
        return null;
    }

    // converte uma propriedade string para boolean com valor padrão
    private static boolean parseBoolean(MapProperties props, String chave, boolean padrao) {
        String val = lerPropriedade(props, chave);
        if (val == null) return padrao;
        return Boolean.parseBoolean(val) || val.equalsIgnoreCase("1") || val.equalsIgnoreCase("yes");
    }

    public List<Porta> getPortas() { return portas; }

    // dados de uma porta carregada do Tiled
    public static class Porta {
        public final Rectangle area;
        public final String    nome;
        public final String    destino;
        public final Vector2   spawn;
        public final String    video;
        public final boolean   usarFade;

        // define em qual mundo a porta é visível e interativa
        public final boolean noUmbra;
        public final boolean noReal;

        public final boolean trancado;
        public final boolean destrancavel;
        public final String  condicao;

        Porta(Rectangle area, String nome, String destino, Vector2 spawn,
              String video, boolean usarFade, boolean noUmbra, boolean noReal,
              boolean trancado, boolean destrancavel, String condicao) {
            this.area        = area;
            this.nome        = nome;
            this.destino     = destino;
            this.spawn       = spawn;
            this.video       = video;
            this.usarFade    = usarFade;
            this.noUmbra     = noUmbra;
            this.noReal      = noReal;
            this.trancado    = trancado;
            this.destrancavel = destrancavel;
            this.condicao    = condicao;
        }

        // retorna true se a porta deve aparecer no mundo atual
        public boolean isAtivo(boolean umbra) {
            return umbra ? noUmbra : noReal;
        }
    }
}
