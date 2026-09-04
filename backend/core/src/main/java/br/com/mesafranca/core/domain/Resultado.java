package br.com.mesafranca.core.domain;

import java.util.List;
import java.util.Objects;

/**
 * Situacao de uma partida do ponto de vista das regras.
 *
 * <p>Selada: o core conhece todos os casos possiveis, entao o compilador pode
 * exigir exaustividade em quem consome.
 */
public sealed interface Resultado permits Resultado.EmAndamento, Resultado.Encerrado {

    record EmAndamento() implements Resultado {
    }

    /** Lista vazia significa empate ou partida sem vencedor. */
    record Encerrado(List<IdJogador> vencedores) implements Resultado {
        public Encerrado {
            vencedores = List.copyOf(Objects.requireNonNullElse(vencedores, List.of()));
        }
    }

    static Resultado emAndamento() {
        return new EmAndamento();
    }

    static Resultado vencidaPor(IdJogador vencedor) {
        return new Encerrado(List.of(Objects.requireNonNull(vencedor, "vencedor")));
    }

    static Resultado empatada() {
        return new Encerrado(List.of());
    }

    default boolean encerrada() {
        return this instanceof Encerrado;
    }
}
