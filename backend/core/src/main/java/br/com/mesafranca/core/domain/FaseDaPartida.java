package br.com.mesafranca.core.domain;

/** Ciclo de vida de uma partida do ponto de vista da plataforma. */
public enum FaseDaPartida {
    /** Aceita jogadores; ainda nao ha estado de jogo. */
    AGUARDANDO,
    /** Estado de jogo criado; aceita acoes. */
    EM_ANDAMENTO,
    /** Terminou; nao aceita mais acoes. */
    ENCERRADA
}
