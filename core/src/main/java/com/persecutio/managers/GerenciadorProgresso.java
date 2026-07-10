package com.persecutio.managers;

import com.badlogic.gdx.math.Rectangle;
import com.persecutio.entities.EntidadeMapa;
import com.persecutio.entities.Jogador;

import java.util.Map;

// Progresso da historia do jogo
public class GerenciadorProgresso {

    // Folga para interacao
    private static final float FOLGA = 8f;

    // Referencia ao sistema de colisao
    private final GerenciadorColisao colisao;
    // Referencia ao gerenciador de portas
    private final GerenciadorPortas  portas;

    // Flag se esta no mundo Umbra
    private boolean mundoUmbra  = false;
    // Missao atual
    private int     missao      = 1;
    // Documentos lidos
    private int     documentos  = 1;

    // Partes coletadas
    private int     partes      = 0;

    // Flag se a porta foi destrancada
    private boolean destrancada  = false;
    // Flag se sabe a palavra magica
    private boolean sabePalavra  = false;
    // Flag se pegou peca do espelho
    private boolean pecaEspelho  = false;
    // Flag se pegou peca da gaveta
    private boolean pecaGaveta   = false;
    // Flag se pegou peca do NPC
    private boolean pecaNpc      = false;
    // Flag se leu o documento
    private boolean leuDocumento = false;

    // Mensagem de aviso atual
    private String  aviso        = "";
    // No de dialogo a iniciar (blade-ink)
    private String  dialogoAlvo  = null;

    // Flag se esta em cinematica
    private boolean cinematica   = false;
    // Flag se abriu o espelho
    private boolean abriuEspelho = false;
    // Flag se abriu a gaveta
    private boolean abriuGaveta  = false;

    // Retangulo temporario para interacao
    private final Rectangle rectTemp = new Rectangle();

    // Construtor do progresso
    public GerenciadorProgresso(GerenciadorColisao colisao, GerenciadorPortas portas) {
        this.colisao = colisao;
        this.portas  = portas;
    }

    // Alterna entre mundo Real e Umbra
    public void alternarUmbra() {
        mundoUmbra = !mundoUmbra; 
    }

    // Cria hitbox com folga para interacao
    private Rectangle hitboxFolga(Jogador jogador) {
        rectTemp.set(
            jogador.hitbox.x - FOLGA,
            jogador.hitbox.y - FOLGA,
            jogador.hitbox.width  + FOLGA * 2f,
            jogador.hitbox.height + FOLGA * 2f
        );
        return rectTemp;
    }

    // Processa interacao do jogador com objetos
    public void tratarInteracao(Jogador jogador) {
        cinematica   = false;
        abriuEspelho = false;
        abriuGaveta  = false;
        dialogoAlvo  = null;

        Rectangle hitboxInteracao = hitboxFolga(jogador);

        if (!mundoUmbra) {
            interagirReal(hitboxInteracao);
        } else {
            interagirUmbra(hitboxInteracao, jogador);
        }
    }

    // Interacoes no mundo Real
    private void interagirReal(Rectangle hitboxInteracao) {
        GerenciadorColisao.ObjetoColisao pilula = colisao.getInterativo("pilula", false);
        if (pilula != null && hitboxInteracao.overlaps(pilula.area)) {
            mundoUmbra = true;
            return;
        }

        EntidadeMapa paciente = colisao.getNpc("paciente", false);
        if (paciente != null && hitboxInteracao.overlaps(paciente.area)) {
            dialogoAlvo = pecaNpc ? "paciente_feito" : sabePalavra ? "paciente_sabe" : "paciente_pergunta";
            return;
        }

        EntidadeMapa enfermeira = colisao.getNpc("enfermeira", false);
        if (enfermeira != null && hitboxInteracao.overlaps(enfermeira.area)) {
            dialogoAlvo = "enfermeira";
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

    // Interacoes no mundo Umbra
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

        GerenciadorColisao.ObjetoColisao gaveta = colisao.getInterativo("gaveta", true);
        if (gaveta != null && hitboxInteracao.overlaps(gaveta.area) && !pecaGaveta) {
            abriuGaveta = true;
        }
    }

    // Valida a senha da gaveta
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

    // Limpa aviso quando jogador sai da area
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

    // Avalia condicao para destrancar porta
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

    // Verifica se pode destrancar uma porta
    public boolean podeDestrancar(GerenciadorPortas.Porta porta) {
        if (!porta.trancado)     return true;
        if (!porta.destrancavel) return false;
        return avaliarCondicao(porta.condicao);
    }

    // Define mensagem de aviso
    public void setAviso(String msg) { this.aviso = msg; }

    // Adiciona uma parte coletada
    public void adicionarParte() {
        if (partes < 3) partes++;
    }

    // Marca que a peca do NPC foi entregue
    public void marcarPecaNpc() {
        pecaNpc = true;
    }

    // Retorna e limpa o no de dialogo pendente
    public String pegarDialogo() {
        String r    = dialogoAlvo;
        dialogoAlvo = null;
        return r;
    }

    // Forca quantidade de partes
    public void forcarPartes(int valor) {
        partes = Math.min(3, Math.max(0, valor));
    }

    // Retorna se esta no mundo Umbra
    public boolean isUmbra()       { return mundoUmbra; }

    // Retorna quantidade de partes
    public int     getPartes()     { return partes; }

    // Retorna missao atual
    public int     getMissao()     { return missao; }

    // Retorna se porta esta destrancada
    public boolean isDestrancada() { return destrancada; }

    // Retorna se pegou peca do espelho
    public boolean isPecaEspelho() { return pecaEspelho; }

    // Retorna se pegou peca da gaveta
    public boolean isPecaGaveta()  { return pecaGaveta; }

    // Retorna se pegou peca do NPC
    public boolean isPecaNpc()     { return pecaNpc; }

    // Retorna se sabe a palavra magica
    public boolean isSabePalavra() { return sabePalavra; }

    // Retorna mensagem de aviso
    public String  getAviso()      { return aviso; }

    // Retorna se esta em cinematica
    public boolean isCinematica()  { return cinematica; }

    // Retorna se abriu o espelho
    public boolean isEspelho()     { return abriuEspelho; }

    // Retorna se abriu a gaveta
    public boolean isGaveta()      { return abriuGaveta; }
}
