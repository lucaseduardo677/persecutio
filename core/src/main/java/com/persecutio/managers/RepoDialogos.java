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

        public Fala(String falante, String texto, String efeito, String retrato, Color corFundo, float opacidade) {
            this.falante = falante;
            this.texto = texto;
            this.efeito = efeito;
            this.retrato = retrato;
            this.corFundo = corFundo != null ? corFundo : Color.valueOf("#2b2b36");
            this.opacidade = opacidade;
        }

        public Fala(String falante, String texto, String efeito) {
            this(falante, texto, efeito, null, null, 1.0f);
        }

        public Fala(String falante, String texto) {
            this(falante, texto, null, null, null, 1.0f);
        }
    }

    // Classe da escolha
    public static class Escolha {
        public final String texto;
        public final int pontos;
        public final String proxNo;

        public Escolha(String texto, int pontos, String proxNo) {
            this.texto = texto;
            this.pontos = pontos;
            this.proxNo = proxNo;
        }

        // Escolha de navegacao simples, sem impacto na pontuacao
        public Escolha(String texto, String proxNo) {
            this(texto, 0, proxNo);
        }
    }

    // Classe do no
    public static class NoDialogo {
        public final List<Fala> falas;
        public final List<Escolha> escolhas;

        public NoDialogo(List<Fala> falas, List<Escolha> escolhas) {
            this.falas = falas;
            this.escolhas = escolhas;
        }

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

    // Constroi os dialogos (GDD Paginas 7-9)
    private void criarDialogos() {
        Color corBase = Color.valueOf("#2b2b36");
        // Sprite 2x2 do Dr. Elimar (o antigo caminho "dr_elimar.png" nao existia nos assets)
        String imgElimar = "img/elimarSprite.png";

        // Alto-falante de Missao 1
        dicNos.put("alto_falante", new NoDialogo(Arrays.asList(
            new Fala("Alto-falante", "Senhorita Maria Clara, favor comparecer a recepcao.")
        )));

        // Dialogos da enfermeira da recepcao (Enfermeira, retrato = null)
        dicNos.put("enfermeira", new NoDialogo(
            Arrays.asList(
                new Fala("Enfermeira", "Bom dia, Maria. Seus remedios chegaram.", null, null, corBase, 1.0f),
                new Fala("Enfermeira", "Tome apenas um comprimido se sentir tontura ou confusao. Ele e forte, da muito sono.", null, null, corBase, 1.0f),
                new Fala("Enfermeira", "Ja deixamos a cartela na sua mesa de cabeceira.", null, null, corBase, 1.0f)
            )
        ));

        // Fala curta em loop apos a primeira conversa com a enfermeira
        dicNos.put("enfermeira_volte_quarto", new NoDialogo(Arrays.asList(
            new Fala("Enfermeira", "Pode voltar para o seu quarto agora.", null, null, corBase, 1.0f)
        )));

        // Constatacao de porta trancada em Umbra
        dicNos.put("porta_quarto_trancada", new NoDialogo(Arrays.asList(
            new Fala("Maria", "... Esta trancada?")
        )));

        // Brilho sutil do relogio no espelho do Umbra revelando a senha
        dicNos.put("espelho_umbra", new NoDialogo(Arrays.asList(
            new Fala("Maria", "O reflexo no espelho revela o relogio de parede brilhando de forma sutil, marcando exatamente 04:10.")
        )));

        // Dialogos de tentativa de passagem do corredor
        dicNos.put("enfermeira_passar", new NoDialogo(
            Arrays.asList(
                new Fala("Enfermeira", "Ninguem sai sem autorizacao do Dr. Gonzalez.", null, null, corBase, 0.60f)
            )
        ));

        // Dialogos de tentativa de passagem do corredor
        dicNos.put("enfermeira_desculpa", new NoDialogo(
            Arrays.asList(
                new Fala("Enfermeira", "Apenas va deitar.", null, null, corBase, 0.60f)
            )
        ));

        // REMOVIDO: dialogo da musica (maria_musica) - nao deve mais tocar

        // Reacao ao encontrar a porta emperrada no Jardim
        dicNos.put("porta_emperrada", new NoDialogo(Arrays.asList(
            new Fala("Maria", "Esta emperrada..."),
            new Fala("Maria", "Talvez exista alguma forma de destranca-la.")
        )));

        // Som de gatilho mecanico de feedback para o puzzle das pedras
        dicNos.put("porta_clique", new NoDialogo(Arrays.asList(
            new Fala("Maria", "...Escutei um clique.")
        )));

        // Documento opcional do banco do Jardim
        dicNos.put("doc_jardim", new NoDialogo(Arrays.asList(
            new Fala("Narrador", "Um documento rasgado... relata pacientes ouvindo musicas antigas que nao existem.")
        )));

        // Questionario final do Dr. Elimar Gonzalez, decide o final do jogo (GDD paginas 4 e 7-9)
        // Maria ao entrar no Umbra na Missao 1
        dicNos.put("maria_entra_umbra_m1", new NoDialogo(Arrays.asList(
            new Fala("Maria", "O que... o que esta acontecendo? Preciso ir ate a recepcao.")
        )));

        // Revelacao curta da Maria apos ler o prontuario no Umbra
        dicNos.put("maria_doc1_revelacao", new NoDialogo(Arrays.asList(
            new Fala("Maria", "Isso e... sobre mim?")
        )));

        // Fala curta em loop: toca sempre que o jogador tenta sair do quarto sem a cartela
        // (apos ja ter visto o lembrete uma vez)
        dicNos.put("maria_precisa_pilulas", new NoDialogo(Arrays.asList(
            new Fala("Maria", "Tenho que pegar as pilulas.")
        )));

        // CORRECAO: Dialogo mais longo sobre a pilula - toca UMA VEZ na primeira vez que
        // o jogador tenta sair do quarto sem a cartela. Depois, e trocado pelo curto acima.
        dicNos.put("maria_pilula_lembrete", new NoDialogo(Arrays.asList(
            new Fala("Maria", "Espera... eu ainda nao tomei meu remedio."),
            new Fala("Maria", "A enfermeira disse que deixou na minha mesa de cabeceira. Melhor pegar antes de sair.")
        )));

        // Desculpa para ir ao jardim, toca apenas no Umbra Missao 2
        dicNos.put("maria_jardim_umbra", new NoDialogo(Arrays.asList(
            new Fala("Maria", "Um vento estranho... e um barulho de pedras vindo do jardim.")
        )));

        dicNos.put("elimar_intro", new NoDialogo(
            Arrays.asList(
                new Fala("Dr. Elimar", "Ola, Maria Clara. Sou o Dr. Elimar Gonzalez.", null, imgElimar, corBase, 1.0f),
                new Fala("Dr. Elimar", "Vou avaliar sua estadia. Responda com sinceridade.", null, imgElimar, corBase, 1.0f),
                new Fala("Maria", "Ok...", null, null, corBase, 1.0f)
            ),
            Arrays.asList(
                new Escolha("> Responder ao questionario", "elimar_p1")
            )
        ));

        dicNos.put("elimar_p1", new NoDialogo(
            Arrays.asList(
                new Fala("Dr. Elimar", "Durante sua estadia aqui, voce descreveu momentos em que dizia a si mesma que aquilo era apenas uma fase. O que faz alguem perceber que uma relacao deixou de ser segura?", null, imgElimar, corBase, 1.0f)
            ),
            Arrays.asList(
                new Escolha("Quando o medo passa a fazer parte da rotina.", 2, "elimar_p2"),
                new Escolha("Quando comecam as agressoes fisicas.", 0, "elimar_p2"),
                new Escolha("Quando as discussoes acontecem com frequencia.", 1, "elimar_p2"),
                new Escolha("Quando outras pessoas dizem que a relacao faz mal.", 1, "elimar_p2")
            )
        ));

        dicNos.put("elimar_p2", new NoDialogo(
            Arrays.asList(
                new Fala("Dr. Elimar", "Algumas pessoas acreditam que controlar quem amam e uma forma de protecao. O que voce pensa sobre isso hoje?", null, imgElimar, corBase, 1.0f)
            ),
            Arrays.asList(
                new Escolha("Quem ama precisa confiar, nao controlar.", 2, "elimar_p3"),
                new Escolha("Depende da intencao da pessoa.", -1, "elimar_p3"),
                new Escolha("E normal sentir ciumes quando se ama.", 0, "elimar_p3"),
                new Escolha("Toda relacao tem um pouco disso.", -2, "elimar_p3")
            )
        ));

        dicNos.put("elimar_p3", new NoDialogo(
            Arrays.asList(
                new Fala("Dr. Elimar", "Voce passou muito tempo tentando entender por que tudo aconteceu. Quando uma pessoa sofre violencia dentro de casa, quem deve responder por essa escolha?", null, imgElimar, corBase, 1.0f)
            ),
            Arrays.asList(
                new Escolha("Quem praticou a violencia.", 2, "elimar_p4"),
                new Escolha("Os dois acabam tendo responsabilidade.", -2, "elimar_p4"),
                new Escolha("Depende do que aconteceu antes.", -1, "elimar_p4"),
                new Escolha("Nem sempre existe um culpado.", -2, "elimar_p4")
            )
        ));

        dicNos.put("elimar_p4", new NoDialogo(
            Arrays.asList(
                new Fala("Dr. Elimar", "Muitas pessoas perguntam por que alguem continua em uma relacao que causa sofrimento. Depois da sua jornada... o que voce responderia?", null, imgElimar, corBase, 1.0f)
            ),
            Arrays.asList(
                new Escolha("Porque medo, dependencia e manipulacao podem fazer a pessoa acreditar que nao ha saida.", 2, "elimar_p5"),
                new Escolha("Porque ela ainda ama quem a machuca.", 1, "elimar_p5"),
                new Escolha("Porque ela escolhe permanecer.", -2, "elimar_p5"),
                new Escolha("Porque nao percebe o que esta acontecendo.", 0, "elimar_p5")
            )
        ));

        dicNos.put("elimar_p5", new NoDialogo(
            Arrays.asList(
                new Fala("Dr. Elimar", "Imagine que alguem lhe conte estar vivendo algo parecido com o que voce viveu. Qual seria sua primeira orientacao?", null, imgElimar, corBase, 1.0f)
            ),
            Arrays.asList(
                new Escolha("Buscar apoio de pessoas de confianca e dos servicos de protecao.", 2, "elimar_p6"),
                new Escolha("Esperar para ver se a situacao melhora.", -2, "elimar_p6"),
                new Escolha("Conversar com o agressor ate ele mudar.", -1, "elimar_p6"),
                new Escolha("Tentar resolver tudo sozinha.", -2, "elimar_p6")
            )
        ));

        dicNos.put("elimar_p6", new NoDialogo(
            Arrays.asList(
                new Fala("Dr. Elimar", "Voce concorda com a frase \"Bons sentimentos curam todos os ferimentos, nao e?\"", null, imgElimar, corBase, 1.0f)
            ),
            Arrays.asList(
                new Escolha("Nao.", 2, "elimar_p7"),
                new Escolha("Sim.", -2, "elimar_p7"),
                new Escolha("Nao sei dizer.", 1, "elimar_p7"),
                new Escolha("Talvez.", -1, "elimar_p7")
            )
        ));

        dicNos.put("elimar_p7", new NoDialogo(
            Arrays.asList(
                new Fala("Narrador", "(O Dr. Elimar fecha a prancheta.)", null, null, corBase, 1.0f),
                new Fala("Dr. Elimar", "Ha uma ultima coisa que preciso saber. Se uma mulher disser que tem medo da pessoa que diz ama-la... voce acredita nela?", null, imgElimar, corBase, 1.0f)
            ),
            Arrays.asList(
                new Escolha("Sim. O medo nunca deveria fazer parte do amor.", 2, "elimar_encerramento"),
                new Escolha("Depende do motivo desse medo.", -1, "elimar_encerramento"),
                new Escolha("E preciso ouvir e acolher antes de julgar.", 2, "elimar_encerramento"),
                new Escolha("Relacionamentos sao complicados, isso acontece.", -2, "elimar_encerramento")
            )
        ));

        // Encerramento do questionario, antes de revelar o final calculado pela pontuacao
        dicNos.put("elimar_encerramento", new NoDialogo(Arrays.asList(
            new Fala("Dr. Elimar", "Obrigado pela sinceridade.", null, imgElimar, corBase, 1.0f),
            new Fala("Maria", "Espere... e so isso?", null, null, corBase, 1.0f),
            new Fala("Dr. Elimar", "Por hoje, sim. Descanse.", null, imgElimar, corBase, 1.0f)
        )));

        // Finais possiveis (GDD item 13), escolhidos pela TelaElimar de acordo com a pontuacao final
        dicNos.put("final_ruim", new NoDialogo(Arrays.asList(
            new Fala("Narrador", "Maria Clara rejeita completamente as memorias que voltaram a superficie.", null, null, corBase, 1.0f),
            new Fala("Narrador", "O Mundo Umbra a consome. Ela permanece na instituicao, incapaz de distinguir realidade e subconsciente.", null, null, corBase, 1.0f)
        )));

        dicNos.put("final_normal", new NoDialogo(Arrays.asList(
            new Fala("Narrador", "Maria Clara aceita parte do seu passado, mas ainda nao consegue encarar tudo.", null, null, corBase, 1.0f),
            new Fala("Narrador", "Ela desperta parcialmente e volta a viver, ainda fragmentada emocionalmente.", null, null, corBase, 1.0f)
        )));

        dicNos.put("final_bom", new NoDialogo(Arrays.asList(
            new Fala("Narrador", "Maria Clara consegue reconstruir a propria memoria.", null, null, corBase, 1.0f),
            new Fala("Narrador", "As sombras deixam de persegui-la. O Mundo Umbra desaparece lentamente enquanto o mundo real permanece.", null, null, corBase, 1.0f)
        )));

        // CORRECAO: Monologo sutil ao despertar no quarto no inicio da Missao 2
        // Mudado para ser mais sutil e natural: "O que foi isso...? Eu preciso ir tomar um ar..."
        dicNos.put("maria_acorda_missao2", new NoDialogo(Arrays.asList(
            new Fala("Maria", "O que foi isso...?"),
            new Fala("Maria", "Eu preciso ir tomar um ar...")
        )));

        // Desculpa in-game de Maria para recolher a cartela inteira na Missao 2
        // Agora com direcionamento ao jardim apos pegar a cartela
        dicNos.put("maria_pega_pilulas", new NoDialogo(Arrays.asList(
            new Fala("Maria", "A cartela inteira... melhor levar comigo.", "tomar_pilula_missao2")
        )));

        // Leitura sutil e narrativa do prontuario da paciente 103 no Mundo Umbra
        dicNos.put("documento1_umbra", new NoDialogo(Arrays.asList(
            new Fala("Narrador", "(O borrao no papel agora esta nitido.)"),
            new Fala("Narrador", "Documento Clinico - Paciente 103. Perda de memoria seletiva ligada a violencia domestica..."),
            new Fala("Narrador", "...bloqueio parcial como mecanismo de autopreservacao."),
            new Fala("Maria", "(Paciente 103... Por que isso parece ser sobre mim?)", "ler_prontuario_umbra")
        )));

        // Leitura do prontuario no mundo real (GDD/Fluxo do Jogo)
        dicNos.put("documento1_real", new NoDialogo(Arrays.asList(
            new Fala("Narrador", "Relatorio de Incidente: panico extremo nas sessoes com o Dr. Gonzalez."),
            new Fala("Narrador", "[Missao 1 Concluida!]"),
            new Fala("Maria", "(Isso confirma minhas suspeitas...)")
        )));
    }
}
