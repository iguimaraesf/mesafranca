package br.com.mesafranca.core.port.in;

import br.com.mesafranca.core.domain.IdPartida;
import java.util.Objects;

/** Cria o estado inicial e passa a partida para EM_ANDAMENTO (RF-14, RF-20). */
public interface IniciarPartida {

    Resposta iniciar(Comando comando);

    record Comando(IdPartida partida) {
        public Comando {
            Objects.requireNonNull(partida, "partida");
        }
    }

    sealed interface Resposta permits Iniciada, Recusado {
    }

    record Iniciada(long versao) implements Resposta {
    }

    record Recusado(String motivo) implements Resposta {
        public Recusado {
            if (motivo == null || motivo.isBlank()) {
                throw new IllegalArgumentException("recusa exige motivo");
            }
        }
    }
}
