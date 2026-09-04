package br.com.mesafranca.games.uno;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.mesafranca.core.domain.Acao;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Guardas de construção e bordas do UNO.
 *
 * <p>Inclui o caso em que monte e descarte secam ao mesmo tempo — situação que
 * uma partida normal raramente alcança e que, sem teste, só apareceria como
 * uma mesa travada.
 */
@DisplayName("UNO - guardas e bordas")
class UnoBordasTest {

    private static final IdJogador ANA = new IdJogador("ana");
    private static final IdJogador BRUNO = new IdJogador("bruno");

    private final UnoDefinicao uno = new UnoDefinicao();

    private static UnoEstado mesa(
            Map<IdJogador, List<CartaUno>> maos,
            List<CartaUno> monte,
            List<CartaUno> descarte,
            Cor corAtiva) {
        List<IdJogador> assentos = List.of(ANA, BRUNO);
        return new UnoEstado(assentos, maos, monte, descarte, corAtiva,
                new OrdemDeTurno(assentos, 0, OrdemDeTurno.Sentido.HORARIO, Set.of()),
                false, Set.of(), Optional.empty());
    }

    private static Map<IdJogador, List<CartaUno>> maos(
            List<CartaUno> deAna, List<CartaUno> deBruno) {
        Map<IdJogador, List<CartaUno>> maos = new LinkedHashMap<>();
        maos.put(ANA, deAna);
        maos.put(BRUNO, deBruno);
        return maos;
    }

    private Transicao transicao(UnoEstado estado, Acao acao) {
        assertThat(uno.validar(estado, acao)).isInstanceOf(ResultadoDeValidacao.Aceita.class);
        return uno.aplicar(estado, acao, Sorteio.comSemente(3L));
    }

    // ------------------------------------------------------ guardas da carta

    @ParameterizedTest
    @ValueSource(ints = {-1, 10, 42})
    void numero_fora_da_faixa_nao_e_carta(int numero) {
        assertThatThrownBy(() -> CartaUno.numero(Cor.AZUL, numero))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("numero fora de 0..9");
    }

    @Test
    void carta_especial_nao_tem_numero() {
        assertThatThrownBy(() -> new CartaUno(Cor.AZUL, TipoDeCarta.PULAR, 3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nao tem numero");
    }

    @Test
    void curinga_e_preto_e_carta_comum_tem_cor() {
        assertThatThrownBy(() -> new CartaUno(Cor.AZUL, TipoDeCarta.CORINGA, CartaUno.SEM_NUMERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("curinga e preto");
        assertThatThrownBy(() -> new CartaUno(Cor.PRETO, TipoDeCarta.PULAR, CartaUno.SEM_NUMERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("curinga e preto");
        assertThatThrownBy(() -> new CartaUno(null, TipoDeCarta.PULAR, CartaUno.SEM_NUMERO))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CartaUno(Cor.AZUL, null, CartaUno.SEM_NUMERO))
                .isInstanceOf(NullPointerException.class);

        assertThat(CartaUno.curinga(TipoDeCarta.CORINGA).cor()).isEqualTo(Cor.PRETO);
        assertThat(Cor.PRETO.jogavel()).isFalse();
        assertThat(Cor.VERDE.jogavel()).isTrue();
    }

    // ------------------------------------------------------ guardas do estado

    @Test
    void o_descarte_nunca_comeca_vazio_e_a_cor_ativa_nunca_e_preta() {
        Map<IdJogador, List<CartaUno>> maos = maos(
                List.of(CartaUno.numero(Cor.AZUL, 1)), List.of(CartaUno.numero(Cor.AZUL, 2)));
        List<IdJogador> assentos = List.of(ANA, BRUNO);
        OrdemDeTurno ordem = new OrdemDeTurno(assentos, 0, OrdemDeTurno.Sentido.HORARIO, Set.of());

        assertThatThrownBy(() -> new UnoEstado(assentos, maos, List.of(), List.of(), Cor.AZUL,
                ordem, false, Set.of(), Optional.empty()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("descarte comeca com uma carta");

        assertThatThrownBy(() -> new UnoEstado(assentos, maos, List.of(),
                List.of(CartaUno.numero(Cor.AZUL, 5)), Cor.PRETO,
                ordem, false, Set.of(), Optional.empty()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cor ativa nao pode ser preta");
    }

    // ------------------------------------------------------------- monte seco

    @Test
    @DisplayName("com monte e descarte secos, comprar não trava a mesa")
    void monte_e_descarte_secos_nao_travam_a_partida() {
        // Uma carta só no descarte: não há o que remontar.
        UnoEstado estado = mesa(
                maos(List.of(CartaUno.numero(Cor.VERDE, 2)), List.of(CartaUno.numero(Cor.VERDE, 9))),
                List.of(), List.of(CartaUno.numero(Cor.AZUL, 5)), Cor.AZUL);

        Transicao compra = transicao(estado, new UnoAcao.Comprar(ANA));
        UnoEstado depois = (UnoEstado) compra.estado();

        // Ninguém comprou nada, e por isso nenhum evento de compra foi emitido.
        assertThat(depois.mao(ANA)).hasSize(1);
        assertThat(compra.eventos()).noneMatch(e -> e instanceof UnoEvento.CartaComprada);
        assertThat(depois.comprouNestaVez()).isTrue();

        // E ainda assim a vez anda: passar continua sendo possível.
        UnoEstado passou = (UnoEstado) transicao(depois, new UnoAcao.PassarVez(ANA)).estado();
        assertThat(passou.jogadorDaVez()).isEqualTo(BRUNO);
    }

    @Test
    void acusacao_com_monte_seco_nao_pode_ser_repetida() {
        // Sem carta para punir, a acusação não muda nada — e sem marcar o
        // acusado como declarado, a mesma acusação valeria para sempre.
        UnoEstado estado = mesa(
                maos(List.of(CartaUno.numero(Cor.AZUL, 3), CartaUno.numero(Cor.AZUL, 4)),
                        List.of(CartaUno.numero(Cor.VERDE, 9))),
                List.of(), List.of(CartaUno.numero(Cor.AZUL, 5)), Cor.AZUL);

        UnoEstado depois = (UnoEstado) transicao(estado, new UnoAcao.AcusarUno(ANA, BRUNO)).estado();

        assertThat(depois.mao(BRUNO)).hasSize(1);
        assertThat(depois.vulneravelAAcusacao(BRUNO)).isFalse();
        assertThat(((ResultadoDeValidacao.Recusada)
                uno.validar(depois, new UnoAcao.AcusarUno(ANA, BRUNO))).motivo())
                .contains("por declarar");
    }

    @Test
    @DisplayName("o +2 remonta o monte no meio da propria jogada, se precisar")
    void mais_dois_com_monte_curto_remonta_para_completar() {
        UnoEstado estado = mesa(
                maos(List.of(CartaUno.especial(Cor.AZUL, TipoDeCarta.MAIS_DOIS),
                                CartaUno.numero(Cor.AZUL, 4)),
                        List.of(CartaUno.numero(Cor.VERDE, 9))),
                List.of(CartaUno.numero(Cor.VERDE, 1)),
                List.of(CartaUno.numero(Cor.AZUL, 5)), Cor.AZUL);

        UnoEstado depois = (UnoEstado) transicao(estado, UnoAcao.JogarCarta.de(
                ANA, CartaUno.especial(Cor.AZUL, TipoDeCarta.MAIS_DOIS))).estado();

        // O monte tinha uma carta só. A segunda veio de um remonte feito no meio
        // da distribuição: ao jogar o +2, o descarte passou a ter duas cartas, e
        // a de baixo voltou para o monte. Bruno recebe as duas.
        assertThat(depois.mao(BRUNO)).hasSize(3);
        assertThat(depois.jogadorDaVez()).isEqualTo(ANA);
    }

    @Test
    void o_estado_expoe_topo_maos_e_jogabilidade() {
        UnoEstado estado = mesa(
                maos(List.of(CartaUno.numero(Cor.AZUL, 2)), List.of(CartaUno.numero(Cor.VERDE, 9))),
                List.of(CartaUno.numero(Cor.VERDE, 1)),
                List.of(CartaUno.numero(Cor.AZUL, 5)), Cor.AZUL);

        assertThat(estado.topo()).isEqualTo(CartaUno.numero(Cor.AZUL, 5));
        assertThat(estado.podeJogar(ANA)).isTrue();
        assertThat(estado.podeJogar(BRUNO)).isFalse();
        assertThat(estado.tamanhoDasMaos().get(BRUNO)).isEqualTo(1);
        assertThat(estado.mao(new IdJogador("estranho"))).isEmpty();
        assertThat(estado.vulneravelAAcusacao(ANA)).isTrue();
        assertThat(estado.comDeclaracao(ANA).vulneravelAAcusacao(ANA)).isFalse();
    }

    @Test
    void acoes_exigem_campos() {
        assertThatThrownBy(() -> new UnoAcao.Comprar(null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new UnoAcao.PassarVez(null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new UnoAcao.DeclararUno(null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new UnoAcao.AcusarUno(ANA, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new UnoAcao.JogarCarta(ANA, null, Optional.empty()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new UnoAcao.JogarCarta(
                ANA, CartaUno.numero(Cor.AZUL, 1), null))
                .isInstanceOf(NullPointerException.class);
    }
}
