package br.com.mesafranca.games.loveletter;

import br.com.mesafranca.core.domain.Acao;
import br.com.mesafranca.core.domain.IdJogador;
import java.util.Objects;
import java.util.Optional;

/** Hierarquia selada de ações do Love Letter. */
public sealed interface LoveLetterAcao extends Acao
        permits LoveLetterAcao.ComprarCarta, LoveLetterAcao.JogarCarta {

    record ComprarCarta(IdJogador autor) implements LoveLetterAcao {
        public ComprarCarta {
            Objects.requireNonNull(autor, "autor");
        }
    }

    /**
     * @param alvo obrigatório para cartas com efeito dirigido, vazio quando
     *     todos os demais estão protegidos ou eliminados
     * @param palpite só para a Guarda
     */
    record JogarCarta(
            IdJogador autor,
            Carta carta,
            Optional<IdJogador> alvo,
            Optional<Carta> palpite) implements LoveLetterAcao {

        public JogarCarta {
            Objects.requireNonNull(autor, "autor");
            Objects.requireNonNull(carta, "carta");
            Objects.requireNonNull(alvo, "alvo");
            Objects.requireNonNull(palpite, "palpite");
        }

        public static JogarCarta simples(IdJogador autor, Carta carta) {
            return new JogarCarta(autor, carta, Optional.empty(), Optional.empty());
        }

        public static JogarCarta mirando(IdJogador autor, Carta carta, IdJogador alvo) {
            return new JogarCarta(autor, carta, Optional.of(alvo), Optional.empty());
        }

        public static JogarCarta adivinhando(
                IdJogador autor, IdJogador alvo, Carta palpite) {
            return new JogarCarta(
                    autor, Carta.GUARDA, Optional.of(alvo), Optional.of(palpite));
        }
    }
}
