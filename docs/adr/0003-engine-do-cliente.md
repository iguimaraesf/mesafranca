# ADR-0003 --- Unity como cliente de produto, web como cliente intermediário

**Status:** Aceita
**Data:** 2026-08-31

## Contexto

O PRD §3.5, §18 e §39 já definem **Unity 3D** como cliente inicial. Essa
decisão é do autor do projeto e tem precedência. Esta ADR não a revoga: ela
registra as alternativas e os trade-offs para que a decisão fique
auditável, e **acrescenta** um passo intermediário que reduz o risco do
MVP sem contrariar nada do que foi definido.

Vale explicitar uma assimetria que muda o peso desta decisão: pelo desenho
do PRD, **o cliente é a parte descartável do sistema**. As regras vivem no
servidor, o cliente só apresenta eventos e envia intenções. Trocar de
engine custa reescrever apresentação, não regra. Isso significa que esta é
a decisão **menos irreversível** de todo o projeto --- e portanto a que menos
merece agonia. O contrário vale para a ADR-0002.

## Alternativas consideradas

### A. Unity (decisão do PRD)

**Prós**

- Único caminho maduro para a visão de longo prazo de AR/VR (PRD §32):
  XR Interaction Toolkit, Quest, Vision Pro. Nenhuma outra opção da lista
  chega perto.
- 3D de verdade: dados com física, peças com volume, câmera --- o que
  sustenta a sensação de "mesa" do PRD §3.1.
- Exportação para desktop, mobile, TV e consoles a partir de um projeto.
- C# é próximo o suficiente de Java para o desenvolvedor aproveitar boa
  parte do que já sabe.
- Unity Personal é gratuito até US$ 200 mil de receita/financiamento anual,
  e a splash screen "Made with Unity" é opcional no Unity 6.

**Contras**

- Motor pesado para o que o MVP precisa: as três primeiras telas são
  tabuleiro, cartas e botões.
- Ciclo de iteração lento comparado a um cliente web; cada teste com
  amigos exige build e distribuição.
- Histórico de licenciamento instável. A Runtime Fee foi cancelada em 2024
  com retorno ao modelo por assento, mas houve reintrodução de cobrança
  por uso na trilha Industry, e Pro/Enterprise sobem 5% em janeiro de 2026.
  É risco de fornecedor, não de tecnologia.
- Exportação WebGL medíocre --- justamente o que serviria para "jogue agora,
  sem instalar" da persona Ana.
- Motor proprietário e fechado.

### B. Godot 4.6

**Prós**

- MIT, gratuito para sempre, sem royalties nem assentos. Elimina o risco
  de fornecedor da opção A.
- Pipeline 2D nativo (não adaptado do 3D) --- excelente para cartas e
  tabuleiros; supera Unity em densidade de sprites.
- Editor leve (~40 MB), iteração rápida, exportações pequenas.
- 4.6 (jan/2026) melhorou export Android, billing e serviços de loja.

**Contras**

- **Suporte a XR bem inferior ao Unity.** Quest é viável; Vision Pro
  praticamente não. Isso colide diretamente com PRD §32.
- GDScript seria uma quarta linguagem; o suporte a C# existe, mas é mais
  fraco nos alvos web e mobile.
- Ecossistema de assets e de contratação muito menor.
- Consoles exigem terceiros.

### C. Phaser ou PixiJS (TypeScript, web)

**Prós**

- Roda com um link, sem instalação --- resolve o requisito da persona Ana
  (RF-03, RF-51) melhor que qualquer outra opção.
- Perfeito para cartas e tabuleiros 2D; PixiJS é um renderizador rápido e
  enxuto, Phaser traz mais infraestrutura de jogo.
- Iteração quase instantânea; ótimo para validar o contrato da API.
- Viabiliza o modo mesa híbrida (PRD §31) sem app: TV abre uma URL, cada
  celular abre outra.

**Contras**

- Sem 3D com física de dado; a "sensação de mesa" fica mais fraca.
- Sem caminho para AR/VR.
- Mobile seria web app (empacotável com Capacitor), não nativo.

### D. React puro + canvas/SVG

**Prós:** simplicidade máxima para jogos de carta, DX excelente.
**Contras:** sem game loop nem infraestrutura de animação; vira luta contra
o framework assim que houver movimento de peça no tabuleiro.

### E. boardgame.io

**Descartada, e não por ser web.** Ela não é uma engine de renderização e
sim um framework de regras: quer hospedar o estado e a lógica de turno no
seu próprio servidor Node. Isso duplicaria a autoridade que o PRD §3.3
coloca no backend e violaria o RNF-50. Some-se a isso o estado inativo do
projeto (sem release em ~4 anos). Não há cenário em que ela caiba nesta
arquitetura.

## Decisão

**Confirmar Unity como cliente de produto**, conforme PRD §18, e **inserir
um cliente web em PixiJS + TypeScript como segundo cliente**, antes do
Unity. Ordem de construção:

``` text
1. CLI          (PRD §8)   → prova que o Core roda sem UI
2. Web/PixiJS   (RF-51)    → prova que a API é agnóstica de cliente
3. Unity        (RF-52)    → cliente de produto, AR/VR no horizonte
```

Razões para o passo 2, que é o único acréscimo ao que o PRD já dizia:

- O critério de sucesso 35.3 é "um cliente externo consegue operar a
  partida pela API". Um segundo cliente **independente** é a única prova
  real disso. Se CLI e Unity forem os únicos, o risco é a API se moldar às
  necessidades do Unity sem que ninguém perceba.
- É o jeito mais barato de colocar o jogo na mão de pessoas reais: um
  link, sem build nem instalação --- exatamente o que a persona Ana exige.
- Valida o modo mesa híbrida (PRD §31) sem escrever app nenhum.
- Custa pouco: o cliente é fino por construção, e o que ele exercita
  (contrato, projeção de visão, reconexão) é reaproveitado pelo Unity.

**Godot fica registrado como plano B documentado**, acionável se o risco de
licenciamento do Unity se materializar. O custo dessa troca é baixo
justamente porque nenhuma regra vive no cliente --- o que é uma
demonstração de que a arquitetura do PRD está fazendo o seu trabalho.

## Encaixe com a arquitetura hexagonal

Nenhum dos clientes é adaptador do hexágono. Eles são **processos externos**
que conversam com os adaptadores REST e WebSocket. A fronteira é o contrato
da API, não uma interface de código:

``` text
CLI (Java)    ─┐
Web (TS)      ─┼─► contrato OpenAPI/AsyncAPI ─► adaptador REST/WS ─► portas de entrada
Unity (C#)    ─┤
Bot (Java)    ─┘
```

Consequência prática: os SDKs de cliente TypeScript e C# são **gerados** a
partir do contrato, não escritos à mão. Isso mantém RNF-53 verdadeiro e
impede que uma regra se instale no cliente por descuido.

## Consequências

### Positivas

- A API ganha um segundo consumidor independente antes do Unity, que é a
  única forma honesta de validar o critério 35.3.
- Playtest com pessoas reais bem antes de existir cliente gráfico final.
- Mesa híbrida testável sem app.
- Se o Unity se tornar inviável, a perda é de apresentação apenas.

### Negativas aceitas

- Mais um cliente para manter. Mitigação: o cliente web é deliberadamente
  feio e funcional --- ele é ferramenta de validação, não produto.
- Uma linguagem a mais no repositório (TypeScript).
- O Unity chega mais tarde no cronograma.

## Quando revisitar

1. Se o licenciamento do Unity mudar de forma adversa --- ativar Godot.
2. Se o cliente web se mostrar bom o bastante para ser o produto (possível,
   para jogos de carta) --- reavaliar se o Unity é necessário antes de AR/VR.
3. Se AR/VR sair da visão de longo prazo --- o principal argumento a favor do
   Unity desaparece junto.

## Fontes

- [Unity is Canceling the Runtime Fee](https://unity.com/blog/unity-is-canceling-the-runtime-fee)
- [Unity Pricing Updates](https://unity.com/products/pricing-updates)
- [The 2026 Game Engine License Shake-Up --- Unity, Unreal, Godot](https://www.gamineai.com/blog/the-2026-game-engine-license-shake-up-unity-unreal-godot-compared)
- [Godot vs Unity 2026](https://godotlearning.com/blog/godot-vs-unity-2026)
- [boardgame.io --- estado do projeto](https://github.com/boardgameio/boardgame.io/issues/1150)
