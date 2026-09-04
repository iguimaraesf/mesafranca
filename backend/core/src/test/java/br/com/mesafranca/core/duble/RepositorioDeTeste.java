package br.com.mesafranca.core.duble;

import br.com.mesafranca.core.domain.EventoRegistrado;
import br.com.mesafranca.core.domain.IdAcao;
import br.com.mesafranca.core.domain.IdPartida;
import br.com.mesafranca.core.domain.Partida;
import br.com.mesafranca.core.port.out.RepositorioDePartidas;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Repositorio de teste, escrito a mao.
 *
 * <p>Escrito a mao e nao com biblioteca de mock por decisao de
 * docs/TESTING.md: a porta e pequena, o dublê e mais legivel que a
 * configuracao de um mock e nao quebra quando a assinatura muda de forma
 * irrelevante.
 */
public final class RepositorioDeTeste implements RepositorioDePartidas {

    private final Map<IdPartida, Partida> partidas = new HashMap<>();
    private final Map<IdPartida, List<EventoRegistrado>> eventos = new HashMap<>();
    private final Set<String> acoes = new HashSet<>();

    public RepositorioDeTeste comPartida(Partida partida) {
        partidas.put(partida.id(), partida);
        return this;
    }

    @Override
    public Optional<Partida> carregar(IdPartida id) {
        return Optional.ofNullable(partidas.get(id));
    }

    @Override
    public void salvar(Partida partida, List<EventoRegistrado> novosEventos) {
        partidas.put(partida.id(), partida);
        eventos.computeIfAbsent(partida.id(), id -> new ArrayList<>()).addAll(novosEventos);
    }

    @Override
    public boolean registrarAcao(IdPartida partida, IdAcao acao) {
        return acoes.add(partida.valor() + "|" + acao.valor());
    }

    @Override
    public List<EventoRegistrado> eventosDesde(IdPartida partida, long sequencia) {
        return eventos.getOrDefault(partida, List.of()).stream()
                .filter(e -> e.sequencia() > sequencia)
                .toList();
    }

    public List<EventoRegistrado> log(IdPartida partida) {
        return List.copyOf(eventos.getOrDefault(partida, List.of()));
    }
}
