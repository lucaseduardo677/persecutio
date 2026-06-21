package com.persecutio.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.video.VideoPlayer;
import com.badlogic.gdx.video.VideoPlayerCreator;

// Gerencia video transicao porta
public class GerenciadorVideo {

    // Ciclo vida reprodução
    public enum Estado { IDLE, PREPARANDO, CARREGANDO, TOCANDO, TERMINADO, ERRO }

    private VideoPlayer player;
    private Estado      estado          = Estado.IDLE;
    private String      caminhoPendente = null;
    private boolean     terminadoSinalizado = false;
    private boolean     comecouTocar    = false;

    // Tempo aguardando buffer antes declarar timeout
    private float timerCarregando = 0f;
    private static final float TIMEOUT_CARREGAMENTO = 2.0f;

    // Registra caminho vídeo iniciado fade escurecer
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

    // Reproducao caminho preparado
    public void iniciar() {
        if (estado != Estado.PREPARANDO || caminhoPendente == null) return;
        tocar(caminhoPendente);
        caminhoPendente = null;
    }

    // Avanca estado video enquanto estiver ativo
    public boolean atualizar(float delta) {
        switch (estado) {
            case IDLE:
            case PREPARANDO:
                return false;

            case CARREGANDO:
                timerCarregando += delta;
                if (timerCarregando >= TIMEOUT_CARREGAMENTO) {
                    // Video buffer tempo limite
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

                // Considera termino so depois tocar
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

    // Frame atual vídeo coordenadas dimensões fornecidas
    public void desenhar(SpriteBatch batch, float x, float y, float w, float h) {
        if (estado != Estado.TOCANDO && estado != Estado.CARREGANDO) return;
        if (player == null) return;
        try {
            com.badlogic.gdx.graphics.Texture tex = player.getTexture();
            if (tex != null) batch.draw(tex, x, y, w, h);
        } catch (Exception ignored) {}
    }

    // Flag termino
    public boolean consumirTerminado() {
        boolean r       = terminadoSinalizado;
        terminadoSinalizado = false;
        return r;
    }

    // Consulta do estado
    public boolean isAtivo()    { return estado == Estado.CARREGANDO || estado == Estado.TOCANDO; }
    // Consulta do estado
    public boolean isPreparado(){ return estado == Estado.PREPARANDO; }
    // Consulta do estado
    public Estado  getEstado()  { return estado; }

    // Liberação dos recursos
    public void dispose() {
        parar();
        caminhoPendente = null;
    }

    // Arquivo reprodução via VideoPlayerCreator
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

            // Listener sinaliza término mesmo loop principal
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

    // Libera player marca vídeo encerrado erro
    private void falhar() {
        if (player != null) {
            try { player.dispose(); } catch (Exception ignored) {}
            player = null;
        }
        estado              = Estado.ERRO;
        terminadoSinalizado = true;
    }

    // Reprodução libera player mudar ERRO
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
