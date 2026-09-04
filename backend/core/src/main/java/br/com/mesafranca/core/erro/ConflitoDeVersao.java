package br.com.mesafranca.core.erro;

import br.com.mesafranca.core.domain.IdPartida;

/**
 * Duas acoes tentaram alterar a mesma partida a partir da mesma versao.
 *
 * <p>Travamento otimista (ADR-0004). Raro num jogo por turnos, mas possivel
 * no modo mesa hibrida, com varios dispositivos na mesma partida.
 */
public class ConflitoDeVersao extends RuntimeException {

    public ConflitoDeVersao(IdPartida id, long versaoEsperada, long versaoEncontrada) {
        super("conflito de versao na partida " + id
                + ": esperada " + versaoEsperada + ", encontrada " + versaoEncontrada);
    }
}
