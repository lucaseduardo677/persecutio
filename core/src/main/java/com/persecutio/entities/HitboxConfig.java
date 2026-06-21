package com.persecutio.entities;

// dimensões e deslocamento da hitbox do jogador em relação à origem do sprite
public record HitboxConfig(
        int   larguraHitbox,
        int   alturaHitbox,
        float offsetX,
        float offsetY
) {
    // valores padrão calibrados para o sprite do personagem
    public static HitboxConfig padrao() {
        return new HitboxConfig(24, 12, -12f, -28f);
    }
}
