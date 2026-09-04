package br.com.mesafranca.core.duble;

import br.com.mesafranca.core.domain.IdPartida;
import br.com.mesafranca.core.port.out.GeradorDeIdentidade;
import java.util.UUID;

/** Gerador previsivel: o teste sabe de antemao o id da partida criada. */
public final class IdentidadeFixa implements GeradorDeIdentidade {

    public static final IdPartida ID =
            new IdPartida(UUID.fromString("00000000-0000-0000-0000-0000000000aa"));

    @Override
    public IdPartida novaPartida() {
        return ID;
    }
}
