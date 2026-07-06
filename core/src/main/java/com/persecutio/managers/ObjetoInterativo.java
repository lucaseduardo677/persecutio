package com.persecutio.managers;

import com.badlogic.gdx.math.Rectangle;

// Objeto interativo simples no mapa
public class ObjetoInterativo {

    // Nome do objeto
    public String  nome;
    // Posicao no mundo
    public float   mundoX, mundoY;
    // Dimensao
    public float   largura, altura;

    // Ativo no mundo Real
    public boolean ativoNoMundoReal;
    // Ativo no mundo Umbra
    public boolean ativoNoMundoUmbra;

    // Construtor do objeto interativo
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

    // Retorna area de colisao do objeto
    public Rectangle getArea() {
        return new Rectangle(mundoX, mundoY, largura, altura);
    }
}