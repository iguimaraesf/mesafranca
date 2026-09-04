package br.com.mesafranca.games.ludo;

import br.com.mesafranca.core.domain.IdJogador;
import br.com.mesafranca.core.domain.Visao;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Projeção do Ludo para um jogador.
 *
 * <p>É idêntica para todos: o tabuleiro inteiro é público. A projeção existe
 * mesmo assim para que o cliente nunca receba o estado bruto — e para que o
 * dia em que houver segredo, o ponto de corte já esteja no lugar certo.
 */
public record LudoVisao(
        List<IdJogador> assentos,
        Map<IdJogador, List<Integer>> pecas,
        Map<IdJogador, Integer> entradas,
        IdJogador jogadorDaVez,
        Optional<Integer> dado,
        Optional<IdJogador> vencedor) implements Visao {

    public LudoVisao {
        assentos = List.copyOf(assentos);
        pecas = Map.copyOf(pecas);
        entradas = Map.copyOf(entradas);
        Objects.requireNonNull(jogadorDaVez, "jogadorDaVez");
        Objects.requireNonNull(dado, "dado");
        Objects.requireNonNull(vencedor, "vencedor");
    }
}
