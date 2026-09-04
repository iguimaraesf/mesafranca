package br.com.mesafranca.games.ludo;

import br.com.mesafranca.core.domain.EstadoDeJogo;
import br.com.mesafranca.core.domain.IdJogador;
import br.com.mesafranca.core.domain.OrdemDeTurno;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Estado do Ludo.
 *
 * <p><strong>Modelo de posição.</strong> Cada peça guarda um único inteiro, o
 * <em>avanço</em>, e não uma casa do tabuleiro:
 *
 * <pre>
 *   -1        na base
 *   0..50     na pista comum (casa absoluta = (entrada + avanço) % 52)
 *   51..55    na coluna final, cinco posições
 *   56        chegou
 * </pre>
 *
 * <p>Guardar avanço em vez de casa absoluta resolve de graça três coisas que
 * seriam código: cada jogador percorre a mesma distância, a entrada na coluna
 * final não precisa de teste especial por cor, e "precisa do número exato
 * para chegar" vira {@code avanço + dado <= 56}.
 *
 * <p><strong>Tabuleiro configurável.</strong> A entrada de cada assento é
 * {@code assento * (52 / número de jogadores)}, o que dá 0/26 para dois
 * jogadores, 0/17/34 para três e 0/13/26/39 para quatro, sem uma
 * implementação por quantidade (PRD §5.1).
 */
public record LudoEstado(
        List<IdJogador> assentos,
        Map<IdJogador, Integer> entradas,
        Map<IdJogador, List<Integer>> pecas,
        OrdemDeTurno ordem,
        Optional<Integer> dado,
        int seisSeguidos,
        Optional<IdJogador> vencedor) implements EstadoDeJogo {

    public static final int CASAS_NA_PISTA = 52;
    public static final int ULTIMO_AVANCO_NA_PISTA = 50;
    public static final int AVANCO_FINAL = 56;
    public static final int PECAS_POR_JOGADOR = 4;
    public static final int FACES_DO_DADO = 6;
    public static final int SEIS_SEGUIDOS_PARA_PERDER_A_VEZ = 3;
    public static final int NA_BASE = -1;

    public LudoEstado {
        assentos = List.copyOf(assentos);
        entradas = Map.copyOf(entradas);
        pecas = congelar(pecas);
        Objects.requireNonNull(ordem, "ordem");
        Objects.requireNonNull(dado, "dado");
        Objects.requireNonNull(vencedor, "vencedor");
        if (seisSeguidos < 0) {
            throw new IllegalArgumentException("contagem de seis negativa: " + seisSeguidos);
        }
    }

    private static Map<IdJogador, List<Integer>> congelar(Map<IdJogador, List<Integer>> origem) {
        Map<IdJogador, List<Integer>> copia = new LinkedHashMap<>();
        origem.forEach((jogador, avancos) -> copia.put(jogador, List.copyOf(avancos)));
        return Map.copyOf(copia);
    }

    static LudoEstado inicial(List<IdJogador> assentos, OrdemDeTurno ordem) {
        int espacamento = CASAS_NA_PISTA / assentos.size();
        Map<IdJogador, Integer> entradas = new LinkedHashMap<>();
        Map<IdJogador, List<Integer>> pecas = new LinkedHashMap<>();
        for (int i = 0; i < assentos.size(); i++) {
            entradas.put(assentos.get(i), i * espacamento);
            pecas.put(assentos.get(i), List.of(NA_BASE, NA_BASE, NA_BASE, NA_BASE));
        }
        return new LudoEstado(assentos, entradas, pecas, ordem, Optional.empty(), 0, Optional.empty());
    }

    public IdJogador jogadorDaVez() {
        return ordem.daVez();
    }

    public List<Integer> pecasDe(IdJogador jogador) {
        return pecas.getOrDefault(jogador, List.of());
    }

    public int avancoDe(IdJogador jogador, int peca) {
        return pecasDe(jogador).get(peca);
    }

    /** Casa absoluta na pista comum, ou vazio se a peça não está na pista. */
    public Optional<Integer> casaNaPista(IdJogador jogador, int avanco) {
        if (avanco < 0 || avanco > ULTIMO_AVANCO_NA_PISTA) {
            return Optional.empty();
        }
        return Optional.of((entradas.get(jogador) + avanco) % CASAS_NA_PISTA);
    }

    public boolean chegou(IdJogador jogador, int peca) {
        return avancoDe(jogador, peca) == AVANCO_FINAL;
    }

    public boolean todasChegaram(IdJogador jogador) {
        return pecasDe(jogador).stream().allMatch(avanco -> avanco == AVANCO_FINAL);
    }

    LudoEstado comPeca(IdJogador jogador, int peca, int avanco) {
        Map<IdJogador, List<Integer>> novas = new LinkedHashMap<>(pecas);
        List<Integer> lista = new ArrayList<>(novas.get(jogador));
        lista.set(peca, avanco);
        novas.put(jogador, lista);
        return new LudoEstado(assentos, entradas, novas, ordem, dado, seisSeguidos, vencedor);
    }

    LudoEstado comDado(Optional<Integer> novoDado, int novosSeisSeguidos) {
        return new LudoEstado(assentos, entradas, pecas, ordem, novoDado, novosSeisSeguidos, vencedor);
    }

    LudoEstado comOrdem(OrdemDeTurno novaOrdem) {
        return new LudoEstado(assentos, entradas, pecas, novaOrdem, dado, seisSeguidos, vencedor);
    }

    LudoEstado comVencedor(IdJogador campeao) {
        return new LudoEstado(
                assentos, entradas, pecas, ordem, Optional.empty(), 0, Optional.of(campeao));
    }
}
