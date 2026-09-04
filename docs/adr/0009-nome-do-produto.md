# ADR-0009 --- Mesa Franca como nome do produto

**Status:** Aceita
**Data:** 2026-09-02

## Contexto

O projeto vinha sendo chamado de "Tabuleiro", e o repositório de
`git-tabuleiro`. Isso nunca foi um nome: é o gênero do produto usado como
rótulo, do mesmo jeito que chamar uma cafeteria de "Café". Serve enquanto o
projeto é uma pasta no disco de uma pessoa; não serve para um domínio, um
handle, uma marca ou a primeira frase que se diz a alguém.

O nome precisava atender quatro eixos que o PRD já descreve, e não apenas
um deles:

1. jogar com amigos (PRD §4);
2. socialização — mas o produto é um **lugar**, não uma rede de ligações
   entre pessoas (PRD §3.1: "mesa, não videogame");
3. torneios públicos como possibilidade futura (PRD §34);
4. a sensação física de mesa de jogo de tabuleiro (PRD §3.1).

Restrições práticas: domínio `.com.br` e `.com` livres, e handle livre no
GitHub — porque o nome que não tem endereço não é nome, é apelido.

## Alternativas consideradas

### A. Mesa Aberta

**Prós:** diz quase exatamente a coisa certa — mesa que aceita quem chegar.
Fácil de falar, fácil de escrever, sem ambiguidade de grafia.

**Contras, e foram decisivos:** o domínio está **tomado e dormente**, que é o
pior dos dois mundos — não dá para usar e não dá para negociar com quem
claramente não está usando. E o registro.br **bloqueia a variante com hífen**
por sintaxe similar ao domínio existente, então nem o contorno óbvio
funciona.

### B. Roda Aberta

**Prós:** "roda" carrega o círculo de pessoas, que é a metáfora social certa.

**Contras:** "roda" puxa para roda de samba, roda de conversa, roda de
capoeira — o campo semântico é de encontro informal, não de jogo com regras.
E perde a mesa, que é justamente o objeto que o produto simula.

### C. Mesa Franca

**Prós:** ver abaixo.

**Contras:** o trocadilho é português e não atravessa. Em inglês, "franca"
não diz nada, e "Free Table" diz outra coisa. Se um dia houver
internacionalização, o nome viaja como som, não como sentido.

## Decisão

**Mesa Franca.**

O adjetivo vem de **porto franco** e **cidade franca**: lugar livre de
barreira, onde se entra sem pedágio e sem precisar pertencer a nada. Não é
"franca" no sentido de sincera — é no sentido territorial, de zona onde a
entrada não é cobrada nem controlada.

Isso é o que faz o nome cobrir os quatro eixos de uma vez:

| Eixo | Como o nome atende |
|------|--------------------|
| Jogar com amigos | "mesa" é literalmente o objeto ao redor do qual se joga |
| Socialização sem ser rede social | franca descreve um **lugar**, não uma ligação entre pessoas — não há "seguir", há "sentar" |
| Torneios públicos | uma mesa franca é aberta a quem chegar, inclusive a desconhecidos |
| Sensação de board game | "mesa" ancora no físico, contra a deriva para videogame que o PRD §3.1 quer evitar |

O ponto que decidiu contra "Mesa Aberta", além do domínio: **"aberta" é
estado, "franca" é natureza.** Uma mesa aberta pode fechar. Uma mesa franca é
franca por definição — a palavra carrega a promessa de que a barreira não
existe, não de que ela está temporariamente levantada. Para um produto cuja
persona principal precisa que convidados entrem **sem cadastro** (RF-03), a
diferença não é estética.

### Disponibilidade verificada em 02/09/2026

| Recurso | Situação |
|---------|----------|
| `mesafranca.com.br` | livre |
| `mesafranca.com` | livre |
| `github.com/mesafranca` | livre |

### Esquema de nomes adotado

| Contexto | Forma | Exemplo |
|----------|-------|---------|
| Nome comercial | Mesa Franca | títulos, PRD, interface |
| Domínio e handle | `mesafranca` | `mesafranca.com.br` |
| Repositório | `mesa-franca` | |
| Pacote Java | `br.com.mesafranca` | `br.com.mesafranca.core.domain` |
| `groupId` Maven | `br.com.mesafranca` | |
| `artifactId` Maven | `mesa-*` | `mesa-core`, `mesa-ludo` |
| Forma curta interna | `mesa` | vocabulário de código e de interface |

A forma curta existe porque `mesafranca-core` é longo e `mesa-core` já é
inequívoco dentro do projeto. E porque "mesa" é o substantivo que o próprio
domínio usa: uma partida acontece **na mesa**.

**O módulo `core` continua se chamando `core`, e não `engine`.** O PRD fala
em "Game Core" desde a seção 3.2, e a documentação inteira usa esse
vocabulário. Trocar para `engine` seria renomear um conceito, não um produto
— duas mudanças diferentes no mesmo commit, e só uma foi pedida.

## Consequências

### Positivas

- Domínio, handle e nome de repositório alinhados, todos livres.
- O nome diz o que o produto é sem descrever a mecânica, o que deixa espaço
  para o catálogo crescer (PRD §34) sem que o nome envelheça.
- `mesa` como prefixo dá nomes de artefato curtos e legíveis.

### Negativas aceitas

- O trocadilho não sobrevive à tradução. Se houver internacionalização, o
  nome vira som, não sentido.
- "Franca" também é o nome de uma cidade em São Paulo, o que pode competir em
  busca. Mitigação: o par "mesa franca" é distintivo o suficiente; sozinho o
  segundo termo não é o alvo.
- Um custo real de renomeação já pago: pacote, artefatos e documentação.

### Sobre o que ficou desatualizado e não foi corrigido

A [ADR-0001](0001-arquitetura-hexagonal.md) mostra a árvore de módulos com a
raiz `tabuleiro/`. Ela **não foi editada**, pela regra do próprio projeto —
ADR aceita não se edita. Leia-se `mesa-franca/`. Este parágrafo existe para
que quem cruzar com aquele diagrama saiba por que ele diverge.

O repositório no GitHub ainda se chama `tabuleiro-api`, e a pasta local ainda
é `git-tabuleiro`. Renomear os dois é decisão de infraestrutura, separada
desta, e depende de uma reestruturação de diretórios que está em aberto.

## Quando revisitar

1. Se uma busca no INPI apontar conflito de marca em classe relevante.
2. Se a internacionalização entrar no roadmap — aí a pergunta é se o produto
   viaja com o nome ou com um segundo nome.
3. Se o produto deixar de ser uma mesa — se virar catálogo, loja ou rede, o
   nome deixa de descrevê-lo.
