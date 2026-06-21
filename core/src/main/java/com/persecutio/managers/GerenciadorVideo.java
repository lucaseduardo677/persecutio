package com.persecutio.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.video.VideoPlayer;
import com.badlogic.gdx.video.VideoPlayerCreator;

// gerencia a reprodução de vídeo VP9/WebM durante o fade de transição de porta
public class GerenciadorVideo {

    // ciclo de vida da reprodução
    public enum Estado { IDLE, PREPARANDO, CARREGANDO, TOCANDO, TERMINADO, ERRO }

    private VideoPlayer player;
    private Estado      estado          = Estado.IDLE;
    private String      caminhoPendente = null;
    private boolean     terminadoSinalizado = false;

    // tempo aguardando buffer antes de declarar timeout
    private float timerCarregando = 0f;
    private static final float TIMEOUT_CARREGAMENTO = 2.0f;

    // registra o caminho do vídeo para ser iniciado quando o fade escurecer
    public void preparar(String caminho) {
        if (caminho == null || caminho.isEmpty()) {
            estado          = Estado.IDLE;
            caminhoPendente = null;
            return;
        }
        caminhoPendente     = caminho;
        estado              = Estado.PREPARANDO;
        terminadoSinalizado = false;
    }

    // inicia a reprodução do caminho preparado, chamado após a tela escurecer
    public void iniciar() {
        if (estado != Estado.PREPARANDO || caminhoPendente == null) return;
        tocar(caminhoPendente);
        caminhoPendente = null;
    }

    // avança a máquina de estados do vídeo, retorna true enquanto o vídeo estiver ativo
    public boolean atualizar(float delta) {
        switch (estado) {
            case IDLE:
            case PREPARANDO:
                return false;

            case CARREGANDO:
                timerCarregando += delta;
                if (timerCarregando >= TIMEOUT_CARREGAMENTO) {
                    // vídeo não bufferizou a tempo, considera concluído
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
                if (!player.isPlaying() && player.isBuffered()) {
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

    // desenha o frame atual do vídeo nas coordenadas e dimensões fornecidas
    public void desenhar(SpriteBatch batch, float x, float y, float w, float h) {
        if (estado != Estado.TOCANDO && estado != Estado.CARREGANDO) return;
        if (player == null) return;
        try {
            com.badlogic.gdx.graphics.Texture tex = player.getTexture();
            if (tex != null) batch.draw(tex, x, y, w, h);
        } catch (Exception ignored) {}
    }

    // consome e reseta o flag de término, usado pelo GerenciadorUI para avançar o fade
    public boolean consumirTerminado() {
        boolean r       = terminadoSinalizado;
        terminadoSinalizado = false;
        return r;
    }

    public boolean isAtivo()    { return estado == Estado.CARREGANDO || estado == Estado.TOCANDO; }
    public boolean isPreparado(){ return estado == Estado.PREPARANDO; }
    public Estado  getEstado()  { return estado; }

    public void dispose() {
        parar();
        caminhoPendente = null;
    }

    // abre o arquivo e inicia a reprodução via VideoPlayerCreator
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

            // listener sinaliza término mesmo que o loop principal não perceba
            player.setOnCompletionListener(file -> {
                if (estado == Estado.TOCANDO || estado == Estado.CARREGANDO) {
                    estado              = Estado.TERMINADO;
                    terminadoSinalizado = true;
                }
            });

            player.play(arquivo);
            estado          = Estado.CARREGANDO;
            timerCarregando = 0f;
        } catch (Exception e) {
            falhar();
        }
    }

    // libera o player e marca o vídeo como encerrado com erro
    private void falhar() {
        if (player != null) {
            try { player.dispose(); } catch (Exception ignored) {}
            player = null;
        }
        estado              = Estado.ERRO;
        terminadoSinalizado = true;
    }

    // para a reprodução e libera o player sem mudar para ERRO
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
