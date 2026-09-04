package br.com.mesafranca.core.domain;

import java.util.Objects;

/**
 * Evento ja gravado no log da partida, com sua posicao.
 *
 * <p>A sequencia e monotonica por partida e e o que permite ao cliente pedir
 * "o que aconteceu depois de N" ao reconectar (ADR-0005).
 */
public record EventoRegistrado(long sequencia, Evento evento) {

    public EventoRegistrado {
        if (sequencia < 1) {
            throw new IllegalArgumentException("sequencia comeca em 1: " + sequencia);
        }
        Objects.requireNonNull(evento, "evento");
    }
}
