package br.com.mesafranca.games.ludo;

import br.com.mesafranca.core.domain.Acao;
import br.com.mesafranca.core.domain.IdJogador;
import java.util.Objects;

/**
 * Hierarquia selada de ações do Ludo.
 *
 * <p>Selar aqui, e não no core, é o que permite {@code switch} exaustivo sem
 * {@code default} sem obrigar o core a conhecer este jogo.
 */
public sealed interface LudoAcao extends Acao
        permits LudoAcao.RolarDado, LudoAcao.MoverPeca, LudoAcao.PassarVez {

    record RolarDado(IdJogador autor) implements LudoAcao {
        public RolarDado {
            Objects.requireNonNull(autor, "autor");
        }
    }

    record MoverPeca(IdJogador autor, int peca) implements LudoAcao {
        public MoverPeca {
            Objects.requireNonNull(autor, "autor");
            if (peca < 0 || peca >= LudoEstado.PECAS_POR_JOGADOR) {
                throw new IllegalArgumentException("peca inexistente: " + peca);
            }
        }
    }

    /** Só é válida quando o dado já foi rolado e nenhuma peça pode se mexer. */
    record PassarVez(IdJogador autor) implements LudoAcao {
        public PassarVez {
            Objects.requireNonNull(autor, "autor");
        }
    }
}
