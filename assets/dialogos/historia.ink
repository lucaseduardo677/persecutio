// historia.ink
// Cada no (===) corresponde a um dialogoAlvo disparado pelo GerenciadorProgresso.
// A tag "falante:" define quem esta falando (lida por GerenciadorDialogo.avancar()).
// A tag "efeito:" dispara TelaJogo.processarEfeito() (ex: entregar peca).
// Depois de editar, exporte para JSON (Inky: File -> Export to JSON) e salve em
// assets/dialogos/historia.ink.json

=== enfermeira ===
# falante: Enfermeira
Volte para o seu quarto. Voce nao devia estar aqui.
* [Por que nao?]
    Porque as regras existem por um motivo. Agora ande.
* [Vou voltar.]
    Isso mesmo. Va com calma.
- -> DONE

=== paciente_pergunta ===
# falante: Paciente
Eu tenho algo util, mas... qual e a palavra magica?
-> DONE

=== paciente_sabe ===
# falante: Paciente
Ah, voce voltou. Entao... qual e a palavra magica?
* [Redencao]
    # efeito: dar_peca
    Isso mesmo... Redencao. Pegue, voce vai precisar disso.
* [Nao lembro agora.]
    Volte quando lembrar.
- -> DONE

=== paciente_feito ===
# falante: Paciente
Va em frente, voce tem o que precisa.
-> DONE
