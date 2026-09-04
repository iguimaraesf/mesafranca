package br.com.mesafranca.core.duble;

import br.com.mesafranca.core.port.out.FonteDeAleatoriedade;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Fonte de acaso que devolve exatamente o que o teste mandou devolver.
 *
 * <p>E o que torna possivel escrever "o dado deu 6" em vez de "o dado deu
 * alguma coisa". Sem ela, todo teste de regra seria nao deterministico
 * (ADR-0006).
 */
public final class AleatoriedadeRoteirizada implements FonteDeAleatoriedade {

    private final Deque<Integer> roteiro = new ArrayDeque<>();
    private final long semente;

    private AleatoriedadeRoteirizada(long semente, int... valores) {
        this.semente = semente;
        for (int valor : valores) {
            roteiro.add(valor);
        }
    }

    public static AleatoriedadeRoteirizada de(int... valores) {
        return new AleatoriedadeRoteirizada(42L, valores);
    }

    public static AleatoriedadeRoteirizada comSemente(long semente, int... valores) {
        return new AleatoriedadeRoteirizada(semente, valores);
    }

    @Override
    public int inteiro(int minimo, int maximo) {
        if (roteiro.isEmpty()) {
            throw new IllegalStateException(
                    "roteiro de aleatoriedade esgotado: o codigo sorteou mais vezes que o teste previu");
        }
        int valor = roteiro.poll();
        if (valor < minimo || valor > maximo) {
            throw new IllegalStateException(
                    "valor roteirizado " + valor + " fora de [" + minimo + ", " + maximo + "]");
        }
        return valor;
    }

    @Override
    public long semente() {
        return semente;
    }

    public boolean consumidoPorCompleto() {
        return roteiro.isEmpty();
    }
}
