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
import com.persecutio.entities.Jogador;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Sistema de colisao do mapa
public class GerenciadorColisao {

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
        // Area publica e mutavel para podermos empurrar os blocos
        public Rectangle area;
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

        public boolean isAtivo(boolean umbra) {
            return umbra ? noUmbra : noReal;
        }
    }

    private final List<ObjetoColisao> paredes;
    private final List<ObjetoColisao> hitboxPortas;
    private final List<ObjetoColisao> objetos;
    private final Map<String, ObjetoColisao> interativos;
    private final Map<String, EntidadeMapa> npcs;

    // Colecoes de pedras e objetivos lidos do Tiled
    private final Map<String, ObjetoColisao> mapaPedras    = new HashMap<>();
    private final Map<String, Rectangle>     mapaObjetivos = new HashMap<>();

    private final List<Rectangle> cacheParedes = new ArrayList<>();
    private final List<ObjetoColisao> cachePortas = new ArrayList<>();
    private final List<ObjetoColisao> cacheObjetos = new ArrayList<>();
    private final Map<String, Rectangle> cacheInterativos = new HashMap<>();
    private final Map<String, ObjetoColisao> cacheInterativosCompletos = new HashMap<>();
    private final Map<String, EntidadeMapa> cacheNpcs = new HashMap<>();

    private final Set<String> destrancados = new HashSet<>();
    private final Map<String, Map<String, Object>> defaults;

    private final Rectangle rectTemp = new Rectangle();
    private boolean colisoesDesativadas = false;

    // Associa o progresso do jogo
    private GerenciadorProgresso progresso;

    public void alternarColisoes() {
        colisoesDesativadas = !colisoesDesativadas;
    }

    public boolean isColisoesDesativadas() { return colisoesDesativadas; }

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

    // Define a referencia de progresso para a avaliacao de condicoes
    public void setProgresso(GerenciadorProgresso progresso) {
        this.progresso = progresso;
    }

    // Verifica se o objeto atende a condicao de existencia
    public boolean isObjetoAtivo(ObjetoColisao obj, boolean umbra) {
        if (!obj.isAtivo(umbra)) return false;
        if (obj.condicao == null || obj.condicao.trim().isEmpty()) return true;
        if (progresso == null) return true;
        return avaliarCondicao(obj.condicao, progresso.getMissao(), progresso.getPartes(), progresso.getDocumentos());
    }

    // Motor de avaliacao de expressoes logicas
    private boolean avaliarCondicao(String condicao, int missao, int partes, int documentos) {
        if (condicao == null || condicao.trim().isEmpty()) return true;
        String c = condicao.trim().replace(" ", "").toLowerCase();

        String operador = "";
        if (c.contains("==")) {
            operador = "==";
        } else if (c.contains(">=")) {
            operador = ">=";
        } else if (c.contains("<=")) {
            operador = "<=";
        } else if (c.contains(">")) {
            operador = ">";
        } else if (c.contains("<")) {
            operador = "<";
        } else {
            return false;
        }

        String[] p = c.split(operador, 2);
        if (p.length != 2) return false;
        String key = p[0];
        String val = p[1];

        try {
            int valorInt = Integer.parseInt(val);
            int varValor;
            switch (key) {
                case "partes":     varValor = partes; break;
                case "missao":     varValor = missao; break;
                case "documentos": varValor = documentos; break;
                default:           return false;
            }

            switch (operador) {
                case "==": return varValor == valorInt;
                case ">=": return varValor >= valorInt;
                case "<=": return varValor <= valorInt;
                case ">":  return varValor > valorInt;
                case "<":  return varValor < valorInt;
                default:   return false;
            }
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String rChave(MapObject objeto) {
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

    private String lerChave(MapObject objeto) {
        return rChave(objeto);
    }

    private String lerClasse(MapObject objeto) {
        MapProperties props = objeto.getProperties();
        String classe = props.get("type") != null ? props.get("type").toString() :
                        props.get("class") != null ? props.get("class").toString() : "";
        return classe.trim().toLowerCase();
    }

    // Carrega paredes e obstaculos estaticos do mapa
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

            if ("objeto".equals(classeReal) || "interativos".equals(classeReal) || "interativo".equals(classeReal)) {
                objetos.add(obj);
            } else if (chave.startsWith("pedra")) {
                // Alinha as coordenadas iniciais da pedra para o tile mais proximo
                ajustarAoTile(obj.area);
                mapaPedras.put(chave, obj);
            } else if (chave.startsWith("objetivo")) {
                Rectangle rMundo = CoordenadasTiled.paraMundo(r);
                // Alinha as coordenadas iniciais do objetivo para o tile mais proximo
                ajustarAoTile(rMundo);
                mapaObjetivos.put(chave, rMundo);
            }

            if (!chave.isEmpty()) {
                interativos.put(chave, obj);
            }
        }
    }

    private void carregarNpcs(TiledMap mapa, String camadaNome) {
        MapLayer camada = mapa.getLayers().get(camadaNome);
        if (camada == null) return;
        for (MapObject objeto : camada.getObjects()) {
            if (!(objeto instanceof TiledMapTileMapObject)) continue;
            TiledMapTileMapObject tileObj = (TiledMapTileMapObject) objeto;
            String classOrig = lerChave(objeto);
            if (classOrig.isEmpty()) continue;

            TextureRegion textura = tileObj.getTextureRegion();
            float escala  = CoordenadasTiled.getEscala();
            float largura = textura.getRegionWidth()  * tileObj.getScaleX() * escala;
            float altura  = textura.getRegionHeight() * tileObj.getScaleY() * escala;
            float x       = tileObj.getX() * escala;
            float y       = tileObj.getY() * escala;

            npcs.put(classOrig, new EntidadeMapa(
                classOrig,
                new Rectangle(x, y, largura, altura),
                textura,
                objeto.getProperties(),
                false));
        }
    }

    // Alinha a hitbox de um objeto/objetivo ao grid de tiles mais proximo de forma matematica
    private void ajustarAoTile(Rectangle r) {
        float tamanhoTile = 32f * 1.375f; // 44f
        r.x = Math.round(r.x / tamanhoTile) * tamanhoTile;
        r.y = Math.round(r.y / tamanhoTile) * tamanhoTile;
    }

    // Retorna a pedra que o jogador esta encarando no mundo real para interacao
    public ObjetoColisao acharPedraEncarada(Jogador jogador, boolean umbra) {
        if (umbra) return null; // Só no mundo real as pedras sao empurradas

        Rectangle rectInteracao = new Rectangle(jogador.hitbox);
        float folga = 16f;
        int dir = jogador.getDirecao();
        if      (dir == Jogador.DIRECAO_DIREITA)  rectInteracao.x += folga;
        else if (dir == Jogador.DIRECAO_ESQUERDA) rectInteracao.x -= folga;
        else if (dir == Jogador.DIRECAO_CIMA)     rectInteracao.y += folga;
        else if (dir == Jogador.DIRECAO_BAIXO)    rectInteracao.y -= folga;

        for (ObjetoColisao pedra : mapaPedras.values()) {
            if (rectInteracao.overlaps(pedra.area)) {
                return pedra;
            }
        }
        return null;
    }

    // Empurra a pedra em 1 tile na direcao que o jogador esta virado se o caminho estiver livre
    public boolean empurrarPedra(Jogador jogador, ObjetoColisao pedra, boolean umbra) {
        int dir = jogador.getDirecao();
        float tamanhoTile = 32f * 1.375f; // 44f
        float novoX = pedra.area.x;
        float novoY = pedra.area.y;

        if      (dir == Jogador.DIRECAO_DIREITA)  novoX += tamanhoTile;
        else if (dir == Jogador.DIRECAO_ESQUERDA) novoX -= tamanhoTile;
        else if (dir == Jogador.DIRECAO_CIMA)     novoY += tamanhoTile;
        else if (dir == Jogador.DIRECAO_BAIXO)    novoY -= tamanhoTile;

        // Verifica colisao no destino, ignorando a si mesma
        if (verificarPosicao(novoX, novoY, pedra.area.width, pedra.area.height, umbra, pedra)) {
            pedra.area.setPosition(novoX, novoY);
            return true;
        }
        return false;
    }

    // Verifica se as 3 pedras estao overlapped com seus respectivos objetivos
    public boolean puzzleResolvido() {
        for (int i = 1; i <= 3; i++) {
            ObjetoColisao pedra = mapaPedras.get("pedra" + i);
            Rectangle obj = mapaObjetivos.get("objetivo" + i);
            if (pedra == null || obj == null) return false;
            if (!pedra.area.overlaps(obj)) return false;
        }
        return true;
    }

    public boolean verificarPosicao(float proximoX, float proximoY, float largura, float altura, boolean umbra) {
        return verificarPosicao(proximoX, proximoY, largura, altura, umbra, null);
    }

    // Verificacao considerando obstaculos e permitindo ignorar um objeto proprio (para empurrar blocos sem auto-colisao)
    public boolean verificarPosicao(float proximoX, float proximoY, float largura, float altura, boolean umbra, ObjetoColisao ignorar) {
        if (colisoesDesativadas) return true;

        rectTemp.set(proximoX, proximoY, largura, altura);

        for (ObjetoColisao parede : paredes) {
            if (!isObjetoAtivo(parede, umbra)) continue;
            if (rectTemp.overlaps(parede.area)) return false;
        }

        for (ObjetoColisao porta : hitboxPortas) {
            if (!isObjetoAtivo(porta, umbra)) continue;
            if (rectTemp.overlaps(porta.area)) return false;
        }

        for (ObjetoColisao obj : objetos) {
            if (obj == ignorar) continue;
            if (!isObjetoAtivo(obj, umbra)) continue;
            if (rectTemp.overlaps(obj.area)) return false;
        }

        for (ObjetoColisao pedra : mapaPedras.values()) {
            if (pedra == ignorar) continue;
            if (!isObjetoAtivo(pedra, umbra)) continue;
            if (rectTemp.overlaps(pedra.area)) return false;
        }

        return true;
    }

    public Rectangle getArea(String nome, boolean umbra) {
        String chave = nome.toLowerCase();

        if (interativos.containsKey(chave)) {
            ObjetoColisao o = interativos.get(chave);
            return (o != null && isObjetoAtivo(o, umbra)) ? o.area : null;
        }

        if (npcs.containsKey(chave)) {
            EntidadeMapa n = npcs.get(chave);
            return (n != null && n.isAtivo(umbra)) ? n.area : null;
        }

        return null;
    }

    public ObjetoColisao getInterativo(String nome, boolean umbra) {
        String        chave = nome.toLowerCase();
        ObjetoColisao o     = interativos.get(chave);
        return (o != null && isObjetoAtivo(o, umbra)) ? o : null;
    }

    public EntidadeMapa getNpc(String nome, boolean umbra) {
        EntidadeMapa n = npcs.get(nome.toLowerCase());
        return (n != null && n.isAtivo(umbra)) ? n : null;
    }

    public Rectangle getReflexoArea(boolean umbra) {
        ObjetoColisao o = interativos.get("reflexo");
        return (o != null && isObjetoAtivo(o, umbra)) ? o.area : null;
    }

    public List<Rectangle> getParedes(boolean umbra) {
        cacheParedes.clear();
        for (ObjetoColisao p : paredes) {
            if (isObjetoAtivo(p, umbra)) cacheParedes.add(p.area);
        }
        for (ObjetoColisao p : hitboxPortas) {
            cacheParedes.add(p.area);
        }
        return cacheParedes;
    }

    public List<Rectangle> getHitboxPortas() {
        cacheParedes.clear();
        for (ObjetoColisao p : hitboxPortas) cacheParedes.add(p.area);
        return cacheParedes;
    }

    public List<ObjetoColisao> getHitboxPortasCompletas() {
        cachePortas.clear();
        cachePortas.addAll(hitboxPortas);
        return cachePortas;
    }

    public List<ObjetoColisao> getObjetosColisaoCompletos() {
        cacheObjetos.clear();
        cacheObjetos.addAll(objetos);
        return cacheObjetos;
    }

    public Map<String, ObjetoColisao> getMapaPedras()    { return mapaPedras; }
    public Map<String, Rectangle>     getMapaObjetivos() { return mapaObjetivos; }

    public Map<String, Rectangle> getInterativos(boolean umbra) {
        cacheInterativos.clear();
        for (Map.Entry<String, ObjetoColisao> e : interativos.entrySet()) {
            if (isObjetoAtivo(e.getValue(), umbra)) cacheInterativos.put(e.getKey(), e.getValue().area);
        }
        return cacheInterativos;
    }

    public Map<String, ObjetoColisao> getInterativosCompletos(boolean umbra) {
        cacheInterativosCompletos.clear();
        for (Map.Entry<String, ObjetoColisao> e : interativos.entrySet()) {
            if (isObjetoAtivo(e.getValue(), umbra)) cacheInterativosCompletos.put(e.getKey(), e.getValue());
        }
        return cacheInterativosCompletos;
    }

    public Map<String, EntidadeMapa> getNpcs(boolean umbra) {
        cacheNpcs.clear();
        for (Map.Entry<String, EntidadeMapa> e : npcs.entrySet()) {
            if (e.getValue().isAtivo(umbra)) cacheNpcs.put(e.getKey(), e.getValue());
        }
        return cacheNpcs;
    }

    public List<Rectangle> getTodasParedesBox2D() {
        List<Rectangle> todas = new ArrayList<>();
        for (ObjetoColisao p : paredes) todas.add(p.area);
        for (ObjetoColisao p : hitboxPortas) todas.add(p.area);
        for (ObjetoColisao p : mapaPedras.values()) todas.add(p.area);
        return todas;
    }

    public List<Rectangle> getParedesBox2D() {
        List<Rectangle> lista = new ArrayList<>();
        for (ObjetoColisao p : paredes) lista.add(p.area);
        for (ObjetoColisao p : mapaPedras.values()) lista.add(p.area);
        return lista;
    }

    public List<Rectangle> getPortasBox2D() {
        List<Rectangle> lista = new ArrayList<>();
        for (ObjetoColisao p : hitboxPortas) lista.add(p.area);
        return lista;
    }

    public void destrancar(String nome) {
        if (nome != null && !nome.isEmpty()) destrancados.add(nome.toLowerCase());
    }

    public Map<String, Map<String, Object>> getDefaults() { return defaults; }

    public boolean isDestrancado(String nome) {
        return nome != null && !nome.isEmpty() && destrancados.contains(nome.toLowerCase());
    }
}
