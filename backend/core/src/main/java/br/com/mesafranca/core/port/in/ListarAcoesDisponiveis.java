package br.com.mesafranca.core.port.in;

import br.com.mesafranca.core.domain.Acao;
import br.com.mesafranca.core.domain.IdJogador;
import br.com.mesafranca.core.domain.IdPartida;
import java.util.List;
import java.util.Objects;

/** O que este jogador pode fazer agora (RF-26). Vazio quando nao e a vez dele. */
public interface ListarAcoesDisponiveis {

    List<Acao> listar(Consulta consulta);

    record Consulta(IdPartida partida, IdJogador jogador) {
        public Consulta {
            Objects.requireNonNull(partida, "partida");
            Objects.requireNonNull(jogador, "jogador");
        }
    }
}
