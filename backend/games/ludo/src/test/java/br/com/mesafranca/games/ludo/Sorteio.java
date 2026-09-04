package br.com.mesafranca.games.ludo;

import br.com.mesafranca.core.port.out.FonteDeAleatoriedade;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Fonte de acaso para teste, em dois modos.
 *
 * <p>{@link #comSemente(long)} produz uma sequencia deterministica, boa para
 * jogar uma partida inteira sem escrever cada valor. {@link #roteirizado(int...)}
 * devolve exatamente o que o teste mandou, para provar uma regra especifica.
 *
 * <p>Esta classe esta duplicada nos tres modulos de jogo. A duplicacao some
 * quando o {@code core} publicar um {@code test-jar} - pendencia registrada em
 * docs/ARCHITECTURE.md.
 */
final class Sorteio implements FonteDeAleatoriedade {

    private final long semente;
    private final Deque<Integer> roteiro = new ArrayDeque<>();
    private long estado;

    private Sorteio(long semente, int... valores) {
        this.semente = semente;
        this.estado = semente;
        for (int valor : valores) {
            roteiro.add(valor);
        }
    }

    static Sorteio comSemente(long semente) {
        return new Sorteio(semente);
    }

    static Sorteio roteirizado(int... valores) {
        return new Sorteio(7L, valores);
    }

    @Override
    public int inteiro(int minimo, int maximo) {
        if (minimo > maximo) {
            throw new IllegalArgumentException("intervalo invalido");
        }
        if (!roteiro.isEmpty()) {
            int valor = roteiro.poll();
            if (valor < minimo || valor > maximo) {
                throw new IllegalStateException(
                        "valor roteirizado " + valor + " fora de [" + minimo + ", " + maximo + "]");
            }
            return valor;
        }
        estado += 0x9E3779B97F4A7C15L;
        long z = estado;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        z = z ^ (z >>> 31);
        long amplitude = (long) maximo - minimo + 1L;
        return (int) (minimo + Math.floorMod(z, amplitude));
    }

    @Override
    public long semente() {
        return semente;
    }

    boolean roteiroConsumido() {
        return roteiro.isEmpty();
    }
}
