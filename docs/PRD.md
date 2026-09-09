# PRD --- Mesa Franca

*Plataforma de jogos de tabuleiro virtual*

**Status:** Arquitetura inicial / definição do produto\
**Data:** 31/08/2026\
**Versão:** 0.2

> **Como ler este documento.** As seções 1--40 descrevem produto, princípios
> e arquitetura conceitual, e foram definidas pelo autor do projeto: elas
> têm precedência sobre qualquer sugestão posterior. As seções 41--47
> formalizam personas, requisitos numerados, a arquitetura hexagonal
> concreta, o índice de ADRs, o estado da implementação e as questões ainda
> em aberto.
>
> Decisões de stack ficam registradas em [`docs/adr/`](adr/README.md).
> Este PRD aponta para elas; ele não as repete.

------------------------------------------------------------------------

## 1. Visão do produto

Criar uma **plataforma digital de jogos de tabuleiro** que preserve a
dinâmica, simplicidade e sensação social dos jogos de mesa
convencionais, usando o digital para eliminar trabalho desnecessário e
ampliar a experiência.

A proposta não é simplesmente transformar jogos de tabuleiro em
videogames.

> **Uma mesa de jogos digital que mantém a dinâmica do board game e usa
> o digital para eliminar o trabalho desnecessário e ampliar a
> experiência.**

A visão de longo prazo contempla:

-   PC;
-   celular/tablet;
-   TV;
-   AR;
-   VR.

A mesma partida deve poder ser apresentada por diferentes interfaces,
sem que as regras do jogo dependam do dispositivo.

------------------------------------------------------------------------

# 2. Problema

Jogos de tabuleiro físicos proporcionam uma experiência social muito
boa, mas exigem diversas tarefas manuais:

-   preparar o jogo;
-   distribuir cartas;
-   controlar turnos;
-   administrar peças;
-   realizar cálculos;
-   controlar estados;
-   aplicar regras;
-   registrar resultados;
-   reorganizar o jogo;
-   lidar com informações privadas.

Por outro lado, muitos jogos digitais substituem a experiência de mesa
por uma experiência de videogame tradicional.

A plataforma pretende ocupar um espaço intermediário:

> **A sensação deve ser de estar sentado à mesa jogando um board game,
> mas com o computador cuidando do trabalho operacional.**

------------------------------------------------------------------------

# 3. Princípios do produto

## 3.1 Mesa, não videogame

A plataforma deve se comportar como uma **mesa digital de jogos**, e não
como uma coleção de videogames.

O usuário deve perceber:

-   peças;
-   cartas;
-   dados;
-   tabuleiro;
-   turnos;
-   ações;
-   interação social.

O sistema cuida daquilo que normalmente seria trabalho manual.

------------------------------------------------------------------------

## 3.2 Game Core compartilhado

Cada jogo será um módulo/plugin sobre um núcleo comum.

``` text
                    BOARD GAME PLATFORM

                         GAME CORE
                            │
             ┌──────────────┼──────────────┐
             │              │              │
             ▼              ▼              ▼
           LUDO        LOVE LETTER      UNO
         (plugin)        (clone)       (clone)
```

O Core fornece infraestrutura e conceitos genéricos.

Os jogos fornecem suas próprias regras.

------------------------------------------------------------------------

## 3.3 Servidor como autoridade

O servidor é a autoridade sobre o estado e o resultado da partida.

O cliente não pode simplesmente declarar uma mudança de estado.

O princípio é:

> **O cliente expressa intenção; o servidor valida e determina o
> resultado; o cliente apresenta o resultado.**

Fluxo:

``` text
Intent
  ↓
Validation
  ↓
Resolution
  ↓
Event
  ↓
Presentation
```

Exemplo:

``` text
Unity:
"quero rolar o dado"

        ↓

Servidor:
é a vez do jogador?
a ação é permitida?

        ↓

Servidor:
RNG → 6

        ↓

GameState atualizado

        ↓

DiceRolled(6)

        ↓

Unity:
anima o dado até mostrar 6
```

A animação pertence à UI.

O resultado pertence ao servidor.

------------------------------------------------------------------------

## 3.4 Visual não é estado do jogo

Uma animação pode começar antes de o resultado chegar, mas ela nunca
determina o resultado.

``` text
VISUAL
3 → 5 → 1 → 4 → 2 → 6

GAME STATE
        DiceRolled(6)
```

A animação pode ser sofisticada, física ou aleatória apenas para fins
visuais.

O resultado autoritativo é sempre o determinado pelo Game Core/servidor.

Isso reduz:

-   trapaças;
-   inconsistências;
-   divergência entre clientes;
-   dependência das regras na UI.

------------------------------------------------------------------------

## 3.5 Máxima experiência com mínima infraestrutura

O projeto deve evitar complexidade prematura.

Princípios:

-   Unity para cliente/renderização inicialmente;
-   backend único inicialmente;
-   PostgreSQL como banco principal;
-   sem Redis inicialmente;
-   sem microserviços prematuros;
-   autenticação integrada ao backend inicialmente;
-   jogos como módulos/plugins;
-   servidor como autoridade;
-   AR/VR somente depois da validação da experiência básica.

------------------------------------------------------------------------

# 4. Público e experiência desejada

O produto é destinado a pessoas que gostam de jogos de tabuleiro e
querem:

-   jogar presencialmente usando uma mesa digital;
-   jogar remotamente;
-   reduzir o trabalho operacional do jogo;
-   manter interação social;
-   eventualmente jogar contra bots/IA;
-   acessar diferentes jogos pela mesma plataforma.

A experiência futura pode incluir:

``` text
             TABLET / TV
          ┌───────────────┐
          │   TABULEIRO   │
          └───────────────┘

       📱                 📱

              📱     📱
```

O tabuleiro pode ficar em uma tela central, enquanto celulares
individuais exibem informações privadas.

------------------------------------------------------------------------

# 5. Jogos iniciais

Os três primeiros jogos têm uma função tanto de produto quanto de
validação arquitetural.

Eles foram escolhidos por exercitarem conceitos diferentes.

## 5.1 Ludo

Testa:

-   tabuleiro;
-   peças;
-   dados;
-   movimentação;
-   posições;
-   turnos;
-   regras espaciais;
-   estado público.

Também deverá ser investigada uma representação configurável/procedural
do tabuleiro para diferentes quantidades de jogadores:

``` text
2 jogadores
3 jogadores
4 jogadores
5 jogadores
6 jogadores
```

A intenção é evitar simplesmente manter uma implementação completamente
independente para cada quantidade, quando uma configuração genérica for
possível.

------------------------------------------------------------------------

## 5.2 Love Letter --- clone

O segundo jogo será um **clone de Love Letter**.

Ele introduz conceitos diferentes do Ludo:

-   cartas;
-   deck;
-   compra;
-   descarte;
-   mão privada;
-   informação escondida;
-   efeitos;
-   dedução;
-   blefe;
-   desafios;
-   eliminação;
-   interação social.

Sua função arquitetural principal é testar:

``` text
PUBLIC STATE
+
PRIVATE STATE
```

Exemplo:

``` text
                    GAME STATE
                         │
             ┌───────────┴───────────┐
             │                       │
       PUBLIC STATE            PRIVATE STATE
             │                       │
       todos recebem          apenas o jogador recebe
```

------------------------------------------------------------------------

## 5.3 UNO --- clone

O terceiro jogo será um **clone de UNO**.

Testa:

-   cartas;
-   mãos privadas;
-   deck;
-   descarte público;
-   compra;
-   turnos;
-   efeitos de cartas;
-   mudança de cor;
-   regras condicionais;
-   múltiplos jogadores;
-   partidas rápidas.

------------------------------------------------------------------------

## 5.4 Matriz de validação

``` text
LUDO
→ espaço / peças / dados / movimento

LOVE LETTER
→ cartas / informação privada / dedução / blefe

UNO
→ cartas / efeitos / regras condicionais / turnos rápidos
```

A combinação dos três deverá revelar o que realmente pertence ao Core e
o que é específico de cada jogo.

------------------------------------------------------------------------

# 6. Estratégia de desenvolvimento

Não construir primeiro um framework gigantesco.

O Core deve surgir dos jogos.

``` text
LUDO
  ↓
Core v0
  ↓
LOVE LETTER
  ↓
descobrir abstrações comuns
  ↓
UNO
  ↓
refinar Core
```

Regra:

> **Se uma funcionalidade não for necessária para pelo menos um dos
> jogos, provavelmente ela não entra no Core inicial.**

Regra arquitetural:

> **Criar um novo jogo não deveria exigir modificar o Core.**

Se o terceiro jogo exigir mudanças generalizadas no Core, investigar se
a abstração está errada.

------------------------------------------------------------------------

# 7. Requisito arquitetural fundamental: jogar sem Unity

O backend deve ser capaz de executar uma partida sem depender do Unity.

Isso é importante para:

-   testes;
-   desenvolvimento;
-   depuração;
-   automação;
-   bots/IA;
-   validação das regras;
-   testes de API;
-   futura criação de outros clientes.

O mesmo jogo deve poder ser controlado por:

``` text
             GAME ENGINE
                  │
       ┌──────────┼──────────┐
       ▼          ▼          ▼
      CLI        Web       Unity
```

Todos devem utilizar o mesmo motor de regras.

------------------------------------------------------------------------

# 8. Cliente de desenvolvimento / CLI

Antes do Unity, deve existir uma forma simples de controlar uma partida
pelo terminal ou por testes automatizados.

Exemplo conceitual:

``` text
$ python play.py ludo

LUDO

Player 1
Roll: 6

Pieces:
1: home
2: home
3: 12
4: home

Choose piece: 3

Moving...
```

Esse cliente não precisa ser produto final.

Seu objetivo é permitir que o jogo seja efetivamente jogável/testável
sem interface gráfica.

------------------------------------------------------------------------

# 9. Níveis de teste

## 9.1 Teste do Game Core

Sem HTTP, banco ou Unity.

Exemplo conceitual:

``` python
game = LudoGame(...)
game.start()

result = game.apply(
    MovePiece(player=1, piece=2)
)

assert result.success
```

Objetivo:

-   testar regras;
-   testar transições de estado;
-   testar ações inválidas;
-   testar aleatoriedade;
-   testar condições de vitória;
-   testar informação pública/privada.

------------------------------------------------------------------------

## 9.2 Teste da API

Simular um cliente usando HTTP.

Fluxo conceitual:

``` text
HTTP Client
    ↓
POST /games
    ↓
Backend
    ↓
Ludo
```

O teste deve verificar:

-   criação da partida;
-   entrada de jogadores;
-   início;
-   consulta do estado;
-   envio de ações;
-   validação;
-   eventos;
-   erros;
-   reconexão.

------------------------------------------------------------------------

## 9.3 Teste de integração com Unity

Somente depois de a API estar definida e testada:

``` text
Unity
   ↓
HTTP / WebSocket
   ↓
Backend
```

O Unity deve ser um cliente do Game API, não uma segunda implementação
das regras.

------------------------------------------------------------------------

# 10. Arquitetura geral

``` text
                    UNITY CLIENT
                  PC / Mobile / AR / VR
                           │
                           │ HTTP / WebSocket
                           ▼
                    BACKEND / API
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
      Auth               Lobby          Game Session
        │                  │                  │
        └──────────────────┼──────────────────┘
                           │
                       Game Core
                           │
                 ┌─────────┼─────────┐
                 ▼         ▼         ▼
               Ludo    Love Letter  UNO
                           │
                           ▼
                      PostgreSQL
```

Inicialmente, o backend pode ser um único processo/serviço modular.

Não criar microserviços apenas porque existem conceitos diferentes.

------------------------------------------------------------------------

# 11. Backend

## 11.1 Game Core

Responsável pelos conceitos genéricos:

-   Player;
-   Game;
-   GameState;
-   Turn;
-   Action;
-   Event;
-   objetos;
-   regras genéricas;
-   processamento de ações;
-   sincronização da partida;
-   separação entre estado público e privado.

O Core não deve conhecer regras específicas de Ludo, Love Letter ou UNO.

------------------------------------------------------------------------

## 11.2 Lobby

Responsável por:

-   criar sala;
-   entrar;
-   sair;
-   convite;
-   jogadores presentes;
-   pronto/não pronto;
-   iniciar partida;
-   matchmaking futuramente.

------------------------------------------------------------------------

## 11.3 Game Session

Representa uma partida específica.

Responsável por:

-   jogo escolhido;
-   jogadores;
-   configuração;
-   estado;
-   início;
-   fim;
-   reconexão;
-   persistência.

------------------------------------------------------------------------

## 11.4 Realtime

Responsável por:

-   ações;
-   eventos;
-   sincronização;
-   presença;
-   reconexão.

Inicialmente pode permanecer dentro do próprio backend/Game Session.

Não precisa ser um microserviço independente.

------------------------------------------------------------------------

## 11.5 Persistence

PostgreSQL.

Responsável por:

-   usuários;
-   partidas;
-   configurações;
-   estado persistente;
-   histórico;
-   estatísticas futuras.

------------------------------------------------------------------------

# 12. PostgreSQL e cache

Não introduzir Redis inicialmente.

Prioridade:

> **menos dependências, menos infraestrutura, menor custo e menor
> complexidade operacional.**

PostgreSQL será o banco principal e, quando possível e adequado, também
poderá atender necessidades que futuramente poderiam ser resolvidas com
cache separado.

Arquitetura inicial:

``` text
Backend
   │
   ▼
PostgreSQL
```

Em vez de:

``` text
Backend
  ├── PostgreSQL
  └── Redis
```

Redis somente deverá ser considerado quando existir uma necessidade
concreta de escala/performance que PostgreSQL não atenda de forma
econômica.

------------------------------------------------------------------------

# 13. Autenticação

Não criar um Auth Microservice no MVP.

A autenticação faz parte da arquitetura geral do backend.

Inicialmente:

``` text
Client
  ↓
Backend
  ↓
Authentication
```

A implementação pode permanecer dentro do próprio backend.

Um provedor externo de identidade poderá ser introduzido posteriormente
se houver uma razão concreta.

Princípio:

> **Não criar um serviço separado simplesmente porque existe um conceito
> separado.**

------------------------------------------------------------------------

# 14. Game API

A API deve trabalhar com **intenções**, e não com declarações
arbitrárias de estado.

## 14.1 Criar partida

``` http
POST /games
```

Exemplo:

``` json
{
  "game": "ludo",
  "players": 4
}
```

------------------------------------------------------------------------

## 14.2 Entrar na partida

``` http
POST /games/{gameId}/players
```

------------------------------------------------------------------------

## 14.3 Iniciar

``` http
POST /games/{gameId}/start
```

------------------------------------------------------------------------

## 14.4 Consultar estado

``` http
GET /games/{gameId}/state
```

O estado retornado deve respeitar a visibilidade do jogador.

Um cliente não deve receber informações privadas de outro jogador.

------------------------------------------------------------------------

## 14.5 Enviar ação

``` http
POST /games/{gameId}/actions
```

Exemplo:

``` json
{
  "type": "move_piece",
  "piece": 2
}
```

Para rolar o dado:

``` json
{
  "type": "roll_dice"
}
```

O cliente não envia:

``` json
{
  "type": "roll_dice",
  "value": 6
}
```

porque o resultado pertence ao servidor.

------------------------------------------------------------------------

# 15. Fluxo de uma ação

O fluxo padrão do Game Core deverá seguir:

``` text
┌──────────┐
│  Intent  │  "quero rolar"
└────┬─────┘
     ↓
┌────────────┐
│ Validation │  "pode?"
└────┬───────┘
     ↓
┌────────────┐
│ Resolution │  "deu 6"
└────┬───────┘
     ↓
┌─────────┐
│  Event  │  DiceRolled(6)
└────┬────┘
     ↓
┌──────────────┐
│ Presentation │  🎲 animação → 6
└──────────────┘
```

Essa separação deve ser aplicada sempre que fizer sentido.

------------------------------------------------------------------------

# 16. Dados e aleatoriedade

Dados são um exemplo importante da separação entre lógica e
apresentação.

## Cliente

Pode:

-   iniciar a animação;
-   executar física visual;
-   mostrar efeitos;
-   tocar som;
-   antecipar visualmente o movimento.

## Servidor

Deve:

-   validar que o jogador pode rolar;
-   executar o RNG;
-   determinar o resultado;
-   atualizar o GameState;
-   emitir o evento.

Exemplo:

``` text
Unity:
🎲 → 🎲 → 🎲 → 🎲

Servidor:
RNG → 6

Servidor → Unity:
DiceRolled(6)

Unity:
🎲 → 6
```

O atraso de rede não deve transformar a animação em uma decisão do jogo.

------------------------------------------------------------------------

# 17. Segurança e trapaça

A preocupação não deve ser apenas "proteger a API".

O modelo inteiro deve impedir que o cliente seja autoridade.

Não confiar em:

-   resultado de dado enviado pelo cliente;
-   posição de peça enviada pelo cliente;
-   captura declarada pelo cliente;
-   pontuação calculada pelo cliente;
-   mudança de turno declarada pelo cliente;
-   cartas privadas fornecidas pelo cliente.

Confiar apenas em:

``` text
Intent
   ↓
Server validation
   ↓
Server resolution
   ↓
Authoritative state
```

Um usuário pode construir um cliente próprio, modificar o Unity ou
chamar diretamente a API. O sistema ainda deve impedir ações inválidas.

------------------------------------------------------------------------

# 18. Unity

Unity 3D é a tecnologia inicial para cliente/renderização.

O Unity não deve conter a autoridade das regras.

Responsabilidades principais:

-   renderização;
-   interação;
-   animações;
-   áudio;
-   câmera;
-   feedback visual;
-   apresentação dos eventos;
-   envio de intenções ao backend.

Fluxo:

``` text
GAME LOGIC
     │
     ▼
GAME CORE
     │
     ▼
GAME API
     │
     ▼
UNITY
     │
     ▼
RENDERING / INTERACTION
```

A lógica dos jogos deve ser independente da forma como o jogo é
renderizado.

------------------------------------------------------------------------

# 19. Comunicação Unity ↔ Backend

A comunicação inicial poderá utilizar:

-   HTTP para comandos/consultas;
-   WebSocket ou mecanismo equivalente para eventos em tempo real.

Conceitualmente:

``` text
Unity
  │
  ├── HTTP → comandos / consultas
  │
  └── WebSocket ← eventos / sincronização
```

A escolha definitiva do transporte é uma decisão técnica posterior.

O requisito de produto é que o Unity seja apenas um cliente do Game API.

------------------------------------------------------------------------

# 20. Estado público e privado

O sistema deve distinguir explicitamente:

``` text
GAME STATE
    │
    ├── PUBLIC STATE
    │
    └── PRIVATE STATE
```

Exemplo de Love Letter:

``` text
                SERVER
                   │
              carta privada
                   │
             ┌─────┴─────┐
             ▼           X
         Player 1     Players 2-4
```

O backend deve gerar uma representação do estado adequada para cada
jogador.

Isso deve fazer parte do contrato do Core, e não ser uma
responsabilidade do Unity.

------------------------------------------------------------------------

# 21. Eventos

Eventos representam fatos que ocorreram na partida.

Exemplos:

``` text
DiceRolled(6)
PieceMoved(player, piece, from, to)
CardDrawn(player, card)
CardPlayed(player, card)
ColorChanged(player, color)
PlayerEliminated(player)
TurnChanged(player)
GameFinished(winner)
```

Um evento não é uma ordem para o cliente.

Ele é a representação de algo que já aconteceu.

A UI usa o evento para apresentar a consequência visual.

------------------------------------------------------------------------

# 22. Ações

Ações representam intenções do jogador.

Exemplos:

``` text
RollDice
MovePiece
DrawCard
PlayCard
ChooseColor
EndTurn
```

Uma ação deve ser validada pelo jogo antes de alterar o estado.

Exemplo:

``` text
MovePiece
   ↓
é o turno do jogador?
   ↓
peça pertence ao jogador?
   ↓
movimento permitido?
   ↓
dados permitem?
   ↓
SIM
   ↓
GameState atualizado
   ↓
PieceMoved
```

------------------------------------------------------------------------

# 23. GameState

O `GameState` deve representar o estado autoritativo da partida.

Deve conter somente aquilo que é necessário para reconstruir e validar a
partida.

Conceitos esperados:

-   jogo;
-   fase da partida;
-   jogadores;
-   turno;
-   objetos;
-   posições;
-   cartas;
-   decks;
-   descarte;
-   resultados;
-   efeitos ativos;
-   informações necessárias às regras.

A representação exata deverá surgir durante a implementação dos três
jogos.

------------------------------------------------------------------------

# 24. Game

`Game` representa a definição e execução das regras de um jogo.

Conceitualmente:

``` text
Game
 ├── initialState()
 ├── availableActions(state, player)
 ├── validate(action, state)
 ├── apply(action, state)
 └── events
```

A interface exata ainda deverá ser definida.

O ponto fundamental é que o Core não precisa saber como Ludo ou UNO
funcionam.

------------------------------------------------------------------------

# 25. Plugin de jogo

Cada jogo deve ser isolado em seu módulo.

Estrutura conceitual:

``` text
/games

├── ludo/
│   ├── Rules
│   ├── State
│   ├── Actions
│   └── Renderer
│
├── love-letter/
│   ├── Rules
│   ├── State
│   ├── Actions
│   └── Renderer
│
└── uno/
    ├── Rules
    ├── State
    ├── Actions
    └── Renderer
```

A estrutura física definitiva do código ainda deverá ser definida.

------------------------------------------------------------------------

# 26. Separação de responsabilidades

A arquitetura deve evitar:

``` text
Ludo
  ↓
WebSocket
  ↓
PostgreSQL
```

O desejado é:

``` text
Ludo
  ↓
Game API / Game Core
  ↓
Infrastructure
  ↓
Network / Database
```

Isso permite trocar posteriormente:

-   transporte;
-   banco;
-   servidor;
-   infraestrutura multiplayer;
-   cliente.

Sem reescrever as regras do jogo.

------------------------------------------------------------------------

# 27. Realtime e sincronização

A partida é mantida pelo servidor.

Quando uma ação válida altera o estado:

``` text
Client A
   │
   │ Action
   ▼
Server
   │
   ├── validate
   ├── resolve
   ├── update state
   └── emit events
          │
      ┌───┴────┐
      ▼        ▼
 Client A   Client B
```

Cada cliente recebe somente as informações que pode conhecer.

O servidor deve permitir que um cliente reconecte e obtenha o estado
atual da partida.

------------------------------------------------------------------------

# 28. Reconexão

A sessão de jogo deve sobreviver à perda temporária da conexão do
cliente.

Após reconectar:

``` text
Unity
  │
  │ reconnect
  ▼
Backend
  │
  └── estado atual + eventos necessários
           ↓
         Unity
```

O cliente deve reconstruir sua apresentação a partir do estado
autoritativo.

------------------------------------------------------------------------

# 29. Arquitetura de testes

A plataforma deve ser testável em camadas:

``` text
             End-to-End
                 │
             Unity/API
                 │
               API
                 │
             Game Core
                 │
               Rules
```

A maior parte das regras deverá ser testada sem Unity.

Isso reduz drasticamente o custo de desenvolvimento e torna possíveis
testes automatizados rápidos.

------------------------------------------------------------------------

# 30. Bots e IA

A arquitetura deve permitir futuramente que uma IA ou bot seja um
jogador.

Como o jogo pode ser controlado sem Unity:

``` text
        ┌── Unity player
        │
        ├── Web player
        │
Game ←──┼── CLI player
        │
        └── AI player
```

O bot deve utilizar as mesmas ações e receber as mesmas informações que
um jogador teria direito de receber.

Isso também serve como validação da qualidade da API.

------------------------------------------------------------------------

# 31. Experiência híbrida futura

Uma possibilidade é usar:

-   TV/tablet como tabuleiro central;
-   celular de cada jogador como interface privada.

Exemplo:

``` text
             TABLET / TV
          ┌───────────────┐
          │   TABULEIRO   │
          └───────────────┘

       📱                 📱

              📱     📱
```

Isso é particularmente interessante para jogos com informação privada.

Love Letter seria um dos primeiros casos para validar esse conceito.

------------------------------------------------------------------------

# 32. AR e VR

AR e VR fazem parte da visão de longo prazo.

Não fazem parte do primeiro MVP.

O princípio é:

> **A mesa é a mesma; o dispositivo muda.**

A mesma partida deve poder ser apresentada por:

-   PC;
-   celular/tablet;
-   TV;
-   AR;
-   VR.

A lógica do jogo não deve precisar ser reescrita para cada plataforma.

------------------------------------------------------------------------

# 33. Concorrentes e posicionamento

Referências a estudar:

-   Tabletop Simulator;
-   Tabletopia;
-   Board Game Arena;
-   aplicativos individuais de jogos de tabuleiro;
-   versões digitais de jogos conhecidos.

O concorrente conceitualmente mais próximo é o Tabletop Simulator.

Posicionamento pretendido:

**Tabletop Simulator:**

> "Aqui está uma mesa virtual; faça o que quiser."

**Nossa proposta:**

> "Aqui está uma mesa virtual; sente-se e jogue."

A plataforma pretende ser menos sandbox de física e mais uma
**experiência estruturada de board game**.

------------------------------------------------------------------------

# 34. Visão de longo prazo

Depois que o Core e alguns jogos estiverem funcionando, poderão surgir:

-   catálogo de jogos;
-   jogos próprios;
-   jogos licenciados;
-   marketplace;
-   criação de jogos por terceiros;
-   IA como árbitro/assistente;
-   AR;
-   VR;
-   jogos gerados/configurados por IA.

Uma hipótese particularmente ambiciosa:

> fotografar um jogo físico + fornecer suas regras → IA gera uma
> especificação/plugin digital.

Isso é visão futura e não faz parte do MVP.

------------------------------------------------------------------------

# 35. Escopo do MVP

O MVP deve provar quatro coisas:

### 35.1 O motor funciona

Um jogo consegue executar suas regras sem Unity.

### 35.2 A arquitetura é realmente genérica

Ludo, Love Letter e UNO conseguem usar o mesmo Core sem incorporar
regras específicas no Core.

### 35.3 A API funciona como contrato

Um cliente externo consegue:

-   criar partida;
-   entrar;
-   iniciar;
-   consultar estado;
-   enviar ações;
-   receber eventos;
-   reconectar.

### 35.4 Unity consegue ser apenas a apresentação

O Unity consegue:

-   conectar;
-   mostrar estado;
-   enviar intenções;
-   receber eventos;
-   animar;
-   atualizar a interface.

------------------------------------------------------------------------

# 36. Fora do escopo inicial

Não priorizar no MVP:

-   microserviços;
-   Redis;
-   Kubernetes;
-   infraestrutura altamente distribuída;
-   AR;
-   VR;
-   marketplace;
-   criação de jogos por terceiros;
-   geração automática de jogos por IA;
-   matchmaking avançado;
-   economia complexa;
-   sistema completo de licenciamento;
-   cliente gráfico definitivo antes de validar o Core.

------------------------------------------------------------------------

# 37. Critérios de sucesso técnicos

O projeto deve ser considerado tecnicamente validado quando:

1.  Ludo puder ser executado sem Unity.
2.  Ludo puder ser controlado por uma API.
3.  Ações inválidas forem rejeitadas pelo servidor.
4.  Resultados aleatórios forem determinados pelo servidor.
5.  O Unity puder apenas solicitar ações e apresentar resultados.
6.  O estado puder ser reconstruído após reconexão.
7.  Informações privadas não vazarem para outros jogadores.
8.  Love Letter puder utilizar o mesmo mecanismo básico de
    ações/estado/eventos.
9.  UNO puder utilizar o mesmo mecanismo básico.
10. O Core não precisar conhecer as regras específicas dos três jogos.

------------------------------------------------------------------------

# 38. Próximo passo técnico

O próximo passo não é escolher Redis, Photon, Kubernetes ou outros
componentes de infraestrutura.

Também não é começar pela interface gráfica definitiva.

O próximo passo é desenhar **os três jogos lado a lado**, começando pelo
Ludo, e identificar:

1.  O que é um `Game`?
2.  O que é um `GameState`?
3.  O que é uma `Action`?
4.  O que é um `Event`?
5.  O que é um `Turn`?
6.  Como representar objetos físicos virtuais?
7.  Como representar informação pública e privada?
8.  Como o jogo valida uma ação?
9.  Como o servidor resolve uma ação?
10. Como o servidor sincroniza o estado?
11. Como um plugin é carregado?
12. Como o jogo pode ser executado sem Unity?
13. Como o Unity consome a API?
14. O que Ludo, Love Letter e UNO têm efetivamente em comum?
15. O que **não** deve entrar no Core?

A meta não é criar um Core "bonito" no papel.

A meta é:

> **Ludo + Love Letter + UNO funcionarem sobre o mesmo Core sem que o
> Core precise conhecer as regras específicas de nenhum deles.**

------------------------------------------------------------------------

# 39. Decisões arquiteturais consolidadas

  Decisão                   Diretriz
  ------------------------- -----------------------------------------------
  Cliente inicial           Unity 3D
  Backend                   Serviço único modular
  Banco                     PostgreSQL
  Cache                     Não usar Redis inicialmente
  Auth                      Integrado ao backend inicialmente
  Jogos                     Plugins/módulos
  Autoridade                Servidor
  Regras                    Backend/Game Core
  UI                        Apresentação/interação
  Aleatoriedade             Servidor
  Animações                 Cliente
  Comunicação               HTTP + realtime
  Estado                    Autoritativo no servidor
  Informação privada        Filtrada por jogador
  Testes                    Game Core → API → integração → Unity
  CLI                       Cliente de desenvolvimento/teste
  Bots/IA                   Possíveis futuramente usando o mesmo contrato
  AR/VR                     Visão futura
  Microserviços             Evitar inicialmente
  Infraestrutura complexa   Evitar até haver necessidade

> **Complemento (ADR-0003).** A tabela permanece válida: Unity é o cliente
> de produto. Foi acrescentado um cliente web intermediário, construído
> **antes** do Unity, cuja função é provar que a API é agnóstica de cliente
> (critério 35.3) e permitir playtest sem instalação. Ordem: CLI → web →
> Unity. Ver [ADR-0003](adr/0003-engine-do-cliente.md).

------------------------------------------------------------------------

# 40. Princípio central do sistema

Toda a arquitetura pode ser resumida em:

``` text
                    PLAYER
                       │
                       ▼
                  UI / UNITY
                       │
                  "quero fazer X"
                       │
                       ▼
                     API
                       │
                       ▼
                 GAME SESSION
                       │
                       ▼
                   GAME CORE
                       │
                ┌──────┴──────┐
                ▼             ▼
            VALIDATION     RESOLUTION
                              │
                         GameState
                              │
                            Event
                              │
                              ▼
                       Realtime / API
                              │
                              ▼
                            UNITY
                              │
                         PRESENTATION
                              │
                              ▼
                         🎲 🎴 ♟️
```

A separação fundamental é:

> **A UI apresenta o jogo. O Game Core executa o jogo. O servidor é a
> autoridade.**

Esse princípio deve orientar as decisões técnicas subsequentes.

------------------------------------------------------------------------

# 41. Personas

As personas abaixo existem para arbitrar prioridades quando dois
requisitos entram em conflito. Elas não são público-alvo de marketing.

## 41.1 Ana --- a anfitriã da mesa presencial

Reúne amigos em casa uma vez por mês. Gosta de jogos de tabuleiro, mas
detesta explicar regras, montar o tabuleiro e conferir pontuação.

- **Contexto:** todos na mesma sala, mesma rede, uma TV/tablet no centro.
- **Precisa de:** setup instantâneo, regras aplicadas automaticamente,
  zero instalação para os convidados.
- **Rejeita:** criar conta para cada convidado; tutorial longo.
- **Requisito que ela impõe:** entrar numa partida deve ser possível com
  um código curto e sem cadastro (convidado efêmero).

## 41.2 Bruno --- o jogador remoto

Joga com amigos de outra cidade, à noite, pelo notebook ou celular.

- **Contexto:** rede instável, sessões de 20--40 minutos.
- **Precisa de:** reconexão sem perder a partida, informação privada
  protegida, latência aceitável.
- **Rejeita:** perder a partida porque o Wi-Fi caiu por 30 segundos.
- **Requisito que ele impõe:** reconexão com reconstrução completa do
  estado visível (seção 28) e sessão sobrevivendo à queda do cliente.

## 41.3 Ivan --- o desenvolvedor da plataforma

Implementa o Core e os jogos. Trabalha com TDD e arquitetura hexagonal.

- **Contexto:** desenvolvedor único ou equipe muito pequena.
- **Precisa de:** rodar uma partida inteira sem abrir o Unity; refatorar
  o Core com segurança quando o segundo e o terceiro jogo revelarem as
  abstrações corretas.
- **Rejeita:** regras acopladas a HTTP, a banco ou a engine gráfica.
- **Requisito que ele impõe:** Core puro, testável sem infraestrutura, e
  um cliente CLI (seção 8).

## 41.4 Carla --- a autora de jogos (futuro)

Quer publicar um jogo próprio na plataforma. Não faz parte do MVP, mas a
existência dela é o que justifica a regra "criar um novo jogo não deve
exigir modificar o Core" (seção 6).

- **Requisito que ela impõe:** a SPI de jogo precisa ser um contrato
  estável, documentado e versionado.

------------------------------------------------------------------------

# 42. Requisitos funcionais

Identificadores `RF-nn`. Prioridade: **MVP**, **v1**, **futuro**.

## 42.1 Conta e identidade

| ID | Requisito | Prioridade |
|----|-----------|------------|
| RF-01 | Usuário pode criar conta com e-mail e senha | MVP |
| RF-02 | Usuário pode autenticar e receber um token de sessão | MVP |
| RF-03 | Convidado pode entrar numa partida sem cadastro, via código de sala, recebendo identidade efêmera | MVP |
| RF-04 | Convidado pode converter identidade efêmera em conta permanente | v1 |
| RF-05 | Login federado (Google/Apple) | futuro |

## 42.2 Lobby e sala

| ID | Requisito | Prioridade |
|----|-----------|------------|
| RF-10 | Usuário pode criar uma sala escolhendo o jogo e o número de jogadores | MVP |
| RF-11 | Sala expõe um código curto de convite | MVP |
| RF-12 | Jogador pode entrar e sair da sala antes do início | MVP |
| RF-13 | Jogador pode marcar-se como pronto | MVP |
| RF-14 | Anfitrião pode iniciar a partida quando o mínimo de jogadores estiver pronto | MVP |
| RF-15 | Sala expõe a lista de participantes em tempo real | MVP |
| RF-16 | Matchmaking público | futuro |

## 42.3 Partida

| ID | Requisito | Prioridade |
|----|-----------|------------|
| RF-20 | Sistema cria o estado inicial da partida a partir da definição do jogo e da configuração escolhida | MVP |
| RF-21 | Jogador pode consultar o estado da partida filtrado pela sua visibilidade | MVP |
| RF-22 | Jogador pode enviar uma ação (intenção) | MVP |
| RF-23 | Servidor valida a ação contra as regras e o turno antes de aplicá-la | MVP |
| RF-24 | Servidor resolve toda a aleatoriedade; o cliente nunca envia resultados | MVP |
| RF-25 | Servidor emite eventos descrevendo o que aconteceu | MVP |
| RF-26 | Sistema informa ao jogador quais ações estão disponíveis no momento | MVP |
| RF-27 | Sistema detecta e registra a condição de fim de partida e o vencedor | MVP |
| RF-28 | Jogador pode reconectar e reconstruir a apresentação a partir do estado autoritativo | MVP |
| RF-29 | Sistema aplica timeout de turno configurável, com ação padrão ou passagem de vez | v1 |
| RF-30 | Jogador pode abandonar a partida; o sistema decide entre substituir por bot, pausar ou encerrar | v1 |
| RF-31 | Sistema persiste o histórico da partida para replay | v1 |
| RF-32 | Espectador pode assistir a uma partida vendo apenas o estado público | futuro |

## 42.4 Jogos

| ID | Requisito | Prioridade |
|----|-----------|------------|
| RF-40 | Ludo jogável de 2 a 4 jogadores | MVP |
| RF-41 | Ludo com tabuleiro gerado por configuração, suportando 2--6 jogadores | v1 |
| RF-42 | Clone de Love Letter jogável, com mão privada e efeitos de carta | MVP |
| RF-43 | Clone de UNO jogável, com efeitos e mudança de cor | MVP |
| RF-44 | Catálogo de jogos consultável via API | MVP |
| RF-45 | Novo jogo pode ser adicionado sem alterar o Core | MVP |

## 42.5 Clientes

| ID | Requisito | Prioridade |
|----|-----------|------------|
| RF-50 | Cliente CLI capaz de jogar uma partida inteira | MVP |
| RF-51 | Cliente web capaz de jogar uma partida inteira | MVP |
| RF-52 | Cliente Unity capaz de jogar uma partida inteira | v1 |
| RF-53 | Modo mesa híbrida: tela central pública + celulares com informação privada | v1 |
| RF-54 | Bot capaz de jogar usando exatamente o mesmo contrato de um humano | v1 |
| RF-55 | Clientes AR/VR | futuro |

------------------------------------------------------------------------

# 43. Requisitos não-funcionais

Identificadores `RNF-nn`. Cada um traz uma métrica verificável --- um RNF
sem número é uma opinião.

## 43.1 Desempenho

| ID | Requisito | Meta |
|----|-----------|------|
| RNF-01 | Latência de resolução de uma ação no servidor (p95), excluindo rede | < 50 ms |
| RNF-02 | Latência ponta a ponta percebida (ação enviada → evento recebido), p95, na mesma região | < 250 ms |
| RNF-03 | Partidas simultâneas suportadas por uma instância de 1 vCPU / 1 GB | ≥ 200 |
| RNF-04 | Conexões WebSocket simultâneas por instância | ≥ 1000 |

Justificativa: jogo por turnos tem carga baixíssima --- alguns eventos por
minuto por partida. As metas acima são propositalmente modestas; o gargalo
deste sistema é a corretude das regras, não a vazão.

## 43.2 Qualidade e testabilidade

| ID | Requisito | Meta |
|----|-----------|------|
| RNF-10 | Cobertura de testes global (linhas e branches) | ≥ 85% |
| RNF-11 | Cobertura do módulo `core` e dos módulos de jogo | ≥ 95% |
| RNF-12 | Regras de jogo testáveis sem HTTP, sem banco e sem engine gráfica | obrigatório |
| RNF-13 | Fronteiras da arquitetura hexagonal verificadas automaticamente no build | obrigatório |
| RNF-14 | Toda aleatoriedade injetada por porta, permitindo teste determinístico por semente | obrigatório |
| RNF-15 | Suíte de testes unitários do Core | < 30 s |
| RNF-16 | Build falha se qualquer meta de cobertura ou de arquitetura for violada | obrigatório |

## 43.3 Confiabilidade

| ID | Requisito | Meta |
|----|-----------|------|
| RNF-20 | Partida sobrevive à queda de qualquer cliente | obrigatório |
| RNF-21 | Partida sobrevive ao reinício do servidor sem perda de estado | obrigatório |
| RNF-22 | Aplicação de ação é idempotente por identificador de ação | obrigatório |
| RNF-23 | Estado da partida é reconstruível a partir do log persistido | obrigatório |

RNF-22 merece destaque: sem idempotência, um cliente que reenvia após
timeout de rede pode rolar o dado duas vezes. Toda ação carrega um
identificador gerado pelo cliente, e o servidor descarta repetições.

## 43.4 Segurança

| ID | Requisito | Meta |
|----|-----------|------|
| RNF-30 | Nenhuma informação privada de um jogador trafega para outro | obrigatório |
| RNF-31 | Nenhuma decisão de regra depende de dado enviado pelo cliente | obrigatório |
| RNF-32 | Aleatoriedade gerada por fonte criptograficamente segura | obrigatório |
| RNF-33 | Senhas armazenadas com hash adaptativo (Argon2id ou bcrypt) | obrigatório |
| RNF-34 | Transporte sempre sobre TLS | obrigatório |

RNF-30 precisa de teste automatizado explícito: para cada jogo, um teste
que serializa a visão de cada jogador e assegura a ausência dos campos
privados dos demais. É a garantia mais fácil de quebrar numa refatoração.

## 43.5 Operação e custo

| ID | Requisito | Meta |
|----|-----------|------|
| RNF-40 | MVP roda em uma única instância + um PostgreSQL gerenciado | obrigatório |
| RNF-41 | Custo mensal de infraestrutura do MVP | < US$ 40 |
| RNF-42 | Subida do ambiente local completo | um comando |
| RNF-43 | Dependências de runtime além de aplicação e PostgreSQL | zero |

## 43.6 Evolutividade

| ID | Requisito | Meta |
|----|-----------|------|
| RNF-50 | Adicionar um jogo não exige alteração no módulo `core` | obrigatório |
| RNF-51 | Trocar o transporte (HTTP/WS) não exige alteração no `core` nem nos jogos | obrigatório |
| RNF-52 | Trocar o banco não exige alteração no `core` nem nos jogos | obrigatório |
| RNF-53 | Trocar o cliente gráfico não exige alteração no servidor | obrigatório |

RNF-50 é o critério que valida a arquitetura inteira. Se implementar UNO
obrigar a mexer no Core, a abstração está errada (seção 6).

------------------------------------------------------------------------

# 44. Arquitetura hexagonal aplicada

A seção 26 já estabelece a separação desejada. Esta seção a traduz em
ports & adapters concretos. O detalhamento de implementação vive em
[`docs/ARCHITECTURE.md`](ARCHITECTURE.md); aqui ficam apenas as
fronteiras que são requisito de produto.

## 44.1 O hexágono

``` text
        ADAPTADORES PRIMÁRIOS            ADAPTADORES SECUNDÁRIOS
        (quem aciona o sistema)          (o que o sistema aciona)

   REST ──┐                                      ┌── PostgreSQL
   WS   ──┤                                      ├── Publicador de eventos
   CLI  ──┼──►  PORTAS DE ENTRADA          PORTAS DE SAÍDA  ──┼── Fonte de aleatoriedade
   Bot  ──┘         │                            │            └── Relógio
                    ▼                            ▲
              ┌──────────────────────────────────────┐
              │            APPLICATION               │
              │  orquestra casos de uso, transação   │
              ├──────────────────────────────────────┤
              │              DOMAIN                  │
              │  GameState, Action, Event, Turn,     │
              │  Player, regras genéricas            │
              └──────────────────────────────────────┘
                              ▲
                              │ SPI de jogo
                    ┌─────────┼─────────┐
                  Ludo   Love Letter   UNO
```

## 44.2 Portas de entrada (driving)

Um caso de uso por porta, com nomes de negócio:

- `CriarPartida`
- `EntrarNaPartida`
- `IniciarPartida`
- `ConsultarEstado` --- devolve a **visão do jogador**, nunca o estado bruto
- `ListarAcoesDisponiveis`
- `SubmeterAcao`
- `Reconectar`

Os adaptadores REST, WebSocket e CLI são três tradutores diferentes para
as mesmas portas. Se um deles precisar de uma porta exclusiva, é sinal de
que lógica vazou para o adaptador.

## 44.3 Portas de saída (driven)

- `RepositorioDePartidas` --- carrega e persiste
- `PublicadorDeEventos` --- entrega eventos, **por destinatário**, para que o
  transporte não possa difundir o que não deve
- `FabricaDeAleatoriedade` --- sorteia a semente da partida uma única vez e
  deriva uma fonte determinística por ação
- `FonteDeAleatoriedade` --- **porta**, nunca chamada direta ao RNG da
  linguagem; é o que torna RNF-14 possível
- `Relogio` --- idem, para timeouts determinísticos em teste
- `GeradorDeIdentidade` --- idem, para que `UUID.randomUUID()` não entre no
  domínio
- `CatalogoDeJogos` --- resolve o identificador do jogo na sua definição

Além das portas, o domínio ganhou um valor compartilhado: **`OrdemDeTurno`**.
Ele não estava previsto. Apareceu quando Ludo, Love Letter e UNO precisaram,
os três, do mesmo laço de "avançar a vez pulando quem saiu da mesa". O que
subiu para o core foi a mecânica de girar a roda; as regras continuam nos
jogos --- Ludo não avança quando tira 6, UNO inverte o sentido, Love Letter
elimina. Ver [ADR-0008](adr/0008-adiar-o-bootstrap-spring.md).

## 44.4 A SPI de jogo

A SPI é a fronteira que sustenta RNF-50. Um jogo implementa:

``` text
DefinicaoDeJogo
 ├── identificador()
 ├── configuracaoSuportada()      → nº de jogadores, variantes
 ├── estadoInicial(config, rng)
 ├── acoesDisponiveis(estado, jogador)
 ├── validar(estado, acao)        → resultado explícito, não exceção
 ├── aplicar(estado, acao, rng)   → novo estado + eventos
 ├── visaoDe(estado, jogador)     → projeção pública + privada do jogador
 ├── eventoVisivelPara(estado, evento, jogador)  → evento ou nada
 └── resultado(estado)            → em andamento | encerrado(vencedor)
```

Quatro decisões embutidas aí, cada uma com razão:

1. **`aplicar` devolve um novo estado** em vez de mutar. Estado imutável
   torna snapshot, replay e teste triviais, e elimina uma classe inteira
   de bugs de aliasing. Custo: mais alocação --- irrelevante nesta escala.
2. **`rng` é parâmetro**, não campo. O jogo não escolhe sua fonte de
   aleatoriedade; ela é injetada. Torna toda partida reproduzível por
   semente.
3. **`validar` devolve resultado, não lança exceção.** Ação inválida é
   fluxo esperado, não excepcional --- o cliente pode ser hostil (seção 17).
4. **`visaoDe` pertence ao jogo, não ao Core.** Só o jogo sabe o que é
   segredo nele. Colocar isso no Core obrigaria o Core a conhecer regras
   específicas, violando a seção 11.1.
5. **`eventoVisivelPara` existe porque `visaoDe` não basta.** `visaoDe`
   protege o estado; os eventos saem por outro caminho. Sem um filtro
   equivalente para eventos, algo como `CartaComprada(jogador, carta)` seria
   difundido para a mesa inteira e o RNF-30 vazaria pela porta dos fundos. O
   padrão trata todo evento como público; jogos de informação oculta
   sobrescrevem.

## 44.5 O que é proibido

- `core` depender de qualquer framework web, ORM ou biblioteca de rede.
- Módulo de jogo depender de `core.adapter`, de HTTP ou de banco.
- Adaptador conter regra de jogo.
- Cliente calcular resultado de regra.

Essas quatro proibições são verificadas por teste automatizado no build
(RNF-13), não por revisão manual.

------------------------------------------------------------------------

# 45. Decisões de arquitetura (ADRs)

A tabela da seção 39 registra as diretrizes. As decisões que exigiram
comparação de alternativas estão documentadas em
[`docs/adr/`](adr/README.md):

| ADR | Decisão |
|-----|---------|
| [0001](adr/0001-arquitetura-hexagonal.md) | Arquitetura hexagonal com módulos de jogo como plugins |
| [0002](adr/0002-linguagem-do-backend.md) | Java como linguagem do backend |
| [0003](adr/0003-engine-do-cliente.md) | Unity como cliente de produto, web como cliente intermediário |
| [0004](adr/0004-persistencia.md) | PostgreSQL com log de eventos e snapshots |
| [0005](adr/0005-transporte.md) | HTTP para comandos, WebSocket para eventos |
| [0006](adr/0006-autoridade-e-aleatoriedade.md) | Servidor autoritativo e RNG por porta |
| [0007](adr/0007-ferramenta-de-build.md) | Maven como ferramenta de build |
| [0008](adr/0008-adiar-o-bootstrap-spring.md) | Adiar o bootstrap Spring Boot ate o Game Core estar provado |
| [0009](adr/0009-nome-do-produto.md) | Mesa Franca como nome do produto |

------------------------------------------------------------------------

# 46. Estado da implementação

Atualizado em 01/09/2026. Esta seção descreve o que **existe**, não o que se
pretende; o resto do documento continua sendo intenção.

## 46.1 O que está pronto

O Game Core está provado pelos três jogos previstos na seção 5. Cada um joga
do primeiro lance ao vencedor dentro de um teste, guiado pelo próprio motor.

| Item | Situação |
|------|----------|
| Game Core: domínio, portas, SPI, casos de uso | pronto |
| Ludo | jogável do início ao fim |
| Love Letter | jogável do início ao fim |
| UNO | jogável do início ao fim |
| Adaptadores em memória (acaso, relógio, identidade, repositório, catálogo) | prontos |
| Portões de cobertura e de arquitetura | ativos, build verde |

Os critérios técnicos da seção 37 já atendidos: **1, 3, 4, 7, 8, 9 e 10**. Os
demais (2, 5, 6) dependem de API e de cliente, que ainda não existem.

## 46.2 O que os jogos provaram

O que a seção 6 pedia — que o Core surgisse dos jogos — aconteceu, e em três
pontos concretos, todos registrados na
[ADR-0008](adr/0008-adiar-o-bootstrap-spring.md):

1. `OrdemDeTurno` nasceu no core porque os três precisavam da mesma mecânica.
2. `eventoVisivelPara` devolve um evento, e não um booleano, para que o Love
   Letter possa **redigir** em vez de esconder.
3. A validação de turno não pôde ficar numa guarda única: `DeclararUno` e
   `AcusarUno`, do UNO, valem fora do turno.

Nenhuma dessas mudanças fez o Core conhecer um jogo específico. **O RNF-50
sobreviveu ao terceiro jogo** — que era a pergunta que a seção 38 mandava
responder.

## 46.3 O que não existe

Não há aplicação executável, API, persistência real nem cliente. O
armazenamento é volátil e **não atende o RNF-21**. Só se joga por teste.

Próximas fronteiras em
[docs/ARCHITECTURE.md](ARCHITECTURE.md#próximas-fronteiras).

------------------------------------------------------------------------

# 47. Questões em aberto

Estas perguntas não bloqueiam o início da implementação do Core, mas
bloqueiam decisões posteriores. Devem ser respondidas antes das fases
indicadas.

> **Q1, Q3, Q4 e Q6 têm respostas propostas em nível de hipótese no
> [Business Model Canvas](BUSINESS-CANVAS.md) (v1.0, 06/09/2026).** São
> hipóteses a testar, não decisões: enquanto não forem validadas com
> usuários reais, este PRD continua sendo a autoridade sobre o produto.

| # | Questão | Bloqueia |
|---|---------|----------|
| Q1 | Há intenção de monetizar? Assinatura, compra por jogo, gratuito? | Modelo de conta, catálogo, escopo de v1 |
| Q2 | Qual a escala realista do primeiro ano --- dezenas ou milhares de partidas simultâneas? | Revisão de RNF-03/04 e da decisão de instância única |
| Q3 | Prioridade entre mesa presencial (Ana) e jogo remoto (Bruno)? | Ordem de RF-51 vs RF-53 |
| Q4 | Os clones de Love Letter e UNO serão distribuídos publicamente? | Risco de propriedade intelectual; pode exigir tema próprio |
| Q5 | Plataforma alvo prioritária do cliente Unity: desktop, mobile ou TV? | Configuração do projeto Unity |
| Q6 | Haverá conteúdo de vídeo/devlog acompanhando o desenvolvimento? | Instrumentação para replay e captura |

Q4 merece atenção: reimplementar as regras de Love Letter e de UNO com
arte e nome próprios é legalmente muito mais seguro do que publicar
clones nomeados. Regras de jogo não são protegidas por direito autoral,
mas nome, arte e texto de cartas são, e "UNO" é marca registrada. Para
uso privado e validação arquitetural não há problema; para distribuição
pública, renomear é o caminho barato.
