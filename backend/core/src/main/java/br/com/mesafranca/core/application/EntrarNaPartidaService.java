package br.com.mesafranca.core.application;

import br.com.mesafranca.core.domain.Assento;
import br.com.mesafranca.core.domain.FaseDaPartida;
import br.com.mesafranca.core.domain.Jogador;
import br.com.mesafranca.core.domain.Partida;
import br.com.mesafranca.core.erro.PartidaNaoEncontrada;
import br.com.mesafranca.core.port.in.EntrarNaPartida;
import br.com.mesafranca.core.port.out.RepositorioDePartidas;
import java.util.List;
import java.util.Objects;

/** Implementacao de {@link EntrarNaPartida}. */
public final class EntrarNaPartidaService implements EntrarNaPartida {

    private final RepositorioDePartidas repositorio;

    public EntrarNaPartidaService(RepositorioDePartidas repositorio) {
        this.repositorio = Objects.requireNonNull(repositorio, "repositorio");
    }

    @Override
    public Resposta entrar(Comando comando) {
        Objects.requireNonNull(comando, "comando");
        Partida partida = repositorio.carregar(comando.partida())
                .orElseThrow(() -> new PartidaNaoEncontrada(comando.partida()));

        if (partida.fase() != FaseDaPartida.AGUARDANDO) {
            return new Recusado("partida ja iniciada");
        }
        if (partida.contem(comando.jogador())) {
            return new Recusado("jogador ja esta na partida");
        }
        if (partida.cheia()) {
            return new Recusado("partida cheia");
        }

        Assento assento = partida.proximoAssento();
        Partida atualizada = partida.com(new Jogador(comando.jogador(), assento, comando.tipo()));
        repositorio.salvar(atualizada, List.of());
        return new Sentou(assento);
    }
}
