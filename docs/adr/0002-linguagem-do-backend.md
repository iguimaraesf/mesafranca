# ADR-0002 --- Java como linguagem do backend

**Status:** Aceita
**Data:** 2026-08-31

## Contexto

O backend concentra tudo que importa neste projeto: o Game Core, as regras
dos jogos, a autoridade sobre o estado e a projeção de informação privada.
O cliente é deliberadamente burro (PRD §3.3, §18). Errar a linguagem aqui é
caro; errar no cliente é barato.

O desenvolvedor domina Java, Python e Node/TypeScript. As três são viáveis.
A escolha precisa ser feita contra o risco dominante do projeto, não contra
uma lista genérica de qualidades.

**Qual é o risco dominante?** Não é vazão nem latência. Um jogo por turnos
gera poucos eventos por minuto por partida; qualquer uma das três
linguagens atende os RNF-01 a RNF-04 com folga. O risco dominante está
declarado no próprio PRD §6:

> O Core deve surgir dos jogos. [...] Se o terceiro jogo exigir mudanças
> generalizadas no Core, investigar se a abstração está errada.

Ou seja: o projeto assume que o Core será **refatorado repetidamente** à
medida que Ludo, Love Letter e UNO revelarem as abstrações corretas. O
critério de escolha, portanto, é **custo e segurança de refatoração
estrutural repetida em um domínio de modelagem rica**, seguido de
testabilidade (RNF-10 a RNF-16).

Um segundo fator, frequentemente citado e aqui deliberadamente descartado:
**não existe ecossistema relevante de "motor de regras de board game"** em
nenhuma das três linguagens. O candidato óbvio seria `boardgame.io` no
Node, mas ele está sem release há cerca de quatro anos e é classificado
como inativo. Além disso, ele impõe o próprio formato de estado e o próprio
servidor, o que colide frontalmente com o Core hexagonal e autoritativo
exigido pelo PRD. Esse critério, portanto, **não desempata nada** --- em
qualquer linguagem o motor será escrito do zero.

## Alternativas consideradas

### A. Java 25 LTS + Spring Boot

**Prós**

- **Tipos algébricos de verdade.** `sealed interface` + `record` +
  `switch` com pattern matching modelam `Action`, `Event` e as fases da
  partida quase exatamente. O compilador exige exaustividade: ao adicionar
  uma variante de ação, o build aponta **todos** os pontos que precisam
  tratá-la. Num motor de regras com dezenas de variantes, essa é a
  diferença entre refatorar com confiança e refatorar torcendo.
- **Imutabilidade barata.** `record` dá value objects imutáveis sem
  cerimônia, o que sustenta a decisão de `aplicar()` devolver novo estado
  (PRD §44.4) e viabiliza snapshot e replay.
- **Fronteiras arquiteturais verificáveis.** ArchUnit transforma "o Core
  não pode depender do adaptador" em teste que roda no build. É o
  cumprimento literal do RNF-13, e não existe equivalente de maturidade
  comparável em Python ou Node.
- **Cobertura como portão.** JaCoCo com `check` falha o build abaixo de
  85% (RNF-16) sem gambiarra.
- **Concorrência sem reatividade.** Virtual threads (estáveis desde o Java
  21) sustentam milhares de conexões WebSocket com código bloqueante
  simples. Evita-se a complexidade de programação reativa que
  historicamente era o preço da escala na JVM.
- **Bots com folga.** Busca tipo MCTS ou minimax (RF-54) é CPU-bound; a
  JVM entrega isso em ordem de grandeza melhor que CPython.
- **Módulos como plugins.** `ServiceLoader` e o sistema de módulos são
  mecanismos de primeira classe para carregar jogos como plugins
  (PRD §25), não convenções improvisadas.

**Contras**

- Mais linhas por funcionalidade que Python, mesmo com `record` e
  `sealed`. O ganho de segurança se paga na refatoração, não na primeira
  escrita.
- Pegada de memória: uma instância Spring Boot parte de ~250--400 MB. Contra
  o RNF-41 (< US$ 40/mês), isso significa uma VM de 1 GB em vez de 512 MB
  --- uma diferença de poucos dólares, não um impedimento.
- Startup de segundos. Irrelevante para um servidor de vida longa;
  relevante apenas se algum dia se cogitar serverless, o que o PRD já
  descarta.
- Nenhum compartilhamento de código com o cliente Unity, que é C#.

### B. Python 3.14 + FastAPI

**Prós**

- Menor cerimônia; protótipo de regra sai mais rápido.
- `pytest` é a melhor experiência de teste das três, e **Hypothesis**
  (property-based testing) é especialmente adequado a motores de regra ---
  invariantes como "o total de cartas se conserva" ou "nenhuma sequência de
  ações válidas leva a estado inconsistente" são testáveis por geração.
- Ecossistema de IA maduro, útil para os bots do RF-54.
- Menor consumo de recursos; hospedagem mais barata.
- O próprio PRD §8 usa Python no exemplo do cliente CLI.

**Contras**

- **Tipagem opcional e apagada em runtime.** Com `mypy --strict`,
  `match` e `assert_never` obtém-se verificação de exaustividade razoável
  --- mas ela depende de disciplina e de um passo externo que pode ser
  silenciado, não do compilador. Sob refatoração estrutural repetida, essa
  diferença aparece.
- Refatoração em larga escala é significativamente menos segura sem um
  compilador; renomear um campo de estado usado por três jogos é uma
  operação de busca textual.
- **GIL.** Para I/O de jogo por turnos, `asyncio` resolve. Para bots
  CPU-bound, não. O build free-threaded do 3.14 saiu do estado experimental,
  mas a compatibilidade do ecossistema ainda é parcial --- apostar nele hoje
  é assumir risco desnecessário.
- Não há equivalente maduro ao ArchUnit; RNF-13 vira convenção de import
  linter, mais frágil.

### C. Node + TypeScript

**Prós**

- **Uniões discriminadas com verificação de exaustividade via `never`** são
  tão expressivas quanto `sealed` do Java, e mais ergonômicas de escrever.
- Melhor ecossistema de WebSocket das três.
- **Uma só linguagem** para backend, cliente CLI (PRD §8) e cliente web
  (RF-51), com o contrato da API compartilhado como tipos.
- Menor pegada de memória e startup instantâneo; hospedagem mais barata.
- Vitest é rápido; cobertura por V8 é trivial de configurar.

**Contras**

- Tipos apagados em runtime: a fronteira precisa de validação em runtime
  (Zod ou equivalente) de qualquer forma. Em arquitetura hexagonal isso
  até é desejável, mas é trabalho adicional.
- **Loop de eventos único.** Um bot fazendo busca em profundidade trava o
  processo inteiro. `worker_threads` existem, mas são bem mais
  desconfortáveis que virtual threads para estado compartilhado.
- Maior churn de ecossistema e maior superfície de risco de cadeia de
  suprimentos --- relevante para um servidor autoritativo que guarda a
  integridade das partidas.
- A vantagem de "uma linguagem só" é parcial: o cliente Unity é C# de
  qualquer maneira, então a unificação completa nunca acontece.

## Decisão

**Java (LTS 25) com Spring Boot, Maven multi-módulo.**

O argumento decisivo é o alinhamento entre o risco dominante e a
ferramenta: o PRD planeja explicitamente descobrir o Core por refatoração
sucessiva ao longo de três jogos, e a combinação `sealed` + `record` +
pattern matching exaustivo é a que torna essa refatoração verificável pelo
compilador em vez de verificável por esperança. Somam-se a isso ArchUnit
(RNF-13) e JaCoCo (RNF-16), que transformam duas exigências obrigatórias do
projeto em portões automáticos de build em vez de acordos de cavalheiros.

Node/TypeScript foi a alternativa mais próxima, e perdeu por duas razões
concretas: o bloqueio do loop de eventos diante do requisito de bots
(RF-54), e o fato de sua principal vantagem --- linguagem única ponta a
ponta --- ser neutralizada por dois lados: o cliente de produto é C#
(ADR-0003), e o contrato da API será gerado a partir de OpenAPI/AsyncAPI,
o que já entrega tipos ao cliente web sem exigir que o servidor seja
JavaScript.

Python perdeu por ser a melhor escolha para uma pergunta diferente daquela
que este projeto faz. Se o objetivo fosse ter Ludo jogável em dois fins de
semana, Python venceria com folga. O objetivo declarado, porém, é um Core
genérico que sobreviva a três jogos sem conhecer nenhum deles --- um problema
de modelagem e refatoração, não de velocidade de primeira escrita.

**Duas ideias de Python e de Node são adotadas mesmo assim**, porque boas
ideias não pertencem a linguagens:

1. Property-based testing das invariantes de regra, via **jqwik** (o
   equivalente do Hypothesis na JVM). Está previsto em
   [`docs/TESTING.md`](../TESTING.md).
2. Contrato de API gerado (OpenAPI + AsyncAPI) como fonte única para os
   clientes TypeScript e C#, em vez de contratos escritos à mão.

**Stack concreta:**

| Item | Escolha |
|------|---------|
| Linguagem | Java 25 LTS |
| Framework | Spring Boot 3.5+ (Web MVC com virtual threads, não WebFlux) |
| Build | Maven multi-módulo |
| Acesso a dados | jOOQ ou JDBC puro no adaptador de persistência --- **não** JPA no Core |
| Testes | JUnit 5, AssertJ, jqwik, Testcontainers, ArchUnit |
| Cobertura | JaCoCo, portão em 85% global / 95% no Core |

Duas escolhas dentro da stack merecem justificativa:

- **MVC com virtual threads, não WebFlux.** Programação reativa custa
  legibilidade e dificulta depuração; virtual threads entregam a mesma
  escala com código sequencial. Nesta carga, WebFlux seria complexidade
  sem contrapartida.
- **Sem JPA no Core.** JPA arrasta entidades mutáveis e gerenciadas, o
  oposto do estado imutável decidido no PRD §44.4. O mapeamento
  estado ↔ tabela fica confinado ao adaptador de persistência.

## Consequências

### Positivas

- Adicionar uma variante de ação ou evento quebra o build nos lugares
  certos, em vez de falhar em produção.
- RNF-13 e RNF-16 viram portões automáticos.
- Bots CPU-bound cabem no mesmo processo sem arquitetura adicional.
- O desenvolvedor trabalha na linguagem em que tem mais profundidade, e
  C# (Unity) é próximo o bastante para reduzir o custo do cliente.

### Negativas aceitas

- Mais código para a mesma funcionalidade do que em Python.
- Instância de 1 GB em vez de 512 MB; alguns dólares a mais por mês.
- Cliente web precisará de uma camada de contrato gerada, em vez de
  importar tipos diretamente do servidor.
- Iteração inicial mais lenta nas primeiras semanas de Ludo.

## Quando revisitar

Revisitar esta decisão se qualquer uma destas condições ocorrer:

1. O projeto passar a ter um cliente web como produto principal e o custo
   de manter dois ecossistemas superar o ganho de tipagem --- reabrir o caso
   do TypeScript.
2. A geração automática de jogos por IA (PRD §34) sair da visão e entrar
   no roadmap --- reabrir o caso do Python.
3. O custo de hospedagem se tornar restrição real (RNF-41 violado de forma
   recorrente) --- avaliar GraalVM native-image antes de trocar de linguagem.
4. A suíte de testes do Core ultrapassar 30 s (RNF-15), indicando que o
   peso do framework vazou para o domínio.

## Fontes

- [State of Java 2026](https://devnewsletter.com/p/state-of-java-2026/)
- [Virtual Threads after JDK 24 --- InfoQ](https://www.infoq.com/articles/virtual-threads-after-jdk24/)
- [Python 3.14 free-threading --- Towards Data Science](https://towardsdatascience.com/python-3-14-and-the-end-of-the-gil/)
- [boardgame.io --- estado do projeto (issue #1150)](https://github.com/boardgameio/boardgame.io/issues/1150)
- [boardgame.io no npm](https://www.npmjs.com/package/boardgame.io)
