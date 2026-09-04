package br.com.mesafranca.core.duble;

import br.com.mesafranca.core.domain.EventoRegistrado;
import br.com.mesafranca.core.domain.IdJogador;
import br.com.mesafranca.core.domain.IdPartida;
import br.com.mesafranca.core.port.out.PublicadorDeEventos;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Guarda o que foi entregue a cada destinatario, para provar o RNF-30. */
public final class PublicadorDeTeste implements PublicadorDeEventos {

    private final Map<IdJogador, List<EventoRegistrado>> entregues = new HashMap<>();

    @Override
    public void publicar(IdPartida partida, IdJogador destinatario, List<EventoRegistrado> eventos) {
        entregues.computeIfAbsent(destinatario, j -> new ArrayList<>()).addAll(eventos);
    }

    public List<EventoRegistrado> recebidosPor(IdJogador jogador) {
        return List.copyOf(entregues.getOrDefault(jogador, List.of()));
    }
}
