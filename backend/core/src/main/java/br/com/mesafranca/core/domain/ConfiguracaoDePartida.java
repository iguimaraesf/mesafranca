package br.com.mesafranca.core.domain;

import java.util.Map;
import java.util.Objects;

/** O que foi escolhido no lobby antes de a partida comecar. */
public record ConfiguracaoDePartida(
        IdentificadorDeJogo jogo,
        int numeroDeJogadores,
        Map<String, String> opcoes) {

    public ConfiguracaoDePartida {
        Objects.requireNonNull(jogo, "jogo");
        if (numeroDeJogadores < 1) {
            throw new IllegalArgumentException(
                    "numero de jogadores deve ser positivo: " + numeroDeJogadores);
        }
        opcoes = Map.copyOf(Objects.requireNonNullElse(opcoes, Map.of()));
    }

    public static ConfiguracaoDePartida de(IdentificadorDeJogo jogo, int numeroDeJogadores) {
        return new ConfiguracaoDePartida(jogo, numeroDeJogadores, Map.of());
    }

    public String opcao(String chave, String padrao) {
        return opcoes.getOrDefault(chave, padrao);
    }
}
