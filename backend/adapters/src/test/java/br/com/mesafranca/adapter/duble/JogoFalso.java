package br.com.mesafranca.adapter.duble;

import br.com.mesafranca.core.domain.Acao;
import br.com.mesafranca.core.domain.ConfiguracaoDePartida;
import br.com.mesafranca.core.domain.ConfiguracaoSuportada;
import br.com.mesafranca.core.domain.EstadoDeJogo;
import br.com.mesafranca.core.domain.IdJogador;
import br.com.mesafranca.core.domain.IdentificadorDeJogo;
import br.com.mesafranca.core.domain.Jogador;
import br.com.mesafranca.core.domain.Resultado;
import br.com.mesafranca.core.domain.ResultadoDeValidacao;
import br.com.mesafranca.core.domain.Transicao;
import br.com.mesafranca.core.domain.Visao;
import br.com.mesafranca.core.port.out.FonteDeAleatoriedade;
import br.com.mesafranca.core.spi.DefinicaoDeJogo;
import java.util.List;

/** Definicao minima, so para exercitar catalogo e controlador. */
public record JogoFalso(IdentificadorDeJogo identificador, ConfiguracaoSuportada configuracaoSuportada)
        implements DefinicaoDeJogo {

    public static JogoFalso chamado(String nome, int minimo, int maximo) {
        return new JogoFalso(
                new IdentificadorDeJogo(nome), ConfiguracaoSuportada.de(minimo, maximo));
    }

    private record EstadoVazio() implements EstadoDeJogo {
    }

    private record VisaoVazia() implements Visao {
    }

    @Override
    public EstadoDeJogo estadoInicial(
            ConfiguracaoDePartida configuracao,
            List<Jogador> jogadores,
            FonteDeAleatoriedade aleatoriedade) {
        return new EstadoVazio();
    }

    @Override
    public List<Acao> acoesDisponiveis(EstadoDeJogo estado, IdJogador jogador) {
        return List.of();
    }

    @Override
    public ResultadoDeValidacao validar(EstadoDeJogo estado, Acao acao) {
        return ResultadoDeValidacao.recusada("jogo falso nao aceita acoes");
    }

    @Override
    public Transicao aplicar(EstadoDeJogo estado, Acao acao, FonteDeAleatoriedade aleatoriedade) {
        return Transicao.de(estado);
    }

    @Override
    public Visao visaoDe(EstadoDeJogo estado, IdJogador jogador) {
        return new VisaoVazia();
    }

    @Override
    public Resultado resultado(EstadoDeJogo estado) {
        return Resultado.emAndamento();
    }
}
