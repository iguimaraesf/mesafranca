package br.com.mesafranca.core.spi;

import br.com.mesafranca.core.domain.Acao;
import br.com.mesafranca.core.domain.ConfiguracaoDePartida;
import br.com.mesafranca.core.domain.ConfiguracaoSuportada;
import br.com.mesafranca.core.domain.EstadoDeJogo;
import br.com.mesafranca.core.domain.Evento;
import br.com.mesafranca.core.domain.IdJogador;
import br.com.mesafranca.core.domain.IdentificadorDeJogo;
import br.com.mesafranca.core.domain.Jogador;
import br.com.mesafranca.core.domain.Resultado;
import br.com.mesafranca.core.domain.ResultadoDeValidacao;
import br.com.mesafranca.core.domain.Transicao;
import br.com.mesafranca.core.domain.Visao;
import br.com.mesafranca.core.port.out.FonteDeAleatoriedade;
import java.util.List;
import java.util.Optional;

/**
 * Contrato que um jogo implementa para existir na plataforma (ADR-0001).
 *
 * <p>Esta e a fronteira que sustenta o RNF-50: acrescentar um jogo e
 * acrescentar uma implementacao desta interface e registra-la via
 * {@code ServiceLoader}. Nenhuma linha do core muda.
 *
 * <p>Implementacoes devem ser <strong>sem estado</strong>: todo o estado da
 * partida trafega por parametro e por retorno. Uma unica instancia atende
 * todas as partidas simultaneas.
 */
public interface DefinicaoDeJogo {

    IdentificadorDeJogo identificador();

    ConfiguracaoSuportada configuracaoSuportada();

    EstadoDeJogo estadoInicial(
            ConfiguracaoDePartida configuracao,
            List<Jogador> jogadores,
            FonteDeAleatoriedade aleatoriedade);

    /** O que este jogador pode fazer agora. Vazio quando nao e a vez dele. */
    List<Acao> acoesDisponiveis(EstadoDeJogo estado, IdJogador jogador);

    /** Veredito sem efeito colateral. Nunca lanca excecao por acao invalida. */
    ResultadoDeValidacao validar(EstadoDeJogo estado, Acao acao);

    /** Chamado somente apos {@link #validar} aprovar. Devolve estado novo. */
    Transicao aplicar(EstadoDeJogo estado, Acao acao, FonteDeAleatoriedade aleatoriedade);

    /** Projecao do estado para um jogador. So o jogo sabe o que e segredo (RNF-30). */
    Visao visaoDe(EstadoDeJogo estado, IdJogador jogador);

    Resultado resultado(EstadoDeJogo estado);

    /**
     * Versao do evento visivel para este jogador, ou vazio se ele nao deve
     * sequer saber que o evento ocorreu.
     *
     * <p>O padrao trata todo evento como publico, que e o caso de Ludo e de
     * UNO. Jogos de informacao oculta - Love Letter - sobrescrevem.
     */
    default Optional<Evento> eventoVisivelPara(
            EstadoDeJogo estado, Evento evento, IdJogador jogador) {
        return Optional.of(evento);
    }
}
