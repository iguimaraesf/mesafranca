package br.com.mesafranca.games.loveletter;

import br.com.mesafranca.core.domain.IdJogador;
import br.com.mesafranca.core.domain.Visao;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Projeção do Love Letter para um jogador.
 *
 * <p>Note o que <strong>não</strong> está aqui: a mão dos outros. O que sai é
 * quantas cartas cada um tem, não quais. Este record é a prova executável do
 * RNF-30 — se um dia alguém acrescentar um campo com a mão alheia, o teste de
 * não vazamento reprova.
 */
public record LoveLetterVisao(
        List<IdJogador> assentos,
        List<Carta> minhaMao,
        Map<IdJogador, Integer> tamanhoDaMao,
        Map<IdJogador, List<Carta>> descartes,
        Set<IdJogador> protegidos,
        Set<IdJogador> eliminados,
        int cartasNoBaralho,
        IdJogador jogadorDaVez,
        Fase fase,
        List<IdJogador> vencedores,
        boolean encerrada) implements Visao {

    public LoveLetterVisao {
        assentos = List.copyOf(assentos);
        minhaMao = List.copyOf(minhaMao);
        tamanhoDaMao = Map.copyOf(tamanhoDaMao);
        descartes = Map.copyOf(descartes);
        protegidos = Set.copyOf(protegidos);
        eliminados = Set.copyOf(eliminados);
        vencedores = List.copyOf(vencedores);
        Objects.requireNonNull(jogadorDaVez, "jogadorDaVez");
        Objects.requireNonNull(fase, "fase");
    }
}
