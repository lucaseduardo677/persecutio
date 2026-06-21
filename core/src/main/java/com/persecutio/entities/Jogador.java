package com.persecutio.entities;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.persecutio.managers.GerenciadorColisao;

// entidade principal controlada pelo jogador
public class Jogador {

    // constantes de direção usadas para selecionar linha do spritesheet
    public static final int DIRECAO_BAIXO    = 0;
    public static final int DIRECAO_DIREITA  = 1;
    public static final int DIRECAO_ESQUERDA = 2;
    public static final int DIRECAO_CIMA     = 3;

    // posição do ponto de origem do sprite no mundo
    public float mundoX;
    public float mundoY;

    // tamanho de cada frame no spritesheet
    private final int TAMANHO = 32;

    // pixels por segundo de movimento
    private final float VELOCIDADE = 180f;

    private final HitboxConfig       hitboxConfig;
    public  final Rectangle          hitbox;
    private final ControladorInput   controladorInput;
    private final AnimadorPersonagem animador;

    private boolean andando = false;

    // grid de frames: [direcao][frame]
    private final TextureRegion[][] framesSprites;

    public Jogador(float x, float y, Texture spriteSheet) {
        this.mundoX = x;
        this.mundoY = y;

        this.hitboxConfig     = HitboxConfig.padrao();
        this.controladorInput = new ControladorInput();
        this.animador         = new AnimadorPersonagem();

        // spriteSheet pode ser null quando instanciado apenas para cálculo de posição
        this.framesSprites = spriteSheet != null
            ? TextureRegion.split(spriteSheet, TAMANHO, TAMANHO)
            : null;

        // posição inicial da hitbox relativa à origem do sprite
        this.hitbox = new Rectangle(
            x + hitboxConfig.offsetX(),
            y + hitboxConfig.offsetY(),
            hitboxConfig.larguraHitbox(),
            hitboxConfig.alturaHitbox()
        );
    }

    public void atualizar(float delta, GerenciadorColisao sistemaColisao, boolean umbra) {
        // sincroniza hitbox com a posição atual antes de mover
        hitbox.setPosition(mundoX + hitboxConfig.offsetX(), mundoY + hitboxConfig.offsetY());

        controladorInput.atualizar();
        Vector2 direcao = controladorInput.getDirecaoMovimento();
        float passo = VELOCIDADE * delta;

        // testa X e Y separadamente para deslizar ao longo das paredes
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

    public void desenhar(SpriteBatch batch, float telaX, float telaY) {
        if (framesSprites == null) return;
        TextureRegion frameAtual = framesSprites[controladorInput.getDirecaoAnimacao()][animador.getFrame()];
        // deslocamento visual para centralizar o sprite sobre a hitbox
        batch.draw(frameAtual, telaX - 28, telaY - 28, 56, 56);
    }

    // move o jogador diretamente para uma posição, atualizando a hitbox
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
