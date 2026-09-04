package br.com.mesafranca.games.loveletter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.mesafranca.core.domain.Acao;
import br.com.mesafranca.core.domain.Evento;
import br.com.mesafranca.core.domain.IdJogador;
import br.com.mesafranca.core.domain.OrdemDeTurno;
import br.com.mesafranca.core.domain.ResultadoDeValidacao;
import br.com.mesafranca.core.domain.Transicao;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * As bordas do Love Letter: empate no Barão, mesa toda protegida, baralho no
 * fim e evento de outro jogo.
 *
 * <p>Caminhos que uma partida normal quase nunca produz e que, por isso, só
 * seriam descobertos em produção — que é o pior lugar para descobrir uma
 * comparação de cartas errada.
 */
@DisplayName("Love Letter - bordas")
class LoveLetterBordasTest {

    private static final IdJogador ANA = new IdJogador("ana");
    private static final IdJogador BRUNO = new IdJogador("bruno");
    private static final IdJogador CARLA = new IdJogador("carla");

    private final LoveLetterDefinicao jogo = new LoveLetterDefinicao();

    private static LoveLetterEstado mesa(
            List<IdJogador> assentos,
            Map<IdJogador, List<Carta>> maos,
            List<Carta> baralho,
            Set<IdJogador> protegidos) {
        Map<IdJogador, List<Carta>> descartes = new LinkedHashMap<>();
        assentos.forEach(jogador -> descartes.put(jogador, List.of()));
        return new LoveLetterEstado(assentos, maos, baralho, Carta.GUARDA, descartes,
                protegidos, new OrdemDeTurno(assentos, 0, OrdemDeTurno.Sentido.HORARIO, Set.of()),
                Fase.JOGAR, List.of(), false);
    }

    private static Map<IdJogador, List<Carta>> maos(
            List<Carta> deAna, List<Carta> deBruno, List<Carta> deCarla) {
        Map<IdJogador, List<Carta>> maos = new LinkedHashMap<>();
        maos.put(ANA, deAna);
        maos.put(BRUNO, deBruno);
        if (deCarla != null) {
            maos.put(CARLA, deCarla);
        }
        return maos;
    }

    private Transicao transicao(LoveLetterEstado estado, Acao acao) {
        assertThat(jogo.validar(estado, acao)).isInstanceOf(ResultadoDeValidacao.Aceita.class);
        return jogo.aplicar(estado, acao, Sorteio.comSemente(1L));
    }

    @Test
    @DisplayName("empate no Barão não elimina ninguém")
    void barao_com_cartas_iguais_nao_elimina() {
        LoveLetterEstado estado = mesa(List.of(ANA, BRUNO),
                maos(List.of(Carta.BARAO, Carta.AIA), List.of(Carta.AIA), null),
                List.of(Carta.REI, Carta.REI), Set.of());

        LoveLetterEstado depois = (LoveLetterEstado) transicao(estado,
                LoveLetterAcao.JogarCarta.mirando(ANA, Carta.BARAO, BRUNO)).estado();

        assertThat(depois.encerrada()).isFalse();
        assertThat(depois.ativo(ANA)).isTrue();
        assertThat(depois.ativo(BRUNO)).isTrue();
        assertThat(depois.jogadorDaVez()).isEqualTo(BRUNO);
    }

    @Test
    @DisplayName("no Barão, quem tem a carta menor sai — inclusive quem jogou")
    void barao_pode_eliminar_o_proprio_autor() {
        LoveLetterEstado estado = mesa(List.of(ANA, BRUNO),
                maos(List.of(Carta.BARAO, Carta.GUARDA), List.of(Carta.PRINCESA), null),
                List.of(Carta.REI, Carta.REI), Set.of());

        LoveLetterEstado depois = (LoveLetterEstado) transicao(estado,
                LoveLetterAcao.JogarCarta.mirando(ANA, Carta.BARAO, BRUNO)).estado();

        assertThat(depois.vencedores()).containsExactly(BRUNO);
    }

    @Test
    @DisplayName("com a mesa toda protegida, a Guarda é jogada sem alvo e sem efeito")
    void guarda_sem_alvo_possivel_nao_exige_palpite() {
        LoveLetterEstado estado = mesa(List.of(ANA, BRUNO),
                maos(List.of(Carta.GUARDA, Carta.CONDESSA), List.of(Carta.PRINCESA), null),
                List.of(Carta.REI, Carta.REI), Set.of(BRUNO));

        assertThat(LoveLetterDefinicao.alvosPossiveis(estado, ANA, Carta.GUARDA)).isEmpty();
        assertThat(jogo.acoesDisponiveis(estado, ANA))
                .contains(LoveLetterAcao.JogarCarta.simples(ANA, Carta.GUARDA));

        LoveLetterEstado depois = (LoveLetterEstado) transicao(estado,
                LoveLetterAcao.JogarCarta.simples(ANA, Carta.GUARDA)).estado();

        assertThat(depois.ativo(BRUNO)).isTrue();
        assertThat(depois.descartes().get(ANA)).containsExactly(Carta.GUARDA);
    }

    @Test
    @DisplayName("Príncipe com o baralho vazio entrega a carta retirada no início")
    void principe_com_baralho_vazio_usa_a_carta_removida() {
        List<IdJogador> assentos = List.of(ANA, BRUNO, CARLA);
        Map<IdJogador, List<Carta>> maos = maos(
                List.of(Carta.PRINCIPE, Carta.GUARDA), List.of(Carta.BARAO), List.of(Carta.AIA));
        Map<IdJogador, List<Carta>> descartes = new LinkedHashMap<>();
        assentos.forEach(jogador -> descartes.put(jogador, List.of()));
        LoveLetterEstado estado = new LoveLetterEstado(assentos, maos, List.of(), Carta.REI,
                descartes, Set.of(),
                new OrdemDeTurno(assentos, 0, OrdemDeTurno.Sentido.HORARIO, Set.of()),
                Fase.JOGAR, List.of(), false);

        LoveLetterEstado depois = (LoveLetterEstado) transicao(estado,
                LoveLetterAcao.JogarCarta.mirando(ANA, Carta.PRINCIPE, BRUNO)).estado();

        assertThat(depois.descartes().get(BRUNO)).containsExactly(Carta.BARAO);
        // Bruno recebeu a carta que fora retirada da rodada.
        assertThat(depois.mao(BRUNO)).containsExactly(Carta.REI);
        // Baralho vazio encerra a rodada logo em seguida.
        assertThat(depois.encerrada()).isTrue();
        assertThat(depois.vencedores()).containsExactly(BRUNO);
    }

    @Test
    @DisplayName("todo tipo de evento passa pela redacao, e so tres perdem conteudo")
    void a_redacao_cobre_a_hierarquia_inteira_de_eventos() {
        LoveLetterEstado estado = mesa(List.of(ANA, BRUNO),
                maos(List.of(Carta.GUARDA, Carta.AIA), List.of(Carta.PRINCESA), null),
                List.of(Carta.REI), Set.of());

        List<Evento> todos = List.of(
                new LoveLetterEvento.CartaComprada(ANA, Optional.of(Carta.REI)),
                new LoveLetterEvento.CartaJogada(
                        ANA, Carta.GUARDA, Optional.of(BRUNO), Optional.of(Carta.REI)),
                new LoveLetterEvento.MaoEspiada(ANA, BRUNO, Optional.of(Carta.PRINCESA)),
                new LoveLetterEvento.MaosComparadas(
                        ANA, BRUNO, Optional.of(Carta.BARAO), Optional.of(Carta.PRINCESA)),
                new LoveLetterEvento.MaosTrocadas(ANA, BRUNO),
                new LoveLetterEvento.JogadorProtegido(ANA),
                new LoveLetterEvento.JogadorEliminado(BRUNO, Carta.PRINCESA, "motivo"),
                new LoveLetterEvento.VezPassada(ANA, BRUNO),
                new LoveLetterEvento.RodadaEncerrada(List.of(ANA), "fim"));

        // Nenhum evento some para o terceiro: a mesa sempre sabe que algo houve.
        for (Evento evento : todos) {
            assertThat(jogo.eventoVisivelPara(estado, evento, CARLA)).isPresent();
            assertThat(jogo.eventoVisivelPara(estado, evento, ANA)).isPresent();
        }

        // E os cinco publicos atravessam identicos, sem copia nem redacao.
        for (Evento publico : todos.subList(4, todos.size())) {
            assertThat(jogo.eventoVisivelPara(estado, publico, CARLA)).contains(publico);
        }
        assertThat(jogo.eventoVisivelPara(estado, todos.get(1), CARLA)).contains(todos.get(1));
    }

    @Test
    @DisplayName("a Condessa e jogavel e nao faz nada")
    void condessa_e_descartada_sem_efeito() {
        LoveLetterEstado estado = mesa(List.of(ANA, BRUNO),
                maos(List.of(Carta.CONDESSA, Carta.AIA), List.of(Carta.BARAO), null),
                List.of(Carta.REI, Carta.REI), Set.of());

        LoveLetterEstado depois = (LoveLetterEstado) transicao(estado,
                LoveLetterAcao.JogarCarta.simples(ANA, Carta.CONDESSA)).estado();

        assertThat(depois.descartes().get(ANA)).containsExactly(Carta.CONDESSA);
        assertThat(depois.mao(ANA)).containsExactly(Carta.AIA);
        assertThat(depois.ativo(ANA)).isTrue();
        assertThat(depois.jogadorDaVez()).isEqualTo(BRUNO);
    }

    @Test
    void evento_de_outro_jogo_atravessa_sem_redacao() {
        LoveLetterEstado estado = mesa(List.of(ANA, BRUNO),
                maos(List.of(Carta.GUARDA, Carta.AIA), List.of(Carta.PRINCESA), null),
                List.of(Carta.REI), Set.of());
        Evento estrangeiro = new Evento() {
        };

        assertThat(jogo.eventoVisivelPara(estado, estrangeiro, ANA)).contains(estrangeiro);
    }

    @Test
    void mao_com_duas_cartas_nao_tem_carta_unica() {
        LoveLetterEstado estado = mesa(List.of(ANA, BRUNO),
                maos(List.of(Carta.GUARDA, Carta.AIA), List.of(Carta.PRINCESA), null),
                List.of(Carta.REI), Set.of());

        assertThat(estado.cartaUnica(BRUNO)).isEqualTo(Carta.PRINCESA);
        assertThatThrownBy(() -> estado.cartaUnica(ANA))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("tem 2 cartas");
    }

    @Test
    void o_estado_expoe_protecao_eliminacao_e_tamanho_das_maos() {
        LoveLetterEstado estado = mesa(List.of(ANA, BRUNO),
                maos(List.of(Carta.GUARDA, Carta.AIA), List.of(Carta.PRINCESA), null),
                List.of(Carta.REI), Set.of(BRUNO));

        assertThat(estado.protegido(BRUNO)).isTrue();
        assertThat(estado.protegido(ANA)).isFalse();
        assertThat(estado.eliminados()).isEmpty();
        assertThat(estado.tamanhoDasMaos().get(ANA)).isEqualTo(2);
        assertThat(estado.mao(new IdJogador("estranho"))).isEmpty();
        assertThat(estado.semProtecao(BRUNO).protegido(BRUNO)).isFalse();
    }

    @Test
    void acoes_exigem_campos() {
        assertThatThrownBy(() -> new LoveLetterAcao.ComprarCarta(null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new LoveLetterAcao.JogarCarta(
                ANA, null, Optional.empty(), Optional.empty()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new LoveLetterAcao.JogarCarta(
                ANA, Carta.AIA, null, Optional.empty()))
                .isInstanceOf(NullPointerException.class);
    }
}
