# Arquitetura

Detalhamento de implementação da arquitetura hexagonal decidida na
[ADR-0001](adr/0001-arquitetura-hexagonal.md). As fronteiras que são
requisito de produto estão no [PRD §44](PRD.md#44-arquitetura-hexagonal-aplicada).

O código do backend vive em `api/`. Build com Maven
([ADR-0007](adr/0007-ferramenta-de-build.md)), Java 25
([ADR-0002](adr/0002-linguagem-do-backend.md)).

> **Estado atual.** `mvn verify` passa de ponta a ponta, com os oito testes de
> fronteira do ArchUnit ativos. Não há aplicação executável: o Spring Boot está
> fora do escopo até o Game Core estar provado
> ([ADR-0008](adr/0008-adiar-o-bootstrap-spring.md)). O build inteiro tem três
> dependências, todas de teste. Joga-se por teste.

## Regra única

**A dependência sempre aponta para dentro.** `core` não conhece ninguém.
Tudo conhece `core`.

``` text
arquitetura  →  adapters  →  core  ←  games
                                       ├── ludo
                                       ├── love-letter
                                       └── uno
```

`games` depende de `core` (implementa a SPI). `core` **não** depende de
`games` — ele os recebe pelo `CatalogoDeJogos`. Nenhum jogo conhece outro.

## Módulos Maven

| Módulo | Artefato | Contém | Depende de |
|--------|----------|--------|------------|
| `api/core` | `mesa-core` | domínio, portas, SPI, casos de uso | nada (só JDK) |
| `api/games/ludo` | `mesa-ludo` | regras do Ludo | `core` |
| `api/games/love-letter` | `mesa-love-letter` | regras do Love Letter | `core` |
| `api/games/uno` | `mesa-uno` | regras do UNO | `core` |
| `api/adapters` | `mesa-adapters` | adaptadores de saída em memória | `core` |
| `api/arquitetura` | `mesa-arquitetura` | só testes de fronteira | todos (teste) |

`core` não declara **nenhuma** dependência de escopo `compile`. Isso não é
convenção: está no `pom.xml` e é verificado por teste de arquitetura.

`arquitetura` não tem código de produção. Existe porque as regras ArchUnit
precisam de um módulo que enxergue `core`, `games` e `adapters` ao mesmo
tempo — papel que era do `bootstrap` antes da ADR-0008.

Duas particularidades dele são intencionais. O `default-jar` está desligado e
`maven.install.skip` está ligado, porque um módulo sem `src/main/java`
produziria um jar vazio e um aviso em todo build. E o JaCoCo pula relatório e
portão ali por falta de `target/classes` — não há o que cobrir.

Um detalhe que já custou um diagnóstico: neste módulo os outros chegam como
**jars de dependência**, não como diretórios de classes. Por isso o
`ClassFileImporter` do `ArquiteturaTest` **não** usa `DO_NOT_INCLUDE_JARS` —
com ele, o importador viria vazio e as oito regras reprovariam por "nenhuma
classe verificada", que parece rigor e é o contrário. Em
`ArquiteturaDoCoreTest`, dentro do próprio `core`, o problema não existe.

Acrescentar um jogo é acrescentar um módulo em `api/games/`, registrá-lo no
agregador e criar um arquivo de serviço. Nenhuma linha do `core` muda
(RNF-50) — e os três jogos atuais são a prova disso.

## Pacotes

Raiz: `br.com.mesafranca`. O agregador do backend é `mesa-api`.

``` text
core/
├── domain/
│   ├── IdPartida, IdJogador, IdAcao, IdentificadorDeJogo, Assento
│   ├── Jogador, TipoDeJogador, FaseDaPartida, Partida
│   ├── ConfiguracaoDePartida, ConfiguracaoSuportada
│   ├── OrdemDeTurno                           → quem joga agora e quem vem depois
│   ├── Acao, Evento, EstadoDeJogo, Visao      → contratos abertos, do jogo
│   ├── Resultado, ResultadoDeValidacao        → selados, do core
│   └── Transicao, EventoRegistrado
├── erro/         RegraDePartidaViolada, PartidaNaoEncontrada,
│                 JogoDesconhecido, ConflitoDeVersao
├── spi/          DefinicaoDeJogo
├── port/
│   ├── in/       CriarPartida, EntrarNaPartida, IniciarPartida, SubmeterAcao,
│   │             ConsultarEstado, ListarAcoesDisponiveis, Reconectar
│   └── out/      RepositorioDePartidas, PublicadorDeEventos, CatalogoDeJogos,
│                 FabricaDeAleatoriedade, FonteDeAleatoriedade,
│                 Relogio, GeradorDeIdentidade
└── application/  um Service por porta de entrada

games/<jogo>/
├── <Jogo>Definicao   implements DefinicaoDeJogo
├── <Jogo>Estado      record imutável
├── <Jogo>Acao        sealed interface + records aninhados
├── <Jogo>Evento      sealed interface + records aninhados
└── <Jogo>Visao       record: a projeção que sai para o cliente

adapters/out/
├── aleatoriedade/  AleatoriedadeComSemente, FabricaDeAleatoriedadeSegura
├── relogio/        RelogioDoSistema
├── memoria/        repositório e publicador voláteis, gerador de identidade
└── catalogo/       CatalogoDeJogosPorServiceLoader
```

## `sealed` mora no jogo, não no core

`Resultado` e `ResultadoDeValidacao` são `sealed`: o core conhece todos os
casos possíveis, e o compilador pode exigir exaustividade.

`Acao` e `Evento` **não** são `sealed`, e isso é deliberado. Selá-las
obrigaria o core a listar em `permits` todas as ações de todos os jogos — ou
seja, o core conheceria Ludo, Love Letter e UNO, exatamente o que o PRD
§11.1 proíbe e o RNF-50 impede.

A exaustividade acontece um nível abaixo: **cada jogo sela a sua própria
hierarquia**, e o `switch` sem `default` sobre ela é verificado pelo
compilador.

``` java
public sealed interface UnoAcao extends Acao
        permits JogarCarta, Comprar, PassarVez, DeclararUno, AcusarUno {}

return switch (uno) {
    case UnoAcao.JogarCarta jogada  -> validarJogada(atual, jogada);
    case UnoAcao.Comprar comprar    -> ...;
    case UnoAcao.PassarVez passar   -> validarPassagem(atual, passar.autor());
    case UnoAcao.DeclararUno d      -> validarDeclaracao(atual, d.autor());
    case UnoAcao.AcusarUno acusar   -> validarAcusacao(atual, acusar);
};
```

Ausência de `default` é convenção do projeto: `default` desliga a verificação
de exaustividade, que é o motivo inteiro de a escolha ter recaído sobre Java.

## A SPI de jogo

``` java
IdentificadorDeJogo identificador();
ConfiguracaoSuportada configuracaoSuportada();
EstadoDeJogo estadoInicial(ConfiguracaoDePartida, List<Jogador>, FonteDeAleatoriedade);
List<Acao> acoesDisponiveis(EstadoDeJogo, IdJogador);
ResultadoDeValidacao validar(EstadoDeJogo, Acao);
Transicao aplicar(EstadoDeJogo, Acao, FonteDeAleatoriedade);
Visao visaoDe(EstadoDeJogo, IdJogador);
Resultado resultado(EstadoDeJogo);
Optional<Evento> eventoVisivelPara(EstadoDeJogo, Evento, IdJogador);  // padrão: público
```

Implementações da SPI devem ser **sem estado**: uma única instância atende
todas as partidas simultâneas.

### O que os três jogos ensinaram sobre a SPI

O PRD §6 previa que o Core surgisse dos jogos. Três coisas mudaram por causa
deles, e vale registrar por quê:

**1. `eventoVisivelPara` devolve `Optional<Evento>`, e não um booleano.**
`visaoDe` protege o *estado*; os eventos saem por outro caminho. Um booleano
permitiria apenas esconder o evento — e esconder entrega pelo silêncio o que
se quer proteger. Devolvendo um evento, o Love Letter *redige*: a mesa vê que
Ana espiou a mão de Bruno; só o conteúdo some.

``` java
case MaoEspiada espiada -> espiada.espiao().equals(jogador)
        ? espiada
        : new MaoEspiada(espiada.espiao(), espiada.alvo(), Optional.empty());
```

**2. A validação de turno não pode morar na SPI.** No Ludo e no Love Letter,
"é a vez dele?" é a primeira linha de `validar`. No UNO não: `DeclararUno` e
`AcusarUno` valem fora do turno. Se a SPI tivesse embutido a checagem, o UNO
não caberia. A checagem desceu para dentro de cada caso do `switch`.

**3. `OrdemDeTurno` subiu para o core.** Ver a seção seguinte.

## `OrdemDeTurno`: mecânica no core, regra no jogo

Os três jogos precisavam do mesmo laço — avançar a vez pulando quem saiu, com
possibilidade de inverter o sentido. Manter isso em cada jogo seria a terceira
cópia do mesmo `floorMod`.

O que **subiu** para o core é só a mecânica de girar a roda: `proxima()`,
`avancando(n)`, `invertida()`, `semJogador(j)`, `ativos()`.

O que **ficou** no jogo é quando girar:

| Jogo | Uso |
|------|-----|
| Ludo | não gira quando tira 6, captura ou chega uma peça |
| UNO | `invertida()` no Inverter, `avancando(2)` no Pular e no +2 |
| Love Letter | `semJogador()` a cada eliminação; `proxima()` pula os eliminados |

O core não sabe o que é um 6, um Pular ou uma Princesa. Essa é a linha.

Um efeito colateral agradável: com dois jogadores, `invertida().proxima()`
devolve a vez a quem jogou. O UNO ainda precisa da regra explícita de que o
Inverter vale como Pular em mesa de dois, mas a mecânica não atrapalha.

## O ciclo de uma ação

``` text
adaptador de entrada
   │ traduz DTO → Acao de domínio
   ▼
SubmeterAcao (porta de entrada)
   │ 1. reserva o actionId; se já existia → Repetida  (RNF-22)
   │ 2. carrega a partida
   │ 3. confere fase e participação
   ▼
DefinicaoDeJogo.validar(estado, acao)
   │ devolve ResultadoDeValidacao, não lança exceção
   ▼
DefinicaoDeJogo.aplicar(estado, acao, rng)
   │ devolve (novoEstado, eventos) — sem mutar nada
   ▼
numeração dos eventos a partir de ultimaSequencia
   ▼
persiste partida + eventos   (travamento otimista por versão)
   ▼
para cada jogador: eventoVisivelPara(...) → PublicadorDeEventos
   ▼
cada cliente recebe só o que pode ver (RNF-30)
```

Três detalhes que não são óbvios:

- **A reserva do `actionId` vem antes de tudo.** Se viesse depois da
  validação, um reenvio por timeout de rede rolaria o dado duas vezes.
- **A filtragem de visibilidade acontece na saída, uma vez por jogador.**
  Nunca enviar tudo e deixar o cliente esconder o que não lhe cabe.
- **Validação e aplicação são separadas** porque `ListarAcoesDisponiveis`
  precisa de validação sem efeito colateral.

## Aleatoriedade: semente por partida, fonte por ação

``` java
long novaSemente();                                        // uma vez por partida
FonteDeAleatoriedade derivada(long semente, long passo);   // determinística
```

Usar a semente da partida diretamente em toda ação faria o dado cair sempre
no mesmo valor. O `passo` — na prática, a versão da partida — muda a cada
ação. O resultado é reprodutível a partir de `(semente, sequência de ações)`,
que é o que torna um bug relatado em produção reproduzível em teste, e o que
sustenta o replay do RF-31.

A derivação usa o finalizador SplitMix64 para que sementes vizinhas não
produzam sequências correlacionadas.

## Estado imutável

`Partida`, `OrdemDeTurno` e os estados de jogo são `record`. `aplicar`
devolve um novo estado. Isso torna snapshot, replay e teste triviais e
elimina bugs de aliasing. O custo é alocação — irrelevante para poucos
eventos por minuto.

Para coleções, use `List.copyOf` / `Map.copyOf` / `Set.copyOf` no construtor
compacto. Um `record` com um `List` mutável dentro é um `record` mutável
disfarçado. Mapas de mapas precisam de cópia em profundidade — ver
`LudoEstado.congelar`.

## Carregamento de jogos

`CatalogoDeJogos` resolve um identificador (`"ludo"`) numa `DefinicaoDeJogo`.
A implementação usa `ServiceLoader`: cada módulo de jogo declara
`META-INF/services/br.com.mesafranca.core.spi.DefinicaoDeJogo` — repare que o
**nome do arquivo** é o do pacote, e por isso ele também muda quando o pacote
muda.

## Fronteiras verificadas no build

Testes ArchUnit reprovam o build se:

1. `core` referenciar `org.springframework`, `jakarta`, `com.fasterxml`,
   `java.net`, `java.sql` ou `javax.sql`.
2. `core` referenciar `games` ou `adapters`; ou um jogo referenciar
   `adapters` ou Spring.
3. Um jogo referenciar outro jogo.
4. `core` ou `games` referenciarem `Random`, `SecureRandom`, `Math.random`,
   `System.currentTimeMillis`, `System.nanoTime`, `Instant.now`,
   `LocalDate.now`, `LocalDateTime.now` ou `UUID.randomUUID`
   ([ADR-0006](adr/0006-autoridade-e-aleatoriedade.md)).
5. Qualquer classe em `core.port.out` não for interface.
6. Qualquer campo de instância em `core.domain` não for final.
7. Qualquer classe de `adapters` implementar `DefinicaoDeJogo` — regra de
   jogo não mora em adaptador.
8. A raiz de `*Acao` ou `*Evento` de um jogo não for interface. O selamento
   em si é garantido pelo compilador: sem `sealed`, o `switch` sem `default`
   da definição do jogo não compila.

Localização: `core/.../arquitetura/ArquiteturaDoCoreTest` (só o core, falha
cedo) e `arquitetura/.../ArquiteturaTest` (repositório inteiro).

## Contratos externos

- REST → OpenAPI
- WebSocket → AsyncAPI
- SDKs TypeScript e C# **gerados** a partir desses contratos, nunca
  escritos à mão ([ADR-0003](adr/0003-engine-do-cliente.md)).

Ambos ficam para quando o `bootstrap` voltar ([ADR-0008](adr/0008-adiar-o-bootstrap-spring.md)).

## Próximas fronteiras

Nada abaixo está começado. A ordem é sugestão, não compromisso.

| Fronteira | O que prova | Depende de |
|-----------|-------------|------------|
| Adaptador PostgreSQL | RNF-21: a partida sobrevive ao reinício | esquema da [ADR-0004](adr/0004-persistencia.md) |
| Cliente CLI | PRD §8: o Core roda sem UI | nada — dá para fazer hoje |
| Volta do `bootstrap` | a API existe e responde | [ADR-0008](adr/0008-adiar-o-bootstrap-spring.md), ADR-0005 |
| Adaptadores REST e WebSocket | critério 35.3: a API é contrato | `bootstrap` |
| Cliente web | a API é agnóstica de cliente | REST + WebSocket |

O CLI é o único que não depende de nada e o único que exercita o Core inteiro
sem framework — coerente com a ordem de construção do PRD.

## O que ainda não está decidido

- Formato de versionamento dos payloads de evento além do campo `versao`.
- Estratégia de substituição de jogador por bot em abandono (RF-30).
- Como compartilhar os dublês de teste entre módulos: hoje o `Sorteio` está
  duplicado nos três jogos. A saída é um `test-jar` do `core` ou um módulo
  `core-test-fixtures` ([ADR-0007](adr/0007-ferramenta-de-build.md)).
- Adaptadores de entrada (REST, WebSocket, CLI) e o adaptador PostgreSQL:
  hoje existem apenas as implementações em memória, que **não** atendem o
  RNF-21.
- O que fazer quando ninguém pode jogar e o monte secou, no UNO. Hoje a mesa
  passa a vez indefinidamente; o desempate por baralho vazio existe no Love
  Letter, mas não no UNO.
