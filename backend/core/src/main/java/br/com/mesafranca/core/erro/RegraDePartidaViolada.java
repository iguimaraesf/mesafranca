package br.com.mesafranca.core.erro;

/**
 * Invariante do agregado {@code Partida} foi violada.
 *
 * <p>Nao e o mecanismo de recusa de acao - esse devolve
 * {@code ResultadoDeValidacao}. Esta excecao e rede de seguranca: se ela
 * chega ate o adaptador, ha defeito de programacao, nao entrada invalida.
 */
public class RegraDePartidaViolada extends RuntimeException {

    public RegraDePartidaViolada(String mensagem) {
        super(mensagem);
    }
}
