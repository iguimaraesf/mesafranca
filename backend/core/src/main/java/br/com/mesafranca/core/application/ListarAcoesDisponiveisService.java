package br.com.mesafranca.core.application;

import br.com.mesafranca.core.domain.Acao;
import br.com.mesafranca.core.domain.FaseDaPartida;
import br.com.mesafranca.core.domain.Partida;
import br.com.mesafranca.core.erro.PartidaNaoEncontrada;
import br.com.mesafranca.core.port.in.ListarAcoesDisponiveis;
import br.com.mesafranca.core.port.out.CatalogoDeJogos;
import br.com.mesafranca.core.port.out.RepositorioDePartidas;
import java.util.List;
import java.util.Objects;

/** Implementacao de {@link ListarAcoesDisponiveis}. */
public final class ListarAcoesDisponiveisService implements ListarAcoesDisponiveis {

    private final RepositorioDePartidas repositorio;
    private final CatalogoDeJogos catalogo;

    public ListarAcoesDisponiveisService(
            RepositorioDePartidas repositorio, CatalogoDeJogos catalogo) {
        this.repositorio = Objects.requireNonNull(repositorio, "repositorio");
        this.catalogo = Objects.requireNonNull(catalogo, "catalogo");
    }

    @Override
    public List<Acao> listar(Consulta consulta) {
        Objects.requireNonNull(consulta, "consulta");
        Partida partida = repositorio.carregar(consulta.partida())
                .orElseThrow(() -> new PartidaNaoEncontrada(consulta.partida()));

        if (partida.fase() != FaseDaPartida.EM_ANDAMENTO) {
            return List.of();
        }
        return List.copyOf(catalogo.resolver(partida.jogo())
                .acoesDisponiveis(partida.estadoObrigatorio(), consulta.jogador()));
    }
}
