package br.com.mesafranca.games.uno;

import br.com.mesafranca.core.domain.EstadoDeJogo;
import br.com.mesafranca.core.domain.IdJogador;
import br.com.mesafranca.core.domain.OrdemDeTurno;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Estado do UNO. O descarte é uma pilha; o topo é o último elemento. */
public record UnoEstado(
        List<IdJogador> assentos,
        Map<IdJogador, List<CartaUno>> maos,
        List<CartaUno> monte,
        List<CartaUno> descarte,
        Cor corAtiva,
        OrdemDeTurno ordem,
        boolean comprouNestaVez,
        Set<IdJogador> declararamUno,
        Optional<IdJogador> vencedor) implements EstadoDeJogo {

    public static final int CARTAS_INICIAIS = 7;
    public static final int PUNICAO_POR_ESQUECER_UNO = 2;

    public UnoEstado {
        assentos = List.copyOf(assentos);
        maos = congelar(maos);
        monte = List.copyOf(monte);
        descarte = List.copyOf(descarte);
        declararamUno = Set.copyOf(declararamUno);
        Objects.requireNonNull(corAtiva, "corAtiva");
        Objects.requireNonNull(ordem, "ordem");
        Objects.requireNonNull(vencedor, "vencedor");
        if (descarte.isEmpty()) {
            throw new IllegalArgumentException("o descarte comeca com uma carta virada");
        }
        if (!corAtiva.jogavel()) {
            throw new IllegalArgumentException("cor ativa nao pode ser preta");
        }
    }

    private static Map<IdJogador, List<CartaUno>> congelar(Map<IdJogador, List<CartaUno>> origem) {
        Map<IdJogador, List<CartaUno>> copia = new LinkedHashMap<>();
        origem.forEach((jogador, cartas) -> copia.put(jogador, List.copyOf(cartas)));
        return Map.copyOf(copia);
    }

    /**
     * Distribui sete cartas a cada um e vira a primeira carta comum do monte.
     *
     * <p>Se a primeira do monte for curinga, procura a próxima carta colorida
     * em vez de reembaralhar: o resultado é o mesmo e continua determinístico
     * a partir da semente. A carta virada não produz efeito.
     */
    static UnoEstado inicial(
            List<IdJogador> assentos, OrdemDeTurno ordem, List<CartaUno> embaralhado) {
        List<CartaUno> resto = new ArrayList<>(embaralhado);
        Map<IdJogador, List<CartaUno>> maos = new LinkedHashMap<>();
        for (IdJogador jogador : assentos) {
            List<CartaUno> mao = new ArrayList<>(resto.subList(0, CARTAS_INICIAIS));
            resto = new ArrayList<>(resto.subList(CARTAS_INICIAIS, resto.size()));
            maos.put(jogador, mao);
        }
        int indice = 0;
        while (resto.get(indice).curinga()) {
            indice++;
        }
        CartaUno virada = resto.remove(indice);
        return new UnoEstado(assentos, maos, resto, List.of(virada), virada.cor(),
                ordem, false, Set.of(), Optional.empty());
    }

    public IdJogador jogadorDaVez() {
        return ordem.daVez();
    }

    public List<CartaUno> mao(IdJogador jogador) {
        return maos.getOrDefault(jogador, List.of());
    }

    public CartaUno topo() {
        return descarte.getLast();
    }

    public Map<IdJogador, Integer> tamanhoDasMaos() {
        Map<IdJogador, Integer> tamanhos = new LinkedHashMap<>();
        maos.forEach((jogador, cartas) -> tamanhos.put(jogador, cartas.size()));
        return Map.copyOf(tamanhos);
    }

    public boolean podeJogar(IdJogador jogador) {
        return mao(jogador).stream().anyMatch(carta -> carta.combinaCom(topo(), corAtiva));
    }

    /** Está com uma carta só e ainda não disse UNO — alvo legítimo de acusação. */
    public boolean vulneravelAAcusacao(IdJogador jogador) {
        return mao(jogador).size() == 1 && !declararamUno.contains(jogador);
    }

    UnoEstado comMao(IdJogador jogador, List<CartaUno> nova) {
        Map<IdJogador, List<CartaUno>> novas = new LinkedHashMap<>(maos);
        novas.put(jogador, List.copyOf(nova));
        Set<IdJogador> declararam = new LinkedHashSet<>(declararamUno);
        if (nova.size() != 1) {
            // Sair de uma carta só apaga a declaração: quem voltar a uma carta
            // precisa declarar de novo.
            declararam.remove(jogador);
        }
        return new UnoEstado(assentos, novas, monte, descarte, corAtiva, ordem,
                comprouNestaVez, declararam, vencedor);
    }

    UnoEstado comMonte(List<CartaUno> novoMonte) {
        return new UnoEstado(assentos, maos, novoMonte, descarte, corAtiva, ordem,
                comprouNestaVez, declararamUno, vencedor);
    }

    UnoEstado comDescarte(List<CartaUno> novoDescarte, Cor novaCor) {
        return new UnoEstado(assentos, maos, monte, novoDescarte, novaCor, ordem,
                comprouNestaVez, declararamUno, vencedor);
    }

    UnoEstado comCor(Cor novaCor) {
        return comDescarte(descarte, novaCor);
    }

    UnoEstado comOrdem(OrdemDeTurno novaOrdem) {
        return new UnoEstado(assentos, maos, monte, descarte, corAtiva, novaOrdem,
                comprouNestaVez, declararamUno, vencedor);
    }

    UnoEstado comCompraRegistrada(boolean comprou) {
        return new UnoEstado(assentos, maos, monte, descarte, corAtiva, ordem,
                comprou, declararamUno, vencedor);
    }

    UnoEstado comDeclaracao(IdJogador jogador) {
        Set<IdJogador> declararam = new LinkedHashSet<>(declararamUno);
        declararam.add(jogador);
        return new UnoEstado(assentos, maos, monte, descarte, corAtiva, ordem,
                comprouNestaVez, declararam, vencedor);
    }

    UnoEstado comVencedor(IdJogador campeao) {
        return new UnoEstado(assentos, maos, monte, descarte, corAtiva, ordem,
                comprouNestaVez, declararamUno, Optional.of(campeao));
    }
}
