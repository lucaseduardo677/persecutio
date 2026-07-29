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

    private static boolean pegarDefault(Map<String, Map<String, Object>> defaults,
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
        // Mundo de origem do objeto definido estritamente pelo mapa em que foi lido
        public final boolean   mundoUmbra;
        public final boolean   trancado;
        public final boolean   destrancavel;
        public final String    condicao;
        // Chave da imagem asset do documento lida do Tiled propriedade docId
        public final String    docId;
        public final boolean   realAtivo;
        public final boolean   umbraAtiva;
        public final boolean   realExplicito;
        public final boolean   umbraExplicita;
        public TextureRegion textura = null;
        public float rotacao = 0f;

        public ObjetoColisao(Rectangle area, String nome, MapProperties props,
                             Map<String, Map<String, Object>> defaults, boolean isUmbraMap) {
            this.area = area;
            this.nome = (nome != null && !nome.isEmpty()) ? nome :
                        ((props.get("name") != null) ? props.get("name").toString() :
                         (props.get("nome") != null) ? props.get("nome").toString() : "");

            String classe = props.get("type")  != null ? props.get("type").toString()  :
                            props.get("class") != null ? props.get("class").toString() : "";

            // O mundo do objeto e sempre o mapa de onde ele foi lido sem flag configuravel
            this.mundoUmbra = isUmbraMap;

            Object t = props.get("trancado");
            this.trancado = (t != null) ? Boolean.parseBoolean(t.toString())
                                        : pegarDefault(defaults, classe, "trancado", false);

            Object d = props.get("destrancavel");
            this.destrancavel = (d != null) ? Boolean.parseBoolean(d.toString())
                                            : pegarDefault(defaults, classe, "destrancavel", false);

            Object c = props.get("condicao");
            this.condicao = (c != null) ? c.toString() : "";

            Object doc = props.get("docId");
            this.docId = (doc != null) ? doc.toString() : "";

            Object realProp = props.get("real");
            this.realExplicito = props.containsKey("real");
            this.realAtivo = (realProp instanceof Boolean) ? (Boolean) realProp : true;
            Object umbraProp = props.get("umbra");
            this.umbraExplicita = props.containsKey("umbra");
            this.umbraAtiva = (umbraProp instanceof Boolean) ? (Boolean) umbraProp : true;
        }

        public boolean checarAtivo(boolean umbra) {
            if (realExplicito || umbraExplicita) {
                return umbra ? umbraAtiva : realAtivo;
            }
            return mundoUmbra == umbra;
        }
    }

    private final List<ObjetoColisao> paredes;
    private final List<ObjetoColisao> hitboxPortas;
    private final List<ObjetoColisao> objetos;
    private final Map<String, ObjetoColisao> interativos;
    private final Map<String, EntidadeMapa> npcs;
    private final List<ObjetoColisao> objetosDesenhaveis;

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

    public boolean colisaoDesativada() { return colisoesDesativadas; }

    public GerenciadorColisao(TiledMap mapaReal, TiledMap mapaUmbra, float escala, String caminhoProjeto) {
        CoordenadasTiled.setEscala(escala);
        defaults     = lerDefaults(caminhoProjeto);

        paredes      = new ArrayList<>();
        hitboxPortas = new ArrayList<>();
        objetos      = new ArrayList<>();
        interativos  = new HashMap<>();
        npcs         = new HashMap<>();
        objetosDesenhaveis = new ArrayList<>();

        // Separa as colisões conforme o mapa de origem
        if (mapaReal != null) {
            lerParedes(mapaReal, "Colisoes", paredes, false);
            lerInterativos(mapaReal, "Interativos", false);
            lerInterativos(mapaReal, "Objetos", false);
            lerNpcs(mapaReal, "NPCs", false);
            lerNpcs(mapaReal, "NPC", false);
            lerParedes(mapaReal, "Portas", hitboxPortas, false);
        }

        if (mapaUmbra != null) {
            lerParedes(mapaUmbra, "Colisoes", paredes, true);
            lerInterativos(mapaUmbra, "Interativos", true);
            lerInterativos(mapaUmbra, "Objetos", true);
            lerNpcs(mapaUmbra, "NPCs", true);
            lerNpcs(mapaUmbra, "NPC", true);
            lerParedes(mapaUmbra, "Portas", hitboxPortas, true);
        }
    }

    public void setProgresso(GerenciadorProgresso progresso) {
        this.progresso = progresso;
    }

    public boolean isUmbra() {
        return progresso != null && progresso.isUmbra();
    }

    public boolean checarAtivo(ObjetoColisao obj) {
        boolean umbra = isUmbra();
        if (!obj.checarAtivo(umbra)) return false;
        if (obj.condicao == null || obj.condicao.trim().isEmpty()) return true;
        if (progresso == null) return true;
        return avaliarCondicao(obj.condicao, progresso.getMissao(),
                               progresso.getDocumentos(), progresso.obterFase());
    }

    // Suporta condicoes compostas unidas por ex missao 1 fasemissao 5
    private boolean avaliarCondicao(String condicao, int missao, int documentos, int faseMissao) {
        if (condicao == null || condicao.trim().isEmpty()) return true;
        String c = condicao.trim().replace(" ", "").toLowerCase();

        for (String parte : c.split("&&")) {
            if (!checarSub(parte, missao, documentos, faseMissao)) return false;
        }
        return true;
    }

    private boolean checarSub(String c, int missao, int documentos, int faseMissao) {
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
            // Avaliacao limpa e generica para flags dinamicas do progresso sem codigo hardcoded
            if (progresso != null) {
                return progresso.temFlag(c);
            }
            return false;
        }

        String[] p = c.split(operador, 2);
        if (p.length != 2) return false;
        String key = p[0];
        String val = p[1];

        // Se for uma comparacao logica de booleano literal na flag
        if ("true".equals(val) || "false".equals(val)) {
            boolean boolVal = Boolean.parseBoolean(val);
            if (progresso != null) {
                return progresso.temFlag(key) == boolVal;
            }
            return false;
        }

        try {
            int valorInt = Integer.parseInt(val);
            int varValor;
            switch (key) {
                case "missao":     varValor = missao; break;
                case "documentos": varValor = documentos; break;
                case "fasemissao": varValor = faseMissao; break;
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

    private String obterChave(MapObject objeto) {
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

        if (chave == null || chave.trim().isEmpty()) {
            String classe = objeto.getProperties().get("class", String.class);
            if (classe != null && !classe.trim().isEmpty() && !"objeto".equalsIgnoreCase(classe)) {
                chave = classe;
            }
        }
        if (chave == null || chave.trim().isEmpty()) {
            String tipo = objeto.getProperties().get("type", String.class);
            if (tipo != null && !tipo.trim().isEmpty() && !"objeto".equalsIgnoreCase(tipo)) {
                chave = tipo;
            }
        }

        return chave != null ? chave.trim().toLowerCase() : "";
    }

    private String lerChave(MapObject objeto) {
        return obterChave(objeto);
    }

    private String lerClasse(MapObject objeto) {
        MapProperties props = objeto.getProperties();
        String classOrig = props.get("type") != null ? props.get("type").toString() :
                           props.get("class") != null ? props.get("class").toString() : "";
        return classOrig.trim().toLowerCase();
    }

    // Leitura segura de floats para o parser de dimensoes de Gid do Tiled
    private float lerFloat(MapProperties props, String chave, float padrao) {
        Object val = props.get(chave);
        if (val instanceof Number) {
            return ((Number) val).floatValue();
        }
        return padrao;
    }

    private void lerParedes(TiledMap mapa, String camadaNome, List<ObjetoColisao> lista, boolean isUmbraMap) {
        MapLayer camada = mapa.getLayers().get(camadaNome);
        if (camada == null) return;
        for (MapObject objeto : camada.getObjects()) {
            if (!(objeto instanceof RectangleMapObject)) continue;
            Rectangle r     = ((RectangleMapObject) objeto).getRectangle();
            String    chave = lerChave(objeto);
            if ("Portas".equals(camadaNome) && !GerenciadorPortas.deveManterPorta(chave)) {
                continue;
            }
            lista.add(new ObjetoColisao(CoordenadasTiled.paraMundo(r), chave,
                                        objeto.getProperties(), defaults, isUmbraMap));
        }
    }

    private void lerInterativos(TiledMap mapa, String camadaNome, boolean isUmbraMap) {
        MapLayer camada = mapa.getLayers().get(camadaNome);
        if (camada == null) return;

        for (MapObject objeto : camada.getObjects()) {
            Rectangle r = null;
            TextureRegion tex = null;
            float rot = 0f;

            if (objeto instanceof RectangleMapObject) {
                r = ((RectangleMapObject) objeto).getRectangle();
            } else if (objeto instanceof TiledMapTileMapObject) {
                TiledMapTileMapObject tmt = (TiledMapTileMapObject) objeto;
                float w = lerFloat(tmt.getProperties(), "width", tmt.getTile().getTextureRegion().getRegionWidth() * tmt.getScaleX());
                float h = lerFloat(tmt.getProperties(), "height", tmt.getTile().getTextureRegion().getRegionHeight() * tmt.getScaleY());
                r = new Rectangle(tmt.getX(), tmt.getY(), w, h);
                tex = tmt.getTextureRegion();
                rot = tmt.getRotation();
            }
            if (r == null) continue;

            String chave = lerChave(objeto);
            String classeReal = lerClasse(objeto);

            ObjetoColisao obj = new ObjetoColisao(CoordenadasTiled.paraMundo(r), chave,
                                                  objeto.getProperties(), defaults, isUmbraMap);
            obj.textura = tex;
            obj.rotacao = rot;

            if (tex != null) {
                objetosDesenhaveis.add(obj);
            }

            // Apenas objetos estáticos puros são adicionados à colisão física sólida.
            // Exceções como o trigger de Elimar2 precisam entrar no pool de interativos,
            // mesmo sem se comportarem como parede ou objeto sólido.
            if ("objeto".equals(classeReal)) {
                objetos.add(obj);
            }
            if ("elimar2".equals(chave) || "interativos".equals(classeReal)) {
                interativos.put(chave.isEmpty() ? "elimar2" : chave, obj);
            }

            if (chave.startsWith("pedra")) {
                alinharTile(obj.area);
                mapaPedras.put(chave, obj);
            } else if (chave.startsWith("objetivo")) {
                Rectangle rMundo = new Rectangle(obj.area);
                alinharTile(rMundo);
                mapaObjetivos.put(chave, rMundo);
            }

            if (!chave.isEmpty()) {
                // Preserva objetos com nomes duplicados usando um sufixo único
                if (interativos.containsKey(chave)) {
                    ObjetoColisao existente = interativos.get(chave);
                    Integer idProp = objeto.getProperties().get("id", Integer.class);
                    String sufixo = chave + "_" + (idProp != null ? idProp : obj.hashCode());

                    if (existente.textura == null && obj.textura != null) {
                        interativos.put(sufixo, existente);
                        interativos.put(chave, obj);
                    } else {
                        interativos.put(sufixo, obj);
                    }
                } else {
                    interativos.put(chave, obj);
                }
            }
        }
    }

    private void lerNpcs(TiledMap mapa, String camadaNome, boolean isUmbraMap) {
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
                isUmbraMap));
        }
    }

    // Alinha unicamente os obstaculos pesados puzzle de pedras ao Grid
    private void alinharTile(Rectangle r) {
        float tamanhoTile = 32f * 1.375f;
        r.x = Math.round(r.x / tamanhoTile) * tamanhoTile;
        r.y = Math.round(r.y / tamanhoTile) * tamanhoTile;
    }

    public ObjetoColisao acharPedra(Jogador jogador) {
        // Permite empurrar pedras apenas no mundo Real
        if (isUmbra()) return null;

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

    public boolean empurrarPedra(Jogador jogador, ObjetoColisao pedra) {
        int dir = jogador.getDirecao();
        float tamanhoTile = 32f * 1.375f;
        float novoX = pedra.area.x;
        float novoY = pedra.area.y;

        if      (dir == Jogador.DIRECAO_DIREITA)  novoX += tamanhoTile;
        else if (dir == Jogador.DIRECAO_ESQUERDA) novoX -= tamanhoTile;
        else if (dir == Jogador.DIRECAO_CIMA)     novoY += tamanhoTile;
        else if (dir == Jogador.DIRECAO_BAIXO)    novoY -= tamanhoTile;

        if (checarPosicao(novoX, novoY, pedra.area.width, pedra.area.height, pedra)) {
            pedra.area.setPosition(novoX, novoY);
            // Se o puzzle estiver resolvido apos mover a pedra notifica o progresso
            try {
                if (puzzleResolvido() && progresso != null) {
                    progresso.onPuzzleSolved();
                }
            } catch (Exception ignored) {
                // Protege contra efeitos colaterais inesperados durante verificacao
            }
            return true;
        }
        return false;
    }

    public boolean puzzleResolvido() {
        for (int i = 1; i <= 3; i++) {
            ObjetoColisao pedra = mapaPedras.get("pedra" + i);
            Rectangle obj = mapaObjetivos.get("objetivo" + i);
            if (pedra == null || obj == null) return false;
            if (!pedra.area.overlaps(obj)) return false;
        }
        return true;
    }

    public boolean checarPosicao(float proximoX, float proximoY, float largura, float altura) {
        return checarPosicao(proximoX, proximoY, largura, altura, null);
    }

    public boolean checarPosicao(float proximoX, float proximoY, float largura, float altura, ObjetoColisao ignorar) {
        if (colisoesDesativadas) return true;

        rectTemp.set(proximoX, proximoY, largura, altura);

        for (ObjetoColisao parede : paredes) {
            if (!checarAtivo(parede)) continue;
            if (rectTemp.overlaps(parede.area)) return false;
        }

        for (ObjetoColisao porta : hitboxPortas) {
            if (!checarAtivo(porta)) continue;
            if (rectTemp.overlaps(porta.area)) return false;
        }

        for (ObjetoColisao obj : objetos) {
            if (obj == ignorar) continue;
            if (!checarAtivo(obj)) continue;
            if (rectTemp.overlaps(obj.area)) return false;
        }

        for (ObjetoColisao pedra : mapaPedras.values()) {
            if (pedra == ignorar) continue;
            if (!checarAtivo(pedra)) continue;
            if (rectTemp.overlaps(pedra.area)) return false;
        }

        return true;
    }

    private ObjetoColisao buscarInterativo(String chave) {
        ObjetoColisao o = interativos.get(chave);
        if (o != null && checarAtivo(o)) return o;

        String prefixo = chave + "_";
        for (Map.Entry<String, ObjetoColisao> entry : interativos.entrySet()) {
            if (entry.getKey().startsWith(prefixo)) {
                ObjetoColisao candidato = entry.getValue();
                if (candidato != null && checarAtivo(candidato)) {
                    return candidato;
                }
            }
        }

        for (Map.Entry<String, ObjetoColisao> entry : interativos.entrySet()) {
            String key = entry.getKey();
            if (!key.equals(chave) && key.startsWith(chave)) {
                ObjetoColisao candidato = entry.getValue();
                if (candidato != null && checarAtivo(candidato)) {
                    return candidato;
                }
            }
        }
        return null;
    }

    public Rectangle getArea(String nome) {
        String chave = nome.toLowerCase();

        ObjetoColisao o = buscarInterativo(chave);
        if (o != null) return o.area;

        if (npcs.containsKey(chave)) {
            EntidadeMapa n = npcs.get(chave);
            return (n != null && n.isAtivo(isUmbra())) ? n.area : null;
        }

        return null;
    }

    public ObjetoColisao getInterativo(String nome) {
        String chave = nome.toLowerCase();
        return buscarInterativo(chave);
    }

    public EntidadeMapa getNpc(String nome) {
        EntidadeMapa n = npcs.get(nome.toLowerCase());
        return (n != null && n.isAtivo(isUmbra())) ? n : null;
    }

    public Rectangle areaReflexo() {
        ObjetoColisao o = interativos.get("reflexo");
        return (o != null && checarAtivo(o)) ? o.area : null;
    }

    public List<Rectangle> getParedes() {
        cacheParedes.clear();
        for (ObjetoColisao p : paredes) {
            if (checarAtivo(p)) cacheParedes.add(p.area);
        }
        for (ObjetoColisao p : hitboxPortas) {
            cacheParedes.add(p.area);
        }
        return cacheParedes;
    }

    public List<Rectangle> obterPortas() {
        cacheParedes.clear();
        for (ObjetoColisao p : hitboxPortas) cacheParedes.add(p.area);
        return cacheParedes;
    }

    public List<ObjetoColisao> portasCompletas() {
        cachePortas.clear();
        cachePortas.addAll(hitboxPortas);
        return cachePortas;
    }

    public List<ObjetoColisao> obterObjetos() {
        cacheObjetos.clear();
        cacheObjetos.addAll(objetos);
        return cacheObjetos;
    }

    public Map<String, ObjetoColisao> mapaPedras()    { return mapaPedras; }
    public Map<String, Rectangle>     mapObjetivos()  { return mapaObjetivos; }

    public Map<String, Rectangle> getInterativos() {
        cacheInterativos.clear();
        for (Map.Entry<String, ObjetoColisao> e : interativos.entrySet()) {
            if (checarAtivo(e.getValue())) cacheInterativos.put(e.getKey(), e.getValue().area);
        }
        return cacheInterativos;
    }

    public Map<String, ObjetoColisao> todosInterativos() {
        cacheInterativosCompletos.clear();
        for (Map.Entry<String, ObjetoColisao> e : interativos.entrySet()) {
            if (checarAtivo(e.getValue())) cacheInterativosCompletos.put(e.getKey(), e.getValue());
        }
        return cacheInterativosCompletos;
    }

    // Localiza documentos e panfletos dentro da área informada
    public ObjetoColisao acharDoc(Rectangle hitbox) {
        for (ObjetoColisao d : todosInterativos().values()) {
            String nomeBase = d.nome.toLowerCase();
            if (nomeBase.startsWith("documento") || nomeBase.startsWith("panfleto")
                    || nomeBase.startsWith("planfeto") || nomeBase.startsWith("objeto")) {
                if (hitbox.overlaps(d.area)) return d;
            }
        }
        return null;
    }

    public Map<String, EntidadeMapa> getNpcs() {
        cacheNpcs.clear();
        boolean umbra = isUmbra();
        for (Map.Entry<String, EntidadeMapa> e : npcs.entrySet()) {
            if (e.getValue().isAtivo(umbra)) cacheNpcs.put(e.getKey(), e.getValue());
        }
        return cacheNpcs;
    }

    public List<Rectangle> todasParedes() {
        List<Rectangle> todas = new ArrayList<>();
        for (ObjetoColisao p : paredes) todas.add(p.area);
        for (ObjetoColisao p : hitboxPortas) todas.add(p.area);
        for (ObjetoColisao p : mapaPedras.values()) todas.add(p.area);
        return todas;
    }

    public List<Rectangle> paredesBox() {
        List<Rectangle> lista = new ArrayList<>();
        for (ObjetoColisao p : paredes) lista.add(p.area);
        for (ObjetoColisao p : mapaPedras.values()) lista.add(p.area);
        return lista;
    }

    public List<Rectangle> portasBox() {
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

    public List<ObjetoColisao> obterDesenhaveis() { return objetosDesenhaveis; }
}
