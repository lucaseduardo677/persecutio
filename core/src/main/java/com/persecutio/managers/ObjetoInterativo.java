package com.persecutio.managers;

import com.badlogic.gdx.math.Rectangle;

// Objeto interativo simples
public class ObjetoInterativo {

    public String  nome;
    public float   mundoX, mundoY;
    public float   largura, altura;

    // Objeto ativo mundo real mundo umbra
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

    // Área colisão objeto Rectangle
    public Rectangle getArea() {
        return new Rectangle(mundoX, mundoY, largura, altura);
    }
}
