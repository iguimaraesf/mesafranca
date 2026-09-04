package br.com.mesafranca.core.domain;

import java.util.regex.Pattern;

/**
 * Identificador estavel de um jogo no catalogo, por exemplo {@code ludo}.
 *
 * <p>E parte do contrato publico da API: uma vez publicado, nao muda.
 */
public record IdentificadorDeJogo(String valor) {

    private static final Pattern FORMATO = Pattern.compile("[a-z][a-z0-9-]{1,31}");

    public IdentificadorDeJogo {
        if (valor == null || !FORMATO.matcher(valor).matches()) {
            throw new IllegalArgumentException(
                    "identificador de jogo invalido: " + valor
                            + " (esperado minusculas, digitos e hifen, de 2 a 32 caracteres)");
        }
    }

    @Override
    public String toString() {
        return valor;
    }
}
