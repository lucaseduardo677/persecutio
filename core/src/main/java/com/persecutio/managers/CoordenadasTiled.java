package com.persecutio.managers;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

// Coordenadas Tiled coordenadas mundo aplicando escala
public final class CoordenadasTiled {

    private static float escala = 1.375f;
    private static float alturaMapa = 1408f;

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

    // Define altura do mapa
    public static void setAlturaMapa(float novaAltura) {
        alturaMapa = novaAltura;
    }

    // Consulta do estado
    public static float getAlturaMapa() {
        return alturaMapa;
    }

    // Valor escalar Tiled mundo
    public static float paraMundo(float tiled) {
        return tiled * escala;
    }

    // Formula de conversao com offset aplicada exclusivamente para o jogador
    public static Vector2 paraMundo(float tiledX, float tiledY) {
        float x = tiledX * 1.375f - 1.0f;
        float y = (alturaMapa - tiledY) * 1.375f - 53.75f;
        return new Vector2(x, y);
    }

    // Retângulo Tiled mundo padrao (sem offsets) para colisoes do cenario
    public static Rectangle paraMundo(Rectangle r) {
        return new Rectangle(
            r.x * escala,
            r.y * escala,
            r.width * escala,
            r.height * escala
        );
    }

    // Coordenadas mundo
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

    // Coordenadas Tiled escala
    public static Vector2 parseCoordenadasTiled(String str) {
        return parseCoordenadasMundo(str);
    }

    // Formula inversa com offset aplicada exclusivamente para o jogador
    public static Vector2 paraTiled(float mundoX, float mundoY) {
        float x = (mundoX + 1.0f) / 1.375f;
        float y = alturaMapa - (mundoY + 53.75f) / 1.375f;
        return new Vector2(x, y);
    }

    // Processamento interno
    public static float paraTiled(float mundo) {
        return mundo / escala;
    }
}
