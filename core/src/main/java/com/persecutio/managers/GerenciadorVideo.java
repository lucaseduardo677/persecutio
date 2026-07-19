package com.persecutio.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.video.VideoPlayer;
import com.badlogic.gdx.video.VideoPlayerCreator;

// Gerencia video de transicao de porta
public class GerenciadorVideo {

    // Estados do ciclo de vida da reproducao
    public enum Estado { IDLE, PREPARANDO, CARREGANDO, TOCANDO, TERMINADO, ERRO }

    // Player de video
    private VideoPlayer player;
    // Estado atual
    private Estado      estado          = Estado.IDLE;
    // Caminho do video pendente
    private String      caminhoPendente = null;
    // Controla termino sinalizado
    private boolean     terminadoSinalizado = false;
    // Indica se comecou a tocar
    private boolean     comecouTocar    = false;

    // Timer de carregamento
    private float timerCarregando = 0f;
    // Timeout de carregamento
    private static final float TIMEOUT_CARREGAMENTO = 2.0f;

    // Prepara caminho do video para iniciar
    public void preparar(String caminho) {
        if (caminho == null || caminho.isEmpty()) {
            estado          = Estado.IDLE;
            caminhoPendente = null;
            return;
        }
        caminhoPendente     = caminho;
        estado              = Estado.PREPARANDO;
        terminadoSinalizado = false;
        comecouTocar        = false;
    }

    // Inicia reproducao do video preparado
    public void iniciar() {
        if (estado != Estado.PREPARANDO || caminhoPendente == null) return;
        tocar(caminhoPendente);
        caminhoPendente = null;
    }

    // Avanca estado do video
    public boolean atualizar(float delta) {
        switch (estado) {
            case IDLE:
            case PREPARANDO:
                return false;

            case CARREGANDO:
                timerCarregando += delta;
                if (timerCarregando >= TIMEOUT_CARREGAMENTO) {
                    falhar();
                    return false;
                }
                if (player == null) {
                    estado              = Estado.TERMINADO;
                    terminadoSinalizado = true;
                    return false;
                }
                if (player.isBuffered()) {
                    estado = Estado.TOCANDO;
                }
                return true;

            case TOCANDO:
                if (player == null) {
                    estado              = Estado.TERMINADO;
                    terminadoSinalizado = true;
                    return false;
                }
                try {
                    player.update();
                } catch (Exception e) {
                    falhar();
                    return false;
                }

                if (player.isPlaying()) {
                    comecouTocar = true;
                }
                if (comecouTocar && !player.isPlaying()) {
                    parar();
                    estado              = Estado.TERMINADO;
                    terminadoSinalizado = true;
                    return false;
                }
                return true;

            case TERMINADO:
            case ERRO:
                return false;
        }
        return false;
    }

    // Desenha frame atual do video
    public void desenhar(SpriteBatch batch, float x, float y, float w, float h) {
        if (estado != Estado.TOCANDO && estado != Estado.CARREGANDO) return;
        if (player == null) return;
        try {
            com.badlogic.gdx.graphics.Texture tex = player.getTexture();
            if (tex != null) batch.draw(tex, x, y, w, h);
        } catch (Exception ignored) {}
    }

    // Consome flag de termino
    public boolean consumirTerminado() {
        boolean r       = terminadoSinalizado;
        terminadoSinalizado = false;
        return r;
    }

    public boolean isAtivo()    { return estado == Estado.CARREGANDO || estado == Estado.TOCANDO; }

    public boolean isPreparado(){ return estado == Estado.PREPARANDO; }

    public Estado  getEstado()  { return estado; }

    // Libera recursos do video
    public void dispose() {
        parar();
        caminhoPendente = null;
    }

    // Inicia reproducao do arquivo
    private void tocar(String caminho) {
        FileHandle arquivo = Gdx.files.internal(caminho);
        if (!arquivo.exists()) {
            falhar();
            return;
        }
        if (player != null) {
            try { player.dispose(); } catch (Exception ignored) {}
            player = null;
        }
        try {
            player = VideoPlayerCreator.createVideoPlayer();

            player.setOnCompletionListener(file -> {
                if (estado == Estado.TOCANDO || estado == Estado.CARREGANDO) {
                    estado              = Estado.TERMINADO;
                    terminadoSinalizado = true;
                }
            });

            player.play(arquivo);
            estado          = Estado.CARREGANDO;
            timerCarregando = 0f;
            comecouTocar    = false;
        } catch (Exception e) {
            falhar();
        }
    }

    // Marca video como encerrado com erro
    private void falhar() {
        if (player != null) {
            try { player.dispose(); } catch (Exception ignored) {}
            player = null;
        }
        estado              = Estado.ERRO;
        terminadoSinalizado = true;
    }

    // Para e libera o player
    private void parar() {
        if (player != null) {
            try { player.stop();    } catch (Exception ignored) {}
            try { player.dispose(); } catch (Exception ignored) {}
            player = null;
        }
        if (estado == Estado.TOCANDO || estado == Estado.CARREGANDO) {
            estado = Estado.TERMINADO;
        }
    }
}
