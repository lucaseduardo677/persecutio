package com.persecutio.managers;

import box2dLight.ConeLight;
import box2dLight.Light;
import box2dLight.PointLight;
import box2dLight.RayHandler;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Sistema de iluminacao do jogo
public class GerenciadorLuzes {

    // Mundo Box2D para raycasting das luzes
    private final World mundoBox2D;
    // RayHandler para gerenciar luzes
    private final RayHandler rayHandler;
    // Lista de corpos de parede para sombra
    private final List<Body> corposParedes = new ArrayList<>();

    // Luz que segue o jogador
    private Light luzJogador;
    // Luz ambiente do Umbra
    private Light luzAmbienteUmbra;

    // Lista de luzes fixas do mapa
    private final List<Light> luzesFixas = new ArrayList<>();

    // Mapa de luzes ativas no Umbra
    private final Map<Light, Boolean> luzNoUmbra = new HashMap<>();
    // Mapa de luzes ativas no Real
    private final Map<Light, Boolean> luzNoReal  = new HashMap<>();
    // Mapa de comodo de cada luz fixa
    private final Map<Light, GerenciadorComodos.Comodo> luzComodo = new HashMap<>();

    // Flag se o ambiente Umbra esta ativo
    private boolean ambienteUmbraAtivo = false;

    // Contador de frames para otimizacao
    private int contadorFrame = 0;

    // Referencia ao gerenciador de comodos
    private GerenciadorComodos gerComodos;

    // Construtor do sistema de luzes
    public GerenciadorLuzes() {
        this.mundoBox2D = new World(new com.badlogic.gdx.math.Vector2(0, 0), true);
        this.rayHandler = new RayHandler(mundoBox2D);

        rayHandler.setShadows(true);
        RayHandler.useDiffuseLight(true);
        rayHandler.setCulling(true);
        RayHandler.setGammaCorrection(true);
    }

    // Define o gerenciador de comodos
    public void setGerenciadorComodos(GerenciadorComodos gerComodos) {
        this.gerComodos = gerComodos;
    }

    // Categoria de fixture de sombra de parede
    private static final short CAT_SOMBRA_PAREDE = 0x0002;
    // Categoria de fixture de sombra de porta
    private static final short CAT_SOMBRA_PORTA  = 0x0004;

    // Tamanho de um tile no mundo em pixels
    private static final float TILE_MUNDO = 16f * 1.375f;

    // Cria corpos de parede e porta para sombra
    public void criarParedes(List<Rectangle> paredes, List<Rectangle> portas) {
        criarCorpos(paredes, CAT_SOMBRA_PAREDE);
        criarCorpos(portas,  CAT_SOMBRA_PORTA);
    }

    // Cria corpos para sombra compativel com chamadas antigas
    public void criarParedes(List<Rectangle> todas) {
        criarCorpos(todas, CAT_SOMBRA_PAREDE);
    }

    // Cria corpos estaticos Box2D para bloquear luz
    private void criarCorpos(List<Rectangle> lista, short categoria) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.StaticBody;

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.filter.categoryBits = categoria;
        fixtureDef.filter.maskBits     = 0x0000;

        boolean ePorta = (categoria == CAT_SOMBRA_PORTA);

        for (Rectangle r : lista) {
            if (r.width <= 0.5f || r.height <= 0.5f) continue;

            boolean horizontal = r.width > r.height * 1.5f;
            float recuoTopo    = (ePorta || horizontal) ? TILE_MUNDO : 0f;

            float cx = r.x + r.width  / 2f;
            float cy = r.y + (r.height - recuoTopo) / 2f;
            float hw = Math.max(0.5f, r.width  / 2f);
            float hh = Math.max(0.5f, (r.height - recuoTopo) / 2f);

            Body body = mundoBox2D.createBody(bodyDef);
            PolygonShape shape = new PolygonShape();
            shape.setAsBox(hw, hh, new com.badlogic.gdx.math.Vector2(cx, cy), 0f);
            fixtureDef.shape = shape;
            body.createFixture(fixtureDef);
            shape.dispose();

            corposParedes.add(body);
        }
    }

    // Carrega luzes definidas no Tiled
    public void carregarLuzesDoTiled(TiledMap mapa) {
        MapLayer camada = mapa.getLayers().get("Luzes");
        if (camada == null) return;

        float escala = CoordenadasTiled.getEscala();

        for (MapObject obj : camada.getObjects()) {
            if (!(obj instanceof RectangleMapObject)) continue;

            MapProperties props = obj.getProperties();
            String classe = props.get("type") != null ? props.get("type").toString() :
                            props.get("class") != null ? props.get("class").toString() : "";

            if (!"luz".equalsIgnoreCase(classe)) continue;

            Rectangle r = ((RectangleMapObject) obj).getRectangle();
            Rectangle rm = CoordenadasTiled.paraMundo(r);
            float cx = rm.x + rm.width / 2f;
            float cy = rm.y + rm.height / 2f;

            String tipo = lerTexto(props, "tipo", "point");
            Color cor = parseCorTiled(props);
            float distancia = lerNumero(props, "distancia", 200f);
            int raios = lerInteiro(props, "raios", 128);
            float alpha = lerNumero(props, "alpha", 0.9f);
            cor.a = alpha;

            boolean suave = lerBool(props, "suave", true);
            float suavidade = lerNumero(props, "suavidade", 20f);
            boolean estatica = lerBool(props, "estatica", true);
            boolean atravessa = lerBool(props, "atravessa", false);
            boolean noUmbra = lerBool(props, "umbra", true);
            boolean noReal = lerBool(props, "real", true);
            boolean segue = lerBool(props, "segue", false);
            boolean ligada = lerBool(props, "ligada", true);
            float direcao = lerNumero(props, "direcao", 0f);
            float abertura = lerNumero(props, "abertura", 45f);

            Light luz = null;

            if ("cone".equalsIgnoreCase(tipo)) {
                luz = new ConeLight(rayHandler, raios, cor, distancia, cx, cy, direcao, abertura);
            } else {
                luz = new PointLight(rayHandler, raios, cor, distancia, cx, cy);
            }

            if (luz != null) {
                luz.setSoft(suave);
                luz.setSoftnessLength(suavidade);
                luz.setStaticLight(estatica);
                luz.setXray(atravessa);
                luz.setActive(ligada);

                short mascaraSombra = (short) (CAT_SOMBRA_PAREDE | CAT_SOMBRA_PORTA);
                luz.setContactFilter(CAT_SOMBRA_PAREDE, (short) 0, mascaraSombra);

                if (segue) {
                    luzJogador = luz;
                } else {
                    luzesFixas.add(luz);
                    luzNoUmbra.put(luz, noUmbra);
                    luzNoReal.put(luz, noReal);

                    if (gerComodos != null) {
                        GerenciadorComodos.Comodo c = gerComodos.achar(cx, cy);
                        luzComodo.put(luz, c);
                    }
                }
            }
        }
    }

    // Inicializa luzes do jogador e ambiente
    public void inicializar(float jogadorX, float jogadorY) {
        short mascaraSombra = (short) (CAT_SOMBRA_PAREDE | CAT_SOMBRA_PORTA);

        if (luzJogador == null) {
            luzJogador = new PointLight(rayHandler, 128,
                new Color(1f, 0.95f, 0.8f, 0.9f),
                250f, jogadorX, jogadorY);
            luzJogador.setSoft(true);
            luzJogador.setSoftnessLength(20f);
            luzJogador.setContactFilter(CAT_SOMBRA_PAREDE, (short) 0, mascaraSombra);
        }

        luzAmbienteUmbra = new PointLight(rayHandler, 64,
            new Color(0.6f, 0f, 0f, 0.3f),
            400f, jogadorX, jogadorY);
        luzAmbienteUmbra.setContactFilter(CAT_SOMBRA_PAREDE, (short) 0, mascaraSombra);
        luzAmbienteUmbra.setActive(false);

        setAmbienteUmbra(false);
    }

    // Atualiza posicao das luzes que seguem o jogador
    public void atualizarPosicaoJogador(float mundoX, float mundoY) {
        if (luzJogador != null) luzJogador.setPosition(mundoX, mundoY);
        if (luzAmbienteUmbra != null) luzAmbienteUmbra.setPosition(mundoX, mundoY);
    }

    // Define o ambiente de iluminacao
    public void setAmbienteUmbra(boolean umbra) {
        if (ambienteUmbraAtivo == umbra) return;
        ambienteUmbraAtivo = umbra;

        if (umbra) {
            rayHandler.setAmbientLight(0.05f, 0.02f, 0.02f, 0.15f);
            if (luzJogador != null) luzJogador.setDistance(180f);
        } else {
            rayHandler.setAmbientLight(0.4f, 0.4f, 0.45f, 0.6f);
            if (luzJogador != null) luzJogador.setDistance(250f);
        }
    }

    // Renderiza as luzes
    public void render(ContextoRender ctx, GerenciadorComodos gerComodosRef,
                       GerenciadorComodos.Comodo comodoJogador) {
        contadorFrame++;

        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);

        List<GerenciadorComodos.Comodo> cullAtivo = gerComodosRef.getCullAtivo(comodoJogador);

        for (Light l : luzesFixas) {
            boolean ativaPorMundo = ambienteUmbraAtivo
                ? luzNoUmbra.getOrDefault(l, true)
                : luzNoReal.getOrDefault(l, true);

            GerenciadorComodos.Comodo c = luzComodo.get(l);
            boolean ativaPorCull = true;
            if (c != null) {
                ativaPorCull = false;
                for (GerenciadorComodos.Comodo ca : cullAtivo) {
                    if (ca == c) {
                        ativaPorCull = true;
                        break;
                    }
                }
            }

            l.setActive(ativaPorMundo && ativaPorCull);
        }

        OrthographicCamera camLuz = new OrthographicCamera();
        camLuz.viewportWidth = ctx.camera.viewportWidth;
        camLuz.viewportHeight = ctx.camera.viewportHeight;
        camLuz.position.set(ctx.vLargura / 2f - ctx.cameraX, ctx.vAltura / 2f - ctx.cameraY, 0);
        camLuz.update();

        rayHandler.setCombinedMatrix(camLuz);
        rayHandler.updateAndRender();
    }

    // Libera recursos de luzes
    public void dispose() {
        rayHandler.dispose();
        mundoBox2D.dispose();
    }

    // Leitura de texto de propriedade
    private String lerTexto(MapProperties p, String chave, String padrao) {
        Object v = p.get(chave);
        return v != null ? v.toString().trim() : padrao;
    }

    // Leitura de numero de propriedade
    private float lerNumero(MapProperties p, String chave, float padrao) {
        try { return Float.parseFloat(lerTexto(p, chave, String.valueOf(padrao))); }
        catch (Exception e) { return padrao; }
    }

    // Leitura de inteiro de propriedade
    private int lerInteiro(MapProperties p, String chave, int padrao) {
        try { return Integer.parseInt(lerTexto(p, chave, String.valueOf(padrao))); }
        catch (Exception e) { return padrao; }
    }

    // Leitura de boolean de propriedade
    private boolean lerBool(MapProperties p, String chave, boolean padrao) {
        String s = lerTexto(p, chave, "").toLowerCase();
        if (s.isEmpty()) return padrao;
        return s.equals("true") || s.equals("1") || s.equals("yes");
    }

    // Parse de cor de propriedade do Tiled
    private Color parseCorTiled(MapProperties props) {
        Object colorObj = props.get("cor");
        if (colorObj instanceof Color) {
            return new Color((Color) colorObj);
        }
        if (colorObj != null) {
            String hex = colorObj.toString().trim();
            if (!hex.isEmpty()) {
                return parseHexColor(hex);
            }
        }

        String corStr = lerTexto(props, "cor_str", "");
        if (!corStr.isEmpty()) {
            return parseCorString(corStr);
        }

        return new Color(1f, 1f, 1f, 1f);
    }

    // Parse de cor hexadecimal
    private Color parseHexColor(String hex) {
        String h = hex.startsWith("#") ? hex.substring(1) : hex;
        h = h.trim();

        if (h.length() == 8) {
            try {
                int a = Integer.parseInt(h.substring(0, 2), 16);
                int r = Integer.parseInt(h.substring(2, 4), 16);
                int g = Integer.parseInt(h.substring(4, 6), 16);
                int b = Integer.parseInt(h.substring(6, 8), 16);
                return new Color(r / 255f, g / 255f, b / 255f, a / 255f);
            } catch (Exception e) {
                return new Color(1f, 1f, 1f, 1f);
            }
        } else if (h.length() == 6) {
            try {
                return Color.valueOf(h);
            } catch (Exception e) {
                return new Color(1f, 1f, 1f, 1f);
            }
        } else if (h.length() == 3) {
            try {
                return Color.valueOf(h);
            } catch (Exception e) {
                return new Color(1f, 1f, 1f, 1f);
            }
        }

        return new Color(1f, 1f, 1f, 1f);
    }

    // Parse de cor a partir de string
    private Color parseCorString(String str) {
        String s = str.trim();
        if (s.startsWith("#")) {
            return parseHexColor(s);
        }
        String[] p = s.split(",");
        try {
            float r = p.length > 0 ? Float.parseFloat(p[0].trim()) : 1f;
            float g = p.length > 1 ? Float.parseFloat(p[1].trim()) : 1f;
            float b = p.length > 2 ? Float.parseFloat(p[2].trim()) : 1f;
            return new Color(r, g, b, 1f);
        } catch (Exception e) {
            return new Color(1f, 1f, 1f, 1f);
        }
    }
}