package br.com.mesafranca.core.domain;

/**
 * Veredito do jogo sobre uma acao.
 *
 * <p>Acao invalida e fluxo esperado, nao excecao: o cliente pode ser hostil
 * (PRD secao 17). Por isso o retorno e um valor, e nao um {@code throw}.
 */
public sealed interface ResultadoDeValidacao
        permits ResultadoDeValidacao.Aceita, ResultadoDeValidacao.Recusada {

    record Aceita() implements ResultadoDeValidacao {
    }

    record Recusada(String motivo) implements ResultadoDeValidacao {
        public Recusada {
            if (motivo == null || motivo.isBlank()) {
                throw new IllegalArgumentException("recusa exige motivo");
            }
        }
    }

    static ResultadoDeValidacao aceita() {
        return new Aceita();
    }

    static ResultadoDeValidacao recusada(String motivo) {
        return new Recusada(motivo);
    }

    default boolean aprovada() {
        return this instanceof Aceita;
    }
}
