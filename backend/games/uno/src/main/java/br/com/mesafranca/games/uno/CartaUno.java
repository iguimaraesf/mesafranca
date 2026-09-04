package br.com.mesafranca.games.uno;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Uma carta do UNO.
 *
 * <p>{@code numero} só tem sentido em cartas de número; nas demais vale
 * {@link #SEM_NUMERO}. Preferi um único record a uma hierarquia selada de
 * cinco tipos porque a diferença entre as cartas é de <em>efeito</em>, e o
 * efeito já está em {@link TipoDeCarta}.
 */
public record CartaUno(Cor cor, TipoDeCarta tipo, int numero) {

    public static final int SEM_NUMERO = -1;

    public CartaUno {
        Objects.requireNonNull(cor, "cor");
        Objects.requireNonNull(tipo, "tipo");
        if (tipo == TipoDeCarta.NUMERO && (numero < 0 || numero > 9)) {
            throw new IllegalArgumentException("numero fora de 0..9: " + numero);
        }
        if (tipo != TipoDeCarta.NUMERO && numero != SEM_NUMERO) {
            throw new IllegalArgumentException(tipo + " nao tem numero");
        }
        if (tipo.curinga() != (cor == Cor.PRETO)) {
            throw new IllegalArgumentException("curinga e preto; carta comum tem cor: " + tipo);
        }
    }

    public static CartaUno numero(Cor cor, int numero) {
        return new CartaUno(cor, TipoDeCarta.NUMERO, numero);
    }

    public static CartaUno especial(Cor cor, TipoDeCarta tipo) {
        return new CartaUno(cor, tipo, SEM_NUMERO);
    }

    public static CartaUno curinga(TipoDeCarta tipo) {
        return new CartaUno(Cor.PRETO, tipo, SEM_NUMERO);
    }

    public boolean curinga() {
        return tipo.curinga();
    }

    /**
     * Pode ser jogada sobre {@code topo}, com {@code corAtiva} valendo.
     *
     * <p>A cor ativa e não a cor do topo é o que manda: depois de um curinga,
     * o topo é preto e quem define o encaixe é a cor escolhida.
     */
    public boolean combinaCom(CartaUno topo, Cor corAtiva) {
        if (curinga()) {
            return true;
        }
        if (cor == corAtiva) {
            return true;
        }
        if (topo.curinga()) {
            return false;
        }
        if (tipo != topo.tipo()) {
            return false;
        }
        return tipo != TipoDeCarta.NUMERO || numero == topo.numero();
    }

    /** 108 cartas: 25 por cor mais oito curingas. */
    public static List<CartaUno> baralhoCompleto() {
        List<CartaUno> cartas = new ArrayList<>();
        for (Cor cor : Cor.values()) {
            if (!cor.jogavel()) {
                continue;
            }
            cartas.add(numero(cor, 0));
            for (int valor = 1; valor <= 9; valor++) {
                cartas.add(numero(cor, valor));
                cartas.add(numero(cor, valor));
            }
            for (TipoDeCarta tipo : List.of(
                    TipoDeCarta.PULAR, TipoDeCarta.INVERTER, TipoDeCarta.MAIS_DOIS)) {
                cartas.add(especial(cor, tipo));
                cartas.add(especial(cor, tipo));
            }
        }
        for (int i = 0; i < 4; i++) {
            cartas.add(curinga(TipoDeCarta.CORINGA));
            cartas.add(curinga(TipoDeCarta.CORINGA_MAIS_QUATRO));
        }
        return List.copyOf(cartas);
    }
}
