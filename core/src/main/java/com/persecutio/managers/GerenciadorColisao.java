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

// Sistema de colisao do mapa
public class GerenciadorColisao {

    // Leitura dos defaults de tipos do projeto Tiled
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

    // Leitura de valor booleano default
    private static boolean getDefault(Map<String, Map<String, Object>> defaults,
                                      String classe, String prop, boolean fallback) {
        Map<String, Object> props = defaults.get(classe.toLowerCase());
        if (props == null) return fallback;
        Object v = props.get(prop);
        return (v instanceof Boolean) ? (Boolean) v : fallback;
    }

    // Representa um objeto com colisao no mapa
    public static class ObjetoColisao {
        public final Rectangle area;
        public final String    nome;
        public final boolean   noUmbra;
        public final boolean   noReal;
        public final boolean   trancado;
        public final boolean   destrancavel;
        public final String    condicao;

        // Construtor do objeto de colisao
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

        // Verifica se o objeto esta ativo no mundo atual
        public boolean isAtivo(boolean umbra) {
            return umbra ? noUmbra : noReal;
        }
    }

    // Lista de paredes do mapa
    private final List<ObjetoColisao> paredes;
    // Lista de hitboxes de portas
    private final List<ObjetoColisao> hitboxPortas;
    // Objetos com classe objeto que possuem colisao
    private final List<ObjetoColisao> objetos;
    // Mapa de objetos interativos
    private final Map<String, ObjetoColisao> interativos;
    // Mapa de NPCs
    private final Map<String, EntidadeMapa> npcs;

    // Cache reutilizavel de paredes
    private final List<Rectangle> cacheParedes = new ArrayList<>();
    // Cache reutilizavel de portas
    private final List<ObjetoColisao> cachePortas = new ArrayList<>();
    // Cache reutilizavel de objetos
    private final List<ObjetoColisao> cacheObjetos = new ArrayList<>();
    // Cache reutilizavel de interativos
    private final Map<String, Rectangle> cacheInterativos = new HashMap<>();
    // Cache reutilizavel de interativos completos
    private final Map<String, ObjetoColisao> cacheInterativosCompletos = new HashMap<>();
    // Cache reutilizavel de NPCs
    private final Map<String, EntidadeMapa> cacheNpcs = new HashMap<>();

    // Conjunto de portas destrancadas
    private final Set<String> destrancados = new HashSet<>();
    // Mapa de defaults do projeto Tiled
    private final Map<String, Map<String, Object>> defaults;

    // Retangulo temporario para verificacoes
    private final Rectangle rectTemp = new Rectangle();
    // Flag para desativar colisoes
    private boolean colisoesDesativadas = false;

    // Alterna estado das colisoes
    public void alternarColisoes() {
        colisoesDesativadas = !colisoesDesativadas;
    }

    // Retorna se as colisoes estao desativadas
    public boolean isColisoesDesativadas() { return colisoesDesativadas; }

    // Construtor do sistema de colisao
    public GerenciadorColisao(TiledMap mapa, float escala, String caminhoProjeto) {
        CoordenadasTiled.setEscala(escala);
        defaults     = lerDefaults(caminhoProjeto);

        paredes      = new ArrayList<>();
        hitboxPortas = new ArrayList<>();
        objetos      = new ArrayList<>();
        interativos  = new HashMap<>();
        npcs         = new HashMap<>();

        carregarParedes(mapa, "Colisoes", paredes);
        carregarInterativos(mapa, "Interativos");
        carregarInterativos(mapa, "Objetos");
        carregarNpcs(mapa, "NPCs");
        carregarParedes(mapa, "Portas", hitboxPortas);
    }

    // Leitura da chave identificadora de um objeto
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

    // Leitura da classe real do objeto (type ou class)
    private String lerClasse(MapObject objeto) {
        MapProperties props = objeto.getProperties();
        String classe = props.get("type") != null ? props.get("type").toString() :
                        props.get("class") != null ? props.get("class").toString() : "";
        return classe.trim().toLowerCase();
    }

    // Carrega paredes de uma camada do Tiled
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

    // Carrega objetos interativos do Tiled
    private void carregarInterativos(TiledMap mapa, String camadaNome) {
        MapLayer camada = mapa.getLayers().get(camadaNome);
        if (camada == null) return;
        for (MapObject objeto : camada.getObjects()) {
            if (!(objeto instanceof RectangleMapObject)) continue;
            String chave = lerChave(objeto);

            String classeReal = lerClasse(objeto);
            Rectangle r = ((RectangleMapObject) objeto).getRectangle();
            ObjetoColisao obj = new ObjetoColisao(CoordenadasTiled.paraMundo(r), chave,
                                                  objeto.getProperties(), defaults);

            if ("objeto".equals(classeReal)) {
                objetos.add(obj);
            }

            if (!chave.isEmpty()) {
                interativos.put(chave, obj);
            }
        }
    }

    // Carrega NPCs do Tiled
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

    // Verifica se uma posicao esta livre de colisoes
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

        for (ObjetoColisao obj : objetos) {
            if (!obj.isAtivo(umbra)) continue;
            if (rectTemp.overlaps(obj.area)) return false;
        }

        return true;
    }

    // Retorna a area de um objeto pelo nome
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

    // Retorna um objeto interativo completo pelo nome
    public ObjetoColisao getInterativo(String nome, boolean umbra) {
        String        chave = nome.toLowerCase();
        ObjetoColisao o     = interativos.get(chave);
        return (o != null && o.isAtivo(umbra)) ? o : null;
    }

    // Retorna um NPC pelo nome
    public EntidadeMapa getNpc(String nome, boolean umbra) {
        EntidadeMapa n = npcs.get(nome.toLowerCase());
        return (n != null && n.isAtivo(umbra)) ? n : null;
    }

    // Retorna a area do reflexo do espelho
    public Rectangle getReflexoArea(boolean umbra) {
        ObjetoColisao o = interativos.get("reflexo");
        return (o != null && o.isAtivo(umbra)) ? o.area : null;
    }

    // Retorna lista de paredes ativas no mundo
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

    // Retorna lista de hitboxes de porta
    public List<Rectangle> getHitboxPortas() {
        cacheParedes.clear();
        for (ObjetoColisao p : hitboxPortas) cacheParedes.add(p.area);
        return cacheParedes;
    }

    // Retorna lista completa de objetos de porta
    public List<ObjetoColisao> getHitboxPortasCompletas() {
        cachePortas.clear();
        cachePortas.addAll(hitboxPortas);
        return cachePortas;
    }

    // Retorna lista completa de objetos com classe objeto
    public List<ObjetoColisao> getObjetosColisaoCompletos() {
        cacheObjetos.clear();
        cacheObjetos.addAll(objetos);
        return cacheObjetos;
    }

    // Retorna mapa de interativos ativos
    public Map<String, Rectangle> getInterativos(boolean umbra) {
        cacheInterativos.clear();
        for (Map.Entry<String, ObjetoColisao> e : interativos.entrySet()) {
            if (e.getValue().isAtivo(umbra)) cacheInterativos.put(e.getKey(), e.getValue().area);
        }
        return cacheInterativos;
    }

    // Retorna mapa de interativos completos ativos
    public Map<String, ObjetoColisao> getInterativosCompletos(boolean umbra) {
        cacheInterativosCompletos.clear();
        for (Map.Entry<String, ObjetoColisao> e : interativos.entrySet()) {
            if (e.getValue().isAtivo(umbra)) cacheInterativosCompletos.put(e.getKey(), e.getValue());
        }
        return cacheInterativosCompletos;
    }

    // Retorna mapa de NPCs ativos
    public Map<String, EntidadeMapa> getNpcs(boolean umbra) {
        cacheNpcs.clear();
        for (Map.Entry<String, EntidadeMapa> e : npcs.entrySet()) {
            if (e.getValue().isAtivo(umbra)) cacheNpcs.put(e.getKey(), e.getValue());
        }
        return cacheNpcs;
    }

    // Retorna todas as paredes e portas para criar corpos Box2D
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

    // Retorna apenas paredes para sombra Box2D
    public List<Rectangle> getParedesBox2D() {
        List<Rectangle> lista = new ArrayList<>();
        for (ObjetoColisao p : paredes) {
            lista.add(p.area);
        }
        return lista;
    }

    // Retorna apenas hitboxes de porta para sombra Box2D
    public List<Rectangle> getPortasBox2D() {
        List<Rectangle> lista = new ArrayList<>();
        for (ObjetoColisao p : hitboxPortas) {
            lista.add(p.area);
        }
        return lista;
    }

    // Destranca uma porta pelo nome
    public void destrancar(String nome) {
        if (nome != null && !nome.isEmpty()) destrancados.add(nome.toLowerCase());
    }

    // Retorna o mapa de defaults
    public Map<String, Map<String, Object>> getDefaults() { return defaults; }

    // Verifica se uma porta foi destrancada
    public boolean isDestrancado(String nome) {
        return nome != null && !nome.isEmpty() && destrancados.contains(nome.toLowerCase());
    }
}
