package br.com.mesafranca.core.domain;

/**
 * Intencao de um jogador. O core nao sabe o que ela significa.
 *
 * <p><strong>Por que esta interface nao e {@code sealed}.</strong> Selar
 * exigiria que o core listasse, em {@code permits}, todas as acoes de todos
 * os jogos - ou seja, o core conheceria Ludo, Love Letter e UNO, exatamente o
 * que o PRD secao 11.1 proibe e o RNF-50 impede.
 *
 * <p>A exaustividade que a ADR-0002 usa como argumento acontece um nivel
 * abaixo: cada jogo sela a <em>sua</em> hierarquia, e o {@code switch} sobre
 * ela e verificado pelo compilador.
 *
 * <pre>{@code
 * sealed interface LudoAcao extends Acao permits RolarDado, MoverPeca {}
 * }</pre>
 */
public interface Acao {

    /** Quem esta pedindo. Validar se ele pode e trabalho do jogo. */
    IdJogador autor();
}
