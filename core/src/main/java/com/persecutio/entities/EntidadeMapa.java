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

    // Ativo no mundo Umbra
    public final boolean noUmbra;
    // Ativo no mundo Real
    public final boolean noReal;

    // Trancado
    public final boolean trancado;
    // Destrancavel
    public final boolean destrancavel;
    // Condicao para destrancar
    public final String  condicao;

    // Construtor da entidade do mapa
    public EntidadeMapa(String nome, Rectangle area, TextureRegion textura,
                        MapProperties props, boolean padraoUmbra) {
        this.nome    = nome;
        this.area    = area;
        this.textura = textura;

        Object u = props.get("umbra");
        Object r = props.get("real");
        this.noUmbra = (u != null) ? Boolean.parseBoolean(u.toString()) : padraoUmbra;
        this.noReal  = (r != null) ? Boolean.parseBoolean(r.toString()) : !padraoUmbra;

        Object t = props.get("trancado");
        this.trancado = (t != null) ? Boolean.parseBoolean(t.toString()) : false;

        Object d = props.get("destrancavel");
        this.destrancavel = (d != null) ? Boolean.parseBoolean(d.toString()) : false;

        Object c = props.get("condicao");
        this.condicao = (c != null) ? c.toString() : "";
    }

    // Verifica se a entidade existe no mundo atual
    public boolean isAtivo(boolean umbra) {
        return umbra ? noUmbra : noReal;
    }
}