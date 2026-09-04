package br.com.mesafranca.core.domain;

import br.com.mesafranca.core.erro.RegraDePartidaViolada;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Agregado da plataforma: quem esta jogando o que, em que fase, sobre qual
 * estado.
 *
 * <p>O core cuida do envelope; o conteudo ({@link EstadoDeJogo}) e do jogo e
 * permanece opaco aqui. Toda mutacao devolve nova instancia.
 *
 * <p>Os metodos abaixo lancam {@link RegraDePartidaViolada} quando a
 * transicao e impossivel. Isso nao contradiz a regra de "recusa e valor, nao
 * excecao": aquela regra vale para acoes de jogo vindas do cliente, que sao
 * validadas antes de chegar aqui. Estas excecoes sao guarda de invariante.
 */
public record Partida(
        IdPartida id,
        ConfiguracaoDePartida configuracao,
        List<Jogador> jogadores,
        FaseDaPartida fase,
        Optional<EstadoDeJogo> estado,
        long semente,
        long versao,
        long ultimaSequencia) {

    public Partida {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(configuracao, "configuracao");
        Objects.requireNonNull(fase, "fase");
        Objects.requireNonNull(estado, "estado");
        jogadores = List.copyOf(Objects.requireNonNullElse(jogadores, List.of()));
        if (versao < 0) {
            throw new IllegalArgumentException("versao nao pode ser negativa: " + versao);
        }
        if (ultimaSequencia < 0) {
            throw new IllegalArgumentException("sequencia nao pode ser negativa: " + ultimaSequencia);
        }
    }

    /** Partida recem-criada, ainda sem jogadores e sem estado de jogo. */
    public static Partida nova(IdPartida id, ConfiguracaoDePartida configuracao, long semente) {
        return new Partida(id, configuracao, List.of(), FaseDaPartida.AGUARDANDO,
                Optional.empty(), semente, 0L, 0L);
    }

    public IdentificadorDeJogo jogo() {
        return configuracao.jogo();
    }

    public boolean cheia() {
        return jogadores.size() >= configuracao.numeroDeJogadores();
    }

    public boolean contem(IdJogador jogador) {
        return jogador(jogador).isPresent();
    }

    public Optional<Jogador> jogador(IdJogador jogador) {
        Objects.requireNonNull(jogador, "jogador");
        return jogadores.stream().filter(j -> j.id().equals(jogador)).findFirst();
    }

    /** Proximo assento livre, contado a partir de zero. */
    public Assento proximoAssento() {
        return new Assento(jogadores.size());
    }

    public Partida com(Jogador novo) {
        Objects.requireNonNull(novo, "novo");
        if (fase != FaseDaPartida.AGUARDANDO) {
            throw new RegraDePartidaViolada("partida nao aceita mais jogadores na fase " + fase);
        }
        if (cheia()) {
            throw new RegraDePartidaViolada("partida cheia: " + configuracao.numeroDeJogadores());
        }
        if (contem(novo.id())) {
            throw new RegraDePartidaViolada("jogador ja esta na partida: " + novo.id());
        }
        if (jogadores.stream().anyMatch(j -> j.assento().equals(novo.assento()))) {
            throw new RegraDePartidaViolada("assento ocupado: " + novo.assento().indice());
        }
        List<Jogador> lista = new ArrayList<>(jogadores);
        lista.add(novo);
        // Toda alteracao incrementa a versao: e o que o travamento otimista do
        // repositorio compara para detectar duas escritas concorrentes (ADR-0004).
        return new Partida(id, configuracao, lista, fase, estado, semente, versao + 1, ultimaSequencia);
    }

    public Partida iniciada(EstadoDeJogo estadoInicial) {
        Objects.requireNonNull(estadoInicial, "estadoInicial");
        if (fase != FaseDaPartida.AGUARDANDO) {
            throw new RegraDePartidaViolada("partida ja iniciada; fase atual " + fase);
        }
        if (!cheia()) {
            throw new RegraDePartidaViolada("faltam jogadores: "
                    + jogadores.size() + " de " + configuracao.numeroDeJogadores());
        }
        return new Partida(id, configuracao, jogadores, FaseDaPartida.EM_ANDAMENTO,
                Optional.of(estadoInicial), semente, versao + 1, ultimaSequencia);
    }

    /** Avanca a partida apos uma acao aceita, reservando a faixa de sequencias dos eventos. */
    public Partida apos(EstadoDeJogo novoEstado, int quantidadeDeEventos) {
        Objects.requireNonNull(novoEstado, "novoEstado");
        if (fase != FaseDaPartida.EM_ANDAMENTO) {
            throw new RegraDePartidaViolada("partida nao aceita acoes na fase " + fase);
        }
        if (quantidadeDeEventos < 0) {
            throw new IllegalArgumentException("quantidade de eventos negativa: " + quantidadeDeEventos);
        }
        return new Partida(id, configuracao, jogadores, fase, Optional.of(novoEstado),
                semente, versao + 1, ultimaSequencia + quantidadeDeEventos);
    }

    public Partida encerrada() {
        if (fase != FaseDaPartida.EM_ANDAMENTO) {
            throw new RegraDePartidaViolada("so encerra partida em andamento; fase atual " + fase);
        }
        return new Partida(id, configuracao, jogadores, FaseDaPartida.ENCERRADA,
                estado, semente, versao + 1, ultimaSequencia);
    }

    /** Estado de jogo, exigindo que a partida ja tenha comecado. */
    public EstadoDeJogo estadoObrigatorio() {
        return estado.orElseThrow(
                () -> new RegraDePartidaViolada("partida " + id + " ainda nao tem estado de jogo"));
    }
}
