# CLAUDE.md

**Mesa Franca** — plataforma de jogos de tabuleiro virtual. Servidor
autoritativo com Game Core genérico; jogos entram como plugins; clientes só
apresentam. O nome está registrado na
[ADR-0009](docs/adr/0009-nome-do-produto.md).

## Leia antes de agir

| Pergunta | Arquivo |
|----------|---------|
| O que o produto é e exige | [docs/PRD.md](docs/PRD.md) |
| Por que cada decisão técnica | [docs/adr/](docs/adr/README.md) |
| Como o código se organiza | [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) |
| Como se testa | [docs/TESTING.md](docs/TESTING.md) |
| Nomes, commits, PR | [docs/CONVENTIONS.md](docs/CONVENTIONS.md) |

## Onde está o código

`api/` — backend Maven multi-módulo: `core`, `games/{ludo,love-letter,uno}`,
`adapters`, `arquitetura`. Pacote raiz `br.com.mesafranca`; artefatos Maven
com prefixo `mesa-` (`mesa-core`, `mesa-ludo`, ...).

Sem framework por enquanto: o `bootstrap` Spring Boot só volta quando houver
o que servir ([ADR-0008](docs/adr/0008-adiar-o-bootstrap-spring.md)).

``` bash
cd api && mvn verify      # compila, testa, cobertura e arquitetura
```

## Estado

Game Core provado pelos três jogos: Ludo, Love Letter e UNO jogam do início ao
vencedor em teste. `mvn verify` verde de ponta a ponta, com os portões de
cobertura e as oito regras de fronteira ativos.

Próximas fronteiras, nenhuma começada: cliente CLI (não depende de nada),
adaptador PostgreSQL, volta do `bootstrap`, adaptadores REST e WebSocket,
cliente web. Detalhe em [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md#próximas-fronteiras).

**O `mvn verify` é a autoridade.** Código validado por qualquer outro caminho
— compilador alternativo, dublê de biblioteca — está *não verificado* até
passar nele. Ver [docs/TESTING.md](docs/TESTING.md#o-javac-é-a-autoridade).

## Stack

Java 25 LTS · Maven multi-módulo · sem framework nesta fase · PostgreSQL,
Spring Boot 4, Unity e TypeScript/PixiJS decididos e adiados · sem Redis.

## Regras inegociáveis

1. **Dependência aponta para dentro.** `core` não conhece framework,
   banco, rede nem jogo específico.
2. **Servidor decide, cliente apresenta.** Nenhum resultado de regra vem
   do cliente.
3. **Acaso, tempo e identidade entram por porta.** Nunca `Random`,
   `Instant.now()` nem `UUID.randomUUID()` em `core` ou `games`.
4. **Estado é imutável.** `aplicar` devolve novo estado.
5. **Adicionar um jogo não altera o `core`.**
6. **`sealed` é do jogo, não do core.** `Acao` e `Evento` são abertas; cada
   jogo sela a sua hierarquia e usa `switch` exaustivo sem `default`.
7. **TDD.** Teste antes. Cobertura ≥ 85% global e ≥ 95% de linhas em `core`
   e `games`; o build falha abaixo disso.
   Cada jogo tem um teste que joga a partida inteira, do início ao vencedor.
8. **Visibilidade é do jogo, não do cliente.** Filtragem por jogador
   acontece no servidor, tanto no estado quanto nos eventos.

## Ordem de construção

CLI → cliente web → Unity. Cada etapa prova que a anterior não dependia da
seguinte.

## Ao trabalhar aqui

- Decisão arquitetural nova exige ADR em `docs/adr/`, seguindo o modelo
  do índice. ADR aceita não se edita: cria-se outra que a substitui.
- Mudança de requisito atualiza `docs/PRD.md` na mesma alteração.
- Código que divergir de `docs/ARCHITECTURE.md` obriga a corrigir o
  documento na mesma alteração — o documento não é aspiracional.
- **Nunca execute comandos git.** Ao final, entregue a mensagem de commit
  e o texto do PR em markdown, prontos para copiar.
