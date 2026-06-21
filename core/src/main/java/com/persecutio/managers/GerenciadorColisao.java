package com.persecutio.managers;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.objects.TiledMapTileMapObject;
import com.badlogic.gdx.math.Rectangle;
import com.persecutio.entities.EntidadeMapa;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// carrega e mantém todas as áreas de colisão, interativos e NPCs lidos do Tiled
public class GerenciadorColisao {

    // representa qualquer objeto com área retangular e presença por mundo
    public static class ObjetoColisao {
        public final Rectangle area;
        public final String    nome;
        public final boolean   noUmbra;
        public final boolean   noReal;
        public final boolean   trancado;
        public final boolean   destrancavel;
        public final String    condicao;

        public ObjetoColisao(Rectangle area, String nome, MapProperties props, boolean padraoUmbra) {
            // prioridade: nome do objeto, depois propriedades "name"/"nome"
            this.area = area;
            this.nome = (nome != null && !nome.isEmpty()) ? nome :
                        ((props.get("name") != null) ? props.get("name").toString() :
                         (props.get("nome") != null) ? props.get("nome").toString() : "");

            // usa o padrão da camada quando a propriedade não está definida no objeto
            Object u = props.get("umbra");
            Object r = props.get("real");
            this.noUmbra = (u != null) ? Boolean.parseBoolean(u.toString()) : padraoUmbra;
            this.noReal  = (r != null) ? Boolean.parseBoolean(r.toString()) : !padraoUmbra;

            Object t = props.get("trancado");
            this.trancado = (t != null) ? Boolean.parseBoolean(t.toString()) : false;

            Object d = props.get("destrancavel");
            this.destrancavel = (d != null) ? Boolean.parseBoolean(d.toString()) : false;

            Object c = props.get("condicao");
            this.condicao = (c != null) ? c.toString() : "";
        }

        // retorna true se o objeto deve existir no mundo atual
        public boolean isAtivo(boolean umbra) {
            return umbra ? noUmbra : noReal;
        }
    }

    // paredes de colisão separadas por mundo
    private final List<ObjetoColisao> paredesReal;
    private final List<ObjetoColisao> paredesUmbra;

    // hitboxes físicas das portas, sempre bloqueiam independente do mundo
    private final List<ObjetoColisao> hitboxPortas;

    // áreas de interação indexadas pelo nome do objeto
    private final Map<String, ObjetoColisao> interativosReal;
    private final Map<String, ObjetoColisao> interativosUmbra;

    // NPCs com textura e área
    private final Map<String, EntidadeMapa> npcsReal;
    private final Map<String, EntidadeMapa> npcsUmbra;

    // nomes das portas que foram destrancadas durante a sessão
    private final Set<String> destrancados = new HashSet<>();

    public GerenciadorColisao(TiledMap mapa, float escala) {
        CoordenadasTiled.setEscala(escala);

        paredesReal      = new ArrayList<>();
        paredesUmbra     = new ArrayList<>();
        hitboxPortas     = new ArrayList<>();
        interativosReal  = new HashMap<>();
        interativosUmbra = new HashMap<>();
        npcsReal         = new HashMap<>();
        npcsUmbra        = new HashMap<>();

        // camadas do Tiled com padrão de mundo conforme o sufixo
        carregarParedes(mapa, "Colisoes",            paredesReal,      false);
        carregarParedes(mapa, "Colisoes_Umbra",      paredesUmbra,     true);
        carregarInterativos(mapa, "Interativos",       interativosReal,  false);
        carregarInterativos(mapa, "Interativos_Umbra", interativosUmbra, true);
        carregarNpcs(mapa, "NPCs",                   npcsReal,         false);
        carregarNpcs(mapa, "NPCs_Umbra",             npcsUmbra,        true);

        // portas são carregadas como paredes mas ficam em lista separada
        carregarParedes(mapa, "Portas", hitboxPortas, false);
    }

    // extrai o identificador do objeto tentando várias propriedades em ordem
    private String lerChave(MapObject objeto) {
        String chave = objeto.getName();
        if (chave == null || chave.trim().isEmpty())
            chave = objeto.getProperties().get("name", String.class);
        if (chave == null || chave.trim().isEmpty())
            chave = objeto.getProperties().get("nome", String.class);

        // para objetos tile, tenta as propriedades do tile em si
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

    // lê objetos retangulares de uma camada e os adiciona à lista de colisão
    private void carregarParedes(TiledMap mapa, String camadaNome,
                                 List<ObjetoColisao> lista, boolean padraoUmbra) {
        MapLayer camada = mapa.getLayers().get(camadaNome);
        if (camada == null) return;
        for (MapObject objeto : camada.getObjects()) {
            if (!(objeto instanceof RectangleMapObject)) continue;
            Rectangle r   = ((RectangleMapObject) objeto).getRectangle();
            String    chave = lerChave(objeto);
            lista.add(new ObjetoColisao(CoordenadasTiled.paraMundo(r), chave,
                                        objeto.getProperties(), padraoUmbra));
        }
    }

    // lê objetos retangulares de uma camada e os indexa por nome para interação
    private void carregarInterativos(TiledMap mapa, String camadaNome,
                                     Map<String, ObjetoColisao> mapaDestino, boolean padraoUmbra) {
        MapLayer camada = mapa.getLayers().get(camadaNome);
        if (camada == null) return;
        for (MapObject objeto : camada.getObjects()) {
            if (!(objeto instanceof RectangleMapObject)) continue;
            String classe = lerChave(objeto);
            if (classe.isEmpty()) continue;
            Rectangle r = ((RectangleMapObject) objeto).getRectangle();
            mapaDestino.put(classe, new ObjetoColisao(CoordenadasTiled.paraMundo(r), classe,
                                                      objeto.getProperties(), padraoUmbra));
        }
    }

    // lê objetos tile de uma camada de NPCs e os indexa por nome
    private void carregarNpcs(TiledMap mapa, String camadaNome,
                               Map<String, EntidadeMapa> mapaDestino, boolean padraoUmbra) {
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

            mapaDestino.put(classe, new EntidadeMapa(
                classe,
                new Rectangle(x, y, largura, altura),
                textura,
                objeto.getProperties(),
                padraoUmbra));
        }
    }

    // retorna false se a posição futura colide com qualquer obstáculo do mundo atual
    public boolean verificarPosicao(float proximoX, float proximoY,
                                    float largura, float altura, boolean umbra) {
        Rectangle hj = new Rectangle(proximoX, proximoY, largura, altura);

        for (ObjetoColisao parede : paredesReal) {
            if (!parede.isAtivo(umbra)) continue;
            if (hj.overlaps(parede.area)) return false;
        }

        // portas bloqueiam em qualquer mundo, o jogador só passa pela animação
        for (ObjetoColisao porta : hitboxPortas) {
            if (hj.overlaps(porta.area)) return false;
        }

        if (umbra) {
            for (ObjetoColisao parede : paredesUmbra) {
                if (!parede.isAtivo(umbra)) continue;
                if (hj.overlaps(parede.area)) return false;
            }
        }

        return true;
    }

    // retorna a área de um interativo ou NPC pelo nome, ou null se não encontrado
    public Rectangle getArea(String nome, boolean umbra) {
        String chave = nome.toLowerCase();
        Map<String, ObjetoColisao> interativos = umbra ? interativosUmbra : interativosReal;
        Map<String, EntidadeMapa>  npcs        = umbra ? npcsUmbra        : npcsReal;

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

    public ObjetoColisao getInterativo(String nome, boolean umbra) {
        String        chave       = nome.toLowerCase();
        ObjetoColisao o           = umbra ? interativosUmbra.get(chave) : interativosReal.get(chave);
        return (o != null && o.isAtivo(umbra)) ? o : null;
    }

    public EntidadeMapa getNpc(String nome, boolean umbra) {
        EntidadeMapa n = umbra ? npcsUmbra.get(nome.toLowerCase()) : npcsReal.get(nome.toLowerCase());
        return (n != null && n.isAtivo(umbra)) ? n : null;
    }

    // retorna todas as áreas sólidas do mundo atual, incluindo as portas
    public List<Rectangle> getParedes(boolean umbra) {
        List<Rectangle> todas = new ArrayList<>();
        for (ObjetoColisao p : paredesReal)  if (p.isAtivo(umbra)) todas.add(p.area);
        for (ObjetoColisao p : hitboxPortas) todas.add(p.area);
        if (umbra) {
            for (ObjetoColisao p : paredesUmbra) if (p.isAtivo(umbra)) todas.add(p.area);
        }
        return todas;
    }

    // retorna apenas as áreas das hitboxes de porta como Rectangle
    public List<Rectangle> getHitboxPortas() {
        List<Rectangle> r = new ArrayList<>();
        for (ObjetoColisao p : hitboxPortas) r.add(p.area);
        return r;
    }

    // retorna os objetos completos de porta, usados pelo debug para exibir nome e estado
    public List<ObjetoColisao> getHitboxPortasCompletas() {
        return new ArrayList<>(hitboxPortas);
    }

    // retorna mapa de áreas de interativos ativos no mundo atual
    public Map<String, Rectangle> getInterativos(boolean umbra) {
        Map<String, Rectangle>    map = new HashMap<>();
        Map<String, ObjetoColisao> src = umbra ? interativosUmbra : interativosReal;
        for (Map.Entry<String, ObjetoColisao> e : src.entrySet()) {
            if (e.getValue().isAtivo(umbra)) map.put(e.getKey(), e.getValue().area);
        }
        return map;
    }

    public Map<String, ObjetoColisao> getInterativosCompletos(boolean umbra) {
        Map<String, ObjetoColisao> map = new HashMap<>();
        Map<String, ObjetoColisao> src = umbra ? interativosUmbra : interativosReal;
        for (Map.Entry<String, ObjetoColisao> e : src.entrySet()) {
            if (e.getValue().isAtivo(umbra)) map.put(e.getKey(), e.getValue());
        }
        return map;
    }

    public Map<String, EntidadeMapa> getNpcs(boolean umbra) {
        Map<String, EntidadeMapa> map = new HashMap<>();
        Map<String, EntidadeMapa> src = umbra ? npcsUmbra : npcsReal;
        for (Map.Entry<String, EntidadeMapa> e : src.entrySet()) {
            if (e.getValue().isAtivo(umbra)) map.put(e.getKey(), e.getValue());
        }
        return map;
    }

    // registra uma porta como destrancada para esta sessão
    public void destrancar(String nome) {
        if (nome != null && !nome.isEmpty()) destrancados.add(nome.toLowerCase());
    }

    public boolean isDestrancado(String nome) {
        return nome != null && !nome.isEmpty() && destrancados.contains(nome.toLowerCase());
    }
}
