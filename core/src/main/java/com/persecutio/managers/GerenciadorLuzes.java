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

public class GerenciadorLuzes {

    private final World mundoBox2D;
    private final RayHandler rayHandler;
    private final List<Body> corposParedes = new ArrayList<>();

    private Light luzJogador;
    private Light luzAmbienteUmbra;

    private final List<Light> luzesFixas = new ArrayList<>();

    private final Map<Light, Boolean> luzNoUmbra = new HashMap<>();
    private final Map<Light, Boolean> luzNoReal  = new HashMap<>();
    private final Map<Light, GerenciadorComodos.Comodo> luzComodo = new HashMap<>();

    private boolean ambienteUmbraAtivo = false;

    private int contadorFrame = 0;

    private GerenciadorComodos gerComodos;

    public GerenciadorLuzes() {
        this.mundoBox2D = new World(new com.badlogic.gdx.math.Vector2(0, 0), true);
        this.rayHandler = new RayHandler(mundoBox2D);

        rayHandler.setShadows(true);
        RayHandler.useDiffuseLight(true);
        rayHandler.setCulling(true);
        RayHandler.setGammaCorrection(true);
    }

    public void setGerenciadorComodos(GerenciadorComodos gerComodos) {
        this.gerComodos = gerComodos;
    }

    // Categoria 0x0002 fixture de sombra de parede bloqueia luz nao colide com player
    // Categoria 0x0004 fixture de sombra de porta bloqueia luz nao colide com player
    private static final short CAT_SOMBRA_PAREDE = 0x0002;
    private static final short CAT_SOMBRA_PORTA  = 0x0004;

    // Tamanho de um tile no mundo 16px escala 1 375
    private static final float TILE_MUNDO = 16f * 1.375f;

    public void criarParedes(List<Rectangle> paredes, List<Rectangle> portas) {
        criarCorpos(paredes, CAT_SOMBRA_PAREDE);
        criarCorpos(portas,  CAT_SOMBRA_PORTA);
    }

    // Compatibilidade com chamadas antigas que passam tudo junto
    public void criarParedes(List<Rectangle> todas) {
        criarCorpos(todas, CAT_SOMBRA_PAREDE);
    }

    private void criarCorpos(List<Rectangle> lista, short categoria) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.StaticBody;

        FixtureDef fixtureDef = new FixtureDef();
        // Nao colide com nada fisicamente so existe para o raycast das luzes
        fixtureDef.filter.categoryBits = categoria;
        fixtureDef.filter.maskBits     = 0x0000;

        boolean ePorta = (categoria == CAT_SOMBRA_PORTA);

        for (Rectangle r : lista) {
            if (r.width <= 0.5f || r.height <= 0.5f) continue;

            // Determina recuo da borda superior do corpo de sombra
            // Porta ou parede horizontal topo de comodo recua 1 tile inteiro
            // Para a luz iluminar o sprite antes de ser bloqueada
            // Parede vertical ou base sem recuo corpo ocupa a hitbox inteira
            boolean horizontal = r.width > r.height * 1.5f;
            float recuoTopo    = (ePorta || horizontal) ? TILE_MUNDO : 0f;

            float cx = r.x + r.width  / 2f;
            // Desloca o centro para baixo na metade do recuo mantendo a base intacta
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

                // So fixtures de sombra 0x0002 0x0004 bloqueiam esta luz
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

    public void inicializar(float jogadorX, float jogadorY) {
        // Mascara de sombra so fixtures de categoria 0x0002 parede e 0x0004 porta
        // Bloqueiam os raios hitboxes fisicas do player 0x0001 sao ignoradas
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

    public void atualizarPosicaoJogador(float mundoX, float mundoY) {
        if (luzJogador != null) luzJogador.setPosition(mundoX, mundoY);
        if (luzAmbienteUmbra != null) luzAmbienteUmbra.setPosition(mundoX, mundoY);
    }

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

    public void dispose() {
        rayHandler.dispose();
        mundoBox2D.dispose();
    }

    private String lerTexto(MapProperties p, String chave, String padrao) {
        Object v = p.get(chave);
        return v != null ? v.toString().trim() : padrao;
    }

    private float lerNumero(MapProperties p, String chave, float padrao) {
        try { return Float.parseFloat(lerTexto(p, chave, String.valueOf(padrao))); }
        catch (Exception e) { return padrao; }
    }

    private int lerInteiro(MapProperties p, String chave, int padrao) {
        try { return Integer.parseInt(lerTexto(p, chave, String.valueOf(padrao))); }
        catch (Exception e) { return padrao; }
    }

    private boolean lerBool(MapProperties p, String chave, boolean padrao) {
        String s = lerTexto(p, chave, "").toLowerCase();
        if (s.isEmpty()) return padrao;
        return s.equals("true") || s.equals("1") || s.equals("yes");
    }

    private Color parseCorTiled(MapProperties props) {
        Object colorObj = props.get("cor");
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