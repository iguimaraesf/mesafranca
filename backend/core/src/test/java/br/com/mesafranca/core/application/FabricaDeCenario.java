package br.com.mesafranca.core.application;

import br.com.mesafranca.core.domain.Assento;
import br.com.mesafranca.core.domain.ConfiguracaoDePartida;
import br.com.mesafranca.core.domain.IdJogador;
import br.com.mesafranca.core.domain.IdPartida;
import br.com.mesafranca.core.domain.Jogador;
import br.com.mesafranca.core.domain.Partida;
import br.com.mesafranca.core.duble.JogoDeContagem;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Monta partidas de teste sobre o {@link JogoDeContagem}. */
final class FabricaDeCenario {

    static final IdPartida PARTIDA =
            new IdPartida(UUID.fromString("00000000-0000-0000-0000-00000000000b"));
    static final IdJogador ANA = new IdJogador("ana");
    static final IdJogador BRUNO = new IdJogador("bruno");
    static final long SEMENTE = 4242L;

    private FabricaDeCenario() {
    }

    static ConfiguracaoDePartida configuracao() {
        return ConfiguracaoDePartida.de(JogoDeContagem.ID, 2);
    }

    static Partida aguardandoJogadores() {
        return Partida.nova(PARTIDA, configuracao(), SEMENTE);
    }

    static Partida comAna() {
        return aguardandoJogadores().com(Jogador.humano(ANA, new Assento(0)));
    }

    static Partida cheia() {
        return comAna().com(Jogador.humano(BRUNO, new Assento(1)));
    }

    static Partida emAndamento() {
        return cheia().iniciada(estadoZerado());
    }

    static JogoDeContagem.Estado estadoZerado() {
        return new JogoDeContagem.Estado(0, Map.of(), Optional.empty());
    }

    static JogoDeContagem.Estado estadoNaContagem(int contagem) {
        return new JogoDeContagem.Estado(contagem, Map.of(), Optional.of(ANA));
    }
}
