# Persecutio

Branch do jogo implementado com **libGDX**. A estrutura está dividida entre o módulo `core`, responsável pela lógica compartilhada do jogo, e o módulo `lwjgl3`, responsável pela execução no desktop.
## A fazer
- [x] Portar as funcionalidades da branch `main`.
- [x] Implementar iluminação usando o `Box2DLights`.
- [x] Desacoplar a lógica e o progresso do jogo da classe `TelaJogo`.
- [ ] Migrar o carregamento de texturas e áudios para o `AssetManager` a fim de evitar memory leaks.
- [ ] Agrupar as imagens soltas da pasta `img/` em um `Texture Atlas` pra não enviar muitos sprites pra GPU.
- [x] Implementar culling automático dos tiles do mapa.
- [ ] Implementar sistema de pontuação com categorias separadas (documentos e sessões).
- [ ] Implementar sistema de finais (bom, médio, ruim) com tela de encerramento.
- [ ] Implementar sistema de diálogo com o Dr. Gonzalez com perguntas, opções de resposta e três sessões progressivas.
- [ ] Implementar sistema de inventário de panfletos/documentos por ID para liberar repertório de diálogo.
- [ ] Implementar puzzle de fichas no mundo umbra com associação de pacientes a tipos de violência.
- [ ] Implementar tela de introdução antes da primeira sessão.
- [ ] Criar `TelaFinal` com três variações de conteúdo conforme pontuação.

## Build

O projeto usa **Gradle** e inclui o wrapper. É necessário ter **Java 17** instalado.

### Linux

```bash
./gradlew clean build
./gradlew lwjgl3:run
```

### Windows

```bat
gradlew clean build
gradlew lwjgl3:run
```

### Gerar o JAR executável

```bash
./gradlew lwjgl3:jar
```

O arquivo gerado fica em:

```text
lwjgl3/build/libs/
```
