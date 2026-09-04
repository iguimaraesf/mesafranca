package br.com.mesafranca.core.domain;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Quem joga agora e quem joga em seguida.
 *
 * <p><strong>Por que isto mora no core.</strong> Não estava no desenho
 * original: apareceu quando Ludo, Love Letter e UNO precisaram, os três, da
 * mesma mecânica de "avançar a vez pulando quem saiu". Mantê-la em cada jogo
 * seria a terceira cópia de um mesmo laço com {@code floorMod}.
 *
 * <p>O que fica aqui é só a <em>mecânica</em> de percorrer assentos. As
 * <em>regras</em> continuam no jogo: Ludo decide não avançar quando tira 6,
 * UNO decide inverter o sentido, Love Letter decide quem sai. O core não
 * conhece nenhuma dessas decisões — ele só sabe girar a roda.
 */
public record OrdemDeTurno(
        List<IdJogador> assentos,
        int posicao,
        Sentido sentido,
        Set<IdJogador> fora) {

    /** Direção em que a vez caminha pela mesa. */
    public enum Sentido {
        HORARIO,
        ANTI_HORARIO;

        public Sentido invertido() {
            return this == HORARIO ? ANTI_HORARIO : HORARIO;
        }
    }

    public OrdemDeTurno {
        Objects.requireNonNull(assentos, "assentos");
        Objects.requireNonNull(sentido, "sentido");
        assentos = List.copyOf(assentos);
        if (assentos.isEmpty()) {
            throw new IllegalArgumentException("ordem de turno precisa de ao menos um assento");
        }
        if (Set.copyOf(assentos).size() != assentos.size()) {
            throw new IllegalArgumentException("assentos repetidos: " + assentos);
        }
        if (posicao < 0 || posicao >= assentos.size()) {
            throw new IllegalArgumentException("posicao fora da mesa: " + posicao);
        }
        fora = Set.copyOf(Objects.requireNonNullElse(fora, Set.of()));
        if (!assentos.containsAll(fora)) {
            throw new IllegalArgumentException("jogador fora da mesa marcado como eliminado");
        }
    }

    /** Ordem inicial: sentido horário, começando pelo primeiro assento. */
    public static OrdemDeTurno de(List<Jogador> jogadores) {
        Objects.requireNonNull(jogadores, "jogadores");
        List<IdJogador> ids = jogadores.stream()
                .sorted((a, b) -> Integer.compare(a.assento().indice(), b.assento().indice()))
                .map(Jogador::id)
                .toList();
        return new OrdemDeTurno(ids, 0, Sentido.HORARIO, Set.of());
    }

    public IdJogador daVez() {
        return assentos.get(posicao);
    }

    public boolean eVezDe(IdJogador jogador) {
        return daVez().equals(jogador);
    }

    public boolean estaFora(IdJogador jogador) {
        return fora.contains(jogador);
    }

    public List<IdJogador> ativos() {
        return assentos.stream().filter(j -> !fora.contains(j)).toList();
    }

    public OrdemDeTurno proxima() {
        return avancando(1);
    }

    /**
     * Avança {@code passos} jogadores ativos na direção corrente.
     *
     * <p>Pular um jogador (carta Skip do UNO) é avançar dois.
     */
    public OrdemDeTurno avancando(int passos) {
        if (passos < 1) {
            throw new IllegalArgumentException("passos deve ser positivo: " + passos);
        }
        if (ativos().isEmpty()) {
            return this;
        }
        int incremento = sentido == Sentido.HORARIO ? 1 : -1;
        int total = assentos.size();
        int atual = posicao;
        for (int i = 0; i < passos; i++) {
            do {
                atual = Math.floorMod(atual + incremento, total);
            } while (fora.contains(assentos.get(atual)));
        }
        return new OrdemDeTurno(assentos, atual, sentido, fora);
    }

    /** Inverte o sentido sem mudar de jogador. */
    public OrdemDeTurno invertida() {
        return new OrdemDeTurno(assentos, posicao, sentido.invertido(), fora);
    }

    /**
     * Marca um jogador como fora.
     *
     * <p>Não avança a vez de propósito, mesmo que o eliminado seja quem está
     * jogando: quem decide se a eliminação encerra o turno é o jogo. Um
     * {@link #proxima()} posterior sai de uma posição inativa sem problema.
     */
    public OrdemDeTurno semJogador(IdJogador jogador) {
        Objects.requireNonNull(jogador, "jogador");
        if (!assentos.contains(jogador)) {
            throw new IllegalArgumentException("jogador nao esta na mesa: " + jogador);
        }
        Set<IdJogador> novos = new LinkedHashSet<>(fora);
        novos.add(jogador);
        return new OrdemDeTurno(assentos, posicao, sentido, novos);
    }
}
