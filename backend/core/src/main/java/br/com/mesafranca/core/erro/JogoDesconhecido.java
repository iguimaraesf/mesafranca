package br.com.mesafranca.core.erro;

import br.com.mesafranca.core.domain.IdentificadorDeJogo;

/** O catalogo nao conhece o jogo pedido. */
public class JogoDesconhecido extends RuntimeException {

    public JogoDesconhecido(IdentificadorDeJogo jogo) {
        super("jogo nao registrado no catalogo: " + jogo);
    }
}
