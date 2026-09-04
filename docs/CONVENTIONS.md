# Convenções

## Idioma

- Documentação, comentários e mensagens de commit: **português**.
- Código, nomes de classe, método e variável: **inglês**, exceto termos de
  domínio do jogo que não têm tradução natural.
- Nomes de portas e casos de uso: **português**, porque são linguagem
  ubíqua de negócio (`SubmeterAcao`, `RepositorioDePartidas`).

Regra de desempate: se um jogador entenderia o termo, é português.

## Nomenclatura

| Elemento | Padrão | Exemplo |
|----------|--------|---------|
| Porta de entrada | verbo no infinitivo | `SubmeterAcao` |
| Porta de saída | substantivo do papel | `FonteDeAleatoriedade` |
| Adaptador | papel + tecnologia | `PostgresRepositorioDePartidas` |
| Implementação de caso de uso | porta + `Service` | `SubmeterAcaoService` |
| Ação | verbo no imperativo | `JogarCarta`, `RolarDado` |
| Evento | fato no passado | `CartaJogada`, `DadoRolado` |

Ação e evento nunca compartilham nome. `RolarDado` é pedido; `DadoRolado`
é fato. Confundir os dois é confundir intenção com resultado --- o erro
central que o PRD §3.3 existe para evitar.

## Estrutura de commit

Conventional Commits, escopo obrigatório:

``` text
<tipo>(<escopo>): <resumo no imperativo, minúsculo, sem ponto>

<corpo: o porquê, não o quê>

<rodapé: refs>
```

Tipos: `feat`, `fix`, `refactor`, `test`, `docs`, `build`, `chore`, `perf`.
Escopos: `core`, `ludo`, `love-letter`, `uno`, `rest`, `ws`, `cli`,
`persistence`, `web`, `unity`, `docs`, `build`.

O corpo explica a decisão. O diff já mostra o que mudou.

## Branches

``` text
main                    sempre verde
feat/<escopo>-<resumo>
fix/<escopo>-<resumo>
docs/<resumo>
```

## Pull request

Um PR precisa de:

- descrição do problema e da abordagem;
- build verde, incluindo portões de cobertura e de arquitetura;
- ADR nova ou atualizada, se houver decisão arquitetural;
- PRD atualizado, se houver mudança de requisito.

PR que reduz cobertura abaixo da meta não entra, mesmo que o código esteja
correto. A meta é do repositório, não do PR.

## Código

- Java 25, `record` e `sealed` como padrão para domínio.
- `switch` exaustivo sem `default` sobre tipos `sealed`
  (ver [ARCHITECTURE.md](ARCHITECTURE.md#por-que-sealed-importa-aqui)).
- Sem `null` no domínio: use `Optional` nas fronteiras e tipos explícitos
  dentro.
- Exceção só para o que é excepcional. Ação inválida devolve `Resultado`
  ([PRD §44.4](PRD.md#444-a-spi-de-jogo)).
- Um `record` que guarda coleção guarda cópia imutável.
- Sem comentário que repita o código. Comentário explica *por quê*.

## Documentação --- quem manda em quê

| Documento | Responde |
|-----------|----------|
| `CLAUDE.md` | como trabalhar neste repositório |
| `docs/PRD.md` | o que o produto é e o que ele exige |
| `docs/adr/` | por que cada decisão técnica foi tomada |
| `docs/ARCHITECTURE.md` | como o código está organizado |
| `docs/TESTING.md` | como se testa aqui |
| `docs/CONVENTIONS.md` | este arquivo |

Nada é duplicado entre eles. Quando um assunto caberia em dois, ele mora
no mais específico e o outro faz link.
