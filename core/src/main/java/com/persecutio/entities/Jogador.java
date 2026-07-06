package com.persecutio.entities;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.persecutio.managers.GerenciadorColisao;

// Estado e movimentacao do jogador
public class Jogador {

    // Direcoes usadas na animacao
    public static final int DIRECAO_BAIXO    = 0;
    public static final int DIRECAO_DIREITA  = 1;
    public static final int DIRECAO_ESQUERDA = 2;
    public static final int DIRECAO_CIMA     = 3;

    // Posicao do jogador no mundo
    public float mundoX;
    public float mundoY;

    // Tamanho do sprite em pixels
    private final int TAMANHO = 32;
    // Velocidade de movimento
    private final float VELOCIDADE = 180f;

    // Configuracao da hitbox
    private final HitboxConfig       hitboxConfig;
    // Hitbox de colisao
    public  final Rectangle          hitbox;
    // Controlador de input
    private final ControladorInput   controladorInput;
    // Animador de personagem
    private final AnimadorPersonagem animador;

    // Flag se esta andando
    private boolean andando = false;

    // Frames do spritesheet
    private final TextureRegion[][] framesSprites;

    // Construtor do jogador
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

    // Desenha o personagem na tela
    public void desenhar(SpriteBatch batch, float telaX, float telaY) {
        if (framesSprites == null) return;
        TextureRegion frameAtual = framesSprites[controladorInput.getDirecaoAnimacao()][animador.getFrame()];
        batch.draw(frameAtual, telaX - 28, telaY - 28, 56, 56);
    }

    // Teleporta jogador para nova posicao
    public void teleportar(float novoX, float novoY) {
        mundoX = novoX;
        mundoY = novoY;
        hitbox.setPosition(mundoX + hitboxConfig.offsetX(), mundoY + hitboxConfig.offsetY());
    }

    // Retorna offset X da hitbox
    public float   hitboxOffsetX()  { return hitboxConfig.offsetX(); }

    // Retorna offset Y da hitbox
    public float   hitboxOffsetY()  { return hitboxConfig.offsetY(); }

    // Retorna se esta andando
    public boolean isAndando()      { return andando; }

    // Retorna direcao atual
    public int     getDirecao()     { return controladorInput.getDirecaoAnimacao(); }

    // Retorna frame atual da animacao
    public int     getFrame()       { return animador.getFrame(); }

    // Retorna tamanho do sprite
    public int     getTamanho()     { return TAMANHO; }
}