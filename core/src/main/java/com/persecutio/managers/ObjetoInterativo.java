package com.persecutio.managers;

import com.badlogic.gdx.math.Rectangle;

// representa um objeto interativo simples com posição, tamanho e presença por mundo
public class ObjetoInterativo {

    public String  nome;
    public float   mundoX, mundoY;
    public float   largura, altura;

    // define se o objeto está ativo no mundo real e no mundo umbra
    public boolean ativoNoMundoReal;
    public boolean ativoNoMundoUmbra;

    public ObjetoInterativo(String nome, float x, float y, float largura, float altura,
                            boolean noReal, boolean noUmbra) {
        this.nome             = nome;
        this.mundoX           = x;
        this.mundoY           = y;
        this.largura          = largura;
        this.altura           = altura;
        this.ativoNoMundoReal  = noReal;
        this.ativoNoMundoUmbra = noUmbra;
    }

    // retorna a área de colisão do objeto como Rectangle
    public Rectangle getArea() {
        return new Rectangle(mundoX, mundoY, largura, altura);
    }
}
