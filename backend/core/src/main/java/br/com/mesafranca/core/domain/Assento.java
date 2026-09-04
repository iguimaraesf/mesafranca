package br.com.mesafranca.core.domain;

/** Posicao do jogador na mesa, contada a partir de zero. */
public record Assento(int indice) {

    public Assento {
        if (indice < 0) {
            throw new IllegalArgumentException("assento nao pode ser negativo: " + indice);
        }
    }
}
