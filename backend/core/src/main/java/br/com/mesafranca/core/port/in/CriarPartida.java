package br.com.mesafranca.core.port.in;

import br.com.mesafranca.core.domain.IdPartida;
import br.com.mesafranca.core.domain.IdentificadorDeJogo;
import java.util.Map;
import java.util.Objects;

/** Abre uma partida vazia para um jogo do catalogo (RF-10). */
public interface CriarPartida {

    Resposta criar(Comando comando);

    record Comando(IdentificadorDeJogo jogo, int numeroDeJogadores, Map<String, String> opcoes) {
        public Comando {
            Objects.requireNonNull(jogo, "jogo");
            opcoes = Map.copyOf(Objects.requireNonNullElse(opcoes, Map.of()));
        }

        public static Comando de(IdentificadorDeJogo jogo, int numeroDeJogadores) {
            return new Comando(jogo, numeroDeJogadores, Map.of());
        }
    }

    sealed interface Resposta permits Criada, Recusado {
    }

    record Criada(IdPartida partida) implements Resposta {
        public Criada {
            Objects.requireNonNull(partida, "partida");
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
