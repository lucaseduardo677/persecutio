# Persecutio

Branch do jogo implementado com **libGDX**. A estrutura está dividida entre o módulo `core`, responsável pela lógica compartilhada do jogo, e o módulo `lwjgl3`, responsável pela execução no desktop.

## A fazer
  - Portar as funcionalidades da branch `main`.
  - Migrar o carregamento de texturas e áudios para o `AssetManager` a fim de evitar memory leaks.
  - Agrupar as imagens soltas da pasta `img/` em um `Texture Atlas` pra não enviar muitos sprites pra GPU.
  - Desacoplar a lógica e o progresso do jogo da classe `TelaJogo`.
  - Implementar `TiledMapRenderer` pra ter um culling automático dos tiles do mapa.

## Build

O projeto usa **Gradle** e inclui o wrapper. É necessário ter **Java 17** instalado.

### Linux

```bash
./gradlew clean build
./gradlew lwjgl3:run
```

### Windows

```bat
gradlew.bat clean build
gradlew.bat lwjgl3:run
```

### Gerar o JAR executável

```bash
./gradlew lwjgl3:jar
```

O arquivo gerado fica em:

```text
lwjgl3/build/libs/
```
