package br.com.mesafranca.core.port.in;

import br.com.mesafranca.core.domain.Acao;
import br.com.mesafranca.core.domain.EventoRegistrado;
import br.com.mesafranca.core.domain.IdAcao;
import br.com.mesafranca.core.domain.IdPartida;
import java.util.List;
import java.util.Objects;

/**
 * Recebe a intencao de um jogador, valida e resolve (RF-22 a RF-25).
 *
 * <p>E o caso de uso central da plataforma e o unico lugar onde o estado da
 * partida avanca.
 */
public interface SubmeterAcao {

    Resposta submeter(Comando comando);

    record Comando(IdPartida partida, IdAcao idAcao, Acao acao) {
        public Comando {
            Objects.requireNonNull(partida, "partida");
            Objects.requireNonNull(idAcao, "idAcao");
            Objects.requireNonNull(acao, "acao");
        }
    }

    sealed interface Resposta permits Aplicada, Recusada, Repetida {
    }

    record Aplicada(List<EventoRegistrado> eventos, long versao) implements Resposta {
        public Aplicada {
            eventos = List.copyOf(Objects.requireNonNullElse(eventos, List.of()));
        }
    }

    record Recusada(String motivo) implements Resposta {
        public Recusada {
            if (motivo == null || motivo.isBlank()) {
                throw new IllegalArgumentException("recusa exige motivo");
            }
        }
    }

    /** A mesma acao ja havia sido processada. Reenvio por rede, nao jogada nova (RNF-22). */
    record Repetida() implements Resposta {
    }
}
