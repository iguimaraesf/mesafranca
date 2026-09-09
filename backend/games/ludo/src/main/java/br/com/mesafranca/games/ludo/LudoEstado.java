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
 *   0..(casas_por_jogador-1)     na pista comum (casa absoluta = (entrada + avanço) % total_casas)
 *   (casas_por_jogador)..(casas_por_jogador+4)    na coluna final, cinco posições
 *   (casas_por_jogador+5)        chegou
 * </pre>
 *
 * <p>Guardar avanço em vez de casa absoluta resolve de graça três coisas que
 * seriam código: cada jogador percorre a mesma distância, a entrada na coluna
 * final não precisa de teste especial por cor, e "precisa do número exato
 * para chegar" vira {@code avanço + dado <= casas_por_jogador + 5}.
 *
 * <p><strong>Tabuleiro adaptativo.</strong> O número de casas varia conforme a
 * quantidade de jogadores para formar o formato geométrico correto:
 * <ul>
 *   <li>2 jogadores: 28 casas (oval)</li>
 *   <li>3 jogadores: 39 casas (triângulo)</li>
 *   <li>4 jogadores: 52 casas (clássico quadrado)</li>
 *   <li>5 jogadores: 65 casas (pentágono)</li>
 *   <li>6 jogadores: 78 casas (hexágono)</li>
 * </ul>
 *
 * <p><strong>Funcionalidades futuras (não implementadas):</strong>
 * <ul>
 *   <li>Casas protegidas (safe spots) - configurável por partida</li>
 *   <li>Opção de 2 ou 4 peças por jogador - configurável por partida</li>
 *   <li>Corredor final adaptativo - hoje fixo em 5 posições</li>
 *   <li>Bônus por chegada - fora do escopo atual</li>
 * </ul>
 */
public record LudoEstado(
        List<IdJogador> assentos,
        Map<IdJogador, Integer> entradas,
        Map<IdJogador, List<Integer>> pecas,
        OrdemDeTurno ordem,
        Optional<Integer> dado,
        int seisSeguidos,
        Optional<IdJogador> vencedor) implements EstadoDeJogo {

    public static final int PECAS_POR_JOGADOR = 4;
    public static final int FACES_DO_DADO = 6;
    public static final int SEIS_SEGUIDOS_PARA_PERDER_A_VEZ = 3;
    public static final int NA_BASE = -1;
    public static final int POSICOES_CORREDOR_FINAL = 5;

    // Tabuleiro adaptativo: calcula casas baseado na quantidade de jogadores
    public static int calcularCasasNaPista(int numeroDeJogadores) {
        return numeroDeJogadores * 13; // 13 casas por jogador = 28/39/52/65/78
    }

    public static int calcularUltimoAvancoNaPista(int numeroDeJogadores) {
        return calcularCasasNaPista(numeroDeJogadores) - 1;
    }

    public static int calcularAvancoFinal(int numeroDeJogadores) {
        return calcularCasasNaPista(numeroDeJogadores) + POSICOES_CORREDOR_FINAL;
    }

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
        int numeroDeJogadores = assentos.size();
        int casasNaPista = calcularCasasNaPista(numeroDeJogadores);
        int espacamento = casasNaPista / numeroDeJogadores;

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
        int numeroDeJogadores = assentos.size();
        int ultimoAvanco = calcularUltimoAvancoNaPista(numeroDeJogadores);
        int casasNaPista = calcularCasasNaPista(numeroDeJogadores);

        if (avanco < 0 || avanco > ultimoAvanco) {
            return Optional.empty();
        }
        return Optional.of((entradas.get(jogador) + avanco) % casasNaPista);
    }

    public boolean chegou(IdJogador jogador, int peca) {
        int numeroDeJogadores = assentos.size();
        int avancoFinal = calcularAvancoFinal(numeroDeJogadores);
        return avancoDe(jogador, peca) == avancoFinal;
    }

    public boolean todasChegaram(IdJogador jogador) {
        int numeroDeJogadores = assentos.size();
        int avancoFinal = calcularAvancoFinal(numeroDeJogadores);
        return pecasDe(jogador).stream().allMatch(avanco -> avanco == avancoFinal);
    }

    // Métodos para informar o front-end sobre o formato do tabuleiro
    public int totalDeCasasNaPista() {
        return calcularCasasNaPista(assentos.size());
    }

    public int casasPorJogador() {
        return calcularCasasNaPista(assentos.size());
    }

    public int posicoesCorredorFinal() {
        return POSICOES_CORREDOR_FINAL;
    }

    public String formatoDoTabuleiro() {
        return switch (assentos.size()) {
            case 2 -> "oval";
            case 3 -> "triangulo";
            case 4 -> "quadrado";
            case 5 -> "pentagono";
            case 6 -> "hexagono";
            default -> "desconhecido";
        };
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
