package com.persecutio.managers;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

// converte coordenadas do Tiled para coordenadas do mundo aplicando a escala global
public final class CoordenadasTiled {

    // escala padrão usada pelo projeto
    private static float escala = 2f;

    private CoordenadasTiled() {}

    public static void setEscala(float novaEscala) {
        escala = novaEscala;
    }

    public static float getEscala() {
        return escala;
    }

    // converte um valor escalar de Tiled para mundo
    public static float paraMundo(float tiled) {
        return tiled * escala;
    }

    public static Vector2 paraMundo(float tiledX, float tiledY) {
        return new Vector2(tiledX * escala, tiledY * escala);
    }

    // converte um retângulo do Tiled para mundo
    public static Rectangle paraMundo(Rectangle r) {
        return new Rectangle(r.x * escala, r.y * escala, r.width * escala, r.height * escala);
    }

    // lê uma string "x,y" em coordenadas de mundo sem aplicar escala
    public static Vector2 parseCoordenadasMundo(String str) {
        if (str == null || str.trim().isEmpty()) return null;
        try {
            String[] partes = str.trim().replace(" ", "").split(",");
            if (partes.length != 2) return null;
            float x = Float.parseFloat(partes[0].trim());
            float y = Float.parseFloat(partes[1].trim());
            return new Vector2(x, y);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // lê uma string "x,y" em coordenadas Tiled e aplica a escala
    public static Vector2 parseCoordenadasTiled(String str) {
        Vector2 mundo = parseCoordenadasMundo(str);
        if (mundo == null) return null;
        return new Vector2(mundo.x * escala, mundo.y * escala);
    }

    // converte coordenada de mundo de volta para Tiled
    public static Vector2 paraTiled(float mundoX, float mundoY) {
        return new Vector2(mundoX / escala, mundoY / escala);
    }

    public static float paraTiled(float mundo) {
        return mundo / escala;
    }
}
