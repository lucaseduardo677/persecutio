package com.persecutio.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.persecutio.game.PersecutioGame;

public class Lwjgl3Launcher {
    public static void main(String[] args) {
        if (StartupHelper.startNewJvmIfRequired()) return;
        createApplication();
    }

    private static Lwjgl3Application createApplication() {
        return new Lwjgl3Application(new PersecutioGame(), getDefaultConfiguration());
    }

    private static Lwjgl3ApplicationConfiguration getDefaultConfiguration() {
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("Persecutio");
        configuration.useVsync(true);

        // Define a taxa de atualização máxima suportada pelo seu monitor (ex: 180Hz)
        configuration.setForegroundFPS(Lwjgl3ApplicationConfiguration.getDisplayMode().refreshRate);

        // Configura para iniciar em tela cheia usando a resolução nativa do monitor do usuário
        configuration.setFullscreenMode(Lwjgl3ApplicationConfiguration.getDisplayMode());

        return configuration;
    }
}
