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

    public ObjetoInterativo(String nome, float x, float y, float largura, float altura) {
        this.nome    = nome;
        this.mundoX  = x;
        this.mundoY  = y;
        this.largura = largura;
        this.altura  = altura;
    }

    public Rectangle getArea() {
        return new Rectangle(mundoX, mundoY, largura, altura);
    }
}
