# Architecture Decision Records

Registro das decisões arquiteturais do projeto. Uma ADR documenta **uma**
decisão: o contexto, as alternativas consideradas com prós e contras
reais, a decisão tomada e suas consequências --- inclusive as ruins.

## Convenções

- Numeração sequencial, quatro dígitos, nunca reutilizada.
- Uma ADR nunca é editada depois de aceita. Para mudar de ideia, cria-se
  uma nova ADR que **substitui** a anterior, e a anterior passa a
  `Substituída por ADR-nnnn`.
- Toda ADR tem uma seção **"Quando revisitar"**. Decisão sem gatilho de
  revisão vira dogma.
- Status possíveis: `Proposta`, `Aceita`, `Substituída`, `Descartada`.

## Índice

| ADR | Título | Status |
|-----|--------|--------|
| [0001](0001-arquitetura-hexagonal.md) | Arquitetura hexagonal com jogos como plugins | Aceita |
| [0002](0002-linguagem-do-backend.md) | Java como linguagem do backend | Aceita |
| [0003](0003-engine-do-cliente.md) | Unity como cliente de produto, web como cliente intermediário | Aceita |
| [0004](0004-persistencia.md) | PostgreSQL com log de eventos e snapshots | Aceita |
| [0005](0005-transporte.md) | HTTP para comandos, WebSocket para eventos | Aceita |
| [0006](0006-autoridade-e-aleatoriedade.md) | Servidor autoritativo e aleatoriedade por porta | Aceita |
| [0007](0007-ferramenta-de-build.md) | Maven como ferramenta de build | Aceita |
| [0008](0008-adiar-o-bootstrap-spring.md) | Adiar o bootstrap Spring Boot ate o Game Core estar provado | Aceita |
| [0009](0009-nome-do-produto.md) | Mesa Franca como nome do produto | Aceita |

## Modelo

```markdown
# ADR-nnnn --- Título

**Status:** Proposta | Aceita | Substituída por ADR-nnnn | Descartada
**Data:** AAAA-MM-DD

## Contexto
## Alternativas consideradas
## Decisão
## Consequências
### Positivas
### Negativas aceitas
## Quando revisitar
```
