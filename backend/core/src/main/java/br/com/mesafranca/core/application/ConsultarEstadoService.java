package br.com.mesafranca.core.application;

import br.com.mesafranca.core.domain.Partida;
import br.com.mesafranca.core.erro.PartidaNaoEncontrada;
import br.com.mesafranca.core.port.in.ConsultarEstado;
import br.com.mesafranca.core.port.out.CatalogoDeJogos;
import br.com.mesafranca.core.port.out.RepositorioDePartidas;
import br.com.mesafranca.core.spi.DefinicaoDeJogo;
import java.util.Objects;

/** Implementacao de {@link ConsultarEstado}. */
public final class ConsultarEstadoService implements ConsultarEstado {

    private final RepositorioDePartidas repositorio;
    private final CatalogoDeJogos catalogo;

    public ConsultarEstadoService(RepositorioDePartidas repositorio, CatalogoDeJogos catalogo) {
        this.repositorio = Objects.requireNonNull(repositorio, "repositorio");
        this.catalogo = Objects.requireNonNull(catalogo, "catalogo");
    }

    @Override
    public Resposta consultar(Consulta consulta) {
        Objects.requireNonNull(consulta, "consulta");
        Partida partida = repositorio.carregar(consulta.partida())
                .orElseThrow(() -> new PartidaNaoEncontrada(consulta.partida()));

        DefinicaoDeJogo definicao = catalogo.resolver(partida.jogo());
        var estado = partida.estadoObrigatorio();

        return new Resposta(
                definicao.visaoDe(estado, consulta.jogador()),
                definicao.resultado(estado),
                partida.ultimaSequencia());
    }
}
