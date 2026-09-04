package br.com.mesafranca.core.duble;

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
import br.com.mesafranca.core.spi.DefinicaoDeJogo;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Jogo minimo, existente apenas em teste.
 *
 * <p>Nao e Ludo nem rascunho de Ludo: e o menor jogo capaz de exercitar todo
 * o contrato da SPI - acaso, estado imutavel, recusa de acao, fim de partida,
 * informacao publica e informacao privada. Serve para provar que o core
 * funciona sem que nenhum jogo real exista ainda.
 */
public final class JogoDeContagem implements DefinicaoDeJogo {

    public static final IdentificadorDeJogo ID = new IdentificadorDeJogo("contagem");
    public static final int ALVO = 3;

    /** Contagem publica, segredo por jogador, e quem jogou por ultimo. */
    public record Estado(int contagem, Map<IdJogador, Integer> segredos, Optional<IdJogador> ultimo)
            implements EstadoDeJogo {
        public Estado {
            segredos = Map.copyOf(segredos);
        }
    }

    public record VisaoDoJogador(int contagem, Optional<Integer> meuSegredo) implements Visao {
    }

    public record Incrementar(IdJogador autor) implements Acao {
    }

    public record Sussurrar(IdJogador autor) implements Acao {
    }

    public record Incrementado(IdJogador quem, int contagem, int dado) implements Evento {
    }

    /** Evento privado: so o autor pode saber que aconteceu. */
    public record Sussurrado(IdJogador quem, int valor) implements Evento {
    }

    @Override
    public IdentificadorDeJogo identificador() {
        return ID;
    }

    @Override
    public ConfiguracaoSuportada configuracaoSuportada() {
        return ConfiguracaoSuportada.de(2, 4);
    }

    @Override
    public EstadoDeJogo estadoInicial(
            ConfiguracaoDePartida configuracao,
            List<Jogador> jogadores,
            FonteDeAleatoriedade aleatoriedade) {
        // Nao consome acaso de proposito: o teste prova que a fonte foi derivada
        // olhando os passos pedidos a fabrica, sem engessar um roteiro aqui.
        return new Estado(0, Map.of(), Optional.empty());
    }

    @Override
    public List<Acao> acoesDisponiveis(EstadoDeJogo estado, IdJogador jogador) {
        if (resultado(estado).encerrada()) {
            return List.of();
        }
        return List.of(new Incrementar(jogador), new Sussurrar(jogador));
    }

    @Override
    public ResultadoDeValidacao validar(EstadoDeJogo estado, Acao acao) {
        if (resultado(estado).encerrada()) {
            return ResultadoDeValidacao.recusada("partida encerrada");
        }
        if (acao instanceof Incrementar || acao instanceof Sussurrar) {
            return ResultadoDeValidacao.aceita();
        }
        return ResultadoDeValidacao.recusada("acao desconhecida neste jogo");
    }

    @Override
    public Transicao aplicar(EstadoDeJogo estado, Acao acao, FonteDeAleatoriedade aleatoriedade) {
        Estado atual = (Estado) estado;
        return switch (acao) {
            case Incrementar incrementar -> {
                int dado = aleatoriedade.dado(6);
                Estado novo = new Estado(
                        atual.contagem() + 1, atual.segredos(), Optional.of(incrementar.autor()));
                yield Transicao.de(novo,
                        new Incrementado(incrementar.autor(), novo.contagem(), dado));
            }
            case Sussurrar sussurrar -> {
                int valor = aleatoriedade.inteiro(1, 100);
                Map<IdJogador, Integer> segredos = new HashMap<>(atual.segredos());
                segredos.put(sussurrar.autor(), valor);
                Estado novo = new Estado(atual.contagem(), segredos, atual.ultimo());
                yield Transicao.de(novo, new Sussurrado(sussurrar.autor(), valor));
            }
            default -> throw new IllegalArgumentException("acao nao suportada: " + acao);
        };
    }

    @Override
    public Visao visaoDe(EstadoDeJogo estado, IdJogador jogador) {
        Estado atual = (Estado) estado;
        return new VisaoDoJogador(
                atual.contagem(), Optional.ofNullable(atual.segredos().get(jogador)));
    }

    @Override
    public Resultado resultado(EstadoDeJogo estado) {
        Estado atual = (Estado) estado;
        if (atual.contagem() >= ALVO && atual.ultimo().isPresent()) {
            return Resultado.vencidaPor(atual.ultimo().get());
        }
        return Resultado.emAndamento();
    }

    @Override
    public Optional<Evento> eventoVisivelPara(
            EstadoDeJogo estado, Evento evento, IdJogador jogador) {
        if (evento instanceof Sussurrado sussurrado && !sussurrado.quem().equals(jogador)) {
            return Optional.empty();
        }
        return Optional.of(evento);
    }

    /** Acao de outro jogo, usada para provar que validar a recusa. */
    public record AcaoEstrangeira(IdJogador autor) implements Acao {
    }
}
