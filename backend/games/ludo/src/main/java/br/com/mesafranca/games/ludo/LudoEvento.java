package br.com.mesafranca.games.ludo;

import br.com.mesafranca.core.domain.Evento;
import br.com.mesafranca.core.domain.IdJogador;

/**
 * Fatos do Ludo. Todos públicos: no Ludo não há informação oculta, e é por
 * isso que ele não exercita {@code eventoVisivelPara}.
 */
public sealed interface LudoEvento extends Evento
        permits LudoEvento.DadoRolado, LudoEvento.PecaSaiuDaBase, LudoEvento.PecaMovida,
                LudoEvento.PecaCapturada, LudoEvento.PecaChegou, LudoEvento.VezPassada,
                LudoEvento.PartidaVencida {

    record DadoRolado(IdJogador jogador, int valor) implements LudoEvento {
    }

    record PecaSaiuDaBase(IdJogador jogador, int peca, int casa) implements LudoEvento {
    }

    record PecaMovida(IdJogador jogador, int peca, int de, int para) implements LudoEvento {
    }

    record PecaCapturada(IdJogador dono, int peca, IdJogador capturador) implements LudoEvento {
    }

    record PecaChegou(IdJogador jogador, int peca) implements LudoEvento {
    }

    record VezPassada(IdJogador de, IdJogador para) implements LudoEvento {
    }

    record PartidaVencida(IdJogador vencedor) implements LudoEvento {
    }
}
