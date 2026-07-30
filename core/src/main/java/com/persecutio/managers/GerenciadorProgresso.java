package com.persecutio.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.persecutio.entities.EntidadeMapa;
import com.persecutio.entities.Jogador;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Progresso da historia do jogo
public class GerenciadorProgresso {

    // Folga para interacao
    private static final float FOLGA = 8f;

    // Referencias opcionais a sistemas externos usadas apenas para efeitos
    private final GerenciadorColisao colisao;

    // Indica se esta no mundo Umbra
    private boolean mundoUmbra = false;
    // Missao atual
    private int missao = 1;
    // Documentos lidos
    private int documentos = 1;

    // Subfase da missao para controle de objetivos
    private int faseMissao = 0;
    // Indica se o documento opcional do jardim foi lido
    private boolean lidoJardim = false;
    // Controla acionamento do evento de pilula para fade
    private boolean eventoPilula = false;
    // Controla acionamento do evento de documento umbra para fade
    private boolean eventoDocumento = false;

    // Posicao salva no jardim para fast travel na Missao 2
    private final Vector2 posicaoJardim = new Vector2();
    private boolean jardimSalvo = false;

    // Pool de flags de acoes ativas no progresso
    private final Set<String> flags = new HashSet<>();

    // Nomes dos documentos ja lidos em ambos os mundos
    private final Set<String> documentosLidos = new HashSet<>();

    // Mantém o sorteio dos panfletos durante toda a partida
    private final Map<String, String> panfletoPorPonto = new HashMap<>();
    private final Set<String> pontosPanfletoLidos = new HashSet<>();
    private final Set<String> tiposPanfletoLidos = new HashSet<>();
    private int pontosPanfletos = 0;
    private static final int PANFLETOS_PARA_ELIMAR2 = 5;

    // Mensagem de aviso atual
    private String aviso = "";
    // No de dialogo a iniciar
    private String dialogoAlvo = null;

    // Controla leitura agendada de imagem de documento
    private boolean docPendente = false;
    private String docChave = "";

    // Indica se esta em cinematica
    private boolean cinematica = false;
    // Indica se acionou espelho
    private boolean abriuEspelho = false;
    // Indica se acionou gaveta
    private boolean abriuGaveta = false;

    // Flag para teleportar o jogador ao mundo real apos fechar documento umbra
    private boolean teleportarAposDocUmbra = false;
    private boolean dialogoRevelacaoDocUmbra = false;
    private boolean dialogoMusicaUmbraM2 = false;

    // Retangulo temporario reutilizavel
    private final Rectangle rectTemp = new Rectangle();

    public GerenciadorProgresso(GerenciadorColisao colisao) {
        this.colisao = colisao;
        sortearPanfletos();
    }

    // Sorteia uma vez por nova instancia partida qual conteudo ocupa cada ponto do mapa
    private void sortearPanfletos() {
        List<String> tipos = new ArrayList<>();
        Collections.addAll(tipos, "fisica", "patrimonial", "moral", "psicologica", "sexual");
        Collections.shuffle(tipos);
        for (int i = 0; i < tipos.size(); i++) {
            panfletoPorPonto.put("panfleto" + (i + 1), tipos.get(i));
        }
    }

    // Normaliza tanto a grafia correta quanto o antigo planfeto usado nos assets mapa
    private String normalizarPontoPanfleto(String nome) {
        String chave = nome == null ? "" : nome.toLowerCase().trim().replace("planfeto", "panfleto");
        String digitos = chave.replaceAll("[^0-9]", "");
        return "panfleto" + (digitos.isEmpty() ? "1" : digitos);
    }

    public boolean ehPanfleto(String nome) {
        if (nome == null) return false;
        String chave = nome.toLowerCase().trim();
        return chave.startsWith("panfleto") || chave.startsWith("planfeto");
    }

    // Registra a descoberta concede um ponto e agenda a arte sorteada para leitura
    public void onPanfletoFound(String nome) {
        String ponto = normalizarPontoPanfleto(nome);
        if (pontosPanfletoLidos.contains(ponto)) {
            dialogoAlvo = "maria_panfleto_repetido";
            return;
        }

        String tipo = panfletoPorPonto.get(ponto);
        if (tipo == null) return;

        pontosPanfletoLidos.add(ponto);
        tiposPanfletoLidos.add(tipo);
        pontosPanfletos++;
        lerDocumento("panfleto_" + tipo);
    }

    public Set<String> obterPanfletosLidos() {
        return new HashSet<>(tiposPanfletoLidos);
    }

    public int obterPontosPanfletos() { return pontosPanfletos; }

    public boolean podeEntrarElimar2() {
        return pontosPanfletos >= PANFLETOS_PARA_ELIMAR2;
    }

    public void completarPanfletosParaTeste() {
        pontosPanfletos = PANFLETOS_PARA_ELIMAR2;
        while (tiposPanfletoLidos.size() < PANFLETOS_PARA_ELIMAR2) {
            tiposPanfletoLidos.add("teste" + tiposPanfletoLidos.size());
        }
    }

    // Alterna entre mundo Real e Umbra
    public void alternarUmbra() {
        mundoUmbra = !mundoUmbra;
    }

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

    public boolean deveIniciarDialogoAposFecharDocumento(String chave) {
        if (chave == null || chave.isEmpty()) return false;
        String chaveNormalizada = chave.toLowerCase().trim();
        return "documento1".equals(chaveNormalizada)
            || "documento1_real".equals(chaveNormalizada)
            || "documento1_umbra".equals(chaveNormalizada);
    }

    public boolean deveExibirSpriteElimarAposFecharDocumento(String chave) {
        if (chave == null || chave.isEmpty()) return false;
        String chaveNormalizada = chave.toLowerCase().trim();
        return "documento3".equals(chaveNormalizada);
    }

    public String obterChave() { return docChave; }

    public boolean temCartela() { return temFlag("temcartela"); }

    public boolean jardimSalvo() { return jardimSalvo; }

    public Vector2 posicaoJardim() { return posicaoJardim; }

    // Salva a posicao atual do jogador se ele estiver no jardim no mundo real na Missao 2
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
        faseMissao = 1;
    }

    // Marca o puzzle de pedras como resolvido e abre a porta do Jardim
    public void resolverPuzzle() {
        faseMissao = 3;
        if (colisao != null) {
            try {
                colisao.destrancar("portaEscritorioJardim");
                colisao.destrancar("portaEscritorioJardim2");
            } catch (Exception ignored) {}
        }
        dialogoAlvo = "porta_clique";
    }

    // Finaliza a leitura do prontuario de Umbra na Missao 1 e aguarda o despertar
    public void lerUmbra() {
        faseMissao = 6;
    }

    // Conclui a Missao 1 e retorna ao mundo real no spawn inicial
    public void concluirPrimeira(float spawnX, float spawnY) {
        missao = 2;
        faseMissao = 0;
        darFlag("porta_destrancada");
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

    // Handler quando o jogador interage com um NPC
    public void onNpcInteract(String npcKey) {
        if (npcKey == null) return;
        String chave = npcKey.toLowerCase().trim();
        if ("enfermeira".equals(chave)) {
            if (!temFlag("falou_enfermeira")) {
                darFlag("falou_enfermeira");
                dialogoAlvo = "enfermeira";
                // Primeira conversa ja avanca direto para fase 2 permitindo pegar a pilula
                if (missao == 1 && faseMissao <= 1) faseMissao = 2;
            } else {
                // Apos a primeira conversa repete uma fala curta em loop
                dialogoAlvo = "enfermeira_volte_quarto";
                if (missao == 1 && faseMissao == 1) faseMissao = 2;
            }
        }
    }

    // Handler quando o jogador interage com um objeto
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
                    // A flag temcartela so e concedida ao final do dialogo via efeito
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

            case "elimar2":
                if (!podeEntrarElimar2()) {
                    aviso = "Ainda faltam panfletos para entrar neste escritorio.";
                    return;
                }
                dialogoAlvo = "elimar2_trigger";
                lerDocumento("documento3");
                return;

            default:
                break;
        }
    }

    // Handler generico para documentos lidos
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

    // Handler para interacoes com portas retornando instrucoes para a UI
    public PortaResponse onPortaInteract(com.persecutio.managers.GerenciadorPortas.Porta porta) {
        if (porta == null) return PortaResponse.cont();

        String nome = porta.nome != null ? porta.nome.toLowerCase() : "";

        if (mundoUmbra && missao == 2 && faseMissao == 1) {
            if (nome.contains("jardim") || nome.contains("escritorio")) {
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

            if (missao == 1 && faseMissao == 4) {
                faseMissao = 5;
            }
            return true;
        }

        aviso = "A porta nao abre. Acho que nao e esta a senha.";
        return false;
    }

    // Limpa aviso quando jogador sai da area de interacao
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

        String nome = porta.nome != null ? porta.nome.toLowerCase() : "";
        if (mundoUmbra && nome.contains("elimar2")) {
            return podeEntrarElimar2();
        }

        return temFlag("porta_destrancada");
    }

    public void darAviso(String msg) { this.aviso = msg; }

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

    public Set<String> obterFlags() {
        return new HashSet<>(flags);
    }

    public void ativarCinematica() { this.cinematica = true; }

    public boolean isUmbra() { return mundoUmbra; }

    public int getMissao() { return missao; }

    public int getDocumentos() { return documentos; }

    public boolean isDestrancada() { return temFlag("porta_destrancada"); }

    public boolean falouEnfermeira() { return temFlag("falou_enfermeira"); }

    public boolean leuDoc(String nome) { return documentosLidos.contains(nome); }

    public String lerAviso() { return aviso; }

    public boolean isCinematica() { return cinematica; }

    public boolean isEspelho() { return abriuEspelho; }

    // Consome o evento de abrir o espelho evitando reabertura indevida
    public boolean consumirEspelho() {
        boolean r = abriuEspelho;
        abriuEspelho = false;
        return r;
    }

    public boolean isGaveta() { return abriuGaveta; }

    // Consome o evento de abrir a gaveta evitando reabertura indevida
    public boolean consumirGaveta() {
        boolean r = abriuGaveta;
        abriuGaveta = false;
        return r;
    }
}
