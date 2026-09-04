package br.com.mesafranca.games.uno;

/** As quatro cores do baralho, mais o preto dos curingas. */
public enum Cor {
    VERMELHO,
    AMARELO,
    VERDE,
    AZUL,
    /** Só dos curingas: nunca é cor ativa. */
    PRETO;

    public boolean jogavel() {
        return this != PRETO;
    }
}
