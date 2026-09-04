package br.com.mesafranca.games.loveletter;

import br.com.mesafranca.core.domain.EstadoDeJogo;
import br.com.mesafranca.core.domain.IdJogador;
import br.com.mesafranca.core.domain.OrdemDeTurno;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Estado do Love Letter.
 *
 * <p>Guarda o segredo inteiro — mãos, baralho e a carta retirada no começo da
 * rodada. Nada disso sai daqui sem passar por
 * {@code visaoDe} ou {@code eventoVisivelPara}: o estado é a verdade do
 * servidor, não o que o cliente vê.
 */
public record LoveLetterEstado(
        List<IdJogador> assentos,
        Map<IdJogador, List<Carta>> maos,
        List<Carta> baralho,
        Carta removida,
        Map<IdJogador, List<Carta>> descartes,
        Set<IdJogador> protegidos,
        OrdemDeTurno ordem,
        Fase fase,
        List<IdJogador> vencedores,
        boolean encerrada) implements EstadoDeJogo {

    public LoveLetterEstado {
        assentos = List.copyOf(assentos);
        maos = congelar(maos);
        baralho = List.copyOf(baralho);
        descartes = congelar(descartes);
        protegidos = Set.copyOf(protegidos);
        vencedores = List.copyOf(vencedores);
        Objects.requireNonNull(removida, "removida");
        Objects.requireNonNull(ordem, "ordem");
        Objects.requireNonNull(fase, "fase");
    }

    private static Map<IdJogador, List<Carta>> congelar(Map<IdJogador, List<Carta>> origem) {
        Map<IdJogador, List<Carta>> copia = new LinkedHashMap<>();
        origem.forEach((jogador, cartas) -> copia.put(jogador, List.copyOf(cartas)));
        return Map.copyOf(copia);
    }

    static LoveLetterEstado inicial(
            List<IdJogador> assentos, OrdemDeTurno ordem, List<Carta> embaralhado) {
        Carta removida = embaralhado.getFirst();
        Map<IdJogador, List<Carta>> maos = new LinkedHashMap<>();
        Map<IdJogador, List<Carta>> descartes = new LinkedHashMap<>();
        int proxima = 1;
        for (IdJogador jogador : assentos) {
            maos.put(jogador, List.of(embaralhado.get(proxima++)));
            descartes.put(jogador, List.of());
        }
        List<Carta> resto = List.copyOf(embaralhado.subList(proxima, embaralhado.size()));
        return new LoveLetterEstado(assentos, maos, resto, removida, descartes,
                Set.of(), ordem, Fase.COMPRAR, List.of(), false);
    }

    public IdJogador jogadorDaVez() {
        return ordem.daVez();
    }

    public List<Carta> mao(IdJogador jogador) {
        return maos.getOrDefault(jogador, List.of());
    }

    /** A única carta de quem não está no meio da própria jogada. */
    public Carta cartaUnica(IdJogador jogador) {
        List<Carta> mao = mao(jogador);
        if (mao.size() != 1) {
            throw new IllegalStateException("mao de " + jogador + " tem " + mao.size() + " cartas");
        }
        return mao.getFirst();
    }

    public boolean ativo(IdJogador jogador) {
        return !ordem.estaFora(jogador);
    }

    public boolean protegido(IdJogador jogador) {
        return protegidos.contains(jogador);
    }

    public Set<IdJogador> eliminados() {
        return ordem.fora();
    }

    public Map<IdJogador, Integer> tamanhoDasMaos() {
        Map<IdJogador, Integer> tamanhos = new LinkedHashMap<>();
        maos.forEach((jogador, cartas) -> tamanhos.put(jogador, cartas.size()));
        return Map.copyOf(tamanhos);
    }

    LoveLetterEstado comMao(IdJogador jogador, List<Carta> novaMao) {
        Map<IdJogador, List<Carta>> novas = new LinkedHashMap<>(maos);
        novas.put(jogador, List.copyOf(novaMao));
        return new LoveLetterEstado(assentos, novas, baralho, removida, descartes,
                protegidos, ordem, fase, vencedores, encerrada);
    }

    LoveLetterEstado comBaralho(List<Carta> novoBaralho) {
        return new LoveLetterEstado(assentos, maos, novoBaralho, removida, descartes,
                protegidos, ordem, fase, vencedores, encerrada);
    }

    LoveLetterEstado comDescarte(IdJogador jogador, Carta carta) {
        Map<IdJogador, List<Carta>> novos = new LinkedHashMap<>(descartes);
        List<Carta> pilha = new ArrayList<>(novos.getOrDefault(jogador, List.of()));
        pilha.add(carta);
        novos.put(jogador, pilha);
        return new LoveLetterEstado(assentos, maos, baralho, removida, novos,
                protegidos, ordem, fase, vencedores, encerrada);
    }

    LoveLetterEstado comProtegido(IdJogador jogador) {
        Set<IdJogador> novos = new LinkedHashSet<>(protegidos);
        novos.add(jogador);
        return comProtegidos(novos);
    }

    LoveLetterEstado semProtecao(IdJogador jogador) {
        Set<IdJogador> novos = new LinkedHashSet<>(protegidos);
        novos.remove(jogador);
        return comProtegidos(novos);
    }

    private LoveLetterEstado comProtegidos(Set<IdJogador> novos) {
        return new LoveLetterEstado(assentos, maos, baralho, removida, descartes,
                novos, ordem, fase, vencedores, encerrada);
    }

    LoveLetterEstado comOrdem(OrdemDeTurno novaOrdem) {
        return new LoveLetterEstado(assentos, maos, baralho, removida, descartes,
                protegidos, novaOrdem, fase, vencedores, encerrada);
    }

    LoveLetterEstado comFase(Fase novaFase) {
        return new LoveLetterEstado(assentos, maos, baralho, removida, descartes,
                protegidos, ordem, novaFase, vencedores, encerrada);
    }

    LoveLetterEstado encerradaCom(List<IdJogador> campeoes) {
        return new LoveLetterEstado(assentos, maos, baralho, removida, descartes,
                protegidos, ordem, fase, campeoes, true);
    }
}
