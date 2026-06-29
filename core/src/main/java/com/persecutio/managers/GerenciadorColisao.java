package com.persecutio.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.objects.TiledMapTileMapObject;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.persecutio.entities.EntidadeMapa;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Colisao do mapa
public class GerenciadorColisao {

    // Leitura do valor
    private static Map<String, Map<String, Object>> lerDefaults(String caminho) {
        Map<String, Map<String, Object>> resultado = new HashMap<>();
        try {
            String    json  = Gdx.files.internal(caminho).readString("UTF-8");
            JsonValue raiz  = new JsonReader().parse(json);
            JsonValue tipos = raiz.get("propertyTypes");
            if (tipos == null) return resultado;

            for (JsonValue tipo : tipos) {
                if (!"class".equals(tipo.getString("type", ""))) continue;
                String    nome    = tipo.getString("name", "").toLowerCase();
                JsonValue membros = tipo.get("members");
                if (membros == null) continue;

                Map<String, Object> props = new HashMap<>();
                for (JsonValue m : membros) {
                    String mNome = m.getString("name", "");
                    switch (m.getString("type", "string")) {
                        case "bool":  props.put(mNome, m.getBoolean("value", false)); break;
                        case "int":   props.put(mNome, m.getInt("value", 0));         break;
                        case "float": props.put(mNome, m.getFloat("value", 0f));      break;
                        default:      props.put(mNome, m.getString("value", ""));     break;
                    }
                }
                resultado.put(nome, props);
            }
        } catch (Exception e) {
            Gdx.app.log("GerenciadorColisao", "erro ao ler projeto: " + e.getMessage());
        }
        return resultado;
    }

    private static boolean getDefault(Map<String, Map<String, Object>> defaults,
                                      String classe, String prop, boolean fallback) {
        Map<String, Object> props = defaults.get(classe.toLowerCase());
        if (props == null) return fallback;
        Object v = props.get(prop);
        return (v instanceof Boolean) ? (Boolean) v : fallback;
    }

    public static class ObjetoColisao {
        public final Rectangle area;
        public final String    nome;
        public final boolean   noUmbra;
        public final boolean   noReal;
        public final boolean   trancado;
        public final boolean   destrancavel;
        public final String    condicao;

        public ObjetoColisao(Rectangle area, String nome, MapProperties props,
                             Map<String, Map<String, Object>> defaults) {
            this.area = area;
            this.nome = (nome != null && !nome.isEmpty()) ? nome :
                        ((props.get("name") != null) ? props.get("name").toString() :
                         (props.get("nome") != null) ? props.get("nome").toString() : "");

            String classe = props.get("type")  != null ? props.get("type").toString()  :
                            props.get("class") != null ? props.get("class").toString() : "";

            Object u = props.get("umbra");
            Object r = props.get("real");
            this.noUmbra = (u != null) ? Boolean.parseBoolean(u.toString())
                                       : getDefault(defaults, classe, "umbra", true);
            this.noReal  = (r != null) ? Boolean.parseBoolean(r.toString())
                                       : getDefault(defaults, classe, "real",  true);

            Object t = props.get("trancado");
            this.trancado = (t != null) ? Boolean.parseBoolean(t.toString())
                                        : getDefault(defaults, classe, "trancado", false);

            Object d = props.get("destrancavel");
            this.destrancavel = (d != null) ? Boolean.parseBoolean(d.toString())
                                            : getDefault(defaults, classe, "destrancavel", false);

            Object c = props.get("condicao");
            this.condicao = (c != null) ? c.toString() : "";
        }

        // Consulta do estado
        public boolean isAtivo(boolean umbra) {
            return umbra ? noUmbra : noReal;
        }
    }

    private final List<ObjetoColisao> paredes;
    private final List<ObjetoColisao> hitboxPortas;
    private final Map<String, ObjetoColisao> interativos;
    private final Map<String, EntidadeMapa> npcs;

    private final List<Rectangle> cacheParedes = new ArrayList<>();
    private final List<ObjetoColisao> cachePortas = new ArrayList<>();
    private final Map<String, Rectangle> cacheInterativos = new HashMap<>();
    private final Map<String, ObjetoColisao> cacheInterativosCompletos = new HashMap<>();
    private final Map<String, EntidadeMapa> cacheNpcs = new HashMap<>();

    private final Set<String> destrancados = new HashSet<>();
    private final Map<String, Map<String, Object>> defaults;

    private final Rectangle rectTemp = new Rectangle();
    private boolean colisoesDesativadas = false;

    public void alternarColisoes() {
        colisoesDesativadas = !colisoesDesativadas;
    }

    public boolean isColisoesDesativadas() { return colisoesDesativadas; }

    // Criacao da colisao do mapa
    public GerenciadorColisao(TiledMap mapa, float escala, String caminhoProjeto) {
        CoordenadasTiled.setEscala(escala);
        defaults     = lerDefaults(caminhoProjeto);

        paredes      = new ArrayList<>();
        hitboxPortas = new ArrayList<>();
        interativos  = new HashMap<>();
        npcs         = new HashMap<>();

        carregarParedes(mapa, "Colisoes", paredes);
        carregarInterativos(mapa, "Interativos");
        carregarNpcs(mapa, "NPCs");
        carregarParedes(mapa, "Portas", hitboxPortas);
    }

    // Leitura do valor
    private String lerChave(MapObject objeto) {
        String chave = objeto.getName();
        if (chave == null || chave.trim().isEmpty())
            chave = objeto.getProperties().get("name", String.class);
        if (chave == null || chave.trim().isEmpty())
            chave = objeto.getProperties().get("nome", String.class);

        if ((chave == null || chave.trim().isEmpty()) && objeto instanceof TiledMapTileMapObject) {
            TiledMapTileMapObject tileObj = (TiledMapTileMapObject) objeto;
            TiledMapTile tile = tileObj.getTile();
            if (tile != null && tile.getProperties() != null) {
                chave = tile.getProperties().get("name",  String.class);
                if (chave == null || chave.trim().isEmpty())
                    chave = tile.getProperties().get("nome",  String.class);
                if (chave == null || chave.trim().isEmpty())
                    chave = tile.getProperties().get("class", String.class);
                if (chave == null || chave.trim().isEmpty())
                    chave = tile.getProperties().get("type",  String.class);
            }
        }

        if (chave == null || chave.trim().isEmpty())
            chave = objeto.getProperties().get("class", String.class);
        if (chave == null || chave.trim().isEmpty())
            chave = objeto.getProperties().get("type",  String.class);

        return chave != null ? chave.trim().toLowerCase() : "";
    }

    // Carregamento dos dados
    private void carregarParedes(TiledMap mapa, String camadaNome, List<ObjetoColisao> lista) {
        MapLayer camada = mapa.getLayers().get(camadaNome);
        if (camada == null) return;
        for (MapObject objeto : camada.getObjects()) {
            if (!(objeto instanceof RectangleMapObject)) continue;
            Rectangle r     = ((RectangleMapObject) objeto).getRectangle();
            String    chave = lerChave(objeto);
            lista.add(new ObjetoColisao(CoordenadasTiled.paraMundo(r), chave,
                                        objeto.getProperties(), defaults));
        }
    }

    // Carregamento dos dados
    private void carregarInterativos(TiledMap mapa, String camadaNome) {
        MapLayer camada = mapa.getLayers().get(camadaNome);
        if (camada == null) return;
        for (MapObject objeto : camada.getObjects()) {
            if (!(objeto instanceof RectangleMapObject)) continue;
            String classe = lerChave(objeto);
            if (classe.isEmpty()) continue;
            Rectangle r = ((RectangleMapObject) objeto).getRectangle();
            interativos.put(classe, new ObjetoColisao(CoordenadasTiled.paraMundo(r), classe,
                                                      objeto.getProperties(), defaults));
        }
    }

    // Carregamento dos dados
    private void carregarNpcs(TiledMap mapa, String camadaNome) {
        MapLayer camada = mapa.getLayers().get(camadaNome);
        if (camada == null) return;
        for (MapObject objeto : camada.getObjects()) {
            if (!(objeto instanceof TiledMapTileMapObject)) continue;
            TiledMapTileMapObject tileObj = (TiledMapTileMapObject) objeto;
            String classe = lerChave(objeto);
            if (classe.isEmpty()) continue;

            TextureRegion textura = tileObj.getTextureRegion();
            float escala  = CoordenadasTiled.getEscala();
            float largura = textura.getRegionWidth()  * tileObj.getScaleX() * escala;
            float altura  = textura.getRegionHeight() * tileObj.getScaleY() * escala;
            float x       = tileObj.getX() * escala;
            float y       = tileObj.getY() * escala;

            npcs.put(classe, new EntidadeMapa(
                classe,
                new Rectangle(x, y, largura, altura),
                textura,
                objeto.getProperties(),
                false));
        }
    }

    public boolean verificarPosicao(float proximoX, float proximoY,
                                    float largura, float altura, boolean umbra) {
        if (colisoesDesativadas) return true;

        rectTemp.set(proximoX, proximoY, largura, altura);

        for (ObjetoColisao parede : paredes) {
            if (!parede.isAtivo(umbra)) continue;
            if (rectTemp.overlaps(parede.area)) return false;
        }

        for (ObjetoColisao porta : hitboxPortas) {
            if (rectTemp.overlaps(porta.area)) return false;
        }

        return true;
    }

    // Consulta do estado
    public Rectangle getArea(String nome, boolean umbra) {
        String chave = nome.toLowerCase();

        if (interativos.containsKey(chave)) {
            ObjetoColisao o = interativos.get(chave);
            return (o != null && o.isAtivo(umbra)) ? o.area : null;
        }

        if (npcs.containsKey(chave)) {
            EntidadeMapa n = npcs.get(chave);
            return (n != null && n.isAtivo(umbra)) ? n.area : null;
        }

        return null;
    }

    // Consulta do estado
    public ObjetoColisao getInterativo(String nome, boolean umbra) {
        String        chave = nome.toLowerCase();
        ObjetoColisao o     = interativos.get(chave);
        return (o != null && o.isAtivo(umbra)) ? o : null;
    }

    // Consulta do estado
    public EntidadeMapa getNpc(String nome, boolean umbra) {
        EntidadeMapa n = npcs.get(nome.toLowerCase());
        return (n != null && n.isAtivo(umbra)) ? n : null;
    }

    // Area reflexo
    public Rectangle getReflexoArea(boolean umbra) {
        ObjetoColisao o = interativos.get("reflexo");
        return (o != null && o.isAtivo(umbra)) ? o.area : null;
    }

    // Consulta do estado
    public List<Rectangle> getParedes(boolean umbra) {
        cacheParedes.clear();
        for (ObjetoColisao p : paredes) {
            if (p.isAtivo(umbra)) cacheParedes.add(p.area);
        }
        for (ObjetoColisao p : hitboxPortas) {
            cacheParedes.add(p.area);
        }
        return cacheParedes;
    }

    // Consulta do estado
    public List<Rectangle> getHitboxPortas() {
        cacheParedes.clear();
        for (ObjetoColisao p : hitboxPortas) cacheParedes.add(p.area);
        return cacheParedes;
    }

    // Consulta do estado
    public List<ObjetoColisao> getHitboxPortasCompletas() {
        cachePortas.clear();
        cachePortas.addAll(hitboxPortas);
        return cachePortas;
    }

    // Consulta do estado
    public Map<String, Rectangle> getInterativos(boolean umbra) {
        cacheInterativos.clear();
        for (Map.Entry<String, ObjetoColisao> e : interativos.entrySet()) {
            if (e.getValue().isAtivo(umbra)) cacheInterativos.put(e.getKey(), e.getValue().area);
        }
        return cacheInterativos;
    }

    // Consulta do estado
    public Map<String, ObjetoColisao> getInterativosCompletos(boolean umbra) {
        cacheInterativosCompletos.clear();
        for (Map.Entry<String, ObjetoColisao> e : interativos.entrySet()) {
            if (e.getValue().isAtivo(umbra)) cacheInterativosCompletos.put(e.getKey(), e.getValue());
        }
        return cacheInterativosCompletos;
    }

    // Consulta do estado
    public Map<String, EntidadeMapa> getNpcs(boolean umbra) {
        cacheNpcs.clear();
        for (Map.Entry<String, EntidadeMapa> e : npcs.entrySet()) {
            if (e.getValue().isAtivo(umbra)) cacheNpcs.put(e.getKey(), e.getValue());
        }
        return cacheNpcs;
    }

    // Retorna TODAS as paredes Colisoes Portas para criar corpos Box2D das luzes
    public List<Rectangle> getTodasParedesBox2D() {
        List<Rectangle> todas = new ArrayList<>();
        for (ObjetoColisao p : paredes) {
            todas.add(p.area);
        }
        for (ObjetoColisao p : hitboxPortas) {
            todas.add(p.area);
        }
        return todas;
    }

    // Retorna so as paredes de colisao sem portas para sombra Box2D
    public List<Rectangle> getParedesBox2D() {
        List<Rectangle> lista = new ArrayList<>();
        for (ObjetoColisao p : paredes) {
            lista.add(p.area);
        }
        return lista;
    }

    // Retorna so as hitboxes de porta para sombra Box2D
    public List<Rectangle> getPortasBox2D() {
        List<Rectangle> lista = new ArrayList<>();
        for (ObjetoColisao p : hitboxPortas) {
            lista.add(p.area);
        }
        return lista;
    }

    // Processamento interno
    public void destrancar(String nome) {
        if (nome != null && !nome.isEmpty()) destrancados.add(nome.toLowerCase());
    }

    // Consulta do estado
    public Map<String, Map<String, Object>> getDefaults() { return defaults; }

    // Consulta do estado
    public boolean isDestrancado(String nome) {
        return nome != null && !nome.isEmpty() && destrancados.contains(nome.toLowerCase());
    }
}