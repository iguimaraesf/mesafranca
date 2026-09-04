# ADR-0004 --- PostgreSQL com log de eventos e snapshots

**Status:** Aceita
**Data:** 2026-08-31

## Contexto

O PRD §12 já decide PostgreSQL como banco único e adia o Redis. Esta ADR
registra essa decisão e resolve a pergunta que o PRD deixa em aberto:
**como** uma partida é persistida, dado que os requisitos exigem sobreviver
ao reinício do servidor (RNF-21), reconstrução de estado (RNF-23),
idempotência de ação (RNF-22) e replay (RF-31).

## Alternativas consideradas

### A. Só snapshot do estado atual (uma linha por partida)

**Prós:** simples, leitura em uma consulta.
**Contras:** sem histórico, sem replay (RF-31), sem auditoria de trapaça.
Uma escrita perdida corrompe a partida sem deixar rastro.

### B. Só log de eventos (event sourcing puro)

**Prós:** histórico completo, replay natural, auditoria perfeita.
**Contras:** reconstruir o estado exige reprocessar todos os eventos; e
versionar eventos antigos quando as regras mudarem é trabalho real
(upcasting). Numa partida de 200 eventos isso é irrelevante, mas o custo
conceitual é permanente.

### C. Log de eventos + snapshot periódico

**Prós:** histórico completo com leitura rápida; reconstrução parte do
último snapshot e aplica poucos eventos.
**Contras:** duas estruturas para manter consistentes.

### D. Estado apenas em memória, com persistência ao final

**Prós:** o mais rápido de todos.
**Contras:** viola RNF-21 diretamente. Um deploy no meio da noite mataria
todas as partidas em andamento.

## Decisão

**Opção C: log de eventos append-only + snapshot do estado**, ambos em
PostgreSQL, com o estado em memória servindo de cache da partida ativa.

Modelo mínimo:

``` text
game_session   (id, game_id, config, status, versao, criado_em)
game_player    (session_id, player_id, seat, tipo)   -- humano | bot | convidado
game_event     (session_id, seq, tipo, payload, criado_em)   -- append-only
game_snapshot  (session_id, seq, estado, criado_em)
action_log     (session_id, action_id, player_id, aceita, seq_resultante)
```

Quatro pontos que essa modelagem resolve:

1. **Idempotência (RNF-22).** `action_log` tem chave única em
   `(session_id, action_id)`, com o `action_id` gerado pelo cliente. Um
   reenvio após timeout de rede colide na chave e devolve o resultado
   original em vez de rolar o dado de novo. Sem isso, toda ação com
   aleatoriedade é uma bomba-relógio de rede.
2. **Reconstrução (RNF-23).** Último snapshot + eventos com `seq` maior.
3. **Concorrência.** `game_session.versao` faz travamento otimista: duas
   ações simultâneas na mesma partida, uma perde e é reprocessada. Numa
   partida por turnos isso é raro, mas acontece --- e o modo mesa híbrida
   (PRD §31), com vários dispositivos, torna mais provável.
4. **Snapshot como otimização, não como verdade.** A verdade é o log. Um
   snapshot corrompido pode ser descartado e recalculado.

**Snapshot a cada N eventos** (começar com N = 20) e sempre ao encerrar a
partida.

**Sobre o Redis:** confirmada a posição do PRD §12 de não adotá-lo. Para os
casos que normalmente o justificariam neste sistema --- cache de sessão e
pub/sub de eventos entre instâncias --- há saídas suficientes enquanto houver
uma única instância: o estado ativo fica no processo e o pub/sub é interno.
Quando houver mais de uma instância, a primeira opção é `LISTEN/NOTIFY` do
próprio PostgreSQL; Redis só entra quando isso comprovadamente não bastar.

**Sem JPA no Core.** O mapeamento é feito no adaptador de persistência, com
jOOQ ou JDBC. Entidades JPA são mutáveis e gerenciadas por sessão --- o
oposto do estado imutável decidido no PRD §44.4. O payload de evento e o
snapshot são JSONB.

## Consequências

### Positivas

- Replay, auditoria e depuração de partida "de graça".
- Reconexão e reinício de servidor resolvidos pelo mesmo mecanismo.
- Reprodução exata de bug de regra: basta a semente do RNG e o log de ações.

### Negativas aceitas

- Duas estruturas para manter coerentes.
- Payloads JSONB não têm o mesmo rigor de esquema que colunas tipadas;
  exige versionamento de evento desde o começo (campo `versao` no payload).
- Escrita por ação em vez de escrita por partida --- irrelevante nesta carga.

## Quando revisitar

1. Ao introduzir uma segunda instância --- decidir entre `LISTEN/NOTIFY` e um
   broker; só então reabrir o caso do Redis.
2. Se o volume de eventos por partida passar de alguns milhares --- ajustar N
   ou compactar o log.
3. Se o versionamento de eventos antigos começar a doer --- avaliar tratar o
   snapshot como verdade e o log como auditoria.
