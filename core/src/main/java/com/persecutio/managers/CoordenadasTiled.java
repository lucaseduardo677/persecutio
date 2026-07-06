package com.persecutio.managers;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

// Conversao de coordenadas Tiled para coordenadas do mundo aplicando escala
public final class CoordenadasTiled {

    // Escala de conversao
    private static float escala = 1.375f;
    // Altura total do mapa em pixels Tiled
    private static float alturaMapa = 1408f;

    // Construtor privado para evitar instanciacao
    private CoordenadasTiled() {}

    // Define a escala de conversao
    public static void setEscala(float novaEscala) {
        escala = novaEscala;
    }

    // Retorna a escala atual
    public static float getEscala() {
        return escala;
    }

    // Define a altura do mapa
    public static void setAlturaMapa(float novaAltura) {
        alturaMapa = novaAltura;
    }

    // Retorna a altura do mapa
    public static float getAlturaMapa() {
        return alturaMapa;
    }

    // Converte valor simples de Tiled para mundo
    public static float paraMundo(float tiled) {
        return tiled * escala;
    }

    // Converte coordenadas Tiled para mundo com offset exclusivo para o jogador
    public static Vector2 paraMundo(float tiledX, float tiledY) {
        float x = tiledX * 1.375f - 1.0f;
        float y = (alturaMapa - tiledY) * 1.375f - 53.75f;
        return new Vector2(x, y);
    }

    // Converte retangulo Tiled para mundo padrao sem offsets
    public static Rectangle paraMundo(Rectangle r) {
        return new Rectangle(
            r.x * escala,
            r.y * escala,
            r.width * escala,
            r.height * escala
        );
    }

    // Parse de coordenadas mundo a partir de string
    public static Vector2 parseCoordenadasMundo(String str) {
        if (str == null || str.trim().isEmpty()) return null;
        try {
            String[] partes = str.trim().replace(" ", "").split(",");
            if (partes.length != 2) return null;
            float x = Float.parseFloat(partes[0].trim());
            float y = Float.parseFloat(partes[1].trim());
            return paraMundo(x, y);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // Parse de coordenadas ja em mundo sem conversao Tiled
    public static Vector2 parseCoordenadasMundoDireto(String str) {
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

    // Parse de coordenadas Tiled com escala
    public static Vector2 parseCoordenadasTiled(String str) {
        return parseCoordenadasMundo(str);
    }

    // Converte coordenadas mundo para Tiled com offset exclusivo para o jogador
    public static Vector2 paraTiled(float mundoX, float mundoY) {
        float x = (mundoX + 1.0f) / 1.375f;
        float y = alturaMapa - (mundoY + 53.75f) / 1.375f;
        return new Vector2(x, y);
    }

    // Converte valor simples de mundo para Tiled
    public static float paraTiled(float mundo) {
        return mundo / escala;
    }
}