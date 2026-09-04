# ADR-0001 --- Arquitetura hexagonal com jogos como plugins

**Status:** Aceita
**Data:** 2026-08-31

## Contexto

O PRD §26 exige que o transporte, o banco e o cliente possam ser trocados
sem reescrever regras, e o §6 estabelece que criar um novo jogo não deve
exigir alterar o Core. É preciso decidir **como** garantir isso, não apenas
declará-lo.

## Alternativas consideradas

### A. Camadas clássicas (controller → service → repository)

**Prós:** familiar, pouca cerimônia, suportado por padrão pelo Spring.
**Contras:** a dependência aponta para baixo, em direção à infraestrutura.
Na prática, entidades de persistência sobem para o serviço e anotações de
framework contaminam o domínio. Cumprir o RNF-52 vira força de vontade.

### B. Arquitetura hexagonal (ports & adapters)

**Prós:** a dependência aponta para dentro, sempre. O domínio não conhece
Spring, JDBC nem HTTP. Cada requisito de troca do PRD §26 vira uma
substituição de adaptador. Regras testáveis sem infraestrutura (RNF-12).
**Contras:** mais interfaces e mais mapeamento entre representações; para
CRUD simples, é peso morto.

### C. Event sourcing pleno como arquitetura central

**Prós:** replay e auditoria nativos; encaixa com a natureza de log de uma
partida.
**Contras:** complexidade alta (projeções, versionamento de eventos,
upcasting) para um projeto que ainda vai descobrir seu modelo. O PRD §3.5
pede explicitamente evitar complexidade prematura.

## Decisão

**Arquitetura hexagonal (B)**, com os jogos entrando por uma SPI e não por
herança de um Core que os conheça.

O contra da opção B --- excesso de cerimônia para CRUD --- não se aplica aqui:
este sistema é quase todo regra de domínio. É exatamente o caso em que
hexagonal se paga. O projeto tem, além disso, uma característica rara que
amplifica o benefício: o domínio (regras de jogo) é **puro** --- sem I/O, sem
tempo, sem rede --- desde que a aleatoriedade e o relógio sejam injetados
por porta. Domínio puro é domínio trivialmente testável.

O event sourcing pleno (C) é rejeitado como arquitetura central, mas o log
de eventos é adotado como **mecanismo de persistência** na ADR-0004 --- o que
captura o benefício de replay sem o custo conceitual.

### Estrutura de módulos

``` text
tabuleiro/
├── core/          domínio + portas + casos de uso   (zero framework)
├── games/         ludo, love-letter, uno            (dependem só de core)
├── adapters/      rest, websocket, cli, persistence, events, random
└── bootstrap/     Spring Boot, wiring, configuração
```

As dependências entre módulos são declaradas no build e verificadas por
teste (ArchUnit). `core` não declara dependência de framework algum: se
alguém tentar importar Spring dentro dele, o build quebra por duas vias ---
falta de dependência no módulo e falha de teste de arquitetura.

## Consequências

### Positivas

- RNF-12, RNF-13 e RNF-50 a RNF-53 passam a ser verificáveis, não
  aspiracionais.
- O Core roda em teste sem PostgreSQL, sem HTTP e sem Unity.
- Um jogo novo é um módulo novo; nenhuma linha do Core muda.

### Negativas aceitas

- Mapeamento explícito entre estado de domínio e representação persistida,
  e entre estado e DTO de API. É código repetitivo e é o preço.
- Mais arquivos e mais interfaces do que uma abordagem em camadas.
- Curva inicial maior para qualquer pessoa nova no projeto.

## Quando revisitar

1. Se, depois dos três jogos, ficar evidente que a SPI não comporta um
   quarto jogo sem alterar o Core --- o problema é a SPI, não a arquitetura;
   nova ADR redefinindo o contrato.
2. Se o custo de mapeamento passar a dominar o tempo de desenvolvimento
   --- avaliar reduzir camadas de tradução, não abandonar o hexágono.
