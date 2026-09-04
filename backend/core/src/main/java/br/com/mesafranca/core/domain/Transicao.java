package br.com.mesafranca.core.domain;

import java.util.List;
import java.util.Objects;

/**
 * O que {@code aplicar} devolve: o estado novo e os fatos que o produziram.
 *
 * <p>Nunca o estado antigo mutado. Estado imutavel e o que torna snapshot,
 * replay e teste triviais (PRD secao 44.4).
 */
public record Transicao(EstadoDeJogo estado, List<Evento> eventos) {

    public Transicao {
        Objects.requireNonNull(estado, "estado");
        eventos = List.copyOf(Objects.requireNonNullElse(eventos, List.of()));
    }

    public static Transicao de(EstadoDeJogo estado, Evento... eventos) {
        return new Transicao(estado, List.of(eventos));
    }
}
