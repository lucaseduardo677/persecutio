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
            this.falante   = falante;
            this.texto     = texto;
            this.efeito    = efeito;
            this.retrato   = retrato;
            this.corFundo  = corFundo != null ? corFundo : Color.valueOf("#2b2b36");
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
        public final String proxNo;

        public Escolha(String texto, String proxNo) {
            this.texto  = texto;
            this.proxNo = proxNo;
        }
    }

    // Classe do no
    public static class NoDialogo {
        public final List<Fala>    falas;
        public final List<Escolha> escolhas;

        public NoDialogo(List<Fala> falas, List<Escolha> escolhas) {
            this.falas    = falas;
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
        Color corBase     = Color.valueOf("#2b2b36");
        String imgElimar  = "img/dr_elimar.png";

        // Alto-falante de Missao 1
        dicNos.put("alto_falante", new NoDialogo(Arrays.asList(
            new Fala("Alto-falante", "Senhorita Maria Clara, favor comparecer a recepcao.")
        )));

        // Diálogos da enfermeira da recepção (Enfermeira, retrato = null)
        dicNos.put("enfermeira", new NoDialogo(
            Arrays.asList(
                new Fala("Enfermeira", "Bom dia, senhorita Maria. Seus medicamentos acabaram de chegar.", null, null, corBase, 1.0f),
                new Fala("Enfermeira", "Eles fazem parte do seu tratamento. Sempre que sentir tonturas ou confusao, tome apenas um comprimido.", null, null, corBase, 1.0f),
                new Fala("Enfermeira", "Mas tenha cuidado. O remedio e bastante forte e costuma provocar muito sono.", null, null, corBase, 1.0f),
                new Fala("Enfermeira", "Ja deixamos a cartela na mesa de cabeceira.", null, null, corBase, 1.0f)
            )
        ));

        // Constatacao de porta trancada em Umbra
        dicNos.put("porta_quarto_trancada", new NoDialogo(Arrays.asList(
            new Fala("Maria", "... Esta trancada?")
        )));

        // Brilho sutil do relógio no espelho do Umbra revelando a senha
        dicNos.put("espelho_umbra", new NoDialogo(Arrays.asList(
            new Fala("Maria", "O reflexo no espelho revela o relogio de parede brilhando de forma sutil, marcando exatamente 04:10.")
        )));

        // Diálogos de tentativa de passagem do corredor
        dicNos.put("enfermeira_passar", new NoDialogo(
            Arrays.asList(
                new Fala("Enfermeira", "Ninguem sai sem autorizacao do Dr. Gonzalez.", null, null, corBase, 0.60f)
            )
        ));

        // Diálogos de tentativa de passagem do corredor
        dicNos.put("enfermeira_desculpa", new NoDialogo(
            Arrays.asList(
                new Fala("Enfermeira", "Apenas va deitar.", null, null, corBase, 0.60f)
            )
        ));

        // Diálogos de Missao 2
        dicNos.put("maria_musica", new NoDialogo(Arrays.asList(
            new Fala("Maria", "Essa musica..."),
            new Fala("Maria", "...acho que esta vindo da ala leste da casa.")
        )));

        // Reação ao encontrar a porta emperrada no Jardim
        dicNos.put("porta_emperrada", new NoDialogo(Arrays.asList(
            new Fala("Maria", "Esta emperrada..."),
            new Fala("Maria", "Talvez exista alguma forma de destranca-la.")
        )));

        // Som de gatilho mecânico de feedback para o puzzle das pedras
        dicNos.put("porta_clique", new NoDialogo(Arrays.asList(
            new Fala("Maria", "...Escutei um clique.")
        )));

        // Documento opcional do banco do Jardim
        dicNos.put("doc_jardim", new NoDialogo(Arrays.asList(
            new Fala("Narrador", "Um documento rasgado... relata pacientes ouvindo musicas antigas que nao existem.")
        )));

        // Diálogos da primeira sessão de perguntas com o Dr. Elimar Gonzalez (GDD Páginas 7-9)
        dicNos.put("elimar_intro", new NoDialogo(
            Arrays.asList(
                new Fala("Dr. Elimar", "Bom dia... Voce consegue me ouvir?", null, imgElimar, corBase, 1.0f),
                new Fala("Maria", "...Ha...?", null, null, corBase, 1.0f),
                new Fala("Dr. Elimar", "Otimo. Nao tente se levantar ainda. Meu nome e Dr. Elimar Gonzales. Voce esta em seguranca.", null, imgElimar, corBase, 1.0f),
                new Fala("Maria", "Seguranca...? Onde... onde eu estou?", null, null, corBase, 1.0f),
                new Fala("Dr. Elimar", "Voce foi trazida para uma casa de repouso apos receber atendimento de emergencia no hospital. Seu corpo sofreu alguns ferimentos e voce passou um tempo inconsciente.", null, imgElimar, corBase, 1.0f),
                new Fala("Dr. Elimar", "Agora, preciso avaliar sua memoria e sua capacidade de orientacao. Tudo bem?", null, imgElimar, corBase, 1.0f),
                new Fala("Maria", "Eu... acho que sim...", null, null, corBase, 1.0f),
                new Fala("Dr. Elimar", "Antes de qualquer coisa, como esta se sentindo?", null, imgElimar, corBase, 1.0f),
                new Fala("Maria", "Minha cabeca doi muito... Meu corpo inteiro parece pesado... Eu... eu sinto que falta alguma coisa...", null, null, corBase, 1.0f),
                new Fala("Maria", "(Leva a mao a cabeca e fecha os olhos por alguns segundos.) Meu Deus...", null, null, corBase, 1.0f),
                new Fala("Dr. Elimar", "O que foi?", null, imgElimar, corBase, 1.0f),
                new Fala("Maria", "Eu... eu nao consigo lembrar meu nome.", null, null, corBase, 1.0f),
                new Fala("Dr. Elimar", "Nao se preocupe. Isso pode acontecer apos um trauma. Nao vou forcar suas lembrancas. Vamos reconstrui-las aos poucos.", null, imgElimar, corBase, 1.0f),
                new Fala("Dr. Elimar", "(Pega uma prancheta) Vou fazer algumas perguntas simples. Nao existem respostas certas ou erradas. Quero apenas entender como sua memoria esta funcionando.", null, imgElimar, corBase, 1.0f),
                new Fala("Dr. Elimar", "Se, em algum momento, alguma pergunta causar desconforto, me avise. Podemos parar quando voce quiser.", null, imgElimar, corBase, 1.0f)
            ),
            Arrays.asList(
                new Escolha("> Começar questionário", "elimar_p1")
            )
        ));

        dicNos.put("elimar_p1", new NoDialogo(
            Arrays.asList(
                new Fala("Dr. Elimar", "Se voce estivesse andando por um corredor vazio e escutasse alguem discutindo atras de uma porta fechada, o que pensaria sobre isso?", null, imgElimar, corBase, 1.0f)
            ),
            Arrays.asList(
                new Escolha("Que ha uma conversa rolando ali.", "elimar_p1_reacao"),
                new Escolha("Acho que todos discutem vez ou outra.", "elimar_p1_reacao"),
                new Escolha("Procuraria alguem para ajudar.", "elimar_p1_reacao")
            )
        ));

        dicNos.put("elimar_p1_reacao", new NoDialogo(
            Arrays.asList(
                new Fala("Dr. Elimar", "Interessante...", null, imgElimar, corBase, 1.0f)
            ),
            Arrays.asList(
                new Escolha("> Próxima pergunta", "elimar_p2")
            )
        ));

        dicNos.put("elimar_p2", new NoDialogo(
            Arrays.asList(
                new Fala("Dr. Elimar", "Voce encontra um vaso quebrado no chao de uma sala completamente vazia. Qual hipotese parece mais provavel?", null, imgElimar, corBase, 1.0f)
            ),
            Arrays.asList(
                new Escolha("Foi um acidente.", "elimar_p2_reacao"),
                new Escolha("Alguem esteve ali antes.", "elimar_p2_reacao"),
                new Escolha("Nao tenho elementos para imaginar.", "elimar_p2_reacao")
            )
        ));

        dicNos.put("elimar_p2_reacao", new NoDialogo(
            Arrays.asList(
                new Fala("Dr. Elimar", "Anotado.", null, imgElimar, corBase, 1.0f)
            ),
            Arrays.asList(
                new Escolha("> Próxima pergunta", "elimar_p3")
            )
        ));

        dicNos.put("elimar_p3", new NoDialogo(
            Arrays.asList(
                new Fala("Dr. Elimar", "Algumas lembrancas desaparecem por completo. Outras ficam apenas como sensacoes. Se voce tivesse que confiar em uma delas, escolheria...", null, imgElimar, corBase, 1.0f)
            ),
            Arrays.asList(
                new Escolha("O que consegue lembrar.", "elimar_p3_reacao"),
                new Escolha("O que sente.", "elimar_p3_reacao"),
                new Escolha("Esperaria mais tempo.", "elimar_p3_reacao")
            )
        ));

        dicNos.put("elimar_p3_reacao", new NoDialogo(
            Arrays.asList(
                new Fala("Dr. Elimar", "Anotado.", null, imgElimar, corBase, 1.0f)
            ),
            Arrays.asList(
                new Escolha("> Próxima pergunta", "elimar_p4")
            )
        ));

        dicNos.put("elimar_p4", new NoDialogo(
            Arrays.asList(
                new Fala("Dr. Elimar", "Imagine que voce passa todos os dias pela mesma rua. Em uma das casas, as cortinas permanecem sempre fechadas e, de repente, voce deixa de ver uma pessoa que costumava aparecer na janela. Qual seria seu primeiro pensamento?", null, imgElimar, corBase, 1.0f)
            ),
            Arrays.asList(
                new Escolha("Ela deve estar ocupada.", "elimar_p4_reacao"),
                new Escolha("Talvez tenha viajado.", "elimar_p4_reacao"),
                new Escolha("Nao sei o que pensar sobre.", "elimar_p4_reacao")
            )
        ));

        dicNos.put("elimar_p4_reacao", new NoDialogo(
            Arrays.asList(
                new Fala("Dr. Elimar", "Certo. Ultima pergunta.", null, imgElimar, corBase, 1.0f)
            ),
            Arrays.asList(
                new Escolha("> Próxima pergunta", "elimar_p5")
            )
        ));

        dicNos.put("elimar_p5", new NoDialogo(
            Arrays.asList(
                new Fala("Dr. Elimar", "Voce ouve alguem dizendo 'esta tudo bem' com a voz tremula, mas percebe que suas maos estao machucadas. No primeiro instante, voce...", null, imgElimar, corBase, 1.0f)
            ),
            Arrays.asList(
                new Escolha("Acreditaria no que ela disse.", "elimar_p5_reacao"),
                new Escolha("Acharia que talvez ela esteja escondendo alguma coisa.", "elimar_p5_reacao"),
                new Escolha("Esperaria mais informacoes antes de pensar qualquer coisa.", "elimar_p5_reacao")
            )
        ));

        dicNos.put("elimar_p5_reacao", new NoDialogo(
            Arrays.asList(
                new Fala("Dr. Elimar", "...", null, imgElimar, corBase, 1.0f),
                new Fala("Dr. Elimar", "Senhorita Maria... acredito que seu caso seja mais complexo do que uma simples perda de memoria. Felizmente, ja acompanhamos pacientes que passaram por experiencias semelhantes.", null, imgElimar, corBase, 1.0f),
                new Fala("Dr. Elimar", "Ainda e cedo para afirmar exatamente o que aconteceu com a senhorita. A mente humana e... peculiar. As vezes, ela encontra maneiras inesperadas de nos proteger.", null, imgElimar, corBase, 1.0f)
            ),
            Arrays.asList(
                new Escolha("> Continuar", "elimar_metafora")
            )
        ));

        dicNos.put("elimar_metafora", new NoDialogo(Arrays.asList(
            new Fala("Dr. Elimar", "Posso explicar usando uma metafora?", null, imgElimar, corBase, 1.0f),
            new Fala("Maria", "...", null, null, corBase, 1.0f),
            new Fala("Dr. Elimar", "Imagine que alguem a trancou em um quarto quando voce era incapaz de escapar. Dentro dele existe apenas uma cama, uma janela fechada... e um unico poster preso a parede.", null, imgElimar, corBase, 1.0f),
            new Fala("Dr. Elimar", "Como voce nunca viu nada alem daquele quarto, acaba acreditando que aquele e todo o seu mundo. Com o tempo, deixa ate de questionar por que esta ali.", null, imgElimar, corBase, 1.0f),
            new Fala("Dr. Elimar", "Mas um dia, por curiosidade ou necessidade, voce afasta o poster e encontra um pequeno buraco escondido na parede.", null, imgElimar, corBase, 1.0f),
            new Fala("Dr. Elimar", "Voce olha de perto de perto atraves dele... e, pela primeira vez, percebe que existe um mundo inteiro do outro lado.", null, imgElimar, corBase, 1.0f),
            new Fala("Dr. Elimar", "So que, antes que consiga sair, alguem a puxa de volta, fecha o buraco e coloca o poster exatamente onde ele estava.", null, imgElimar, corBase, 1.0f),
            new Fala("Dr. Elimar", "O buraco continua ali. O mundo alem dele tambem. Mas, aos poucos, voce esquece que eles existem.", null, imgElimar, corBase, 1.0f),
            new Fala("Dr. Elimar", "Nao porque eles desapareceram...", null, imgElimar, corBase, 1.0f),
            new Fala("Dr. Elimar", "Mas porque sua mente decidiu esconde-los para que voce nao precisasse sentir a mesma dor outra vez.", null, imgElimar, corBase, 1.0f),
            new Fala("Maria", "(Permanece em silencio)", null, null, corBase, 1.0f),
            new Fala("Dr. Elimar", "E assim que imagino o seu estado neste momento.", null, imgElimar, corBase, 1.0f),
            new Fala("Dr. Elimar", "Suas lembrancas nao foram destruidas. Elas apenas ficaram atras desse poster.", null, imgElimar, corBase, 1.0f),
            new Fala("Dr. Elimar", "E eu nao posso arranca-lo por voce.", null, imgElimar, corBase, 1.0f),
            new Fala("Maria", "...", null, null, corBase, 1.0f),
            new Fala("Dr. Elimar", "Se eu fizer isso antes da hora, posso causar ainda mais sofrimento.", null, imgElimar, corBase, 1.0f),
            new Fala("Dr. Elimar", "O que podemos fazer e ajuda-la a levantar, aos poucos, cada canto desse papel... ate que consiga olhar para o outro lado sem que ele volte a esconde-la.", null, imgElimar, corBase, 1.0f),
            new Fala("Dr. Elimar", "Por hoje e so. Descanse senhorita Maria, amanha e um outro dia.", null, imgElimar, corBase, 1.0f)
        )));

        // Monólogo sutil ao despertar no quarto no início da Missão 2
        dicNos.put("maria_acorda_missao2", new NoDialogo(Arrays.asList(
            new Fala("Maria", "Acordei de novo no mundo real... Senti um vento estranho vindo do jardim externo."),
            new Fala("Maria", "E ouvi um barulho distante de pedras se movendo... Talvez eu deva ir la dar uma olhada.")
        )));

        // Desculpa in-game de Maria para recolher a cartela inteira na Missão 2
        dicNos.put("maria_pega_pilulas", new NoDialogo(Arrays.asList(
            new Fala("Maria", "A enfermeira deixou a cartela inteira na mesa de cabeceira..."),
            new Fala("Maria", "Nao quero ter que voltar toda vez que precisar de um comprimido. E melhor levar a cartela inteira comigo.", "tomar_pilula_missao2")
        )));

        // Leitura sutil e narrativa do prontuário da paciente 103 no Mundo Umbra
        dicNos.put("documento1_umbra", new NoDialogo(Arrays.asList(
            new Fala("Narrador", "(O papel em cima do balcao, que antes parecia um borrao sem nexo... agora esta nitido aos meus olhos.)"),
            new Fala("Narrador", "Documento Clinico - Acesso Restrito. Casa de Repouso Elimar Gonzalez. Paciente 103..."),
            new Fala("Narrador", "A paciente apresenta perda de memoria seletiva associada a episodios graves de violencia em ambiente domestico..."),
            new Fala("Narrador", "Demonstra incapacidade de recordar vinculos familiares ou de reconhecer determinados espacos residenciais..."),
            new Fala("Narrador", "Considerando o trauma, o cerebro da paciente parece ter desenvolvido um bloqueio de memoria parcial como mecanismo de autopreservacao..."),
            new Fala("Maria", "(Paciente 103... Por que a leitura destas linhas me causa um arrepio tao profundo? Esse prontuario... parece ser sobre mim.)", "ler_prontuario_umbra")
        )));

        // Leitura do prontuário no mundo real (GDD/Fluxo do Jogo)
        dicNos.put("documento1_real", new NoDialogo(Arrays.asList(
            new Fala("Narrador", "CONTEUDO DO PAPEL: Relatorio de Incidente..."),
            new Fala("Narrador", "A paciente Maria Clara demonstrou surtos de panico extremos durante as sessoes com o Dr. Gonzalez..."),
            new Fala("Narrador", "[Missao 1 Concluida!]"),
            new Fala("Maria", "(Isso... isso confirma as minhas suspeitas. Eu preciso continuar investigando...)")
        )));
    }
}
