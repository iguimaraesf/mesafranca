# ADR-0007 --- Maven como ferramenta de build

**Status:** Aceita
**Data:** 2026-08-31

## Contexto

A [ADR-0002](0002-linguagem-do-backend.md) menciona "Maven multi-módulo" ao
descrever a stack, mas nunca comparou Maven com Gradle. Como o build deste
projeto carrega obrigações reais — portão de cobertura (RNF-16) e verificação
de fronteiras arquiteturais (RNF-13) — a escolha merece registro próprio.

O que o build precisa fazer aqui é modesto e bem delimitado:

- quatro módulos com dependências em linha reta (`core ← games`,
  `core ← adapters ← bootstrap`);
- compilar Java 25;
- rodar JUnit;
- JaCoCo com limite por módulo, falhando o build abaixo do mínimo;
- empacotar um jar executável do Spring Boot;
- exigir o JDK correto.

Não há geração de código, não há variantes de produto, não há build
multiplataforma, não há monorepo poliglota — o cliente web da
[ADR-0003](0003-engine-do-cliente.md) terá seu próprio ecossistema Node, sem
integração com o build Java.

## Alternativas consideradas

### A. Maven

**Prós**

- **O POM é dado, não código.** Isso limita o que o build pode virar. Num
  projeto cujo valor declarado é ter fronteiras verificáveis, um build que
  não consegue crescer lógica própria é uma vantagem, não uma limitação.
- Ciclo de vida fixo (`validate → compile → test → package → verify →
  install`), igual em qualquer projeto Maven. Quem chega sabe onde as coisas
  acontecem sem ler script nenhum.
- Documentação de referência de Spring Boot, JaCoCo, ArchUnit e Surefire usa
  Maven como exemplo primário; menos tradução, menos erro.
- `maven-enforcer-plugin` expressa "exija JDK 25" de forma declarativa.
- `dependencyManagement` com BOM importado permite gerenciar versões **sem**
  herdar do `spring-boot-starter-parent` — que é exatamente o que este
  projeto precisa para manter o `core` livre de configuração de framework.
- Estabilidade: um POM de 2016 ainda constrói hoje.

**Contras**

- XML verboso; o POM raiz deste projeto tem ~180 linhas para fazer pouco.
- Builds completos mais lentos; sem cache de build local por padrão.
- Expressar qualquer coisa fora do previsto exige plugin ou profile, o que
  é desconfortável.

### B. Gradle (Kotlin DSL)

**Prós**

- Build incremental e cache — em repositório grande, diferença de minutos.
- Kotlin DSL com autocompletar e checagem de tipo no script.
- Version catalogs (`libs.versions.toml`) são melhores que propriedades de
  POM para centralizar versões.
- Muito superior em monorepo poliglota ou build com etapas customizadas.
- `java-test-fixtures` resolveria com elegância o compartilhamento dos dublês
  de teste entre `core` e os futuros módulos de jogo — hoje isso exige
  `test-jar` no Maven, que é mais desajeitado.

**Contras**

- **O build vira código, e código cresce.** É comum um `build.gradle.kts`
  acumular lógica condicional que ninguém revisa e que, em último caso,
  consegue desligar um portão de qualidade. O risco aqui não é teórico: as
  regras de cobertura e de arquitetura deste projeto só valem se forem
  difíceis de contornar.
- Upgrades de major quebram scripts e plugins com frequência bem maior.
- Curva maior para quem não usa Gradle no dia a dia — e a equipe é uma
  pessoa, com contexto limitado para gastar em ferramenta de build.
- Vantagem de velocidade irrelevante nesta escala: quatro módulos pequenos.

### C. Bazel e afins

Descartada sem análise longa: resolve problemas de escala (build
hermético, remoto, multi-linguagem) que este projeto não tem, ao custo de
uma configuração que ninguém aqui vai manter.

## Decisão

**Maven, multi-módulo, sem herdar de `spring-boot-starter-parent`.**

O argumento decisivo não é ergonomia nem velocidade — nesses quesitos Gradle
ganha — mas **resistência à erosão**. Os RNF-13 e RNF-16 transformam duas
regras arquiteturais em portões automáticos, e o valor desses portões é
proporcional à dificuldade de removê-los sob pressão de prazo. Num POM,
baixar o mínimo de cobertura é uma linha explícita e visível em revisão. Num
build programável, o mesmo efeito pode se esconder atrás de uma condicional.

A vantagem real do Gradle que este projeto perde é `java-test-fixtures`. O
custo é conhecido e contornável: quando os módulos de jogo precisarem dos
dublês do `core` (`AleatoriedadeRoteirizada` e afins), a saída é publicar um
`test-jar` do `core` ou extrair um módulo `core-test-fixtures`. Fica
registrado como consequência aceita, não como surpresa futura.

### Configuração adotada

| Item | Escolha | Razão |
|------|---------|-------|
| Herança do parent Spring | **não** | o parent aplicaria configuração de framework ao `core` |
| BOM do Spring Boot | importado em `dependencyManagement` | versões gerenciadas sem imposição |
| Portão de cobertura | `jacoco:check` na fase `verify` | RNF-16 |
| Limite por módulo | propriedade `cobertura.linhas` / `cobertura.branches` | cada módulo eleva ou abaixa explicitamente |
| Verificação de JDK | `maven-enforcer-plugin` | build falha em JDK errado, não em runtime |
| Wrapper | `mvnw` | pendência: gerar com `mvn -N wrapper:wrapper` |

## Consequências

### Positivas

- Portões de qualidade visíveis e difíceis de contornar sem que apareça no
  diff.
- Nenhum conhecimento de ferramenta além do trivial é necessário para
  contribuir.
- `core` fica sem qualquer configuração de framework, inclusive no build.

### Negativas aceitas

- POM verboso.
- Builds completos mais lentos do que seriam com Gradle e cache.
- Compartilhar dublês de teste entre módulos exigirá `test-jar` ou um módulo
  dedicado, em vez de `java-test-fixtures`.
- Sem `mvnw` no repositório até que alguém com acesso à rede o gere.

## Quando revisitar

1. Se o repositório passar a conter build de TypeScript ou de Unity
   integrado ao mesmo pipeline — Gradle lida melhor com poliglota.
2. Se o tempo de build passar de alguns minutos.
3. Se a duplicação de dublês entre módulos de jogo se tornar incômoda antes
   do terceiro jogo — reavaliar `java-test-fixtures`.
