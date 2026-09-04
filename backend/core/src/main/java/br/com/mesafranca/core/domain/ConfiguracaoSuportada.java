package br.com.mesafranca.core.domain;

/** Faixa de jogadores que um jogo aceita. Declarada pela SPI, checada pelo core. */
public record ConfiguracaoSuportada(int minimoDeJogadores, int maximoDeJogadores) {

    public ConfiguracaoSuportada {
        if (minimoDeJogadores < 1) {
            throw new IllegalArgumentException("minimo deve ser positivo: " + minimoDeJogadores);
        }
        if (maximoDeJogadores < minimoDeJogadores) {
            throw new IllegalArgumentException(
                    "maximo (" + maximoDeJogadores + ") menor que minimo (" + minimoDeJogadores + ")");
        }
    }

    public static ConfiguracaoSuportada de(int minimo, int maximo) {
        return new ConfiguracaoSuportada(minimo, maximo);
    }

    public boolean suporta(int numeroDeJogadores) {
        return numeroDeJogadores >= minimoDeJogadores && numeroDeJogadores <= maximoDeJogadores;
    }
}
