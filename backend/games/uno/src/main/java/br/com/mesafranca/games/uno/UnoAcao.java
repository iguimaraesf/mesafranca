package br.com.mesafranca.games.uno;

import br.com.mesafranca.core.domain.Acao;
import br.com.mesafranca.core.domain.IdJogador;
import java.util.Objects;
import java.util.Optional;

/**
 * Hierarquia selada de ações do UNO.
 *
 * <p>Duas delas — {@link DeclararUno} e {@link AcusarUno} — valem <strong>fora
 * do turno</strong>. É o caso que o Ludo e o Love Letter não produzem, e que
 * obriga o motor a não confundir "ação válida" com "ação de quem está
 * jogando".
 */
public sealed interface UnoAcao extends Acao
        permits UnoAcao.JogarCarta, UnoAcao.Comprar, UnoAcao.PassarVez,
                UnoAcao.DeclararUno, UnoAcao.AcusarUno {

    record JogarCarta(IdJogador autor, CartaUno carta, Optional<Cor> corEscolhida)
            implements UnoAcao {
        public JogarCarta {
            Objects.requireNonNull(autor, "autor");
            Objects.requireNonNull(carta, "carta");
            Objects.requireNonNull(corEscolhida, "corEscolhida");
        }

        public static JogarCarta de(IdJogador autor, CartaUno carta) {
            return new JogarCarta(autor, carta, Optional.empty());
        }

        public static JogarCarta escolhendo(IdJogador autor, CartaUno carta, Cor cor) {
            return new JogarCarta(autor, carta, Optional.of(cor));
        }
    }

    record Comprar(IdJogador autor) implements UnoAcao {
        public Comprar {
            Objects.requireNonNull(autor, "autor");
        }
    }

    record PassarVez(IdJogador autor) implements UnoAcao {
        public PassarVez {
            Objects.requireNonNull(autor, "autor");
        }
    }

    /** Fora de turno: qualquer jogador com uma carta só pode se declarar. */
    record DeclararUno(IdJogador autor) implements UnoAcao {
        public DeclararUno {
            Objects.requireNonNull(autor, "autor");
        }
    }

    /** Fora de turno: acusa quem ficou com uma carta e não declarou. */
    record AcusarUno(IdJogador autor, IdJogador acusado) implements UnoAcao {
        public AcusarUno {
            Objects.requireNonNull(autor, "autor");
            Objects.requireNonNull(acusado, "acusado");
        }
    }
}
