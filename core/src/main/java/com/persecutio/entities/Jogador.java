package com.persecutio.entities;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.persecutio.managers.GerenciadorColisao;

// Estado do jogador e movimentacao
public class Jogador {

// Direcoes usadas na animacao
    public static final int DIRECAO_BAIXO    = 0;
    public static final int DIRECAO_DIREITA  = 1;
    public static final int DIRECAO_ESQUERDA = 2;
    public static final int DIRECAO_CIMA     = 3;

// Posicao do jogador no mundo
    public float mundoX;
    public float mundoY;

    private final int TAMANHO = 32;
    private final float VELOCIDADE = 180f;

// Componentes de movimento e colisao
    private final HitboxConfig       hitboxConfig;
    public  final Rectangle          hitbox;
    private final ControladorInput   controladorInput;
    private final AnimadorPersonagem animador;

    private boolean andando = false;

    private final TextureRegion[][] framesSprites;

// Cria o jogador com a textura do personagem
    public Jogador(float x, float y, Texture spriteSheet) {
        this.mundoX = x;
        this.mundoY = y;

        this.hitboxConfig     = HitboxConfig.padrao();
        this.controladorInput = new ControladorInput();
        this.animador         = new AnimadorPersonagem();

        this.framesSprites = spriteSheet != null
            ? TextureRegion.split(spriteSheet, TAMANHO, TAMANHO)
            : null;

        this.hitbox = new Rectangle(
            x + hitboxConfig.offsetX(),
            y + hitboxConfig.offsetY(),
            hitboxConfig.larguraHitbox(),
            hitboxConfig.alturaHitbox()
        );
    }

// Atualiza movimento e animacao do jogador
    public void atualizar(float delta, GerenciadorColisao sistemaColisao, boolean umbra) {
        hitbox.setPosition(mundoX + hitboxConfig.offsetX(), mundoY + hitboxConfig.offsetY());

        controladorInput.atualizar();
        Vector2 direcao = controladorInput.getDirecaoMovimento();
        float passo = VELOCIDADE * delta;

        if (direcao.x != 0f) {
            float novoX = mundoX + direcao.x * passo;
            if (sistemaColisao.verificarPosicao(
                    novoX + hitboxConfig.offsetX(), mundoY + hitboxConfig.offsetY(),
                    hitboxConfig.larguraHitbox(), hitboxConfig.alturaHitbox(), umbra)) {
                mundoX = novoX;
            }
        }

        if (direcao.y != 0f) {
            float novoY = mundoY + direcao.y * passo;
            if (sistemaColisao.verificarPosicao(
                    mundoX + hitboxConfig.offsetX(), novoY + hitboxConfig.offsetY(),
                    hitboxConfig.larguraHitbox(), hitboxConfig.alturaHitbox(), umbra)) {
                mundoY = novoY;
            }
        }

        andando = controladorInput.isMovendo();
        animador.atualizar(delta, andando);
    }

// Desenha o frame atual do personagem
    public void desenhar(SpriteBatch batch, float telaX, float telaY) {
        if (framesSprites == null) return;
        TextureRegion frameAtual = framesSprites[controladorInput.getDirecaoAnimacao()][animador.getFrame()];
        batch.draw(frameAtual, telaX - 28, telaY - 28, 56, 56);
    }

// Move o jogador para outra posicao
    public void teleportar(float novoX, float novoY) {
        mundoX = novoX;
        mundoY = novoY;
        hitbox.setPosition(mundoX + hitboxConfig.offsetX(), mundoY + hitboxConfig.offsetY());
    }

    public float   hitboxOffsetX()  { return hitboxConfig.offsetX(); }
    public float   hitboxOffsetY()  { return hitboxConfig.offsetY(); }
    public boolean isAndando()      { return andando; }
    public int     getDirecao()     { return controladorInput.getDirecaoAnimacao(); }
    public int     getFrame()       { return animador.getFrame(); }
    public int     getTamanho()     { return TAMANHO; }
}
