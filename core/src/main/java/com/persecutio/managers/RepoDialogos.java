package com.persecutio.managers;

import com.badlogic.gdx.graphics.Color;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Repositorio de dialogos
public class RepoDialogos {

    // Classe da fala
    public static class Fala {
        // Nome do falante
        public final String falante;
        // Texto da fala
        public final String texto;
        // Efeito da fala
        public final String efeito;
        // Caminho da imagem
        public final String retrato;
        // Cor do fundo
        public final Color corFundo;
        // Opacidade do fundo
        public final float opacidade;

        // Construtor completo
        public Fala(String falante, String texto, String efeito, String retrato, Color corFundo, float opacidade) {
            this.falante   = falante;
            this.texto     = texto;
            this.efeito    = efeito;
            this.retrato   = retrato;
            this.corFundo  = corFundo != null ? corFundo : Color.valueOf("#2b2b36");
            this.opacidade = opacidade;
        }

        // Construtor parcial
        public Fala(String falante, String texto, String efeito) {
            this(falante, texto, efeito, null, null, 1.0f);
        }

        // Construtor simples
        public Fala(String falante, String texto) {
            this(falante, texto, null, null, null, 1.0f);
        }
    }

    // Classe da escolha
    public static class Escolha {
        // Texto da opcao
        public final String texto;
        // Proximo no
        public final String proxNo;

        // Construtor da escolha
        public Escolha(String texto, String proxNo) {
            this.texto  = texto;
            this.proxNo = proxNo;
        }
    }

    // Classe do no
    public static class NoDialogo {
        // Lista de falas
        public final List<Fala>    falas;
        // Lista de escolhas
        public final List<Escolha> escolhas;

        // Construtor com escolhas
        public NoDialogo(List<Fala> falas, List<Escolha> escolhas) {
            this.falas    = falas;
            this.escolhas = escolhas;
        }

        // Construtor sem escolhas
        public NoDialogo(List<Fala> falas) {
            this(falas, null);
        }
    }

    // Dicionario de nos
    private final Map<String, NoDialogo> dicNos = new HashMap<>();

    // Inicializa o repositorio
    public RepoDialogos() {
        criarDialogos();
    }

    // Retorna um no
    public NoDialogo getNo(String chave) {
        return dicNos.get(chave);
    }

    // Constroi os dialogos
    private void criarDialogos() {
        // Cor base do fundo
        Color corBase     = Color.valueOf("#2b2b36");
        // Cor do paciente
        Color corPaciente = Color.valueOf("#4c1a1a");

        // Pergunta inicial do Paciente
        // Como usar: use falas de NPCs e finalize com opcoes de escolha da Maria (protagonista nao usa Fala)
        dicNos.put("paciente_pergunta", new NoDialogo(
            Arrays.asList(
                new Fala("Paciente", "Quem e voce? O que esta fazendo aqui?", null, "img/rosto_paciente.png", corPaciente, 1.0f),
                new Fala("Paciente", "Entender? Ninguem entende nada aqui. Mas eu perdi minha peca... sem ela nao consigo lembrar.", null, "img/rosto_paciente.png", corPaciente, 1.0f)
            ),
            Arrays.asList(
                new Escolha("Que peca?", "paciente_peca"),
                new Escolha("Sinto muito, tenho que ir.", "paciente_tchau")
            )
        ));

        // Explicacao da peca
        dicNos.put("paciente_peca", new NoDialogo(
            Arrays.asList(
                new Fala("Paciente", "Uma peca brilhante. Se voce achar, traga pra mim. Eu te conto um segredo.", null, "img/rosto_paciente.png", corPaciente, 1.0f)
            )
        ));

        // Despedida do paciente
        dicNos.put("paciente_tchau", new NoDialogo(
            Arrays.asList(
                new Fala("Paciente", "Todos vao... todos sempre vao.", null, "img/rosto_paciente.png", corPaciente, 1.0f)
            )
        ));

        // Entrega da peca
        dicNos.put("paciente_sabe", new NoDialogo(
            Arrays.asList(
                new Fala("Paciente", "Voce achou! Essa peca... e minha!", null, "img/rosto_paciente.png", corPaciente, 1.0f)
            ),
            Arrays.asList(
                new Escolha("Entregar a peca.", "paciente_entrega")
            )
        ));

        // Reacao de entrega
        dicNos.put("paciente_entrega", new NoDialogo(
            Arrays.asList(
                new Fala("Paciente", "Obrigado... O segredo e: a mente cria as proprias grades.", "dar_peca", "img/rosto_paciente.png", corPaciente, 1.0f)
            )
        ));

        // Paciente ja ajudado
        dicNos.put("paciente_feito", new NoDialogo(
            Arrays.asList(
                new Fala("Paciente", "A mente cria as proprias grades... nunca se esqueca.", null, "img/rosto_paciente.png", corPaciente, 1.0f)
            )
        ));

        // Encontro da enfermeira
        dicNos.put("enfermeira", new NoDialogo(
            Arrays.asList(
                new Fala("Enfermeira", "Senhorita, esta fora do seu quarto. Volte imediatamente.", null, null, corBase, 1.0f)
            ),
            Arrays.asList(
                new Escolha("Tentar passar.", "enfermeira_passar"),
                new Escolha("Pedir desculpas.", "enfermeira_desculpa")
            )
        ));

        // Tentativa de passar
        dicNos.put("enfermeira_passar", new NoDialogo(
            Arrays.asList(
                new Fala("Enfermeira", "Ninguem sai sem autorizacao do Dr. Gonzalez.", null, null, corBase, 0.60f)
            )
        ));

        // Desculpas enfermeira
        dicNos.put("enfermeira_desculpa", new NoDialogo(
            Arrays.asList(
                new Fala("Enfermeira", "Apenas va deitar.", null, null, corBase, 0.60f)
            )
        ));
    }
}
