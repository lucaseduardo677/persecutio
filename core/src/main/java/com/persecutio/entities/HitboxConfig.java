package com.persecutio.entities;

// Dimensoes e deslocamento da hitbox do jogador
public record HitboxConfig(
        int   larguraHitbox,
        int   alturaHitbox,
        float offsetX,
        float offsetY
) {
    // Valores padrao calibrados para o sprite do personagem
    public static HitboxConfig padrao() {
        return new HitboxConfig(24, 12, -12f, -28f);
    }
}