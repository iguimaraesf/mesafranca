package br.com.mesafranca.games.uno;

/** Natureza da carta, que decide o efeito. */
public enum TipoDeCarta {
    NUMERO,
    PULAR,
    INVERTER,
    MAIS_DOIS,
    CORINGA,
    CORINGA_MAIS_QUATRO;

    public boolean curinga() {
        return this == CORINGA || this == CORINGA_MAIS_QUATRO;
    }

    /** Quantas cartas o próximo jogador compra por causa desta carta. */
    public int compraForcada() {
        return switch (this) {
            case MAIS_DOIS -> 2;
            case CORINGA_MAIS_QUATRO -> 4;
            case NUMERO, PULAR, INVERTER, CORINGA -> 0;
        };
    }
}
