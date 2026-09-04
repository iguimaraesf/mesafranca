# Estratégia de testes

Cumpre os RNF-10 a RNF-16 do [PRD §43.2](PRD.md#432-qualidade-e-testabilidade).

## Metas

| Escopo | Linhas | Branches | Portão |
|--------|--------|----------|--------|
| Piso do repositório | 85% | 85% | build falha |
| `core` | 95% | 95% | build falha |
| `games/*` | 95% | 90% | build falha |
| `adapters` | 85% | 85% | build falha |
| `arquitetura` | — | — | sem código de produção |

Os limites são propriedades Maven (`cobertura.linhas`, `cobertura.branches`)
herdadas do POM raiz: um módulo eleva ou abaixa **explicitamente**, e a
mudança aparece no diff. `jacoco:check` roda na fase `verify`.

`arquitetura` não tem classe de produção — só as regras ArchUnit — então não
há o que cobrir. Duas consequências disso são **esperadas** e não são defeito:

- o JaCoCo pula relatório e portão no módulo, por falta de `target/classes`;
- o `default-jar` está desligado no `arquitetura/pom.xml`, junto com
  `maven.install.skip`, porque o jar sairia vazio. Sem isso, todo build
  imprimiria `JAR will be empty - no content was marked for inclusion!`.

Nos jogos, o piso de *branches* é 90% e não 95%, por margem e não por
princípio: os três jogos concentram muito `switch` sobre enum, e cada enum de
oito valores vira oito ramos que precisam de oito caminhos de teste distintos.
Linhas continuam em 95%.

## O que fazer quando o portão reprovar

Aconteceu na primeira execução real: o `core` parou em 93% de branches, com o
piso em 95%, e o reactor inteiro foi pulado atrás dele. O procedimento que
funcionou, e que deve ser repetido:

**1. Ler o relatório, não adivinhar.** `api/core/target/site/jacoco/jacoco.xml`
tem, por método e por linha, quantos ramos faltam:

``` bash
python3 - <<'EOF'
import xml.etree.ElementTree as ET
r = ET.parse('api/core/target/site/jacoco/jacoco.xml').getroot()
for pkg in r.findall('package'):
    for cls in pkg.findall('class'):
        for m in cls.findall('method'):
            for c in m.findall('counter'):
                if c.get('type') == 'BRANCH' and int(c.get('missed')):
                    print(c.get('missed'), cls.get('name'), m.get('name'), 'linha', m.get('line'))
EOF
```

**2. Classificar cada lacuna em real ou sintética.** Real é comportamento que
ninguém exercitou. Sintética é ramo que o compilador gera e que nenhuma
entrada alcança.

**3. Só então escolher o remédio**, nesta ordem de preferência:

| Diagnóstico | Remédio |
|---|---|
| Comportamento real não testado | **escrever o teste** |
| Ramo defensivo inalcançável no código próprio | **apagar o ramo** |
| Ramo sintético do compilador | filtro no JaCoCo, com justificativa aqui |
| — | **nunca**: baixar o limite |

Baixar o número transforma o portão em enfeite. Se o limite estiver errado,
isso é uma decisão consciente que muda esta tabela e o `pom.xml` no mesmo
commit — não uma reação a um build vermelho.

### O que o primeiro diagnóstico encontrou

Nove ramos, **todos reais, nenhum sintético**. O filtro de `record` do JaCoCo
já tinha removido `equals`, `hashCode` e `toString` gerados, então não sobrou
nada de artificial para filtrar:

- as quatro respostas de recusa (`CriarPartida`, `EntrarNaPartida`,
  `IniciarPartida`, `SubmeterAcao`) nunca tinham sido construídas com motivo
  nulo ou em branco — uma recusa sem motivo chegaria ao cliente como uma tela
  vazia;
- `OrdemDeTurno` validava assento fora da mesa só pelo lado de cima: o teste
  passava `posicao = 2` numa mesa de dois, nunca `-1`.

Nos jogos, a mesma varredura encontrou oito guardas de construção sem teste e
um ramo genuinamente morto — o tratamento de mão vazia em
`LoveLetterDefinicao.eliminar`, inalcançável porque o descarte forçado do
Príncipe segue por outro caminho. Esse foi **apagado**, não filtrado: ramo
inalcançável não é segurança, é código que ninguém vai testar nem manter.

`adapters` ficou em 85% e não nos 70% que uma "camada fina" costuma
justificar: os adaptadores de saída deste projeto carregam decisões de
verdade — derivação de semente, travamento otimista, deduplicação de ação —
e essas não são glue code.

## Pirâmide

``` text
        e2e            poucos, lentos, caros
      ───────
     integração        adaptadores contra Postgres real
    ─────────────
  regra de jogo        a grande maioria — rápidos, sem I/O
─────────────────────
```

O PRD §29 já estabelece isso. A consequência prática: se um teste de regra
precisar de Spring, de banco ou de rede, **o teste está no lugar errado ou a
regra vazou**. Trate isso como defeito de arquitetura, não como
inconveniência de teste.

## Ferramentas

| Uso | Ferramenta |
|-----|------------|
| Framework | JUnit 5 |
| Asserções | AssertJ |
| Property-based | jqwik |
| Dublês | escritos à mão no `core`; Mockito só nos adaptadores |
| Integração | Testcontainers (PostgreSQL) |
| Arquitetura | ArchUnit |
| Cobertura | JaCoCo |
| Mutação | PIT (opcional, no `core`) |

**Mockito não entra no `core`.** As portas são poucas e pequenas; um dublê
manual de `FonteDeAleatoriedade` ou de `RepositorioDePartidas` é mais
legível, mais rápido e não quebra quando a assinatura muda de forma
irrelevante. Mock de domínio próprio é sinal de desenho ruim.

**Ressalva sobre o jqwik:** o projeto está em modo de manutenção declarado —
sem desenvolvimento de novas funcionalidades. Continua adequado, porque
property-based testing é um conceito estável e a biblioteca é madura, mas a
dependência fica marcada para revisão se o JUnit 6 passar a oferecer
equivalente nativo.

## Jogar a partida inteira no teste

Cada jogo tem um teste que joga **do primeiro lance ao vencedor**, guiado
pelo próprio motor: pega `acoesDisponiveis`, escolhe uma, aplica, repete. Ele
prova três coisas de uma vez — que existe sempre pelo menos uma ação legal
enquanto a partida está aberta, que toda ação oferecida é aceita por
`validar`, e que a partida termina.

Uma lição desse teste vale registro. A primeira versão do bot do UNO escolhia
sempre a primeira ação da lista, e `AcusarUno` vem antes das jogadas. O
resultado é que toda vez que alguém chegava a uma carta, levava mais duas — e
**ninguém jamais vencia**. O código estava certo; a estratégia é que precisava
querer terminar a partida. O bot hoje declara, joga, compra e passa, nessa
ordem, e as acusações são exercitadas por testes próprios.

Junto com o ciclo completo, cada jogo tem um teste de **conservação**: as 16
cartas do Love Letter e as 108 do UNO são contadas a cada jogada, somando
mãos, monte e descarte. É a forma mais barata de pegar uma carta duplicada ou
sumida numa refatoração.

## Dublês do core

Vivem em `core/src/test/java/.../duble/` e são a infraestrutura que torna os
testes determinísticos:

| Dublê | Papel |
|-------|-------|
| `AleatoriedadeRoteirizada` | devolve exatamente os valores que o teste declarou; reclama se o código sortear mais vezes que o previsto |
| `FabricaRoteirizada` | entrega a fonte roteirizada e registra os passos de derivação pedidos |
| `RepositorioDeTeste` | armazenamento em memória com log de eventos |
| `PublicadorDeTeste` | caixa de entrada por jogador, para provar o RNF-30 |
| `CatalogoFixo`, `IdentidadeFixa` | catálogo e identidade previsíveis |
| `JogoDeContagem` | o menor jogo capaz de exercitar todo o contrato da SPI |

`JogoDeContagem` merece explicação: não é rascunho de Ludo. É um jogo mínimo
com contagem pública, segredo por jogador e evento privado, escrito para
provar que o core funciona **antes de qualquer jogo real existir**. Se o core
depender de alguma peculiaridade do Ludo, esse dublê expõe o problema.

## TDD

Ciclo padrão: teste que falha → implementação mínima → refatoração.

Para regra de jogo, o teste vem da regra escrita em português antes do
código:

``` java
@Test
void peca_na_base_so_sai_com_seis() {
    var rng = AleatoriedadeRoteirizada.de(3);
    var estado = ludo.estadoInicial(config4, jogadores, rng);

    var resultado = ludo.validar(estado, new SairDaBase(JOGADOR_1, PECA_1));

    assertThat(resultado).isInstanceOf(ResultadoDeValidacao.Recusada.class);
}
```

`AleatoriedadeRoteirizada` é o que torna isso possível: o teste declara o que
o dado vai dar. Nenhum teste de regra pode ser não determinístico
([ADR-0006](adr/0006-autoridade-e-aleatoriedade.md)).

## Property-based testing

Regras de jogo têm invariantes que valem para *toda* sequência válida de
ações. Escrever exemplos um a um não cobre isso; gerar sequências cobre.

Invariantes que devem existir por jogo:

- **Conservação.** O número de cartas em mãos + deck + descarte é constante.
  O número de peças por jogador é constante.
- **Terminação.** Toda sequência de ações válidas termina em `Encerrado`
  dentro de um limite de turnos.
- **Legalidade.** Nenhuma ação listada por `acoesDisponiveis` é rejeitada por
  `validar`.
- **Determinismo.** Mesma semente + mesma sequência de ações ⇒ mesmo estado
  final.
- **Não vazamento.** Para todo estado alcançável e todo par de jogadores
  A ≠ B, `visaoDe(estado, A)` não contém nenhum campo privado de B, e
  `eventoVisivelPara(estado, evento, A)` não devolve evento privado de B.

O último merece destaque: é o teste que protege o RNF-30, e o mais fácil de
quebrar numa refatoração inocente de serialização. Deve existir para cada
jogo, gerado sobre estados aleatórios, não sobre três exemplos.

## O javac é a autoridade

O ambiente onde este código é escrito nem sempre tem Maven nem as bibliotecas
de teste. Quando não tem, a saída é compilar com **ECJ** e rodar a suíte
contra **dublês de JUnit e AssertJ** escritos à mão. Funciona, e já pegou
defeito real — mas produz uma categoria de erro específica que vale conhecer.

**Regra:** código validado só por esse caminho está **não verificado** até
passar num `mvn verify`. O relatório de uma sessão deve dizer isso com essas
palavras.

### O caso concreto

Este teste passou no ambiente de desenvolvimento e não compilou na máquina do
autor:

``` java
assertThat(entradas).containsExactly(caso.getValue().toArray());
```

O diagnóstico intuitivo — "o ECJ é mais permissivo que o javac" — está
**errado**, e foi verificado. Com a assinatura real do AssertJ, o ECJ recusa
exatamente como o javac:

```
The method containsExactly(Integer...) in the type ListAssert<Integer>
is not applicable for the arguments (Object[])
```

A causa era o **dublê**, não o compilador. O AssertJ de verdade declara
`ListAssert<ELEMENT>.containsExactly(ELEMENT... values)`; o dublê declarava
`containsExactly(Object... valores)`. `List<Integer>.toArray()` devolve
`Object[]`, que serve para o dublê e não serve para o real.

Correção, preservando a intenção do teste:

``` java
assertThat(entradas).containsExactly(caso.getValue().toArray(new Integer[0]));
```

### O que isso ensina

O elo fraco não é o compilador alternativo — é **qualquer dublê com assinatura
mais frouxa que a da biblioteca real**. Genéricos são o ponto exato onde a
frouxidão passa despercebida, porque o erro só aparece na compilação contra a
API verdadeira, nunca em tempo de execução.

Ao usar esse caminho de verificação:

- prefira passar **elementos** a varargs, nunca arrays: `containsExactly(a, b)`
  em vez de `containsExactly(lista.toArray())`;
- se precisar de array, tipe-o: `toArray(new Integer[0])`;
- desconfie de `extracting`, `allMatch`, `anyMatch` e `noneMatch`, que no
  AssertJ real são genéricos sobre o tipo do elemento;
- limpe imports que a IDE deixa para trás — um `import` não usado passa no
  `mvn verify` e quebra a compilação contra o dublê, que não tem a classe.

## Testes de arquitetura

Ver [ARCHITECTURE.md](ARCHITECTURE.md#fronteiras-verificadas-no-build).
São testes JUnit comuns; rodam em cada build; falham o build. Estão em dois
lugares de propósito: o do `core` falha cedo, o do módulo `arquitetura` cobre
o repositório inteiro.

## Testes de API

Conforme PRD §9.2: um cliente HTTP percorre criar → entrar → iniciar →
consultar → agir → receber evento → reconectar. Rodam contra a aplicação
completa com Testcontainers. Ficam para quando o `bootstrap` voltar
([ADR-0008](adr/0008-adiar-o-bootstrap-spring.md)); hoje o equivalente é o
teste dos casos de uso do `core` contra dublês.

Um teste obrigatório nesse nível: **reenviar a mesma ação com o mesmo
`actionId`** e verificar que o estado não mudou duas vezes (RNF-22). Esse já
existe, em `SubmeterAcaoServiceTest`.

## O cliente CLI como ferramenta de teste

O CLI (PRD §8) não é só conveniência: é a prova executável de que o core roda
sem UI. Deve existir um teste que joga uma partida completa de Ludo via CLI,
do início ao vencedor, com semente fixa.

## O que não testar

- Getters, accessors de `record`, DTOs.
- Configuração de framework.
- Bibliotecas de terceiros.

Cobertura é indicador, não meta. Cobertura de 85% com testes que só executam
código sem afirmar nada é pior que 60% honestos — por isso o PIT existe como
verificação ocasional da qualidade real da suíte.

## Como rodar

``` bash
cd api
mvn verify                       # compila, testa, cobertura e arquitetura
mvn -pl core test                # só o core, ciclo rápido de TDD
mvn -pl games/uno test           # só um jogo
```

Sem Spring no caminho ([ADR-0008](adr/0008-adiar-o-bootstrap-spring.md)), o
build tem três dependências — JUnit, AssertJ e ArchUnit — e a suíte roda em
segundos.
