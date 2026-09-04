package br.com.mesafranca.games.loveletter;

import br.com.mesafranca.core.domain.Evento;
import br.com.mesafranca.core.domain.IdJogador;
import java.util.List;
import java.util.Optional;

/**
 * Fatos do Love Letter.
 *
 * <p>Três destes carregam informação que só alguns podem ver, e o campo
 * sigiloso é sempre um {@link Optional}. Isso é proposital: o mesmo tipo de
 * evento sai <em>redigido</em> para quem não tem direito, em vez de sumir. A
 * mesa continua sabendo que alguém espiou uma mão; só não sabe o quê.
 */
public sealed interface LoveLetterEvento extends Evento
        permits LoveLetterEvento.CartaComprada, LoveLetterEvento.CartaJogada,
                LoveLetterEvento.MaoEspiada, LoveLetterEvento.MaosComparadas,
                LoveLetterEvento.MaosTrocadas, LoveLetterEvento.JogadorProtegido,
                LoveLetterEvento.JogadorEliminado, LoveLetterEvento.VezPassada,
                LoveLetterEvento.RodadaEncerrada {

    /** {@code carta} vazio para quem não é o comprador. */
    record CartaComprada(IdJogador jogador, Optional<Carta> carta) implements LoveLetterEvento {
    }

    record CartaJogada(
            IdJogador jogador,
            Carta carta,
            Optional<IdJogador> alvo,
            Optional<Carta> palpite) implements LoveLetterEvento {
    }

    /** {@code carta} só para o espião. */
    record MaoEspiada(IdJogador espiao, IdJogador alvo, Optional<Carta> carta)
            implements LoveLetterEvento {
    }

    /** As cartas só para os dois envolvidos. */
    record MaosComparadas(
            IdJogador autor,
            IdJogador alvo,
            Optional<Carta> cartaDoAutor,
            Optional<Carta> cartaDoAlvo) implements LoveLetterEvento {
    }

    record MaosTrocadas(IdJogador autor, IdJogador alvo) implements LoveLetterEvento {
    }

    record JogadorProtegido(IdJogador jogador) implements LoveLetterEvento {
    }

    record JogadorEliminado(IdJogador jogador, Carta cartaRevelada, String motivo)
            implements LoveLetterEvento {
    }

    record VezPassada(IdJogador de, IdJogador para) implements LoveLetterEvento {
    }

    record RodadaEncerrada(List<IdJogador> vencedores, String motivo) implements LoveLetterEvento {
    }
}
