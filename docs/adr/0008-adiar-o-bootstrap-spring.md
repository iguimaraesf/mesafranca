# ADR-0008 --- Adiar o bootstrap Spring Boot até o Game Core estar provado

**Status:** Aceita
**Data:** 2026-09-01

## Contexto

A entrega anterior criou quatro módulos, incluindo um `bootstrap` com Spring
Boot, um adaptador REST e a fiação das portas. Nenhum jogo existia ainda.

Duas coisas ficaram claras depois:

1. **O framework não estava provando nada.** O `bootstrap` só demonstrava que
   o Spring consegue instanciar sete beans. O risco real do projeto — a SPI de
   jogo estar errada — continuava intocado, porque não havia jogo algum para
   pressioná-la.
2. **O framework era o maior risco do build.** Das versões fixadas sem
   possibilidade de resolução, Spring Boot 4.0.0 era a de encaixe mais
   incerto, e arrastava o maior grafo de dependências. Ele estava entre o
   trabalho e a possibilidade de compilar qualquer coisa.

O PRD §6 já dizia o que fazer: *o Core deve surgir dos jogos*. A ordem tinha
sido invertida.

## Alternativas consideradas

### A. Manter o bootstrap e acrescentar os jogos ao lado

**Prós:** nada se perde; a aplicação sobe desde já.
**Contras:** mantém no build a dependência mais pesada e mais incerta para
sustentar código que não valida nenhuma hipótese. Cada `mvn verify` paga o
preço de baixar e subir um contexto Spring para testar regras de jogo que não
precisam dele.

### B. Remover o bootstrap e os adaptadores de entrada; manter o resto

**Prós:** o build inteiro passa a ter três dependências, todas de teste
(JUnit, AssertJ, ArchUnit). O `core` e os jogos são exercitados sem
framework, que é justamente a propriedade que a ADR-0001 promete. O ciclo de
teste fica em segundos.
**Contras:** não há aplicação executável ao fim desta entrega; a API HTTP e o
WebSocket ficam para depois; a fiação terá de ser reescrita.

### C. Substituir Spring por um contêiner mais leve agora

**Contras:** trocar de framework é uma decisão que merece dados de uso, e não
existe uso. Decidir agora seria adivinhar. A ADR-0002 já escolheu Spring Boot
com argumentos que continuam válidos; nada aqui os contradiz.

## Decisão

**Opção B.** O módulo `bootstrap` sai do build, junto com o adaptador REST.
Entram três módulos de jogo — `ludo`, `love-letter`, `uno` — e um módulo
`arquitetura`, sem código de produção, que hospeda os testes de fronteira que
antes viviam no `bootstrap`.

O que fica:

``` text
api/
├── core          domínio, portas, SPI, casos de uso   (zero dependências)
├── games/        ludo, love-letter, uno
├── adapters      memória, aleatoriedade, relógio, identidade, catálogo
└── arquitetura   só testes ArchUnit sobre o repositório inteiro
```

Esta ADR **não substitui** a ADR-0002 nem a ADR-0005: Spring Boot continua
sendo a escolha, HTTP e WebSocket continuam sendo o transporte. O que muda é
apenas *quando*.

O `bootstrap` volta quando houver o que servir: um jogo completo, uma API que
alguém consiga chamar e um adaptador de persistência real. Trazê-lo antes
disso seria construir a fachada antes da casa.

## Consequências

### Positivas

- O build tem três dependências, todas de escopo de teste.
- Regras de jogo rodam sem contexto de framework, o que torna o ciclo de TDD
  quase instantâneo.
- O maior risco de resolução de dependências saiu do caminho.
- Os três jogos passaram a existir e, com eles, a primeira evidência real de
  que a SPI aguenta — ver a seção de consequências arquiteturais abaixo.

### Negativas aceitas

- Não há aplicação executável nesta entrega. Só é possível jogar por teste.
- A fiação do `ConfiguracaoDePortas` terá de ser reescrita quando o
  `bootstrap` voltar.
- O `api/bootstrap` continua no disco até um `git rm -r api/bootstrap`; ele
  não é construído, mas está lá.
- O adaptador REST está excluído da compilação por configuração no
  `adapters/pom.xml`, o que é feio e some junto com o `git rm`.

### Consequências arquiteturais descobertas pelos jogos

Escrever os três jogos mudou o core em três pontos, exatamente como o PRD §6
previa:

1. **Nasceu `OrdemDeTurno` no core.** Ludo, Love Letter e UNO precisavam do
   mesmo laço de "avançar a vez pulando quem saiu". A mecânica subiu para o
   domínio; as regras (Ludo repete no 6, UNO inverte, Love Letter elimina)
   ficaram nos jogos.
2. **`eventoVisivelPara` provou-se necessário, e no formato certo.** Devolver
   `Optional<Evento>` — e não um booleano — é o que permite ao Love Letter
   *redigir* um evento em vez de escondê-lo. Esconder daria pelo silêncio a
   informação que a redação protege.
3. **A validação de turno não podia ser uma guarda única.** `DeclararUno` e
   `AcusarUno` valem fora do turno. No Ludo e no Love Letter, uma checagem no
   topo de `validar` bastava; no UNO ela teve de descer para dentro de cada
   caso. Se a SPI tivesse embutido "toda ação é do jogador da vez", o UNO não
   caberia.

Nenhuma dessas três mudanças exigiu que o core conhecesse um jogo específico.
O RNF-50 sobreviveu ao terceiro jogo.

## Quando revisitar

1. Quando existir um jogo pronto para ser servido por HTTP — trazer o
   `bootstrap` de volta, com a ADR-0002 e a ADR-0005 valendo como estavam.
2. Se o cliente CLI (PRD §8) exigir empacotamento executável antes disso —
   avaliar um `main` sem framework no próprio `adapters`.
