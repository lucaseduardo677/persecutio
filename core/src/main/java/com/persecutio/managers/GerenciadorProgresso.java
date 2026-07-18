package com.persecutio.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.persecutio.entities.EntidadeMapa;
import com.persecutio.entities.Jogador;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// Progresso da historia do jogo
public class GerenciadorProgresso {

    // Folga para interacao
    private static final float FOLGA = 8f;

    // Referencias opcionais a sistemas externos (usadas apenas para efeitos)
    private final GerenciadorColisao colisao;

    // Flag se esta no mundo Umbra
    private boolean mundoUmbra = false;
    // Missao atual
    private int missao = 1;
    // Documentos lidos
    private int documentos = 1;

    // Subfase da Missao para controle de objetivos
    private int faseMissao = 0;
    // Flag se o documento opcional do jardim foi lido
    private boolean lidoJardim = false;
    // Flag de acionamento do evento de pilula para fade
    private boolean eventoPilula = false;
    // Flag de acionamento do evento de documento umbra para fade
    private boolean eventoDocumento = false;

    // Posicao salva no jardim para fast-travel na Missao 2
    private final Vector2 posicaoJardim = new Vector2();
    private boolean jardimSalvo = false;

    // Pool de flags de acoes ativas no progresso (Arquitetura dinamica)
    private final Set<String> flags = new HashSet<>();

    // Nomes dos documentos ja lidos (Real e Umbra), evita releitura e permite
    // qualquer quantidade de documentos sem codigo especifico
    private final Set<String> documentosLidos = new HashSet<>();

    // Mensagem de aviso atual
    private String aviso = "";
    // No de dialogo a iniciar (blade-ink)
    private String dialogoAlvo = null;

    // Flag de leitura agendada de imagem de documento
    private boolean docPendente = false;
    private String docChave = "";

    // Flag se esta em cinematica
    private boolean cinematica = false;
    // Flag se acionou espelho
    private boolean abriuEspelho = false;
    // Flag se acionou gaveta
    private boolean abriuGaveta = false;

    // Flag para teleportar o jogador ao mundo real apos fechar documento umbra
    private boolean teleportarAposDocUmbra = false;
    private boolean dialogoRevelacaoDocUmbra = false;
    private boolean dialogoMusicaUmbraM2 = false;

    // Retangulo temporario
    private final Rectangle rectTemp = new Rectangle();

    // Construtor do progresso
    public GerenciadorProgresso(GerenciadorColisao colisao) {
        this.colisao = colisao;
    }

    // Alterna entre mundo Real e Umbra
    public void alternarUmbra() {
        mundoUmbra = !mundoUmbra;
    }

    // Retorna a subfase atual da missao
    public int obterFase() { return faseMissao; }

    // Altera a subfase da missao de forma manual
    public void mudarFase(int novaFase) {
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

    // Agenda a abertura visual do documento
    public void lerDocumento(String chave) {
        docPendente = true;
        docChave = chave;
    }

    // Consome e limpa a flag de exibicao de documento
    public boolean consumirPendente() {
        boolean r = docPendente;
        docPendente = false;
        return r;
    }

    public String obterChave() { return docChave; }

    // Retorna se o jogador ja possui a cartela de pilulas
    public boolean temCartela() { return temFlag("temcartela"); }

    // Retorna se o ponto de fast-travel do jardim ja foi salvo
    public boolean jardimSalvo() { return jardimSalvo; }

    // Obtem a coordenada salva do jardim
    public Vector2 posicaoJardim() { return posicaoJardim; }

    // Salva a posicao atual do jogador se ele estiver no jardim no mundo real (Missao 2)
    private void salvarJardim(float x, float y) {
        if (!mundoUmbra && missao == 2) {
            posicaoJardim.set(x, y);
            jardimSalvo = true;
        }
    }

    // Jogador recolhe a cartela inteira na Missao 2 e viaja para o Umbra
    public void pegarCartela() {
        darFlag("temcartela");
        mundoUmbra = true;
        faseMissao = 1; // "Investigue a origem do som."
    }

    // Marca o puzzle de pedras como resolvido e abre a porta do Jardim
    public void resolverPuzzle() {
        faseMissao = 3; // "Verifique a porta do Jardim."
        if (colisao != null) {
            try {
                colisao.destrancar("portaEscritorioJardim");
                colisao.destrancar("portaEscritorioJardim2");
            } catch (Exception ignored) {}
        }
        // Compatibilidade com a UI: agenda o dialogo de clique de porta
        dialogoAlvo = "porta_clique";
    }

    // Finaliza a leitura do prontuario de Umbra na Missao 1 e aguarda o despertar automatico
    public void lerUmbra() {
        faseMissao = 6;
    }

    // Conclui a Missao 1 e retorna ao mundo real no spawn inicial
    public void concluirPrimeira(float spawnX, float spawnY) {
        missao = 2;
        faseMissao = 0; // "Tome seu remedio."
        darFlag("porta_destrancada"); // Porta do quarto aberta no mundo real
        mundoUmbra = false;
        aviso = "";
    }

    // Conclui a Missao 2 e prepara para a proxima
    public void concluirSegunda() {
        missao = 3;
        faseMissao = 4;
        aviso = "[Missao 2 Concluida!]";
    }

    // Cria hitbox com folga para interacao
    public Rectangle hitboxFolga(Jogador jogador) {
        rectTemp.set(
            jogador.hitbox.x - FOLGA,
            jogador.hitbox.y - FOLGA,
            jogador.hitbox.width + FOLGA * 2f,
            jogador.hitbox.height + FOLGA * 2f
        );
        return rectTemp;
    }

    // Handler quando o jogador interage com um NPC (ex: "enfermeira")
    public void onNpcInteract(String npcKey) {
        if (npcKey == null) return;
        String chave = npcKey.toLowerCase().trim();
        if ("enfermeira".equals(chave)) {
            if (!temFlag("falou_enfermeira")) {
                darFlag("falou_enfermeira");
                dialogoAlvo = "enfermeira";
                // CORRECAO: Primeira conversa ja avanca direto para fase 2,
                // permitindo pegar a pilula logo apos falar com a enfermeira
                if (missao == 1 && faseMissao <= 1) faseMissao = 2;
            } else {
                // Apos a primeira conversa, so repete uma fala curta em loop
                dialogoAlvo = "enfermeira_volte_quarto";
                if (missao == 1 && faseMissao == 1) faseMissao = 2;
            }
        }
    }

    // Handler quando o jogador interage com um objeto (ex: "pilula", "espelho", "gaveta")
    public void onObjectInteract(String objectKey) {
        if (objectKey == null) return;
        String chave = objectKey.toLowerCase().trim();

        switch (chave) {
            case "pilula":
                if (!temFlag("falou_enfermeira")) {
                    aviso = "A enfermeira da recepcao ainda nao autorizou a pilula.";
                    return;
                }
                if (missao == 1 && faseMissao == 2) {
                    eventoPilula = true;
                    return;
                }
                if (missao == 2 && faseMissao == 0 && !mundoUmbra) {
                    // A flag "temcartela" so e concedida ao final deste dialogo
                    // (efeito tomar_pilula_missao2), nao apenas por tocar na cabeceira
                    dialogoAlvo = "maria_pega_pilulas";
                    return;
                }
                if (missao == 2 && mundoUmbra) {
                    dialogoAlvo = "maria_jardim_umbra";
                    return;
                }
                return;

            case "espelho":
                if (missao == 1 && (faseMissao == 3 || faseMissao == 4)) {
                    faseMissao = 4;
                    dialogoAlvo = "espelho_umbra";
                    return;
                }
                abriuEspelho = true;
                if (!temFlag("espelho_visto")) {
                    darFlag("espelho_visto");
                }
                break;

            case "gaveta":
                if (!temFlag("senha_revelada")) {
                    abriuGaveta = true;
                }
                break;

            case "cama":
                mundoUmbra = false;
                break;

            default:
                // objetos genericos nao disparam logica de progresso aqui
                break;
        }
    }

    // Handler generico para documentos lidos (nome do objeto e opcional docId)
    public void onDocumentFound(String nome, String docId, boolean isUmbra) {
        if (nome == null) return;
        String chave = nome.toLowerCase().trim();

        if (!isUmbra) {
            if (!temFlag("porta_destrancada")) {
                aviso = "As letras estao borradas, parecem dancar. Nao consigo ler...";
                return;
            }
            if (!documentosLidos.contains(chave)) {
                documentosLidos.add(chave);
                documentos++;
                String chaveDoc = (docId != null && !docId.isEmpty()) ? docId : chave;
                lerDocumento(chaveDoc);

                if (missao == 1) faseMissao = 0;
            } else {
                aviso = "Voce ja leu este documento.";
            }
        } else {
            if (!documentosLidos.contains(chave)) {
                documentosLidos.add(chave);
                documentos++;
                // Documento1 no umbra: mostra documento e agenda teleporte ao fechar
                if ("documento1".equals(chave)) {
                    lerDocumento("documento1_umbra");
                    teleportarAposDocUmbra = true;
                } else {
                    String chaveDoc = (docId != null && !docId.isEmpty()) ? docId : chave + "_umbra";
                    lerDocumento(chaveDoc);
                }
            }
        }
    }

    // Consome e retorna a flag de teleporte apos documento umbra
    public boolean consumirTeleporteAposDocUmbra() {
        boolean r = teleportarAposDocUmbra;
        teleportarAposDocUmbra = false;
        return r;
    }

    public boolean isTeleporteAposDocUmbra() {
        return teleportarAposDocUmbra;
    }

    public void agendarDialogoMusicaUmbraM2() {
        dialogoMusicaUmbraM2 = true;
    }

    public boolean consumirDialogoMusicaUmbraM2() {
        boolean r = dialogoMusicaUmbraM2;
        dialogoMusicaUmbraM2 = false;
        return r;
    }

    public void agendarDialogoRevelacaoDocUmbra() {
        dialogoRevelacaoDocUmbra = true;
    }

    public boolean consumirDialogoRevelacaoDocUmbra() {
        boolean r = dialogoRevelacaoDocUmbra;
        dialogoRevelacaoDocUmbra = false;
        return r;
    }

    // Notifica que o puzzle de pedras foi resolvido
    public void onPuzzleSolved() {
        resolverPuzzle();
    }

    // Resposta de interacao com porta para a UI agir de acordo
    public static class PortaResponse {
        public enum Action { DIALOG, FADE_MOVE_AND_CONCLUDE, CONTINUE }
        public final Action action;
        public final String dialogNode;
        public final String video;
        public final boolean usarFade;

        public PortaResponse(Action action, String dialogNode, String video, boolean usarFade) {
            this.action = action;
            this.dialogNode = dialogNode;
            this.video = video;
            this.usarFade = usarFade;
        }

        public static PortaResponse dialog(String node) { return new PortaResponse(Action.DIALOG, node, null, false); }
        public static PortaResponse fadeMoveAndConclude(String video, boolean usarFade) { return new PortaResponse(Action.FADE_MOVE_AND_CONCLUDE, null, video, usarFade); }
        public static PortaResponse cont() { return new PortaResponse(Action.CONTINUE, null, null, false); }
    }

    // Handler para interacoes com portas: retorna instrucoes para a UI
    public PortaResponse onPortaInteract(com.persecutio.managers.GerenciadorPortas.Porta porta) {
        if (porta == null) return PortaResponse.cont();

        String nome = porta.nome != null ? porta.nome.toLowerCase() : "";

        if (mundoUmbra && missao == 2 && faseMissao == 1) {
            if (nome.contains("jardim") || nome.contains("escritorio")) {
                // Porta emperrada: mostra dialogo e avanca fase
                mudarFase(2);
                return PortaResponse.dialog("porta_emperrada");
            }
        }

        if (mundoUmbra && missao == 2 && faseMissao == 3) {
            if (nome.contains("jardim") || nome.contains("escritorio")) {
                return PortaResponse.fadeMoveAndConclude(porta.video, porta.usarFade);
            }
        }

        return PortaResponse.cont();
    }

    // Salva posicao do jardim
    public void onSaveJardim(float x, float y) {
        salvarJardim(x, y);
    }

    // Valida a senha via evento
    public boolean onPasswordEntered(String senha) {
        return validarSenha(senha);
    }

    // Valida a senha da gaveta
    private boolean validarSenha(String senha) {
        if (temFlag("senha_revelada")) return true;

        if ("0410".equals(senha)) {
            darFlag("senha_revelada");
            darFlag("porta_destrancada");
            aviso = "A porta abriu...";

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
    public void checarLonge(Jogador jogador) {
        if (aviso.isEmpty()) return;

        Rectangle hitboxInteracao = hitboxFolga(jogador);

        for (Rectangle area : colisao.getInterativos().values()) {
            if (hitboxInteracao.overlaps(area)) return;
        }

        for (EntidadeMapa npc : colisao.getNpcs().values()) {
            if (hitboxInteracao.overlaps(npc.area)) return;
        }

        aviso = "";
    }

    // Verifica se pode destrancar uma porta
    public boolean podeDestrancar(GerenciadorPortas.Porta porta) {
        if (!porta.trancado) return true;
        if (!porta.destrancavel) return false;

        return temFlag("porta_destrancada");
    }

    // Define mensagem de aviso
    public void darAviso(String msg) { this.aviso = msg; }

    // Retorna e limpa o no de dialogo pendente
    public String pegarDialogo() {
        String r = dialogoAlvo;
        dialogoAlvo = null;
        return r;
    }

    // Verifica se uma flag de acao especifica esta ativa
    public boolean temFlag(String flag) {
        return flags.contains(flag.toLowerCase().trim());
    }

    // Ativa uma flag no pool
    public void darFlag(String flag) {
        flags.add(flag.toLowerCase().trim());
    }

    // Remove uma flag do pool
    public void tirarFlag(String flag) {
        flags.remove(flag.toLowerCase().trim());
    }

    // Obtem uma copia de todas as acoes completas/flags ativas
    public Set<String> obterFlags() {
        return new HashSet<>(flags);
    }

    public void ativarCinematica() { this.cinematica = true; }

    // Retorna se esta no mundo Umbra
    public boolean isUmbra() { return mundoUmbra; }

    // Retorna missao atual
    public int getMissao() { return missao; }

    // Retorna quantidade de documentos lidos
    public int getDocumentos() { return documentos; }

    // Retorna se porta esta destrancada
    public boolean isDestrancada() { return temFlag("porta_destrancada"); }

    // Retorna se ja falou com a enfermeira na recepcao
    public boolean falouEnfermeira() { return temFlag("falou_enfermeira"); }

    // Retorna se um documento (pelo nome do objeto no Tiled) ja foi lido
    public boolean leuDoc(String nome) { return documentosLidos.contains(nome); }

    // Retorna mensagem de aviso
    public String lerAviso() { return aviso; }

    // Retorna se esta em cinematica
    public boolean isCinematica() { return cinematica; }

    // Retorna se abriu o espelho
    public boolean isEspelho() { return abriuEspelho; }

    // Consome o evento de abrir o espelho
    public boolean consumirEspelho() {
        boolean r = abriuEspelho;
        abriuEspelho = false;
        return r;
    }

    // Retorna se abriu a gaveta
    public boolean isGaveta() { return abriuGaveta; }

    // Consome o evento de abrir a gaveta
    public boolean consumirGaveta() {
        boolean r = abriuGaveta;
        abriuGaveta = false;
        return r;
    }
}
