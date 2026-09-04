package br.com.mesafranca.core.application;

import br.com.mesafranca.core.domain.Partida;
import br.com.mesafranca.core.erro.PartidaNaoEncontrada;
import br.com.mesafranca.core.port.in.Reconectar;
import br.com.mesafranca.core.port.out.CatalogoDeJogos;
import br.com.mesafranca.core.port.out.RepositorioDePartidas;
import java.util.Objects;

/** Implementacao de {@link Reconectar}. */
public final class ReconectarService implements Reconectar {

    private final RepositorioDePartidas repositorio;
    private final CatalogoDeJogos catalogo;

    public ReconectarService(RepositorioDePartidas repositorio, CatalogoDeJogos catalogo) {
        this.repositorio = Objects.requireNonNull(repositorio, "repositorio");
        this.catalogo = Objects.requireNonNull(catalogo, "catalogo");
    }

    @Override
    public Resposta reconectar(Comando comando) {
        Objects.requireNonNull(comando, "comando");
        Partida partida = repositorio.carregar(comando.partida())
                .orElseThrow(() -> new PartidaNaoEncontrada(comando.partida()));

        var visao = catalogo.resolver(partida.jogo())
                .visaoDe(partida.estadoObrigatorio(), comando.jogador());

        return new Resposta(
                visao,
                repositorio.eventosDesde(partida.id(), comando.ultimaSequenciaVista()),
                partida.ultimaSequencia());
    }
}
