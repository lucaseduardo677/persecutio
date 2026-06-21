package com.persecutio.managers;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

// Coordenadas Tiled coordenadas mundo aplicando escala
public final class CoordenadasTiled {

    // Escala padrão pelo projeto
    private static float escala = 2f;

    // Criação da conversão de coordenadas
    private CoordenadasTiled() {}

    // Definição do valor
    public static void setEscala(float novaEscala) {
        escala = novaEscala;
    }

    // Consulta do estado
    public static float getEscala() {
        return escala;
    }

    // Valor escalar Tiled mundo
    public static float paraMundo(float tiled) {
        return tiled * escala;
    }

    // Processamento interno
    public static Vector2 paraMundo(float tiledX, float tiledY) {
        return new Vector2(tiledX * escala, tiledY * escala);
    }

    // Retângulo Tiled mundo
    public static Rectangle paraMundo(Rectangle r) {
        return new Rectangle(r.x * escala, r.y * escala, r.width * escala, r.height * escala);
    }

    // Coordenadas mundo
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

    // Coordenadas Tiled escala
    public static Vector2 parseCoordenadasTiled(String str) {
        Vector2 mundo = parseCoordenadasMundo(str);
        if (mundo == null) return null;
        return new Vector2(mundo.x * escala, mundo.y * escala);
    }

    // Coordenada mundo volta Tiled
    public static Vector2 paraTiled(float mundoX, float mundoY) {
        return new Vector2(mundoX / escala, mundoY / escala);
    }

    // Processamento interno
    public static float paraTiled(float mundo) {
        return mundo / escala;
    }
}
