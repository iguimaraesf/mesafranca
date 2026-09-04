package br.com.mesafranca.games.uno;

import br.com.mesafranca.core.domain.IdJogador;
import br.com.mesafranca.core.domain.OrdemDeTurno;
import br.com.mesafranca.core.domain.Visao;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Projeção do UNO.
 *
 * <p>Sai a própria mão e o tamanho da mão alheia — que é exatamente o que se
 * vê numa mesa de verdade, e o que torna possível acusar alguém de não ter
 * dito UNO.
 */
public record UnoVisao(
        List<IdJogador> assentos,
        List<CartaUno> minhaMao,
        Map<IdJogador, Integer> tamanhoDaMao,
        CartaUno topoDoDescarte,
        Cor corAtiva,
        IdJogador jogadorDaVez,
        OrdemDeTurno.Sentido sentido,
        int cartasNoMonte,
        Set<IdJogador> declararamUno,
        Optional<IdJogador> vencedor) implements Visao {

    public UnoVisao {
        assentos = List.copyOf(assentos);
        minhaMao = List.copyOf(minhaMao);
        tamanhoDaMao = Map.copyOf(tamanhoDaMao);
        declararamUno = Set.copyOf(declararamUno);
        Objects.requireNonNull(topoDoDescarte, "topoDoDescarte");
        Objects.requireNonNull(corAtiva, "corAtiva");
        Objects.requireNonNull(jogadorDaVez, "jogadorDaVez");
        Objects.requireNonNull(sentido, "sentido");
        Objects.requireNonNull(vencedor, "vencedor");
    }
}
