package br.com.mesafranca.core.application;

import br.com.mesafranca.core.domain.EventoRegistrado;
import br.com.mesafranca.core.domain.FaseDaPartida;
import br.com.mesafranca.core.domain.Jogador;
import br.com.mesafranca.core.domain.Partida;
import br.com.mesafranca.core.domain.ResultadoDeValidacao;
import br.com.mesafranca.core.domain.Transicao;
import br.com.mesafranca.core.erro.PartidaNaoEncontrada;
import br.com.mesafranca.core.port.in.SubmeterAcao;
import br.com.mesafranca.core.port.out.CatalogoDeJogos;
import br.com.mesafranca.core.port.out.FabricaDeAleatoriedade;
import br.com.mesafranca.core.port.out.PublicadorDeEventos;
import br.com.mesafranca.core.port.out.RepositorioDePartidas;
import br.com.mesafranca.core.spi.DefinicaoDeJogo;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Implementacao de {@link SubmeterAcao}: o unico caminho pelo qual o estado
 * de uma partida avanca.
 *
 * <p>A ordem dos passos nao e arbitraria. A reserva do identificador de acao
 * vem <em>antes</em> de qualquer leitura, porque e ela que impede que um
 * reenvio de rede role o dado uma segunda vez (RNF-22).
 */
public final class SubmeterAcaoService implements SubmeterAcao {

    private final RepositorioDePartidas repositorio;
    private final CatalogoDeJogos catalogo;
    private final FabricaDeAleatoriedade aleatoriedade;
    private final PublicadorDeEventos publicador;

    public SubmeterAcaoService(
            RepositorioDePartidas repositorio,
            CatalogoDeJogos catalogo,
            FabricaDeAleatoriedade aleatoriedade,
            PublicadorDeEventos publicador) {
        this.repositorio = Objects.requireNonNull(repositorio, "repositorio");
        this.catalogo = Objects.requireNonNull(catalogo, "catalogo");
        this.aleatoriedade = Objects.requireNonNull(aleatoriedade, "aleatoriedade");
        this.publicador = Objects.requireNonNull(publicador, "publicador");
    }

    @Override
    public Resposta submeter(Comando comando) {
        Objects.requireNonNull(comando, "comando");

        if (!repositorio.registrarAcao(comando.partida(), comando.idAcao())) {
            return new Repetida();
        }

        Partida partida = repositorio.carregar(comando.partida())
                .orElseThrow(() -> new PartidaNaoEncontrada(comando.partida()));

        if (partida.fase() != FaseDaPartida.EM_ANDAMENTO) {
            return new Recusada("partida nao esta em andamento");
        }
        if (!partida.contem(comando.acao().autor())) {
            return new Recusada("jogador nao participa desta partida");
        }

        DefinicaoDeJogo definicao = catalogo.resolver(partida.jogo());
        ResultadoDeValidacao veredito =
                definicao.validar(partida.estadoObrigatorio(), comando.acao());

        // switch exaustivo sobre tipo selado: acrescentar um caso quebra o build aqui.
        switch (veredito) {
            case ResultadoDeValidacao.Recusada recusada -> {
                return new Recusada(recusada.motivo());
            }
            case ResultadoDeValidacao.Aceita ignorada -> {
                // segue o fluxo
            }
        }

        Transicao transicao = definicao.aplicar(
                partida.estadoObrigatorio(),
                comando.acao(),
                aleatoriedade.derivada(partida.semente(), partida.versao()));

        List<EventoRegistrado> registrados = numerar(partida.ultimaSequencia(), transicao);

        Partida avancada = partida.apos(transicao.estado(), registrados.size());
        if (definicao.resultado(transicao.estado()).encerrada()) {
            avancada = avancada.encerrada();
        }

        repositorio.salvar(avancada, registrados);
        publicarParaCadaJogador(avancada, definicao, registrados);

        return new Aplicada(registrados, avancada.versao());
    }

    private static List<EventoRegistrado> numerar(long ultimaSequencia, Transicao transicao) {
        List<EventoRegistrado> registrados = new ArrayList<>();
        long sequencia = ultimaSequencia;
        for (var evento : transicao.eventos()) {
            registrados.add(new EventoRegistrado(++sequencia, evento));
        }
        return List.copyOf(registrados);
    }

    /**
     * Filtra por destinatario antes de publicar (RNF-30).
     *
     * <p>Nunca difundir a lista inteira e deixar o cliente esconder o que nao
     * lhe cabe: o vazamento mais comum em jogo de informacao oculta e
     * exatamente esse.
     */
    private void publicarParaCadaJogador(
            Partida partida, DefinicaoDeJogo definicao, List<EventoRegistrado> registrados) {

        for (Jogador jogador : partida.jogadores()) {
            List<EventoRegistrado> visiveis = new ArrayList<>();
            for (EventoRegistrado registrado : registrados) {
                definicao.eventoVisivelPara(
                                partida.estadoObrigatorio(), registrado.evento(), jogador.id())
                        .ifPresent(e -> visiveis.add(new EventoRegistrado(registrado.sequencia(), e)));
            }
            if (!visiveis.isEmpty()) {
                publicador.publicar(partida.id(), jogador.id(), List.copyOf(visiveis));
            }
        }
    }
}
