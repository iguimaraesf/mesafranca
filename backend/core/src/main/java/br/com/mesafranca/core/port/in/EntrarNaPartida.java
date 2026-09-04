package br.com.mesafranca.core.port.in;

import br.com.mesafranca.core.domain.Assento;
import br.com.mesafranca.core.domain.IdJogador;
import br.com.mesafranca.core.domain.IdPartida;
import br.com.mesafranca.core.domain.TipoDeJogador;
import java.util.Objects;

/** Senta um jogador a mesa antes do inicio (RF-12). */
public interface EntrarNaPartida {

    Resposta entrar(Comando comando);

    record Comando(IdPartida partida, IdJogador jogador, TipoDeJogador tipo) {
        public Comando {
            Objects.requireNonNull(partida, "partida");
            Objects.requireNonNull(jogador, "jogador");
            Objects.requireNonNull(tipo, "tipo");
        }
    }

    sealed interface Resposta permits Sentou, Recusado {
    }

    record Sentou(Assento assento) implements Resposta {
        public Sentou {
            Objects.requireNonNull(assento, "assento");
        }
    }

    record Recusado(String motivo) implements Resposta {
        public Recusado {
            if (motivo == null || motivo.isBlank()) {
                throw new IllegalArgumentException("recusa exige motivo");
            }
        }
    }
}
