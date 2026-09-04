package br.com.mesafranca.core.domain;

import java.util.Objects;

/** Participante de uma partida, ja sentado a mesa. */
public record Jogador(IdJogador id, Assento assento, TipoDeJogador tipo) {

    public Jogador {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(assento, "assento");
        Objects.requireNonNull(tipo, "tipo");
    }

    public static Jogador humano(IdJogador id, Assento assento) {
        return new Jogador(id, assento, TipoDeJogador.HUMANO);
    }
}
