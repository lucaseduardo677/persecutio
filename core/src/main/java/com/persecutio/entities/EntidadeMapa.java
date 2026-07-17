package com.persecutio.entities;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.math.Rectangle;

// Representa um NPC com sprite e hitbox no mapa
public class EntidadeMapa {

    // Nome da entidade
    public final String        nome;
    // Area de colisao
    public final Rectangle     area;
    // Textura do sprite
    public final TextureRegion textura;

    // Mundo de origem da entidade, definido estritamente pelo mapa em que foi lida
    public final boolean mundoUmbra;

    // Trancado
    public final boolean trancado;
    // Destrancavel
    public final boolean destrancavel;
    // Condicao para destrancar
    public final String  condicao;

    // Construtor da entidade do mapa
    public EntidadeMapa(String nome, Rectangle area, TextureRegion textura,
                        MapProperties props, boolean mundoUmbra) {
        this.nome       = nome;
        this.area       = area;
        this.textura    = textura;
        this.mundoUmbra = mundoUmbra;

        Object t = props.get("trancado");
        this.trancado = (t != null) ? Boolean.parseBoolean(t.toString()) : false;

        Object d = props.get("destrancavel");
        this.destrancavel = (d != null) ? Boolean.parseBoolean(d.toString()) : false;

        Object c = props.get("condicao");
        this.condicao = (c != null) ? c.toString() : "";
    }

    // Verifica se a entidade pertence ao mundo atual
    public boolean isAtivo(boolean umbra) {
        return mundoUmbra == umbra;
    }
}
