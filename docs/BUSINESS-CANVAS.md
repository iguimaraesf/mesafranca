# Business Model Canvas --- Mesa Franca

**Status:** esboço v1.0 --- hipóteses, não decisões
**Data:** 06/09/2026
**Método:** *Business Model Generation* (Osterwalder & Pigneur)

> **Como ler este documento.** Nada aqui é decisão tomada. Este é o primeiro
> Canvas do produto e, como o próprio método recomenda, ele deve ser refeito
> à medida que cada hipótese for testada. Onde o documento afirma um número
> ou um fato de mercado sem fonte, ele está marcado como **[a pesquisar]**.
>
> Este Canvas responde, em nível de hipótese, à **Q1** do
> [PRD](PRD.md#47-questões-em-aberto) --- *"Há intenção de monetizar?"* --- e
> toca em **Q2**, **Q3**, **Q4** e **Q6**. Enquanto as hipóteses não forem
> testadas, o PRD continua sendo a autoridade sobre o produto.

---

## 0. Onde o negócio está hoje

Separar o estado técnico do estado comercial evita a ilusão mais cara desta
fase: achar que código pronto é negócio validado.

| Dimensão | Estado real |
|----------|-------------|
| Técnico | Game Core provado por três jogos; `mvn verify` verde |
| Produto | **Não existe aplicação executável, API nem cliente** ([PRD §46.3](PRD.md)) |
| Comercial | Zero clientes, zero conversas com usuários, zero receita |
| Time | Uma pessoa |
| Capital | Zero |

**Consequência para o Canvas:** o produto ainda não pode ser mostrado a
ninguém. Isso restringe fortemente o que faz sentido fazer agora --- em
particular no que diz respeito a parcerias e a investimento (seções 8 e 10).

---

## 1. Segmentos de Clientes

**Tipo (taxonomia do método):** mercado de **nicho**.

Não é mercado de massa. Board game moderno é um hobby com barreira de entrada
--- tempo, disposição para aprender regras e um grupo disponível. Tratá-lo
como massa levaria a superestimar o público e a errar canais e preço.

**Quem paga:** pessoa física.

### Sub-segmentos

| # | Segmento | Origem | Paga? |
|---|----------|--------|-------|
| **S1** | Anfitrião de mesa presencial | Persona Ana ([PRD §41.1](PRD.md)) | **Sim** |
| **S2** | Jogador remoto | Persona Bruno ([PRD §41.2](PRD.md)) | **Sim** |
| S3 | Convidado da mesa | Persona Ana (convidados) | Não |
| S4 | Autor de jogos | Persona Carla ([PRD §41.4](PRD.md)) | Não (futuro: fornece) |

### O ponto que mais importa aqui

S1 tem estrutura **parcialmente multilateral**: um pagante traz de três a
cinco não-pagantes para dentro da partida. Isso corta nos dois sentidos.

- **A favor:** cada anfitriã expõe o produto a várias pessoas sem custo de
  aquisição. É o canal mais barato que este negócio pode ter.
- **Contra:** a receita por partida é baixa. Um modelo que exija cadastro e
  pagamento de todos na mesa mata o requisito da Ana --- *"entrar com um
  código curto e sem cadastro"* --- e com ele mata a viralidade.

Isso condiciona a seção 5: **quem paga é quem hospeda, não quem joga.**

### Fora do escopo v1

Empresas, escolas e bootcamps ficam de fora deliberadamente. São segmentos
com ciclo de venda longo, exigências de nota fiscal, contrato e suporte
dedicado --- incompatíveis com um time de uma pessoa sem produto executável.

**[a pesquisar]** Tamanho do público de board game no Brasil. Proxies úteis:
base de usuários da Ludopedia, público de Ludopedia Con / Diversão Offline,
faturamento declarado por editoras nacionais.

---

## 2. Proposta de Valor

**Tipos dominantes:** *conveniência/usabilidade* e *"fazer o que deve ser
feito"*. **Secundários:** *acessibilidade* e *redução de risco*.

**Não** é *novidade* nem *marca/status*: Tabletop Simulator, Tabletopia e
Board Game Arena existem há anos. Vender novidade aqui seria vender uma
promessa que o mercado já sabe ser falsa.

| Segmento | Valor entregue | Tipo |
|----------|----------------|------|
| S1 Anfitrião | Setup instantâneo, regras aplicadas sozinhas, pontuação automática, convidado entra sem instalar nada | Conveniência / usabilidade |
| S1 Anfitrião | Ninguém erra regra; acaba a discussão sobre "pode ou não pode" | Redução de risco |
| S2 Remoto | Jogar com quem mora longe; reconexão sem perder a partida | Acessibilidade |
| S2 Remoto | Informação privada protegida por construção (servidor autoritativo) | Redução de risco |

### Posicionamento

> **Tabletop Simulator:** "aqui está uma mesa virtual; faça o que quiser."
> **Mesa Franca:** "aqui está uma mesa virtual; sente-se e jogue."

### Contraponto honesto

Board Game Arena **já** aplica regras automaticamente, já tem catálogo grande
e tem uma camada gratuita. Contra o BGA, "o servidor aplica as regras" não é
diferencial --- é paridade.

O que o BGA **não** faz é a **mesa híbrida presencial**: tabuleiro numa TV ou
tablet no centro, celular de cada jogador como tela privada
([PRD §31](PRD.md)). Esse é o único elemento do Canvas que não tem
concorrente direto óbvio.

**Recomendação:** tratar a mesa híbrida presencial como a proposta de valor
central, não como "experiência futura". Se o diferencial for adiado para
depois do MVP, o MVP nasce competindo de frente com o BGA em desvantagem de
catálogo, de time e de capital. Isso empurra a persona Ana para antes da
persona Bruno --- e portanto responde a **Q3** do PRD.

---

## 3. Canais

| Canal | Tipo | Função | Custo |
|-------|------|--------|-------|
| Landing page + lista de espera | Particular direto | Validar interesse antes de construir | ~R$ 0 |
| Devlog em vídeo | Particular direto | Audiência, credibilidade, atrair autores | Tempo |
| Discord da plataforma | Particular direto | Suporte, comunidade, recrutar testadores | ~R$ 0 |
| Ludopedia | Parceiro indireto | Alcance direto no nicho brasileiro | ~R$ 0 |
| Criadores de conteúdo de board game | Parceiro indireto | Prova social | Permuta / R$ |
| Reddit r/boardgames, grupos de Discord | Parceiro indireto | Alcance internacional | ~R$ 0 |
| Catarse / Kickstarter | Parceiro indireto | Financia **e** valida ao mesmo tempo | % da arrecadação |
| Steam | Parceiro indireto | Distribuição, quando houver cliente | US$ 100 + 30% |

O devlog merece destaque: já é uma competência instalada do fundador. É o
canal de menor custo marginal disponível --- e responde a **Q6** do PRD, que
pergunta justamente se haverá conteúdo de vídeo acompanhando o
desenvolvimento. A resposta que este Canvas propõe é **sim, e como canal
principal**, não como subproduto.

**Ação imediata, antes de qualquer linha de código nova:** landing page com
lista de espera. A aula 1.03 do curso é explícita nisso --- pesquisar e
validar antes de gastar meses construindo. Custa quase nada e é o único jeito
de responder **Q2** (escala do primeiro ano) com dado em vez de palpite.

---

## 4. Relacionamento com Clientes

| Tipo | Aplicação |
|------|-----------|
| **Self-service** | Entrar em partida com código curto, sem cadastro. É requisito da Ana, não conveniência. |
| **Comunidades** | Discord: suporte entre pares, playtest, bug report. Único modelo que escala com uma pessoa. |
| **Atendimento automatizado** | Tutorial embutido, FAQ, mensagens de erro que ensinam. |
| **Assistência pessoal** | Só na fase beta, feita pelo fundador. Não escala --- e é exatamente por isso que é valiosa agora. |
| **Cocriação** | Futuro: autores publicando jogos sobre a SPI ([PRD §41.4](PRD.md)). |

Assistência pessoal nos primeiros meses não é ineficiência: é a única forma
barata de descobrir por que as pessoas param de jogar.

---

## 5. Fontes de Receita

Esta seção responde à **Q1** do PRD. Como **hipótese**, não como decisão.

### Avaliadas

| # | Modelo | Tipo | Veredito |
|---|--------|------|----------|
| H1 | Assinatura mensal/anual do catálogo | Taxa de assinatura | **Adotar na v1** |
| H2 | Compra avulsa por jogo | Venda de recursos | Adotar como complemento |
| H3 | Núcleo gratuito + catálogo pago | Freemium | **Adotar na v1** |
| H4 | Publicar jogo de terceiro e repassar royalty | Licenciamento | Futuro (ver seção 8) |
| H5 | Anúncios | Anúncios | **Descartar** |
| H6 | Venda de dados | Venda de dados | **Descartar** |
| H7 | Cobrar de todos na mesa | Taxa de uso | **Descartar** |

### Por que descartar H5, H6 e H7

- **H5 --- anúncios.** Destroem a proposta de valor. Toda a premissa do
  produto é *"a sensação de estar sentado à mesa"*; um anúncio no meio da
  partida é exatamente a quebra de imersão que o produto existe para evitar.
- **H6 --- venda de dados.** Incompatível com LGPD no desenho pretendido,
  incompatível com uma comunidade de nicho que percebe esse tipo de coisa, e
  destrutivo para a confiança que sustenta S1.
- **H7 --- cobrar de todos.** Mata o requisito de convidado efêmero e, com
  ele, o único mecanismo de aquisição barato que o negócio tem.

### Modelo proposto para a v1

**Freemium com assinatura do anfitrião.**

- Dois ou três jogos próprios (tema original), gratuitos e completos.
- Convidado nunca paga e nunca cria conta --- requisito da Ana preservado.
- Quem hospeda a mesa assina para liberar o catálogo completo.
- Assinatura anual com desconto, para reduzir churn e antecipar caixa.

**Preço: [a pesquisar].** Não há base para arbitrar valor aqui. A referência
óbvia é a assinatura do BGA; a pesquisa precisa ser feita com S1 e S2 reais,
não estimada em documento.

**Ordem de grandeza que muda tudo:** com infraestrutura abaixo de US$ 40/mês
([RNF-41](PRD.md#435-operação-e-custo)), o ponto de equilíbrio da operação é
da ordem de algumas dezenas de assinantes. Isso não paga um salário --- mas
significa que o projeto **pode se sustentar tecnicamente sem investimento
externo**. Esse é o argumento central da seção 10.

---

## 6. Recursos Principais

| Tipo | Recurso | Situação |
|------|---------|----------|
| **Intelectual** | Game Core, SPI de jogo, ADRs, cobertura ≥ 85% | **Existe e é o ativo real** |
| **Intelectual** | Marca "Mesa Franca" ([ADR-0009](adr/0009-nome-do-produto.md)) | Registrada no projeto; **[a pesquisar]** registro no INPI |
| **Humano** | Uma pessoa: arquitetura, código, testes, vídeo, produto | **Gargalo do modelo inteiro** |
| **Físico** | Infraestrutura < US$ 40/mês | Barato por decisão de arquitetura |
| **Financeiro** | Nenhum | --- |

O recurso intelectual é genuinamente bom e raro: uma arquitetura em que
**adicionar um jogo não altera o core** ([RNF-50](PRD.md#436-evolutividade)),
já provada por três jogos com mecânicas diferentes. É o que torna um catálogo
viável para um time minúsculo.

O recurso humano é o problema. Um Canvas honesto precisa dizer isto: **todos
os nove blocos dependem da mesma pessoa.** Qualquer plano que ignore esse
fato é ficção.

---

## 7. Atividades-Chave

**Tipo dominante:** *plataforma/rede*. **Secundário:** *resolução de
problemas*.

**Não é *produção*.** Mesa Franca não fabrica nada físico. Esse ponto é
retomado na seção 8, porque é ele que decide se uma gráfica cabe ou não neste
Canvas.

| Atividade | Tipo | Estado |
|-----------|------|--------|
| Manter e evoluir o Game Core | Plataforma/rede | Em andamento |
| Construir API e cliente | Plataforma/rede | **Não iniciado** ([PRD §46.3](PRD.md)) |
| Produzir jogos do catálogo | Plataforma/rede | Três provados em teste |
| Operar servidores | Plataforma/rede | Não iniciado |
| Comunidade e suporte | Resolução de problemas | Não iniciado |
| Devlog e conteúdo | Plataforma/rede | Competência instalada |

---

## 8. Parcerias Principais

> No método, parceria principal serve para **reduzir riscos, otimizar custos
> ou obter recursos que não temos**. Os três tipos são: *atividades
> particulares*, *otimização e economia de escala* e *redução de riscos e
> incertezas*.

### 8.1 Antes de falar da Estrela: gráfica não cabe neste Canvas

O pedido registrado foi *"preciso de uma gráfica e uma empresa de design"*.
Mas Mesa Franca, como descrito no PRD, é **software puro**. Não há produto
impresso, não há caixa, não há componente físico. Atividade-chave de
*produção* não existe neste modelo (seção 7) --- e gráfica é fornecedor de
produção.

Há três leituras possíveis, e elas levam a lugares muito diferentes:

**(a) Existe intenção de edição física ou híbrida que não está no PRD.**
Tabuleiro impresso com marcadores de AR, edição física acompanhando a
digital, componentes de mesa. Se for isso, **gráfica é parceria legítima ---
mas o PRD precisa mudar antes.** Isso adicionaria atividade-chave de
produção, estoque, logística, frete e capital de giro. É outro negócio,
com outra estrutura de custos e outro risco.

**(b) "Design" aqui significa arte digital e UI/UX, não impressão.**
Esta é a leitura mais provável. O produto precisa de identidade visual, arte
de cartas e tabuleiros, e interface --- tudo digital, nada impresso. Nesse
caso a parceria necessária é com **estúdio ou artista de game art e UI/UX**,
e gráfica simplesmente não entra.

**(c) É material de marketing** (banner de evento, flyer, brinde). Aí é
custo de marketing pontual, não parceria principal. Não pertence a este
bloco do Canvas.

**Isto precisa ser decidido antes da v1.1 deste documento.** A hipótese
adotada aqui, até haver decisão, é **(b)**.

### 8.2 Estrela --- o que a verificação de fatos mudou

O motivo declarado para escolher a Estrela foi que *"ela está numa situação
financeira bem ruim, mas é bem conhecida"*. As duas metades merecem
tratamento diferente: a segunda é um argumento de negócio; a primeira não é.
Parceria principal existe para **reduzir** risco --- escolher um parceiro
pela fragilidade dele coloca essa fragilidade dentro do caminho crítico.

Este Canvas ia propor um reposicionamento: tirar a Estrela do papel de
gráfica e colocá-la como licenciadora de catálogo, sob o argumento de que *"o
ativo real dela é a propriedade intelectual"*. **A verificação de fatos
derrubou boa parte desse argumento.**

#### Dois fatos públicos, verificados em 06/09/2026

**1. A Estrela está em recuperação judicial.** O plano foi protocolado em
agosto de 2026, para reestruturar dívida de **R$ 109,1 milhões**. Isso é
substancialmente mais grave do que "situação financeira ruim".

**2. A Estrela perdeu 18 marcas para a Hasbro.** Após quinze anos de litígio,
o TJ-SP confirmou que **Genius, Cilada, Jogo da Vida, Combate, Cara a Cara,
Detetive, Leilão de Arte, Super Massa, Lig 4** e outras voltam à Hasbro ---
eram **licenciadas**, não próprias. Há ainda cobrança de cerca de **R$ 64
milhões** em royalties não pagos desde 2006, e determinação de destruição de
estoque.

**O que continua sendo da Estrela:** Banco Imobiliário, Falcon, Comandos em
Ação e Dona Cabeça de Batata. **Destes, apenas o Banco Imobiliário é jogo de
tabuleiro.**

Ou seja: o "catálogo de IP" que justificaria a parceria, em larga medida,
nunca foi dela.

#### Avaliação corrigida

**Contra:**

- **O ativo licenciável encolheu para um título.** Um jogo não sustenta uma
  estratégia de catálogo.
- **Recuperação judicial é risco contratual concreto.** Contrato firmado com
  empresa em RJ fica exposto: se a recuperação virar falência, a licença
  entra no acervo a ser disputado por credores. Um catálogo digital
  construído sobre essa licença pode precisar ser retirado do ar.
- **Há histórico documentado de royalties não honrados** --- R$ 64 milhões,
  desde 2006. Isso é diretamente relevante para quem pretende ser contraparte
  num contrato de royalties.
- **Competência errada para o pedido original.** Fabricante de brinquedos não
  é gráfica de board game nem estúdio de UI/UX.
- **A marca é forte no público errado.** Reconhecimento da Estrela está no
  varejo de brinquedo e na nostalgia familiar, não no jogador de board game
  moderno --- que é o segmento do PRD.
- **Assimetria de negociação.** Empresa em RJ tem prioridades urgentes;
  desenvolvedor solo sem produto executável não é uma delas.

**A favor:**

- **Banco Imobiliário é provavelmente o jogo de tabuleiro mais reconhecido do
  Brasil.** Como título único, é forte.
- **O incentivo econômico existe e é real.** Licenciar IP para o digital tem
  custo marginal quase zero para o licenciante e gera receita nova sem
  competir com o produto físico. Uma empresa sob pressão de caixa tende a
  ouvir esse tipo de proposta.
- **Credibilidade de marca** em conversas posteriores com investidores.

#### Veredito

**Não descartar --- rebaixar.** A Estrela sai de "parceira desejada" e vira
*uma* hipótese de licenciamento entre outras, a ser testada **depois do MVP
jogável**, e depois de conversar com editoras cujo catálogo não está em
disputa. Se avançar, exige due diligence jurídica sobre o efeito da
recuperação judicial no contrato --- não é uma negociação comercial comum.

#### Um achado inesperado: jurisprudência para a Q4 do PRD

No mesmo julgamento, o tribunal decidiu que **Banco Imobiliário permanece com
a Estrela**, mesmo sendo mecanicamente equivalente ao Monopoly. O fundamento:

> *"Ambas as marcas são diferentes e únicas, embora representem um produto
> com os mesmos objetivos e regras de jogo."*

Isto é precedente brasileiro concreto para exatamente a tese registrada na
[Q4 do PRD](PRD.md#47-questões-em-aberto): **regra de jogo não tem proteção;
nome e arte têm.** A conclusão do PRD --- reimplementar as regras com tema,
nome e arte próprios --- não é só a opção barata: é a que a jurisprudência
brasileira sustenta.

### 8.3 Alternativas a considerar

**Licenciamento de catálogo** (candidatos sem passivo de IP em disputa):
Galápagos Jogos, Devir Brasil, Mandala Jogos, Grow, Redbox. Todas mais
próximas do nicho de board game moderno que a Estrela. **Conversar com mais
de uma antes de fechar com qualquer.**

**Arte e UI/UX** (o que a leitura (b) da seção 8.1 indica): estúdios de game
art brasileiros e artistas independentes com portfólio em board game. Modelo
por projeto, ou permuta por participação em receita enquanto não há caixa.

**Gráfica** --- só se a leitura (a) for confirmada: Copag (cartas), Grafo
Games, PaperGames, Meeple BR. Para board game, a referência internacional é
Panda Game Manufacturing, com quantidade mínima alta.

**[a pesquisar]** Disposição, capacidade e situação atual de todas as
empresas citadas nesta seção. São candidatos a investigar, não recomendações
fechadas.

### 8.4 Quadro de parcerias

| Parceiro | Tipo (método) | O que resolve | Quando |
|----------|---------------|---------------|--------|
| Estúdio de arte / UI-UX | Atividades particulares | Recurso que não temos e não vamos desenvolver | Antes do beta |
| Provedor de nuvem | Otimização e economia de escala | Manter custo sob RNF-41 | No MVP |
| Ludopedia, criadores de conteúdo | Otimização e economia de escala | Alcance no nicho sem custo de mídia | Já |
| Editora de board game (**não** a Estrela como 1ª opção) | Redução de riscos e incertezas | Catálogo com IP conhecida; resolve Q4 | Pós-MVP |
| Autores independentes (persona Carla) | Atividades particulares | Catálogo sem custo de produção próprio | v2 |
| Gráfica | Atividades particulares | Só existe se a leitura (a) for confirmada | Indefinido |

---

## 9. Estrutura de Custos

**Tipo:** *direcionada pelo valor*, com forte **economia de escopo**.

A economia de escopo não é aspiração: é consequência direta do
[RNF-50](PRD.md#436-evolutividade). Um Core que serve todos os jogos significa
que o custo de adicionar o quarto jogo é uma fração do custo do primeiro. **A
decisão arquitetural central do projeto é, também, a decisão de custo
central.**

| Custo | Natureza | Ordem de grandeza |
|-------|----------|-------------------|
| Infraestrutura | Fixo | < US$ 40/mês (RNF-41) |
| Domínio e certificados | Fixo | ~R$ 100/ano |
| Arte por jogo | Variável | **[a pesquisar]** |
| Royalties de licenciamento | Variável | % da receita do título |
| Gateway de pagamento | Variável | ~4% a 6% + fixo por transação |
| Loja (Steam) | Variável | 30% |
| CNPJ e contabilidade | Fixo | ~R$ 300/mês, quando abrir |
| **Tempo do fundador** | **Oculto** | **O maior custo do projeto** |

O tempo do fundador não passa pelo caixa e por isso costuma sumir da planilha
--- mas é o recurso mais escasso do modelo (seção 6) e o único que não pode
ser comprado de volta.

---

## 10. Investimento --- discordância sobre o momento

O pedido menciona abertura a investidores-anjo. O material do curso é
explícito sobre quando isso faz sentido:

> *"O mais indicado é que esse tipo de investidor venha somar na etapa de
> validação, momento no qual a empresa está provando, ou já provou, que o seu
> modelo de negócio é realmente viável."*

Mesa Franca não está nessa etapa. Não há aplicação executável, não há usuário,
não há métrica. O mesmo material lista o que um anjo pede --- ROI, CAC, LTV,
churn, usuários ativos, crescimento mês a mês. **Hoje, todos esses números
seriam inventados.**

Procurar anjo agora tem três custos concretos: queima o contato (não se
apresenta duas vezes pela primeira vez), dilui participação na pior avaliação
possível, e consome semanas de um recurso já escasso.

E há um fato que muda a conversa: **com infraestrutura abaixo de US$ 40/mês,
este projeto não precisa de capital para existir.** Precisa de tempo. Capital
externo compraria tempo --- mas ao preço de participação e de obrigação de
crescimento acelerado, num negócio de nicho que talvez não queira crescer
assim.

### Ordem proposta

| # | Etapa | Por quê |
|---|-------|---------|
| 1 | Landing page + lista de espera | Custo ~zero. Responde Q2 e Q3 com dado. |
| 2 | MVP jogável (API + cliente web) | Sem isso não há nada a mostrar a ninguém. |
| 3 | Beta fechado com mesas reais | Gera as métricas que um anjo vai pedir. |
| 4 | Catarse / Kickstarter | Financia **e** valida ao mesmo tempo, sem diluir. |
| 5 | Editais (FINEP, Sebrae, FAPESP PIPE) | Capital não-diluidor. **[a pesquisar]** elegibilidade. |
| 6 | Anjo, se ainda fizer sentido | Anjos do Brasil, Gávea Angels, Bossa Invest, Curitiba Angels. |

O crowdfunding merece atenção especial: é o instrumento mais alinhado a este
mercado. A comunidade de board game já compra por financiamento coletivo, e
uma campanha que não atinge a meta é a resposta mais barata possível para a
pergunta *"alguém quer isto?"*.

---

## 11. Hipóteses mais arriscadas

Ordenadas por quanto o modelo inteiro desmorona se estiverem erradas.

| # | Hipótese | Como testar barato |
|---|----------|--------------------|
| **1** | Alguém paga por isto, havendo BGA com camada gratuita | Landing com preço e botão de intenção |
| **2** | A mesa híbrida presencial é o diferencial real | Protótipo tosco com TV e dois celulares |
| **3** | Catálogo sem IP conhecida atrai público | Medir conversão dos jogos de tema próprio |
| **4** | Uma pessoa mantém plataforma **e** catálogo | Cronometrar o custo real do quarto jogo |
| 5 | Anfitriões trazem convidados que viram anfitriões | Medir conversão convidado → anfitrião no beta |

A hipótese 1 é a que mata o negócio se for falsa, e é testável esta semana,
sem escrever uma linha de código.

---

## 12. Próximos passos

1. **Decidir a leitura da seção 8.1** --- há ou não intenção de produto
   físico? Isso muda PRD, custos e parcerias.
2. **Landing page com lista de espera.** Antes de qualquer código novo.
3. **Conversar com dez jogadores reais** --- cinco perfil Ana, cinco perfil
   Bruno. Perguntar o que fazem hoje e o que os incomoda; não apresentar a
   solução.
4. **Registrar as respostas de Q1 a Q6** no PRD, com base nesses dados.
5. **Refazer este Canvas** como v2.0. O método pede isso explicitamente: o
   primeiro Canvas serve para descobrir o que não se sabia.
6. Só então: MVP, beta, e a conversa sobre catálogo licenciado.

---

## 13. Reflexo nas questões abertas do PRD

| Questão | Situação após este Canvas |
|---------|---------------------------|
| **Q1** Monetizar? | Hipótese: freemium + assinatura do anfitrião (seção 5). A confirmar com S1/S2. |
| **Q2** Escala do 1º ano | Ainda aberta. A lista de espera é o instrumento. |
| **Q3** Ana ou Bruno primeiro? | Este Canvas argumenta **Ana primeiro** --- é onde está o diferencial (seção 2). |
| **Q4** Clones distribuídos? | Este Canvas propõe **não** --- e agora com precedente: TJ-SP decidiu que regra de jogo não é protegida, nome e arte são (seção 8.2). Tema próprio. |
| **Q5** Plataforma do Unity | Se Q3 for Ana, a resposta tende a **TV/tablet + celular**. |
| **Q6** Devlog em vídeo? | Este Canvas propõe **sim, como canal principal** (seção 3). |

---

## Fontes

Fatos de mercado citados na seção 8.2, verificados em 06/09/2026:

- [Estrela (ESTR3) protocola plano para renegociar dívida de R$ 109 milhões na recuperação judicial --- Forbes Brasil](https://forbes.com.br/forbes-money/2026/08/estrela-plano-recuperacao-judicial/)
- [Grupo Estrela apresenta plano de recuperação judicial para dívidas de R$ 109,1 milhões --- O Tempo](https://www.otempo.com.br/economia/2026/8/21/grupo-estrela-apresenta-plano-de-recuperacao-judicial-para-dividas-de-r-109-1-milhoes)
- [Estrela em recuperação judicial: entenda os motivos da crise --- InfoMoney](https://www.infomoney.com.br/business/estrela-em-recuperacao-judicial-entenda-motivos-da-crise-da-fabricante-de-brinquedos/)
- [Estrela terá que devolver Banco Imobiliário e outros jogos à Hasbro --- Exame](https://exame.com/negocios/estrela-tera-que-devolver-banco-imobiliario-e-outros-jogos-a-hasbro/)
- [Estrela x Hasbro: entenda a disputa --- Metrópoles](https://www.metropoles.com/brasil/estrela-x-hasbro-entenda-a-disputa-que-pode-destruir-brinquedos)

Metodologia: *Business Model Generation*, Alexander Osterwalder e Yves
Pigneur --- taxonomia de segmentos, propostas de valor, canais,
relacionamento, receitas, recursos, atividades-chave, parcerias e custos
conforme o material de curso em `D:\\Tools\\cursos\\business-model-canvas`.

---

*Documento vivo. Refazer a cada hipótese testada.*
