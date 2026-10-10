# Alex vs Bus: The Race — Megalodonte

Adaptação jogável de [Alex vs Bus](https://github.com/M374LX/alexvsbus) para **Megalodonte Engine 2D**, com as 13 fases originais: 5 Normal, 5 Difícil e 3 Super. O projeto é aberto e editável pela engine; gameplay fica nos scripts Java, sem lógica específica de Alex dentro do núcleo.

Abra **game.json** na engine e clique **Play**. A cena inicial é o menu. A cena em edição é o título. Cada submenu é uma cena independente, com sua própria HUD visível; use Próxima cena para percorrer título, dificuldade, seleção de fase, configurações, vídeo, áudio, tamanho virtual, jukebox, sobre, créditos e saída. Depois vêm as 13 fases e o final.

## Jogar

- A/D ou setas: correr; há aceleração e desaceleração.
- Espaço, W, seta para cima ou Ctrl: pular. Salto tem buffer e tolerância ao sair da borda.
- No Android: esquerda/direita e salto por HUD, com multitoque.
- Esc ou botão II: pausa. Continuar, reiniciar e voltar ao menu ficam no painel.
- F9 ou Som no título: áudio on/off. F10 alterna scanlines durante a fase. F11 alterna tela cheia no Desktop.
- No menu: clique/toque o ícone Play, escolha a dificuldade e a fase. Enter avança com Normal/fase 1; teclas 1–5 selecionam a fase. Esc volta à tela anterior.

Há 90 segundos para chegar à porta do ônibus. Moedas de prata valem 50 e de ouro 100. Ao terminar, cada segundo restante acrescenta 10 pontos; a fase seguinte começa após a cena de conclusão. Reiniciar limpa a pontuação. Quedas usam checkpoints; bananas derrubam Alex, jatos/galinhas o lançam para trás, molas aumentam o salto, cordas atravessam buracos e caixas móveis abrem entradas dos túneis. Carros acionados pelo trajeto lançam bananas. Após a última fase da dificuldade há uma sequência final.

## Menus e configurações

Os menus reproduzem o fundo preto, a tipografia de 8 pixels, os ícones e as bordas do atlas original. Há telas separadas de dificuldade, fase, configurações, vídeo, áudio, tamanho virtual, jukebox, sobre, créditos e confirmação de saída.

Áudio, música e efeitos podem ser alternados separadamente. A jukebox reproduz as quatro músicas. Vídeo permite alternar scanlines e escolher a largura/altura virtual; no modo fixo as dimensões são aplicadas à câmera da corrida. Os botões de toque podem ser ocultados; as zonas de toque continuam funcionando (parte superior para salto). As preferências duram a sessão atual. Todas as dificuldades/fases ficam acessíveis para testar a engine; o cinza de Hard/Super reproduz a aparência da referência, sem bloquear a seleção.

Os submenus são editáveis sem executar scripts: suas posições, imagens e ações estão nas HUDs das cenas `menu-*`. A UI é declarativa em `game.json`; `tools/menu-assets.py` compõe os PNGs usando os sprites e caracteres originais, sem depender de fontes instaladas no celular. Os textos das telas Sobre/Créditos identificam a origem e a adaptação.

## Builds

A partir desta pasta:

```bash
bash tools/build.sh desktop
bash tools/build.sh android
# ou ambas:
bash tools/build.sh all
```

O script encontra a engine na estrutura local megalodonte-world. Em outra estrutura, configure `MEGALODONTE_ENGINE=/caminho/megalodonte-engine`.

Desktop nesta máquina: **Linux x86_64**, aplicativo independente com Java e JavaFX incluídos. Rode `build/desktop/alexvsbus/bin/alexvsbus`. A versão compactada está em `build/desktop/alexvsbus-linux-x86_64.tar.gz`; extraia antes de executar. Também pode iniciar o binário de qualquer diretório: o jogo é localizado junto ao aplicativo.

Android: `build/android/game-debug.apk`, com pacote próprio `megalodonte.games.alexvsbus`, SDK/mínimo API 35. É uma build debug instalável; não substitui o pacote padrão de demonstração da engine. O JDK/SDK usados pelo exportador são documentados no README da engine.

Ao concluir **cada build**, sua pasta abre automaticamente no explorador e o projeto é registrado para ser restaurado na próxima abertura do editor. Builds que falham não publicam nem abrem a saída. Para CI/ambiente sem interface, use `ENGINE_NO_OPEN=1`. A engine também tem os botões Exportar Desktop e Exportar Android.

## Editar os scripts

- `scripts/game/alexvsbus/Menu.java`: dificuldade, fase, controles e áudio.
- `scripts/game/alexvsbus/Race.java`: física, colisões, moedas, perigos, cronômetro, checkpoints e progressão.
- `scripts/game/alexvsbus/Ending.java`: sequência final.

Na engine, selecione a cena e use Script da cena. Abrir IDE gera/atualiza as bibliotecas locais. O projeto Gradle gerado usa Java 17; é possível executar `bash gradlew compileJava` após preparar a IDE. A engine recompila ao iniciar Play, e os exportadores compilam os scripts para Desktop/DEX Android.

## Fontes, assets e reprodução

A revisão original está fixada em `upstream/ORIGIN.json`. `upstream/assets` contém o atlas, arquivos de fase, sons WAV e músicas XM; `upstream/src` mantém a referência de programação. `tools/create-project.py` converte essas entradas para mapas/objetos da engine, extrai os frames e converte XM para MP3 com FFmpeg/libopenmpt. Requer Python 3, Pillow e FFmpeg.

```bash
python3 tools/create-project.py --force
```

**Esse comando substitui game.json**; preserve suas edições antes de regenerar. Os scripts Java são mantidos. Os PNGs/MP3s e avisos de licença ficam em assets/. As fases geradas usam o DTO TileMap comum, com regiões do atlas original; a renderização fica nos backends normais da engine.

## Diferenças da adaptação

As fases, sprites, sons e músicas são os originais. A física foi convertida para unidades de mundo e colisões AABB; curvas de veículos são aproximadas por retângulos. As cinco cenas de chegada e o final preservam a ideia do original com sequências simplificadas. A seleção de fases é livre para testar a engine. Não há suporte dedicado a gamepad, opção AZERTY ou as opções de escala de janela do original. Os menus usam a fonte bitmap, ícones e bordas originais; o HUD durante a corrida ainda usa textos nativos dos hosts. O editor da engine continua separado do executável do jogo.

## Créditos e licenças

Jogo, programação, música, efeitos e gráficos originais: **Alexandre Almeida / M374LX**, © 2021–2025. Design adicional: Hoton Bastos. Testes originais: Harim Pires. Fontes gráficas: Telegrama (YoWorks) e Press Start 2P (codeman38).

Código/adaptação: **GPL-3.0-or-later**. Assets originais e derivados: **CC BY-SA 4.0**. Os textos integrais estão em licenses/ e os créditos em LICENSE.txt; avisos também acompanham os assets exportados em assets/legal/. Este é um port para a engine Megalodonte, com a origem registrada e as alterações descritas acima.

As HUDs dos menus usam âncoras relativas à área central de conteúdo: voltar é `left/top`, volume é `right/top`, títulos são `center/top`, e controles inferiores usam `bottom`. Corrida mantém seus controles nas bordas da tela segura; pausa e scanlines usam a área de conteúdo. O inspetor mostra a referência e permite alterná-la sem deslocar o elemento.

A engine também permite editar as animações usadas por Alex: selecione `player` na fase e abra **Animações / prévia**. Colisores, visibilidade, espelhamento e propriedades como `kind`/`goalScene` ficam em **Colisor / propriedades**; camadas, tiles e obstáculos ficam no editor de mapa. O fluxo completo e a revisão dos campos estão em [Recursos do editor](docs/EDITOR_FEATURES.md). Os scripts continuam editáveis em Java dentro da engine.
