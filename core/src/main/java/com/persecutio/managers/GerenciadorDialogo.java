package com.persecutio.managers;

import com.badlogic.gdx.graphics.Color;
import java.util.ArrayList;
import java.util.List;

// Gerencia os dialogos do jogo
public class GerenciadorDialogo {

    // Repositorio de dialogos
    private RepoDialogos repositorio;
    // No atual
    private RepoDialogos.NoDialogo noAtual;
    // Voz animalese das falas
    private GerenciadorVoz voz;

    // Falante da linha
    private String falante = "";
    // Texto da linha
    private String texto = "";
    // Caminho da imagem
    private String retrato = null;
    // Cor do fundo
    private Color corFundo = Color.valueOf("#2b2b36");
    // Opacidade de fundo
    private float opacidade = 1.0f;

    // Indice atual
    private int idxAtual = 0;
    // Flag de dialogo
    private boolean ativo = false;

    // Flag para adiar voz e texto ate o fade terminar
    private boolean aguardandoFade = false;

    // Escolhas disponiveis
    private final List<String> escolhas = new ArrayList<>();
    // Efeitos pendentes
    private final List<String> efeitos = new ArrayList<>();

    // Instancia o gerenciador
    public GerenciadorDialogo() {
        repositorio = new RepoDialogos();
    }

    // Define o gerenciador de voz usado para falar as falas
    public void setVoz(GerenciadorVoz vozRef) {
        voz = vozRef;
    }

    // Inicia um no de dialogo com voz e texto adiados ate o fade terminar
    public void iniciar(String noAlvo) {
        noAtual = repositorio.getNo(noAlvo);
        if (noAtual == null) {
            ativo = false;
            return;
        }
        ativo = true;
        idxAtual = -1;
        escolhas.clear();
        aguardandoFade = true;
        avancar();
    }

    // Avanca a fala
    public void avancar() {
        if (!ativo || noAtual == null) return;
        if (!escolhas.isEmpty()) return;

        idxAtual++;

        if (idxAtual < noAtual.falas.size()) {
            RepoDialogos.Fala fala = noAtual.falas.get(idxAtual);
            falante = fala.falante;
            texto = fala.texto;
            retrato = fala.retrato;
            corFundo = fala.corFundo;
            opacidade = fala.opacidade;

            if (fala.efeito != null && !fala.efeito.isEmpty()) {
                efeitos.add(fala.efeito);
            }

            // So inicia a voz se nao estiver aguardando o fade terminar
            if (!aguardandoFade && voz != null) voz.falar(falante, texto);

            if (idxAtual == noAtual.falas.size() - 1) {
                if (noAtual.escolhas != null && !noAtual.escolhas.isEmpty()) {
                    for (RepoDialogos.Escolha e : noAtual.escolhas) {
                        escolhas.add(e.texto);
                    }
                }
            }
        } else {
            encerrar();
        }
    }

    // Retorna os pontos associados a uma escolha do no atual
    public int obterPontos(int indice) {
        if (noAtual == null || noAtual.escolhas == null) return 0;
        if (indice < 0 || indice >= noAtual.escolhas.size()) return 0;
        return noAtual.escolhas.get(indice).pontos;
    }

    // Processa uma escolha
    public void escolher(int indice) {
        if (noAtual == null || noAtual.escolhas == null) return;
        if (indice < 0 || indice >= noAtual.escolhas.size()) return;

        String proxNo = noAtual.escolhas.get(indice).proxNo;
        escolhas.clear();
        iniciar(proxNo);
    }

    // Finaliza o dialogo
    public void encerrar() {
        ativo = false;
        noAtual = null;
        escolhas.clear();
        aguardandoFade = false;
        if (voz != null) voz.parar();
    }

    // Coleta efeitos gerados
    public List<String> pegarEfeitos() {
        List<String> res = new ArrayList<>(efeitos);
        efeitos.clear();
        return res;
    }

    // Destrava a voz e o texto apos o fade terminar
    public void destravarAposFade() {
        if (!aguardandoFade) return;
        aguardandoFade = false;
        if (voz != null && ativo && noAtual != null && idxAtual >= 0 && idxAtual < noAtual.falas.size()) {
            RepoDialogos.Fala fala = noAtual.falas.get(idxAtual);
            voz.falar(fala.falante, fala.texto);
        }
    }

    // Retorna se esta aguardando o fade terminar para iniciar voz e texto
    public boolean isAguardandoFade() { return aguardandoFade; }

    // Verifica estado ativo
    public boolean estaAtivo() { return ativo; }

    // Verifica se possui escolha
    public boolean temEscolhas() { return !escolhas.isEmpty(); }

    // Obtem o falante
    public String getFalante() { return falante; }

    // Obtem o texto
    public String getTexto() { return texto; }

    // Obtem o texto revelado ate agora em sincronia com a fala animalese
    public String getTextoVisivel() {
        if (aguardandoFade) return "";
        if (voz == null) return texto;
        int letras = Math.min(texto.length(), voz.obterLetraAtual());
        return texto.substring(0, letras);
    }

    // Obtem as escolhas
    public List<String> getEscolhas() { return escolhas; }

    // Obtem o retrato
    public String getRetrato() { return retrato; }

    // Obtem a cor de fundo
    public Color getCorFundo() { return corFundo; }

    // Obtem a opacidade
    public float getOpacidade() { return opacidade; }
}
