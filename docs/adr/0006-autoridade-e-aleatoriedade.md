# ADR-0006 --- Servidor autoritativo e aleatoriedade por porta

**Status:** Aceita
**Data:** 2026-08-31

## Contexto

O PRD §3.3, §16 e §17 estabelecem o servidor como autoridade e o RNG como
responsabilidade dele. Isso é decisão do autor e não está em discussão.
O que esta ADR decide é **como** a aleatoriedade entra no domínio sem
destruir a testabilidade exigida pelo RNF-14 e sem abrir brecha de trapaça.

O conflito é real: uma regra de jogo precisa de aleatoriedade, mas uma
função que sorteia internamente é impossível de testar de forma
determinística. E um teste de regra não determinístico é pior do que
nenhum teste --- ele falha aleatoriamente e é desligado em duas semanas.

## Alternativas consideradas

### A. Chamada direta ao RNG dentro da regra

**Prós:** simples de escrever.
**Contras:** regra não determinística, portanto não testável de forma
confiável; impossível reproduzir um bug relatado; impossível fazer replay
fiel (RF-31). Inaceitável frente ao RNF-14.

### B. Resultado sorteado fora e passado pronto para a regra

**Prós:** regra 100% pura e determinística.
**Contras:** quem está fora precisa saber *o que* sortear --- quantos dados,
quantas faces, quantas cartas embaralhar. Isso é conhecimento de regra
vazando para fora do jogo, violando o PRD §11.1. E, pior, cria uma
assinatura em que um valor de dado trafega como parâmetro, que é
exatamente a forma que um cliente malicioso adoraria alcançar.

### C. Fonte de aleatoriedade injetada como porta

**Prós:** a regra pede o que precisa (`d6()`, `embaralhar(deck)`) sem saber
de onde vem o número; em teste injeta-se uma fonte com semente fixa ou uma
sequência roteirizada; a semente é persistida, tornando a partida
reproduzível. A regra permanece determinística **dada a fonte**.
**Contras:** uma indireção a mais; exige disciplina para ninguém chamar
`Math.random`/`new Random()` diretamente.

### D. Commit-reveal criptográfico (servidor publica hash antes, revela depois)

**Prós:** o jogador pode provar que o servidor não manipulou o dado.
**Contras:** complexidade considerável para um problema que só existe em
contexto de aposta ou competição com prêmio. Nada no PRD sugere isso.

## Decisão

**Opção C.** `FonteDeAleatoriedade` é uma porta de saída do hexágono
(PRD §44.3), passada como parâmetro para `estadoInicial` e `aplicar`.

``` text
FonteDeAleatoriedade
 ├── inteiro(min, max)
 ├── dado(faces)
 ├── embaralhar(lista)
 └── semente()          → registrada no início da partida
```

Três implementações:

| Implementação | Uso |
|---------------|-----|
| `AleatoriedadeSegura` | produção --- `SecureRandom` (RNF-32) |
| `AleatoriedadeComSemente` | reprodução de partida e replay |
| `AleatoriedadeRoteirizada` | teste --- devolve a sequência exata que o teste pediu |

O contra da opção C (alguém chamar o RNG diretamente) é neutralizado por
teste ArchUnit que proíbe qualquer referência a `java.util.Random`,
`Math.random` ou `SecureRandom` nos módulos `core` e `games`. Disciplina que
depende de memória humana não é disciplina; disciplina que quebra o build
é.

**A semente de cada partida é persistida** junto com a sessão (ADR-0004).
Com semente + log de ações, qualquer partida é reproduzível bit a bit ---
que é como um bug de regra relatado por um jogador vira um teste
automatizado em minutos.

**Commit-reveal (D) fica registrado como possibilidade futura**, condicionada
a existir competição com prêmio. Antes disso, é complexidade sem demanda.

### Corolário: o que nunca entra no domínio

Pela mesma razão, `Relogio` também é porta. Uma regra que consulta o
relógio do sistema é tão não determinística quanto uma que sorteia. Isso
importa para o timeout de turno (RF-29).

Regra prática: **o domínio não conhece nem o acaso nem o tempo; ambos são
entregues a ele.**

## Consequências

### Positivas

- Toda regra é testável de forma determinística; RNF-14 cumprido por
  construção.
- Bug relatado é reproduzível a partir de semente e log.
- Replay (RF-31) sai como efeito colateral.
- A superfície de trapaça fica confinada: nenhum valor sorteado atravessa
  a fronteira de entrada do sistema, em nenhuma direção.

### Negativas aceitas

- Uma indireção a mais em toda regra que envolve acaso.
- A fonte precisa ser passada por parâmetro, engordando assinaturas de
  método.
- Sem commit-reveal, um jogador desconfiado não tem como provar
  matematicamente que o servidor foi honesto. Aceitável para jogo social
  entre amigos; inaceitável se algum dia houver aposta.

## Quando revisitar

1. Se surgir competição com prêmio ou ranqueamento sério --- implementar
   commit-reveal.
2. Se houver mais de uma instância resolvendo ações da mesma partida --- a
   fonte com semente precisará de coordenação; hoje isso não existe porque
   uma partida é resolvida por um único processo.
