package br.com.mesafranca.core.application;

import br.com.mesafranca.core.domain.ConfiguracaoDePartida;
import br.com.mesafranca.core.domain.Partida;
import br.com.mesafranca.core.port.in.CriarPartida;
import br.com.mesafranca.core.port.out.CatalogoDeJogos;
import br.com.mesafranca.core.port.out.FabricaDeAleatoriedade;
import br.com.mesafranca.core.port.out.GeradorDeIdentidade;
import br.com.mesafranca.core.port.out.RepositorioDePartidas;
import br.com.mesafranca.core.spi.DefinicaoDeJogo;
import java.util.List;
import java.util.Objects;

/** Implementacao de {@link CriarPartida}. */
public final class CriarPartidaService implements CriarPartida {

    private final RepositorioDePartidas repositorio;
    private final CatalogoDeJogos catalogo;
    private final FabricaDeAleatoriedade aleatoriedade;
    private final GeradorDeIdentidade identidade;

    public CriarPartidaService(
            RepositorioDePartidas repositorio,
            CatalogoDeJogos catalogo,
            FabricaDeAleatoriedade aleatoriedade,
            GeradorDeIdentidade identidade) {
        this.repositorio = Objects.requireNonNull(repositorio, "repositorio");
        this.catalogo = Objects.requireNonNull(catalogo, "catalogo");
        this.aleatoriedade = Objects.requireNonNull(aleatoriedade, "aleatoriedade");
        this.identidade = Objects.requireNonNull(identidade, "identidade");
    }

    @Override
    public Resposta criar(Comando comando) {
        Objects.requireNonNull(comando, "comando");
        DefinicaoDeJogo definicao = catalogo.resolver(comando.jogo());

        if (!definicao.configuracaoSuportada().suporta(comando.numeroDeJogadores())) {
            return new Recusado(comando.jogo() + " nao aceita "
                    + comando.numeroDeJogadores() + " jogadores");
        }

        ConfiguracaoDePartida configuracao = new ConfiguracaoDePartida(
                comando.jogo(), comando.numeroDeJogadores(), comando.opcoes());

        Partida partida = Partida.nova(
                identidade.novaPartida(), configuracao, aleatoriedade.novaSemente());

        repositorio.salvar(partida, List.of());
        return new Criada(partida.id());
    }
}
