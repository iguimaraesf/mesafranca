package br.com.mesafranca.core.port.in;

import br.com.mesafranca.core.domain.EventoRegistrado;
import br.com.mesafranca.core.domain.IdJogador;
import br.com.mesafranca.core.domain.IdPartida;
import br.com.mesafranca.core.domain.Visao;
import java.util.List;
import java.util.Objects;

/**
 * Reconstroi a apresentacao de um cliente que caiu (RF-28).
 *
 * <p>Devolve a visao atual e os eventos posteriores a sequencia que o cliente
 * diz ter visto. Se a lacuna for grande, o cliente usa apenas a visao - o
 * estado autoritativo basta para redesenhar tudo.
 */
public interface Reconectar {

    Resposta reconectar(Comando comando);

    record Comando(IdPartida partida, IdJogador jogador, long ultimaSequenciaVista) {
        public Comando {
            Objects.requireNonNull(partida, "partida");
            Objects.requireNonNull(jogador, "jogador");
            if (ultimaSequenciaVista < 0) {
                throw new IllegalArgumentException(
                        "sequencia nao pode ser negativa: " + ultimaSequenciaVista);
            }
        }
    }

    record Resposta(Visao visao, List<EventoRegistrado> eventosPerdidos, long ultimaSequencia) {
        public Resposta {
            Objects.requireNonNull(visao, "visao");
            eventosPerdidos = List.copyOf(Objects.requireNonNullElse(eventosPerdidos, List.of()));
        }
    }
}
