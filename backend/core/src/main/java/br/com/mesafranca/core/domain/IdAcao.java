package br.com.mesafranca.core.domain;

/**
 * Identidade de uma acao, gerada pelo cliente.
 *
 * <p>E o que torna a submissao idempotente (RNF-22): um reenvio por timeout
 * de rede chega com o mesmo id e nao rola o dado duas vezes.
 */
public record IdAcao(String valor) {

    public IdAcao {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("id de acao nao pode ser vazio");
        }
        valor = valor.trim();
    }

    @Override
    public String toString() {
        return valor;
    }
}
