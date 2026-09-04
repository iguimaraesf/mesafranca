package br.com.mesafranca.core.domain;

/**
 * Natureza do participante.
 *
 * <p>O jogo nao consulta este tipo: um bot joga pelo mesmo contrato de um
 * humano (PRD secao 30). Ele existe para lobby, reconexao e estatistica.
 */
public enum TipoDeJogador {
    HUMANO,
    CONVIDADO,
    BOT
}
