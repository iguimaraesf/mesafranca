package br.com.mesafranca.core.erro;

import br.com.mesafranca.core.domain.IdPartida;

/** Nao existe partida com o identificador informado. */
public class PartidaNaoEncontrada extends RuntimeException {

    public PartidaNaoEncontrada(IdPartida id) {
        super("partida nao encontrada: " + id);
    }
}
