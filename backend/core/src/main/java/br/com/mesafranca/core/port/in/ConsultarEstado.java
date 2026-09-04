package br.com.mesafranca.core.port.in;

import br.com.mesafranca.core.domain.IdJogador;
import br.com.mesafranca.core.domain.IdPartida;
import br.com.mesafranca.core.domain.Resultado;
import br.com.mesafranca.core.domain.Visao;
import java.util.Objects;

/** Devolve o estado ja filtrado pela visibilidade do jogador (RF-21, RNF-30). */
public interface ConsultarEstado {

    Resposta consultar(Consulta consulta);

    record Consulta(IdPartida partida, IdJogador jogador) {
        public Consulta {
            Objects.requireNonNull(partida, "partida");
            Objects.requireNonNull(jogador, "jogador");
        }
    }

    record Resposta(Visao visao, Resultado resultado, long ultimaSequencia) {
        public Resposta {
            Objects.requireNonNull(visao, "visao");
            Objects.requireNonNull(resultado, "resultado");
        }
    }
}
