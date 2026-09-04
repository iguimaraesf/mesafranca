# ADR-0005 --- HTTP para comandos, WebSocket para eventos

**Status:** Aceita
**Data:** 2026-08-31

## Contexto

O PRD §19 indica HTTP para comandos e WebSocket "ou mecanismo equivalente"
para eventos, deixando a escolha definitiva como decisão técnica
posterior. Esta ADR fecha a questão.

Carga real do problema: um jogo por turnos produz poucos eventos por
minuto por partida, todos pequenos, e a tolerância a latência é da ordem de
centenas de milissegundos (RNF-02). Isso elimina de saída qualquer
argumento por protocolos de baixa latência do tipo usado em jogos de ação.

## Alternativas consideradas

### A. HTTP + polling

**Prós:** o mais simples possível; funciona em qualquer rede e atrás de
qualquer proxy.
**Contras:** latência ruim ou custo alto (é preciso escolher um dos dois);
tráfego constante mesmo com a partida parada. Ruim para bateria de celular
--- relevante para o modo mesa híbrida.

### B. HTTP para comandos + Server-Sent Events para eventos

**Prós:** unidirecional é exatamente o formato do problema (eventos só
descem); reconexão automática com `Last-Event-ID` já faz parte do
protocolo, o que resolve boa parte do RF-28 de graça; é HTTP puro,
atravessa qualquer infraestrutura.
**Contras:** limite de conexões por domínio em HTTP/1.1 (irrelevante sobre
HTTP/2); suporte fraco em algumas bibliotecas de cliente não-web ---
notadamente no Unity, onde não há suporte nativo.

### C. HTTP para comandos + WebSocket para eventos

**Prós:** bidirecional; suporte maduro em Unity, navegador, Java e
TypeScript; caminho pronto caso surja necessidade de canal de baixa
latência (chat, presença, arrastar peça em tempo real).
**Contras:** protocolo com estado --- exige gerenciar heartbeat, reconexão e
backpressure manualmente; alguns proxies corporativos atrapalham.

### D. WebSocket para tudo, inclusive comandos

**Prós:** um único canal.
**Contras:** perde-se toda a infraestrutura de HTTP --- cache, códigos de
status, ferramentas, teste com `curl`, documentação OpenAPI. E o requisito
de idempotência (RNF-22) fica mais difícil de raciocinar sem semântica de
requisição/resposta.

## Decisão

**Opção C: HTTP para comandos e consultas, WebSocket para eventos.**

SSE (B) é tecnicamente o encaixe mais elegante para este problema --- o fluxo
é genuinamente unidirecional --- e sua reconexão nativa seria um presente
para o RF-28. Perde por um motivo prático e não por mérito: o cliente de
produto é Unity (ADR-0003), onde WebSocket tem suporte maduro e SSE não.
Manter dois mecanismos de evento diferentes, um para web e outro para
Unity, custaria mais do que o benefício.

O ponto que torna essa escolha barata é que **ela não vaza para dentro do
hexágono**. REST e WebSocket são dois adaptadores primários sobre as mesmas
portas de entrada (PRD §44.2). Trocar o transporte é trocar adaptador ---
literalmente o RNF-51.

### Contrato

- Comandos e consultas: REST, documentado em **OpenAPI**.
- Eventos: WebSocket, documentado em **AsyncAPI**.
- SDKs de cliente TypeScript e C# **gerados** a partir desses contratos.

### Regras de operação

1. **Toda ação carrega um `actionId` gerado pelo cliente** (RNF-22). O
   servidor deduplica. Sem isso, um reenvio de rede pode rolar o dado duas
   vezes --- e essa é a falha mais provável do sistema inteiro.
2. **Todo evento carrega `seq` monotônico por partida.** Na reconexão o
   cliente informa o último `seq` visto e recebe o que falta, ou um
   snapshot se a lacuna for grande demais.
3. **O WebSocket não é fonte de verdade.** Se um evento se perder, o
   cliente busca o estado por HTTP. O canal em tempo real é uma otimização
   de latência sobre um sistema que funciona sem ele --- essa propriedade é
   o que torna o cliente CLI possível.
4. **Heartbeat a cada 30 s**, com reconexão exponencial no cliente.
5. **A visão enviada é sempre filtrada por jogador** (RNF-30). A filtragem
   acontece na porta, não no adaptador --- caso contrário cada transporte
   precisaria reimplementá-la, e um deles esqueceria.

## Consequências

### Positivas

- Comandos testáveis com `curl`, o que sustenta o cliente CLI e os testes
  de API do PRD §9.2.
- Um único mecanismo de evento para web, Unity, CLI e bot.
- O sistema permanece jogável --- ainda que com latência --- se o WebSocket
  cair por completo.

### Negativas aceitas

- Heartbeat, reconexão e backpressure ficam por conta do projeto.
- Dois contratos para manter (OpenAPI e AsyncAPI).
- Conexões WebSocket com estado dificultam deploy sem interrupção; exigirá
  drenagem de conexões quando houver mais de uma instância.

## Quando revisitar

1. Se surgir necessidade de interação contínua (arrastar peça vista pelos
   outros em tempo real) --- o WebSocket já está lá; reavaliar apenas o
   formato das mensagens.
2. Se o cliente Unity for abandonado em favor de web --- SSE volta à mesa.
3. Se houver mais de uma instância --- decidir como rotear eventos entre
   elas (ver ADR-0004).
