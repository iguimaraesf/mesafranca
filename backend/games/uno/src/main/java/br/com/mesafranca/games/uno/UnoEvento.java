package br.com.mesafranca.games.uno;

import br.com.mesafranca.core.domain.Evento;
import br.com.mesafranca.core.domain.IdJogador;
import br.com.mesafranca.core.domain.OrdemDeTurno;
import java.util.List;
import java.util.Optional;

/** Fatos do UNO. Só a compra carrega segredo: o resto acontece na mesa. */
public sealed interface UnoEvento extends Evento
        permits UnoEvento.CartaJogada, UnoEvento.CartaComprada, UnoEvento.CorEscolhida,
                UnoEvento.JogadorPulado, UnoEvento.SentidoInvertido, UnoEvento.MonteRemontado,
                UnoEvento.UnoDeclarado, UnoEvento.UnoPunido, UnoEvento.VezPassada,
                UnoEvento.PartidaVencida {

    record CartaJogada(IdJogador jogador, CartaUno carta) implements UnoEvento {
    }

    /** {@code cartas} vazio para quem não é o comprador. */
    record CartaComprada(IdJogador jogador, int quantidade, Optional<List<CartaUno>> cartas)
            implements UnoEvento {
    }

    record CorEscolhida(IdJogador jogador, Cor cor) implements UnoEvento {
    }

    record JogadorPulado(IdJogador jogador) implements UnoEvento {
    }

    record SentidoInvertido(OrdemDeTurno.Sentido sentido) implements UnoEvento {
    }

    record MonteRemontado(int cartas) implements UnoEvento {
    }

    record UnoDeclarado(IdJogador jogador) implements UnoEvento {
    }

    record UnoPunido(IdJogador acusado, IdJogador acusador, int cartas) implements UnoEvento {
    }

    record VezPassada(IdJogador de, IdJogador para) implements UnoEvento {
    }

    record PartidaVencida(IdJogador vencedor) implements UnoEvento {
    }
}
