package com.persecutio.entities;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.math.Rectangle;

// Representa um NPC ou sprite colocado no Tiled com hitbox e visibilidade por mundo
public class EntidadeMapa {

    public final String        nome;
    public final Rectangle     area;
    public final TextureRegion textura;

    // Define em qual mundo a entidade aparece
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

        // Lê as propriedades do Tiled ou usa o padrão da camada
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

    // Retorna true se a entidade deve existir no mundo atual
    public boolean isAtivo(boolean umbra) {
        return umbra ? noUmbra : noReal;
    }
}
