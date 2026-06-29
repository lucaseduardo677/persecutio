package com.persecutio.entities;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.math.Rectangle;

// Representa NPC sprite colocado Tiled hitbox
public class EntidadeMapa {

    public final String        nome;
    public final Rectangle     area;
    public final TextureRegion textura;

    // Qual mundo entidade aparece
    public final boolean noUmbra;
    public final boolean noReal;

    public final boolean trancado;
    public final boolean destrancavel;
    public final String  condicao;

    public EntidadeMapa(String nome, Rectangle area, TextureRegion textura,
                        MapProperties props, boolean padraoUmbra) {
        this.nome    = nome;
        this.area    = area;
        this.textura = textura;

        // Propriedades Tiled padrão camada
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

    // Entidade deve existir mundo atual
    public boolean isAtivo(boolean umbra) {
        return umbra ? noUmbra : noReal;
    }
}