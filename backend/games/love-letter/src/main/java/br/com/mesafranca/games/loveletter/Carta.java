package br.com.mesafranca.games.loveletter;

import java.util.ArrayList;
import java.util.List;

/** As oito cartas do baralho, com valor e quantidade. */
public enum Carta {
    GUARDA(1, 5),
    PADRE(2, 2),
    BARAO(3, 2),
    AIA(4, 2),
    PRINCIPE(5, 2),
    REI(6, 1),
    CONDESSA(7, 1),
    PRINCESA(8, 1);

    private final int valor;
    private final int quantidade;

    Carta(int valor, int quantidade) {
        this.valor = valor;
        this.quantidade = quantidade;
    }

    public int valor() {
        return valor;
    }

    public int quantidade() {
        return quantidade;
    }

    /** Precisa de alvo para ter efeito. */
    public boolean exigeAlvo() {
        return switch (this) {
            case GUARDA, PADRE, BARAO, PRINCIPE, REI -> true;
            case AIA, CONDESSA, PRINCESA -> false;
        };
    }

    /** Único caso em que o alvo pode ser o próprio jogador. */
    public boolean podeMirarEmSi() {
        return this == PRINCIPE;
    }

    public static List<Carta> baralhoCompleto() {
        List<Carta> cartas = new ArrayList<>();
        for (Carta carta : values()) {
            for (int i = 0; i < carta.quantidade(); i++) {
                cartas.add(carta);
            }
        }
        return List.copyOf(cartas);
    }
}
