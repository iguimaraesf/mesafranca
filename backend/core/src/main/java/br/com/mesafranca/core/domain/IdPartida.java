package br.com.mesafranca.core.domain;

import java.util.Objects;
import java.util.UUID;

/** Identidade de uma partida. */
public record IdPartida(UUID valor) {

    public IdPartida {
        Objects.requireNonNull(valor, "valor");
    }

    public static IdPartida de(String texto) {
        Objects.requireNonNull(texto, "texto");
        return new IdPartida(UUID.fromString(texto));
    }

    @Override
    public String toString() {
        return valor.toString();
    }
}
