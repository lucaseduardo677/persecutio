package com.persecutio.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.AudioDevice;
import com.badlogic.gdx.files.FileHandle;
import java.util.concurrent.atomic.AtomicInteger;

// Sintetiza as falas dos diálogos no estilo animalese
public class GerenciadorVoz {

    // Caminho da biblioteca de letras dentro dos assets
    private static final String CAMINHO_BIBLIOTECA = "audio/animalese/animalese.wav";
    // Tamanho do cabecalho de um wav PCM canonico 44 bytes
    private static final int   TAMANHO_CABECALHO   = 44;

    // Frequencia de amostragem da biblioteca e da reproducao
    private static final int   FREQ_AMOSTRA            = 44100;
    // Duracao de cada letra dentro da biblioteca gravada em segundos
    private static final float DURACAO_LETRA_LIB        = 0.15f;
    // Duracao de cada letra na fala gerada em segundos
    private static final float DURACAO_LETRA_SAIDA      = 0.050f;
    // Volume de reproducao das falas
    private static final float VOLUME_PADRAO            = 0.35f;
    // Tom minimo aceito voz mais grave e lenta
    private static final float TOM_MINIMO               = 0.2f;
    // Tom maximo aceito voz mais aguda e rapida
    private static final float TOM_MAXIMO               = 2.0f;

    // Amostras de cada letra dentro da biblioteca calculado a partir da duracao
    private static final int AMOSTRAS_LETRA_LIB   = Math.round(DURACAO_LETRA_LIB   * FREQ_AMOSTRA);
    // Amostras de cada letra na fala gerada calculado a partir da duracao
    private static final int AMOSTRAS_LETRA_SAIDA = Math.round(DURACAO_LETRA_SAIDA * FREQ_AMOSTRA);

    // Amostras da biblioteca de letras A Z sem o cabecalho do wav
    private byte[] biblioteca;

    private final GerenciadorTom tons;

    // Controla qual fala e a mais recente para interromper falas antigas
    private final AtomicInteger geracaoAtual = new AtomicInteger(0);
    // Quantos caracteres da fala atual ja comecaram a ser falados
    private final AtomicInteger letraAtual   = new AtomicInteger(0);
    // Referencia a thread de fala em andamento para nao abrir dois AudioDevice ao mesmo tempo
    private Thread threadFalaAtual = null;

    // Cria o gerenciador e carrega a biblioteca de letras
    public GerenciadorVoz() {
        tons = new GerenciadorTom();
        carregarBiblioteca();
    }

    // Carrega a biblioteca de amostras a partir do wav dos assets
    private void carregarBiblioteca() {
        FileHandle arquivo = Gdx.files.internal(CAMINHO_BIBLIOTECA);
        if (!arquivo.exists()) {
            biblioteca = null;
            return;
        }

        byte[] bruto = arquivo.readBytes();
        biblioteca = new byte[bruto.length - TAMANHO_CABECALHO];
        System.arraycopy(bruto, TAMANHO_CABECALHO, biblioteca, 0, biblioteca.length);
    }

    // Normaliza os caracteres com acentos do portugues para letras equivalentes da biblioteca
    private char normalizarChar(char c) {
        switch (c) {
            case 'Á': case 'À': case 'Â': case 'Ã': case 'Ä': return 'A';
            case 'É': case 'È': case 'Ê': case 'Ë': return 'E';
            case 'Í': case 'Ì': case 'Î': case 'Ï': return 'I';
            case 'Ó': case 'Ò': case 'Ô': case 'Õ': case 'Ö': return 'O';
            case 'Ú': case 'Ù': case 'Û': case 'Ü': return 'U';
            case 'Ç': return 'C';
            default: return c;
        }
    }

    // Fala o texto de uma linha de dialogo usando o tom do falante informado
    public void falar(String falante, String texto) {
        int minhaGeracao = geracaoAtual.incrementAndGet();
        letraAtual.set(0);

        if (biblioteca == null || texto == null || texto.isEmpty() || !contemLetra(texto)) {
            // Sem audio para tocar libera o texto inteiro de uma vez sem sincronia
            letraAtual.set(Integer.MAX_VALUE);
            return;
        }

        // Fallback se o falante não for declarado nulo ou vazio define um falante padrao neutro Narrador
        String falanteFinal = (falante == null || falante.trim().isEmpty()) ? "Narrador" : falante;

        float tom = tons.obterTom(falanteFinal);
        byte[] dados = gerarAmostras(texto, tom);

        // Aguarda a fala anterior terminar antes de iniciar outro áudio
        if (threadFalaAtual != null && threadFalaAtual.isAlive()) {
            try {
                threadFalaAtual.join(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        Thread threadFala = new Thread(() -> tocarFala(dados, minhaGeracao));
        threadFala.setDaemon(true);
        threadFalaAtual = threadFala;
        threadFala.start();
    }

    // Verifica se o texto possui ao menos uma letra pronunciavel
    private boolean contemLetra(String texto) {
        for (int i = 0; i < texto.length(); i++) {
            char c = normalizarChar(Character.toUpperCase(texto.charAt(i)));
            if (c >= 'A' && c <= 'Z') return true;
        }
        return false;
    }

    // Gera as amostras em PCM 8 bits sem sinal para o texto informado
    private byte[] gerarAmostras(String texto, float tom) {
        float tomSeguro = Math.max(TOM_MINIMO, Math.min(TOM_MAXIMO, tom));
        byte[] dados = new byte[texto.length() * AMOSTRAS_LETRA_SAIDA];

        for (int indiceChar = 0; indiceChar < texto.length(); indiceChar++) {
            char c = normalizarChar(Character.toUpperCase(texto.charAt(indiceChar)));
            int baseSaida = indiceChar * AMOSTRAS_LETRA_SAIDA;

            if (c >= 'A' && c <= 'Z') {
                int inicioLetra = AMOSTRAS_LETRA_LIB * (c - 'A');

                for (int i = 0; i < AMOSTRAS_LETRA_SAIDA; i++) {
                    int indiceAmostra = inicioLetra + (int) (i * tomSeguro);
                    if (indiceAmostra >= biblioteca.length) indiceAmostra = biblioteca.length - 1;
                    dados[baseSaida + i] = biblioteca[indiceAmostra];
                }
            } else {
                // Caractere nao pronunciavel espaco ou pontuacao vira silencio
                for (int i = 0; i < AMOSTRAS_LETRA_SAIDA; i++) {
                    dados[baseSaida + i] = (byte) 127;
                }
            }
        }
        return dados;
    }

    // Toca as amostras geradas em uma thread separada letra por letra
    private void tocarFala(byte[] dados, int minhaGeracao) {
        AudioDevice dispositivo = Gdx.audio.newAudioDevice(FREQ_AMOSTRA, true);
        float[] bufferLetra = new float[AMOSTRAS_LETRA_SAIDA];

        for (int inicio = 0; inicio < dados.length; inicio += AMOSTRAS_LETRA_SAIDA) {
            if (geracaoAtual.get() != minhaGeracao) break;

            int tamanho = Math.min(AMOSTRAS_LETRA_SAIDA, dados.length - inicio);
            for (int i = 0; i < tamanho; i++) {
                bufferLetra[i] = aplicarVolume(dados[inicio + i]);
            }
            letraAtual.incrementAndGet();
            dispositivo.writeSamples(bufferLetra, 0, tamanho);
        }

        dispositivo.dispose();
    }

    // Converte uma amostra PCM 8 bits sem sinal 0 a 255 para float 1 a 1 ja com o volume aplicado
    private float aplicarVolume(byte amostraPcm) {
        return ((amostraPcm & 0xFF) - 128) / 128f * VOLUME_PADRAO;
    }

    // Interrompe a fala em andamento se houver
    public void parar() {
        geracaoAtual.incrementAndGet();
    }

    public int obterLetraAtual() {
        return letraAtual.get();
    }

    // Libera os recursos do gerenciador
    public void dispose() {
        parar();
    }
}
