package com.persecutio.managers;

import com.badlogic.gdx.math.Rectangle;
import com.persecutio.entities.EntidadeMapa;
import com.persecutio.entities.Jogador;

import java.util.Map;

// controla o estado da narrativa, coleta de peças e interações com objetos
public class GerenciadorProgresso {

    // margem extra ao redor da hitbox para detectar interações
    private static final float FOLGA = 8f;

    private final GerenciadorColisao colisao;
    private final GerenciadorPortas  portas;

    // estado do mundo e missão atual
    private boolean mundoUmbra  = false;
    private int     missao      = 1;
    private int     documentos  = 1;

    // contagem de fragmentos coletados para desbloquear a porta umbra
    private int     partes      = 0;

    // flags de coleta e progressão
    private boolean destrancada  = false;
    private boolean sabePalavra  = false;
    private boolean pecaEspelho  = false;
    private boolean pecaGaveta   = false;
    private boolean pecaNpc      = false;
    private boolean leuDocumento = false;

    // texto exibido na tela após uma interação
    private String  aviso        = "";

    // flags usados pelo render para abrir telas especiais
    private boolean cinematica   = false;
    private boolean abriuEspelho = false;
    private boolean abriuGaveta  = false;

    public GerenciadorProgresso(GerenciadorColisao colisao, GerenciadorPortas portas) {
        this.colisao = colisao;
        this.portas  = portas;
    }

    // alterna entre mundo real e umbra diretamente, usado pelo debug
    public void alternarUmbra() {
        mundoUmbra = !mundoUmbra;
    }

    // retorna uma hitbox levemente expandida para tolerância de interação
    private Rectangle hitboxFolga(Jogador jogador) {
        return new Rectangle(
            jogador.hitbox.x - FOLGA,
            jogador.hitbox.y - FOLGA,
            jogador.hitbox.width  + FOLGA * 2f,
            jogador.hitbox.height + FOLGA * 2f
        );
    }

    // ponto de entrada para interação com E, delega para o mundo correto
    public void tratarInteracao(Jogador jogador) {
        cinematica   = false;
        abriuEspelho = false;
        abriuGaveta  = false;

        Rectangle hitboxInteracao = hitboxFolga(jogador);

        if (!mundoUmbra) {
            interagirReal(hitboxInteracao);
        } else {
            interagirUmbra(hitboxInteracao, jogador);
        }
    }

    // interações disponíveis no mundo real
    private void interagirReal(Rectangle hitboxInteracao) {
        GerenciadorColisao.ObjetoColisao pilula = colisao.getInterativo("pilula", false);
        if (pilula != null && hitboxInteracao.overlaps(pilula.area)) {
            mundoUmbra = true;
            return;
        }

        EntidadeMapa paciente = colisao.getNpc("paciente", false);
        if (paciente != null && hitboxInteracao.overlaps(paciente.area)) {
            if (!pecaNpc) {
                if (sabePalavra) {
                    // jogador já sabe a palavra, concede a peça do NPC
                    pecaNpc = true;
                    partes++;
                    cinematica = true;
                    aviso = "";
                } else {
                    aviso = "Paciente: Eu tenho algo util, mas... qual e a palavra magica?";
                }
            } else {
                aviso = "Paciente: Va em frente, voce tem o que precisa.";
            }
            return;
        }

        EntidadeMapa enfermeira = colisao.getNpc("enfermeira", false);
        if (enfermeira != null && hitboxInteracao.overlaps(enfermeira.area)) {
            aviso = "Enfermeira: Volte para o seu quarto. Voce nao devia estar aqui.";
            return;
        }

        GerenciadorColisao.ObjetoColisao doc = colisao.getInterativo("documento", false);
        if (doc != null && hitboxInteracao.overlaps(doc.area)) {
            if (!destrancada) {
                aviso = "As letras estao borradas, parecem dancar. Nao consigo ler...";
            } else if (!leuDocumento) {
                leuDocumento = true;
                documentos++;
                missao = 2;
                aviso  = "CONTEUDO DO PAPEL: Relatorio de Incidente...\n[Missao 1 Concluida!]";
            } else {
                aviso = "Voce ja leu este documento.";
            }
        }
    }

    // interações disponíveis no mundo umbra
    private void interagirUmbra(Rectangle hitboxInteracao, Jogador jogador) {
        GerenciadorColisao.ObjetoColisao cama = colisao.getInterativo("cama", true);
        if (cama != null && hitboxInteracao.overlaps(cama.area)) {
            mundoUmbra = false;
            return;
        }

        GerenciadorColisao.ObjetoColisao pilula = colisao.getInterativo("pilula", true);
        if (pilula != null && hitboxInteracao.overlaps(pilula.area)) {
            mundoUmbra = false;
            return;
        }

        GerenciadorColisao.ObjetoColisao espelho = colisao.getInterativo("espelho", true);
        if (espelho != null && hitboxInteracao.overlaps(espelho.area)) {
            abriuEspelho = true;
            if (!pecaEspelho) {
                pecaEspelho = true;
                partes++;
                aviso = "Voce encontrou um fragmento no espelho!";
            }
            return;
        }

        // abre o puzzle de senha se a gaveta ainda não foi aberta
        GerenciadorColisao.ObjetoColisao gaveta = colisao.getInterativo("gaveta", true);
        if (gaveta != null && hitboxInteracao.overlaps(gaveta.area) && !pecaGaveta) {
            abriuGaveta = true;
        }
    }

    // valida a senha digitada no puzzle e concede a peça se correta
    public boolean validarSenha(String senha) {
        if (pecaGaveta) return true;

        if ("0410".equals(senha)) {
            pecaGaveta  = true;
            sabePalavra = true;
            partes++;
            aviso = "Voce achou um fragmento e a palavra 'Redencao'.";
            return true;
        }

        aviso = "A porta nao abre. Acho que nao e esta a senha.";
        return false;
    }

    // limpa o aviso quando o jogador se afasta dos objetos interativos
    public void verificarAfastamento(Jogador jogador) {
        if (aviso.isEmpty()) return;

        Rectangle hitboxInteracao = hitboxFolga(jogador);

        for (Rectangle area : colisao.getInterativos(mundoUmbra).values()) {
            if (hitboxInteracao.overlaps(area)) return;
        }

        for (EntidadeMapa npc : colisao.getNpcs(mundoUmbra).values()) {
            if (hitboxInteracao.overlaps(npc.area)) return;
        }

        aviso = "";
    }

    // avalia uma condição de desbloqueio no formato "variavel==valor"
    private boolean avaliarCondicao(String condicao) {
        if (condicao == null || condicao.trim().isEmpty()) return true;
        String c = condicao.trim();

        if (c.contains("==")) {
            String[] p = c.split("==", 2);
            String key = p[0].trim();
            String val = p[1].trim();
            try {
                switch (key) {
                    case "partes":     return partes     >= Integer.parseInt(val);
                    case "missao":     return missao     >= Integer.parseInt(val);
                    case "documentos": return documentos >= Integer.parseInt(val);
                    default: return false;
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }

        return true;
    }

    // retorna true se o jogador cumpre a condição para destrancar a porta
    public boolean podeDestrancar(GerenciadorPortas.Porta porta) {
        if (!porta.trancado)     return true;
        if (!porta.destrancavel) return false;
        return avaliarCondicao(porta.condicao);
    }

    public void setAviso(String msg) { this.aviso = msg; }

    // adiciona uma parte, usado pelo debug para testar desbloqueio
    public void adicionarParte() {
        if (partes < 3) partes++;
    }

    // força o contador de partes para um valor, usado pelo debug
    public void forcarPartes(int valor) {
        partes = Math.min(3, Math.max(0, valor));
    }

    public boolean isUmbra()       { return mundoUmbra; }
    public int     getPartes()     { return partes; }
    public int     getMissao()     { return missao; }
    public boolean isDestrancada() { return destrancada; }
    public boolean isPecaEspelho() { return pecaEspelho; }
    public boolean isPecaGaveta()  { return pecaGaveta; }
    public boolean isPecaNpc()     { return pecaNpc; }
    public boolean isSabePalavra() { return sabePalavra; }

    public String  getAviso()      { return aviso; }
    public boolean isCinematica()  { return cinematica; }
    public boolean isEspelho()     { return abriuEspelho; }
    public boolean isGaveta()      { return abriuGaveta; }
}
