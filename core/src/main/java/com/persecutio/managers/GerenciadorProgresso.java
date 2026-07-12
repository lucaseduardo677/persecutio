package com.persecutio.managers;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
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

    // Subfase da Missao para controle de objetivos
    private int faseMissao = 0;
    // Flag se o documento opcional do jardim foi lido
    private boolean lidoJardim = false;
    // Flag de acionamento do evento de pílula para fade
    private boolean eventoPilula = false;
    // Flag de acionamento do evento de documento umbra para fade
    private boolean eventoDocumento = false;

    // Posicao salva no jardim para fast-travel na Missao 2
    private final Vector2 posicaoJardim = new Vector2();
    private boolean jardimSalvo = false;
    private boolean temCartela = false;

    // Flag se a porta foi destrancada
    private boolean destrancada  = false;
    // Flag se sabe a palavra magica / senha
    private boolean sabePalavra  = false;
    // Flag se pegou peca do espelho
    private boolean pecaEspelho  = false;
    // Flag se pegou peca da gaveta
    private boolean pecaGaveta   = false;
    // Flag se pegou peca do NPC
    private boolean pecaNpc      = false;
    // Flag se ja falou com a enfermeira na recepcao
    private boolean falouComEnfermeira = false;
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

    // Retangulo temporario
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

    // Retorna a subfase atual da missao
    public int getFaseMissao() { return faseMissao; }

    // Altera a subfase da missao de forma manual
    public void setFaseMissao(int novaFase) {
        faseMissao = novaFase;
    }

    // Consome o evento de pilula pendente
    public boolean consumirPilula() {
        boolean r = eventoPilula;
        eventoPilula = false;
        return r;
    }

    // Consome o evento de documento pendente
    public boolean consumirDocumento() {
        boolean r = eventoDocumento;
        eventoDocumento = false;
        return r;
    }

    // Retorna se o jogador ja possui a cartela de pilulas
    public boolean hasCartela() { return temCartela; }

    // Retorna se o ponto de fast-travel do jardim ja foi salvo
    public boolean isJardimSalvo() { return jardimSalvo; }

    // Obtem a coordenada salva do jardim
    public Vector2 getPosicaoJardim() { return posicaoJardim; }

    // Salva a posicao atual do jogador se ele estiver no jardim no mundo real (Missao 2)
    public void salvarPosicaoJardim(float x, float y) {
        if (!mundoUmbra && missao == 2) {
            posicaoJardim.set(x, y);
            jardimSalvo = true;
        }
    }

    // Jogador recolhe a cartela inteira na Missao 2 e viaja para o Umbra
    public void pegarCartela() {
        temCartela = true;
        mundoUmbra = true;
        faseMissao = 1; // "Investigue a origem do som."
    }

    // Marca o puzzle de pedras como resolvido e abre a porta do Jardim
    public void resolverPuzzle() {
        faseMissao = 3; // "Verifique a porta do Jardim."
        colisao.destrancar("portaEscritorioJardim");
        colisao.destrancar("portaEscritorioJardim2");
    }

    // Finaliza a leitura do prontuario de Umbra na Missao 1 e aguarda prompt de acordar
    public void lerDocumentoUmbra() {
        faseMissao = 6;
        aviso = "Para acordar aperte [E]";
    }

    // Conclui a Missao 1 e retorna ao mundo real no spawn inicial
    public void concluirMissao1(float spawnX, float spawnY) {
        missao = 2;
        faseMissao = 0; // "Tome seu remedio."
        destrancada = true; // Porta do quarto aberta no mundo real
        mundoUmbra = false;
        aviso = "";
    }

    // Conclui a Missao 2 e prepara para a proxima
    public void concluirMissao2() {
        missao = 3;
        faseMissao = 4;
        aviso = "[Missao 2 Concluida!]";
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

    // Interacoes no mundo Real (Recepcionista/Enfermeira unificadas)
    private void interagirReal(Rectangle hitboxInteracao) {
        EntidadeMapa enfermeira = colisao.getNpc("enfermeira", false);
        if (enfermeira == null) {
            enfermeira = colisao.getNpc("npcRecepcao", false);
        }

        if (enfermeira != null && hitboxInteracao.overlaps(enfermeira.area)) {
            falouComEnfermeira = true;
            dialogoAlvo = "enfermeira";

            // Missao 1, fase inicial -> avanca ao falar com a recepcionista
            if (missao == 1 && faseMissao == 0) {
                faseMissao = 1; // "Fale com a recepcionista." (Missao 1)
            }
            if (missao == 1 && faseMissao == 1) {
                faseMissao = 2; // "Volte ao seu quarto e tome o remédio." (Missao 1)
            }
            return;
        }

        GerenciadorColisao.ObjetoColisao pilula = colisao.getInterativo("pilula", false);
        if (pilula != null && hitboxInteracao.overlaps(pilula.area)) {
            if (!falouComEnfermeira) {
                aviso = "A enfermeira da recepcao ainda nao autorizou a pílula.";
                return;
            }

            // Se estiver na Missão 1 e fase correta, sinaliza o fade para o primeiro mergulho
            if (missao == 1 && faseMissao == 2) {
                eventoPilula = true;
                return;
            }

            // Se estiver na Missão 2 e fase inicial (Tome seu remédio), aciona o dialogo de recolher a cartela inteira
            if (missao == 2 && faseMissao == 0) {
                dialogoAlvo = "maria_pega_pilulas";
                return;
            }

            mundoUmbra = true;
            return;
        }

        GerenciadorColisao.ObjetoColisao doc = colisao.getInterativo("documento1", false);
        if (doc == null) doc = colisao.getInterativo("documento", false);

        if (doc != null && hitboxInteracao.overlaps(doc.area)) {
            if (!destrancada) {
                aviso = "As letras estao borradas, parecem dancar. Nao consigo ler...";
            } else if (!leuDocumento) {
                leuDocumento = true;
                documentos++;
                missao = 2;
                faseMissao = 0; // Inicia a Missao 2 na fase 0
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
            // Se olhar no espelho em Umbra na missão 1 (fase 3 - "Saia do quarto", que muda para fase 4)
            if (missao == 1 && (faseMissao == 3 || faseMissao == 4)) {
                faseMissao = 4; // "Descubra como abrir a porta."
                dialogoAlvo = "espelho_umbra";
                return;
            }

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
            return;
        }

        // Leitura do prontuario da paciente 103 na recepcao de Umbra (finaliza Missao 1)
        GerenciadorColisao.ObjetoColisao doc = colisao.getInterativo("documento1", true);
        if (doc == null) doc = colisao.getInterativo("documento", true);

        if (doc != null && hitboxInteracao.overlaps(doc.area)) {
            if (missao == 1 && faseMissao == 5) {
                eventoDocumento = true; // Sinaliza fim da Missao 1 para TelaJogo
                return;
            }
        }

        // Documento opcional do jardim na Missao 2
        if (faseMissao >= 3) {
            GerenciadorColisao.ObjetoColisao banco = colisao.getInterativo("banco", true);
            if (banco == null) {
                banco = colisao.getInterativo("banco_jardim", true);
            }
            if (banco != null && hitboxInteracao.overlaps(banco.area)) {
                if (!lidoJardim) {
                    lidoJardim = true;
                    dialogoAlvo = "doc_jardim";
                }
            }
        }
    }

    // Valida a senha da gaveta
    public boolean validarSenha(String senha) {
        if (pecaGaveta) return true;

        if ("0410".equals(senha)) {
            pecaGaveta  = true;
            sabePalavra = true;
            partes++;
            aviso = "Voce destrancou a gaveta e obteve a palavra 'Redencao'. A porta do quarto agora pode ser aberta!";

            // Avanca fase da Missao 1 se estiver nela
            if (missao == 1 && faseMissao == 4) {
                faseMissao = 5; // "Leia o documento na recepcao."
            }
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

    // Verifica se pode destrancar uma porta (requisito de pecas removido, exige apenas a senha)
    public boolean pordeDestrancar(GerenciadorPortas.Porta porta) {
        return podeDestrancar(porta);
    }

    public boolean podeDestrancar(GerenciadorPortas.Porta porta) {
        if (!porta.trancado)     return true;
        if (!porta.destrancavel) return false;

        // Exige apenas a confirmacao da senha correta inserida na gaveta
        return sabePalavra;
    }

    // Define mensagem de aviso
    public void setAviso(String msg) { this.aviso = msg; }

    // Adicionar uma parte coletada
    public void adicionarParte() {
        if (partes < 2) partes++;
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
        partes = Math.min(2, Math.max(0, valor));
    }

    // Retorna se esta no mundo Umbra
    public boolean isUmbra()       { return mundoUmbra; }

    // Retorna quantidade de partes
    public int     getPartes()     { return partes; }

    // Retorna missao atual
    public int     getMissao()     { return missao; }

    // Retorna quantidade de documentos lidos
    public int     getDocumentos() { return documentos; }

    // Retorna se porta esta destrancada
    public boolean isDestrancada() { return destrancada; }

    // Retorna se pegou peca do espelho
    public boolean isPecaEspelho() { return pecaEspelho; }

    // Retorna se pegou peca da gaveta
    public boolean isPecaGaveta()  { return pecaGaveta; }

    // Retorna se pegou peca do NPC
    public boolean isPecaNpc()     { return pecaNpc; }

    // Retorna se ja falou com a enfermeira na recepcao
    public boolean isFalouComEnfermeira() { return falouComEnfermeira; }

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
