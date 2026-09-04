package br.com.mesafranca.core.application;

import br.com.mesafranca.core.domain.EstadoDeJogo;
import br.com.mesafranca.core.domain.FaseDaPartida;
import br.com.mesafranca.core.domain.Partida;
import br.com.mesafranca.core.erro.PartidaNaoEncontrada;
import br.com.mesafranca.core.port.in.IniciarPartida;
import br.com.mesafranca.core.port.out.CatalogoDeJogos;
import br.com.mesafranca.core.port.out.FabricaDeAleatoriedade;
import br.com.mesafranca.core.port.out.RepositorioDePartidas;
import br.com.mesafranca.core.spi.DefinicaoDeJogo;
import java.util.List;
import java.util.Objects;

/** Implementacao de {@link IniciarPartida}. */
public final class IniciarPartidaService implements IniciarPartida {

    private final RepositorioDePartidas repositorio;
    private final CatalogoDeJogos catalogo;
    private final FabricaDeAleatoriedade aleatoriedade;

    public IniciarPartidaService(
            RepositorioDePartidas repositorio,
            CatalogoDeJogos catalogo,
            FabricaDeAleatoriedade aleatoriedade) {
        this.repositorio = Objects.requireNonNull(repositorio, "repositorio");
        this.catalogo = Objects.requireNonNull(catalogo, "catalogo");
        this.aleatoriedade = Objects.requireNonNull(aleatoriedade, "aleatoriedade");
    }

    @Override
    public Resposta iniciar(Comando comando) {
        Objects.requireNonNull(comando, "comando");
        Partida partida = repositorio.carregar(comando.partida())
                .orElseThrow(() -> new PartidaNaoEncontrada(comando.partida()));

        if (partida.fase() != FaseDaPartida.AGUARDANDO) {
            return new Recusado("partida ja iniciada");
        }
        if (!partida.cheia()) {
            return new Recusado("faltam jogadores: " + partida.jogadores().size()
                    + " de " + partida.configuracao().numeroDeJogadores());
        }

        DefinicaoDeJogo definicao = catalogo.resolver(partida.jogo());
        EstadoDeJogo inicial = definicao.estadoInicial(
                partida.configuracao(),
                partida.jogadores(),
                aleatoriedade.derivada(partida.semente(), partida.versao()));

        Partida iniciada = partida.iniciada(inicial);
        repositorio.salvar(iniciada, List.of());
        return new Iniciada(iniciada.versao());
    }
}
