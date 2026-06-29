package com.persecutio.entities;

// Dimensões deslocamento hitbox jogador relação origem
public record HitboxConfig(
        int   larguraHitbox,
        int   alturaHitbox,
        float offsetX,
        float offsetY
) {
    // Valores padrão calibrados sprite personagem
    public static HitboxConfig padrao() {
        return new HitboxConfig(24, 12, -12f, -28f);
    }
}