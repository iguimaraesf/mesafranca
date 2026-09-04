package br.com.mesafranca.core.domain;

/** Identidade de um jogador, humano ou nao. */
public record IdJogador(String valor) {

    public IdJogador {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("id de jogador nao pode ser vazio");
        }
        valor = valor.trim();
    }

    @Override
    public String toString() {
        return valor;
    }
}
