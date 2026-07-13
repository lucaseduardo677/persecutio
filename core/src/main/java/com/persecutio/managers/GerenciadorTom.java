package com.persecutio.managers;

import java.util.HashMap;
import java.util.Map;

// Define o tom de voz animalese de cada personagem do RepoDialogos
public class GerenciadorTom {

    // Tom usado quando o falante nao possui um tom cadastrado
    private static final float TOM_PADRAO = 1.0f;

    // Tons cadastrados por nome de falante
    private final Map<String, Float> tons = new HashMap<>();

    // Cadastra os tons dos personagens conhecidos do jogo
    public GerenciadorTom() {
        cadastrarPadroes();
    }

    // Cadastra os tons padrao (nomes iguais aos usados em RepoDialogos.Fala)
    private void cadastrarPadroes() {
        definirTom("Maria",         1.35f); // Voz aguda, jovem e fragil
        definirTom("Dr. Elimar",    0.55f); // Voz grave e pausada, tom de autoridade
        definirTom("Enfermeira",    0.85f); // Voz media, tom neutro e profissional
        definirTom("Alto-falante",  0.35f); // Voz bem grave e robotica, de auto-falante
        definirTom("Paciente",      0.70f); // Voz media-grave, tom cansado
    }

    // Cadastra ou substitui o tom de um personagem
    public void definirTom(String personagem, float tom) {
        tons.put(normalizar(personagem), tom);
    }

    // Retorna o tom cadastrado de um personagem, ou o tom padrao
    public float obterTom(String personagem) {
        Float tom = tons.get(normalizar(personagem));
        return tom != null ? tom : TOM_PADRAO;
    }

    // Normaliza o nome do falante para usar como chave
    private String normalizar(String personagem) {
        return personagem == null ? "" : personagem.trim().toLowerCase();
    }
}
