package br.com.mesafranca.games.uno;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.mesafranca.core.domain.Acao;
import br.com.mesafranca.core.domain.Assento;
import br.com.mesafranca.core.domain.ConfiguracaoDePartida;
import br.com.mesafranca.core.domain.Evento;
import br.com.mesafranca.core.domain.IdJogador;
import br.com.mesafranca.core.domain.Jogador;
import br.com.mesafranca.core.domain.OrdemDeTurno;
import br.com.mesafranca.core.domain.Resultado;
import br.com.mesafranca.core.domain.ResultadoDeValidacao;
import br.com.mesafranca.core.domain.Transicao;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("UNO")
class UnoDefinicaoTest {

    private static final IdJogador ANA = new IdJogador("ana");
    private static final IdJogador BRUNO = new IdJogador("bruno");
    private static final IdJogador CARLA = new IdJogador("carla");

    private final UnoDefinicao uno = new UnoDefinicao();

    private static List<Jogador> trio() {
        return List.of(
                Jogador.humano(ANA, new Assento(0)),
                Jogador.humano(BRUNO, new Assento(1)),
                Jogador.humano(CARLA, new Assento(2)));
    }

    private UnoEstado inicial(long semente) {
        return (UnoEstado) uno.estadoInicial(
                ConfiguracaoDePartida.de(UnoDefinicao.ID, 3), trio(), Sorteio.comSemente(semente));
    }

    private static UnoEstado mesa(
            List<IdJogador> assentos,
            Map<IdJogador, List<CartaUno>> maos,
            List<CartaUno> monte,
            CartaUno topo,
            Cor corAtiva) {
        return new UnoEstado(assentos, maos, monte, List.of(topo), corAtiva,
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

    private UnoEstado aplicar(UnoEstado estado, Acao acao) {
        assertThat(uno.validar(estado, acao)).isInstanceOf(ResultadoDeValidacao.Aceita.class);
        return (UnoEstado) uno.aplicar(estado, acao, Sorteio.comSemente(3L)).estado();
    }

    private Transicao transicao(UnoEstado estado, Acao acao) {
        assertThat(uno.validar(estado, acao)).isInstanceOf(ResultadoDeValidacao.Aceita.class);
        return uno.aplicar(estado, acao, Sorteio.comSemente(3L));
    }

    private static String motivo(ResultadoDeValidacao resultado) {
        return ((ResultadoDeValidacao.Recusada) resultado).motivo();
    }

    /**
     * Estrategia do jogador simulado: declarar, jogar, comprar, passar.
     *
     * <p>Nao acusa ninguem de proposito. Acusar sempre e legitimo pelas regras e
     * foi o que o primeiro rascunho deste teste fez - com o efeito de que
     * ninguem jamais vencia, porque toda vez que alguem chegava a uma carta
     * levava mais duas. O bot precisa querer terminar a partida.
     */
    private static Acao escolher(List<Acao> disponiveis) {
        for (Acao acao : disponiveis) {
            if (acao instanceof UnoAcao.DeclararUno) {
                return acao;
            }
        }
        for (Acao acao : disponiveis) {
            if (acao instanceof UnoAcao.JogarCarta) {
                return acao;
            }
        }
        for (Acao acao : disponiveis) {
            if (acao instanceof UnoAcao.Comprar) {
                return acao;
            }
        }
        for (Acao acao : disponiveis) {
            if (acao instanceof UnoAcao.PassarVez) {
                return acao;
            }
        }
        return disponiveis.getFirst();
    }

    // ---------------------------------------------------------------- ciclo

    @Test
    @DisplayName("uma partida inteira, da distribuição ao vencedor")
    void partida_completa_termina_com_vencedor() {
        for (long semente : new long[] {1L, 2L, 3L, 11L, 99L}) {
            Sorteio sorteio = Sorteio.comSemente(semente);
            UnoEstado estado = inicial(semente);

            int jogadas = 0;
            while (estado.vencedor().isEmpty() && jogadas < 20_000) {
                List<Acao> disponiveis = uno.acoesDisponiveis(estado, estado.jogadorDaVez());
                assertThat(disponiveis).isNotEmpty();
                Acao escolhida = escolher(disponiveis);
                assertThat(uno.validar(estado, escolhida))
                        .isInstanceOf(ResultadoDeValidacao.Aceita.class);
                estado = (UnoEstado) uno.aplicar(estado, escolhida, sorteio).estado();
                jogadas++;
            }

            assertThat(estado.vencedor()).isPresent();
            assertThat(estado.mao(estado.vencedor().orElseThrow())).isEmpty();
            assertThat(uno.resultado(estado)).isInstanceOf(Resultado.Encerrado.class);
        }
    }

    @Test
    void as_cento_e_oito_cartas_permanecem_conservadas() {
        Sorteio sorteio = Sorteio.comSemente(77L);
        UnoEstado estado = inicial(77L);

        int jogadas = 0;
        while (estado.vencedor().isEmpty() && jogadas++ < 20_000) {
            int naMesa = estado.monte().size() + estado.descarte().size();
            for (IdJogador jogador : estado.assentos()) {
                naMesa += estado.mao(jogador).size();
            }
            assertThat(naMesa).isEqualTo(CartaUno.baralhoCompleto().size());
            estado = (UnoEstado) uno.aplicar(estado,
                    escolher(uno.acoesDisponiveis(estado, estado.jogadorDaVez())), sorteio).estado();
        }
        assertThat(estado.vencedor()).isPresent();
    }

    @Test
    void a_distribuicao_inicial_da_sete_cartas_e_vira_uma_carta_comum() {
        UnoEstado estado = inicial(5L);

        estado.assentos().forEach(jogador ->
                assertThat(estado.mao(jogador)).hasSize(UnoEstado.CARTAS_INICIAIS));
        assertThat(estado.topo().curinga()).isFalse();
        assertThat(estado.corAtiva()).isEqualTo(estado.topo().cor());
    }

    // ----------------------------------------------------------- fora de turno

    @Test
    @DisplayName("declarar UNO vale fora do próprio turno")
    void declarar_uno_vale_fora_de_turno() {
        UnoEstado estado = mesa(List.of(ANA, BRUNO),
                maos(List.of(CartaUno.numero(Cor.AZUL, 3), CartaUno.numero(Cor.AZUL, 4)),
                        List.of(CartaUno.numero(Cor.VERDE, 9))),
                List.of(CartaUno.numero(Cor.VERDE, 1)),
                CartaUno.numero(Cor.AZUL, 5), Cor.AZUL);

        // É a vez de Ana, mas quem declara é Bruno.
        assertThat(estado.jogadorDaVez()).isEqualTo(ANA);
        UnoEstado depois = aplicar(estado, new UnoAcao.DeclararUno(BRUNO));

        assertThat(depois.declararamUno()).contains(BRUNO);
        assertThat(depois.jogadorDaVez()).isEqualTo(ANA);
        assertThat(uno.acoesDisponiveis(depois, BRUNO)).isEmpty();
    }

    @Test
    @DisplayName("acusar quem esqueceu o UNO vale fora do próprio turno")
    void acusar_uno_vale_fora_de_turno_e_pune() {
        UnoEstado estado = mesa(List.of(ANA, BRUNO),
                maos(List.of(CartaUno.numero(Cor.AZUL, 3), CartaUno.numero(Cor.AZUL, 4)),
                        List.of(CartaUno.numero(Cor.VERDE, 9))),
                List.of(CartaUno.numero(Cor.VERDE, 1), CartaUno.numero(Cor.VERDE, 2)),
                CartaUno.numero(Cor.AZUL, 5), Cor.AZUL);

        assertThat(uno.acoesDisponiveis(estado, ANA)).contains(new UnoAcao.AcusarUno(ANA, BRUNO));
        Transicao punicao = transicao(estado, new UnoAcao.AcusarUno(ANA, BRUNO));
        UnoEstado depois = (UnoEstado) punicao.estado();

        assertThat(depois.mao(BRUNO)).hasSize(1 + UnoEstado.PUNICAO_POR_ESQUECER_UNO);
        assertThat(depois.jogadorDaVez()).isEqualTo(ANA);
        assertThat(punicao.eventos()).anyMatch(e -> e instanceof UnoEvento.UnoPunido);
    }

    @Test
    void quem_declarou_nao_pode_mais_ser_acusado() {
        UnoEstado estado = mesa(List.of(ANA, BRUNO),
                maos(List.of(CartaUno.numero(Cor.AZUL, 3), CartaUno.numero(Cor.AZUL, 4)),
                        List.of(CartaUno.numero(Cor.VERDE, 9))),
                List.of(CartaUno.numero(Cor.VERDE, 1), CartaUno.numero(Cor.VERDE, 2)),
                CartaUno.numero(Cor.AZUL, 5), Cor.AZUL);

        UnoEstado declarou = aplicar(estado, new UnoAcao.DeclararUno(BRUNO));

        assertThat(motivo(uno.validar(declarou, new UnoAcao.AcusarUno(ANA, BRUNO))))
                .contains("por declarar");
        assertThat(motivo(uno.validar(declarou, new UnoAcao.DeclararUno(BRUNO))))
                .contains("ja declarou");
    }

    @Test
    void acusacao_improcedente_e_recusada() {
        UnoEstado estado = mesa(List.of(ANA, BRUNO),
                maos(List.of(CartaUno.numero(Cor.AZUL, 3), CartaUno.numero(Cor.AZUL, 4)),
                        List.of(CartaUno.numero(Cor.VERDE, 9), CartaUno.numero(Cor.VERDE, 8))),
                List.of(CartaUno.numero(Cor.VERDE, 1)),
                CartaUno.numero(Cor.AZUL, 5), Cor.AZUL);

        assertThat(motivo(uno.validar(estado, new UnoAcao.AcusarUno(ANA, BRUNO))))
                .contains("por declarar");
        assertThat(motivo(uno.validar(estado, new UnoAcao.AcusarUno(ANA, ANA))))
                .contains("ninguem se acusa");
        assertThat(motivo(uno.validar(estado, new UnoAcao.AcusarUno(ANA, CARLA))))
                .contains("nao esta na mesa");
        assertThat(motivo(uno.validar(estado, new UnoAcao.DeclararUno(ANA))))
                .contains("uma carta");
    }

    @Test
    void voltar_a_ter_mais_de_uma_carta_apaga_a_declaracao() {
        UnoEstado estado = mesa(List.of(ANA, BRUNO),
                maos(List.of(CartaUno.numero(Cor.AZUL, 3), CartaUno.numero(Cor.AZUL, 4)),
                        List.of(CartaUno.numero(Cor.VERDE, 9))),
                List.of(CartaUno.numero(Cor.VERDE, 1), CartaUno.numero(Cor.VERDE, 2)),
                CartaUno.numero(Cor.AZUL, 5), Cor.AZUL);

        UnoEstado declarou = aplicar(estado, new UnoAcao.DeclararUno(BRUNO));
        UnoEstado comprou = declarou.comMao(BRUNO,
                List.of(CartaUno.numero(Cor.VERDE, 9), CartaUno.numero(Cor.VERDE, 1)));

        assertThat(comprou.declararamUno()).isEmpty();
    }

    // ---------------------------------------------------------------- efeitos

    @Test
    void carta_precisa_combinar_em_cor_ou_em_valor() {
        UnoEstado estado = mesa(List.of(ANA, BRUNO),
                maos(List.of(CartaUno.numero(Cor.AZUL, 3), CartaUno.numero(Cor.VERDE, 5),
                                CartaUno.numero(Cor.VERDE, 2)),
                        List.of(CartaUno.numero(Cor.VERDE, 9))),
                List.of(CartaUno.numero(Cor.VERDE, 1)),
                CartaUno.numero(Cor.AZUL, 5), Cor.AZUL);

        assertThat(motivo(uno.validar(estado,
                UnoAcao.JogarCarta.de(ANA, CartaUno.numero(Cor.VERDE, 2)))))
                .contains("nao combina");
        assertThat(uno.validar(estado, UnoAcao.JogarCarta.de(ANA, CartaUno.numero(Cor.AZUL, 3))))
                .isInstanceOf(ResultadoDeValidacao.Aceita.class);
        assertThat(uno.validar(estado, UnoAcao.JogarCarta.de(ANA, CartaUno.numero(Cor.VERDE, 5))))
                .isInstanceOf(ResultadoDeValidacao.Aceita.class);
    }

    @Test
    void curinga_exige_cor_e_muda_a_cor_ativa() {
        CartaUno curinga = CartaUno.curinga(TipoDeCarta.CORINGA);
        UnoEstado estado = mesa(List.of(ANA, BRUNO),
                maos(List.of(curinga, CartaUno.numero(Cor.AZUL, 4)),
                        List.of(CartaUno.numero(Cor.VERDE, 9))),
                List.of(CartaUno.numero(Cor.VERDE, 1)),
                CartaUno.numero(Cor.AZUL, 5), Cor.AZUL);

        assertThat(motivo(uno.validar(estado, UnoAcao.JogarCarta.de(ANA, curinga))))
                .contains("escolher a cor");
        assertThat(motivo(uno.validar(estado,
                UnoAcao.JogarCarta.escolhendo(ANA, curinga, Cor.PRETO))))
                .contains("preto nao e cor");
        assertThat(motivo(uno.validar(estado, UnoAcao.JogarCarta.escolhendo(
                ANA, CartaUno.numero(Cor.AZUL, 4), Cor.VERDE))))
                .contains("so curinga escolhe cor");

        Transicao jogada = transicao(estado,
                UnoAcao.JogarCarta.escolhendo(ANA, curinga, Cor.VERMELHO));

        assertThat(((UnoEstado) jogada.estado()).corAtiva()).isEqualTo(Cor.VERMELHO);
        assertThat(jogada.eventos())
                .anyMatch(e -> e.equals(new UnoEvento.CorEscolhida(ANA, Cor.VERMELHO)));
    }

    @Test
    void pular_salta_o_proximo_jogador() {
        CartaUno pular = CartaUno.especial(Cor.AZUL, TipoDeCarta.PULAR);
        List<IdJogador> assentos = List.of(ANA, BRUNO, CARLA);
        Map<IdJogador, List<CartaUno>> maos = new LinkedHashMap<>();
        maos.put(ANA, List.of(pular, CartaUno.numero(Cor.AZUL, 4)));
        maos.put(BRUNO, List.of(CartaUno.numero(Cor.VERDE, 9)));
        maos.put(CARLA, List.of(CartaUno.numero(Cor.VERDE, 8)));
        UnoEstado estado = mesa(assentos, maos, List.of(CartaUno.numero(Cor.VERDE, 1)),
                CartaUno.numero(Cor.AZUL, 5), Cor.AZUL);

        Transicao jogada = transicao(estado, UnoAcao.JogarCarta.de(ANA, pular));

        assertThat(((UnoEstado) jogada.estado()).jogadorDaVez()).isEqualTo(CARLA);
        assertThat(jogada.eventos()).anyMatch(e -> e.equals(new UnoEvento.JogadorPulado(BRUNO)));
    }

    @Test
    void inverter_troca_o_sentido_da_mesa() {
        CartaUno inverter = CartaUno.especial(Cor.AZUL, TipoDeCarta.INVERTER);
        List<IdJogador> assentos = List.of(ANA, BRUNO, CARLA);
        Map<IdJogador, List<CartaUno>> maos = new LinkedHashMap<>();
        maos.put(ANA, List.of(inverter, CartaUno.numero(Cor.AZUL, 4)));
        maos.put(BRUNO, List.of(CartaUno.numero(Cor.VERDE, 9)));
        maos.put(CARLA, List.of(CartaUno.numero(Cor.VERDE, 8)));
        UnoEstado estado = mesa(assentos, maos, List.of(CartaUno.numero(Cor.VERDE, 1)),
                CartaUno.numero(Cor.AZUL, 5), Cor.AZUL);

        Transicao jogada = transicao(estado, UnoAcao.JogarCarta.de(ANA, inverter));
        UnoEstado depois = (UnoEstado) jogada.estado();

        assertThat(depois.ordem().sentido()).isEqualTo(OrdemDeTurno.Sentido.ANTI_HORARIO);
        assertThat(depois.jogadorDaVez()).isEqualTo(CARLA);
        assertThat(jogada.eventos()).anyMatch(e -> e instanceof UnoEvento.SentidoInvertido);
    }

    @Test
    void com_dois_jogadores_inverter_devolve_a_vez_a_quem_jogou() {
        CartaUno inverter = CartaUno.especial(Cor.AZUL, TipoDeCarta.INVERTER);
        UnoEstado estado = mesa(List.of(ANA, BRUNO),
                maos(List.of(inverter, CartaUno.numero(Cor.AZUL, 4)),
                        List.of(CartaUno.numero(Cor.VERDE, 9))),
                List.of(CartaUno.numero(Cor.VERDE, 1)),
                CartaUno.numero(Cor.AZUL, 5), Cor.AZUL);

        UnoEstado depois = aplicar(estado, UnoAcao.JogarCarta.de(ANA, inverter));

        assertThat(depois.jogadorDaVez()).isEqualTo(ANA);
    }

    @Test
    void mais_dois_faz_o_proximo_comprar_e_perder_a_vez() {
        CartaUno maisDois = CartaUno.especial(Cor.AZUL, TipoDeCarta.MAIS_DOIS);
        List<IdJogador> assentos = List.of(ANA, BRUNO, CARLA);
        Map<IdJogador, List<CartaUno>> maos = new LinkedHashMap<>();
        maos.put(ANA, List.of(maisDois, CartaUno.numero(Cor.AZUL, 4)));
        maos.put(BRUNO, List.of(CartaUno.numero(Cor.VERDE, 9)));
        maos.put(CARLA, List.of(CartaUno.numero(Cor.VERDE, 8)));
        UnoEstado estado = mesa(assentos, maos,
                List.of(CartaUno.numero(Cor.VERDE, 1), CartaUno.numero(Cor.VERDE, 2),
                        CartaUno.numero(Cor.VERDE, 3)),
                CartaUno.numero(Cor.AZUL, 5), Cor.AZUL);

        UnoEstado depois = aplicar(estado, UnoAcao.JogarCarta.de(ANA, maisDois));

        assertThat(depois.mao(BRUNO)).hasSize(3);
        assertThat(depois.jogadorDaVez()).isEqualTo(CARLA);
    }

    @Test
    void mais_quatro_faz_o_proximo_comprar_quatro() {
        CartaUno maisQuatro = CartaUno.curinga(TipoDeCarta.CORINGA_MAIS_QUATRO);
        List<CartaUno> monte = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            monte.add(CartaUno.numero(Cor.VERDE, i));
        }
        UnoEstado estado = mesa(List.of(ANA, BRUNO),
                maos(List.of(maisQuatro, CartaUno.numero(Cor.AZUL, 4)),
                        List.of(CartaUno.numero(Cor.VERMELHO, 9))),
                monte, CartaUno.numero(Cor.AZUL, 5), Cor.AZUL);

        UnoEstado depois = aplicar(estado,
                UnoAcao.JogarCarta.escolhendo(ANA, maisQuatro, Cor.AMARELO));

        assertThat(depois.mao(BRUNO)).hasSize(5);
        assertThat(depois.corAtiva()).isEqualTo(Cor.AMARELO);
        assertThat(depois.jogadorDaVez()).isEqualTo(ANA);
    }

    @Test
    void comprar_vale_uma_vez_por_turno_e_depois_so_resta_passar() {
        UnoEstado estado = mesa(List.of(ANA, BRUNO),
                maos(List.of(CartaUno.numero(Cor.VERDE, 2)),
                        List.of(CartaUno.numero(Cor.VERDE, 9))),
                List.of(CartaUno.numero(Cor.AZUL, 1)),
                CartaUno.numero(Cor.AZUL, 5), Cor.AZUL);

        assertThat(motivo(uno.validar(estado, new UnoAcao.PassarVez(ANA))))
                .contains("compre antes");

        UnoEstado comprou = aplicar(estado, new UnoAcao.Comprar(ANA));
        assertThat(comprou.mao(ANA)).hasSize(2);
        assertThat(comprou.jogadorDaVez()).isEqualTo(ANA);
        assertThat(motivo(uno.validar(comprou, new UnoAcao.Comprar(ANA)))).contains("ja comprou");
        assertThat(motivo(uno.validar(comprou, new UnoAcao.PassarVez(ANA))))
                .contains("carta jogavel");

        UnoEstado semSaida = comprou.comMao(ANA,
                List.of(CartaUno.numero(Cor.VERDE, 2), CartaUno.numero(Cor.VERDE, 3)));
        UnoEstado passou = aplicar(semSaida, new UnoAcao.PassarVez(ANA));
        assertThat(passou.jogadorDaVez()).isEqualTo(BRUNO);
    }

    @Test
    void monte_vazio_e_remontado_a_partir_do_descarte() {
        List<IdJogador> assentos = List.of(ANA, BRUNO);
        Map<IdJogador, List<CartaUno>> maos = maos(
                List.of(CartaUno.numero(Cor.VERDE, 2)), List.of(CartaUno.numero(Cor.VERDE, 9)));
        List<CartaUno> descarte = List.of(
                CartaUno.numero(Cor.VERMELHO, 1), CartaUno.numero(Cor.VERMELHO, 2),
                CartaUno.numero(Cor.AZUL, 5));
        UnoEstado estado = new UnoEstado(assentos, maos, List.of(), descarte, Cor.AZUL,
                new OrdemDeTurno(assentos, 0, OrdemDeTurno.Sentido.HORARIO, Set.of()),
                false, Set.of(), Optional.empty());

        Transicao compra = transicao(estado, new UnoAcao.Comprar(ANA));
        UnoEstado depois = (UnoEstado) compra.estado();

        assertThat(compra.eventos()).anyMatch(e -> e instanceof UnoEvento.MonteRemontado);
        assertThat(depois.mao(ANA)).hasSize(2);
        assertThat(depois.descarte()).containsExactly(CartaUno.numero(Cor.AZUL, 5));
        assertThat(depois.monte()).hasSize(1);
    }

    // ------------------------------------------------------- informação oculta

    @Test
    void a_compra_sai_redigida_para_quem_nao_comprou() {
        UnoEstado estado = mesa(List.of(ANA, BRUNO),
                maos(List.of(CartaUno.numero(Cor.VERDE, 2)),
                        List.of(CartaUno.numero(Cor.VERDE, 9))),
                List.of(CartaUno.numero(Cor.VERMELHO, 7)),
                CartaUno.numero(Cor.AZUL, 5), Cor.AZUL);

        Transicao compra = transicao(estado, new UnoAcao.Comprar(ANA));
        Evento evento = compra.eventos().getFirst();

        UnoEvento.CartaComprada paraAna = (UnoEvento.CartaComprada)
                uno.eventoVisivelPara(compra.estado(), evento, ANA).orElseThrow();
        UnoEvento.CartaComprada paraBruno = (UnoEvento.CartaComprada)
                uno.eventoVisivelPara(compra.estado(), evento, BRUNO).orElseThrow();

        assertThat(paraAna.cartas()).isPresent();
        assertThat(paraBruno.cartas()).isEmpty();
        // A quantidade continua pública: é o que se vê numa mesa de verdade.
        assertThat(paraBruno.quantidade()).isEqualTo(1);
    }

    @Test
    void a_visao_mostra_a_propria_mao_e_so_a_contagem_das_outras() {
        UnoEstado estado = inicial(13L);

        UnoVisao deAna = (UnoVisao) uno.visaoDe(estado, ANA);
        UnoVisao deBruno = (UnoVisao) uno.visaoDe(estado, BRUNO);

        assertThat(deAna.minhaMao()).isEqualTo(estado.mao(ANA));
        assertThat(deAna).isNotEqualTo(deBruno);
        assertThat(deAna.tamanhoDaMao().get(BRUNO)).isEqualTo(UnoEstado.CARTAS_INICIAIS);
        assertThat(deAna.topoDoDescarte()).isEqualTo(estado.topo());
        assertThat(deAna.sentido()).isEqualTo(OrdemDeTurno.Sentido.HORARIO);
    }

    @Test
    void eventos_publicos_passam_intactos() {
        UnoEstado estado = inicial(13L);
        Evento publico = new UnoEvento.JogadorPulado(BRUNO);

        assertThat(uno.eventoVisivelPara(estado, publico, ANA)).contains(publico);
    }

    // ------------------------------------------------------------- validação

    @Test
    void recusa_acao_fora_de_turno_de_outro_jogo_e_apos_o_fim() {
        UnoEstado estado = inicial(13L);

        assertThat(motivo(uno.validar(estado, new UnoAcao.Comprar(BRUNO)))).contains("nao e a vez");
        assertThat(motivo(uno.validar(estado, new UnoAcao.PassarVez(BRUNO)))).contains("nao e a vez");
        assertThat(motivo(uno.validar(estado,
                UnoAcao.JogarCarta.de(BRUNO, estado.mao(BRUNO).getFirst()))))
                .contains("nao e a vez");

        Acao estrangeira = new Acao() {
            @Override
            public IdJogador autor() {
                return ANA;
            }
        };
        assertThat(motivo(uno.validar(estado, estrangeira))).contains("nao pertence ao UNO");

        UnoEstado encerrada = estado.comVencedor(ANA);
        assertThat(motivo(uno.validar(encerrada, new UnoAcao.Comprar(ANA)))).contains("encerrada");
        assertThat(uno.acoesDisponiveis(encerrada, ANA)).isEmpty();
    }

    @Test
    void recusa_carta_que_nao_esta_na_mao() {
        UnoEstado estado = inicial(13L);

        assertThat(motivo(uno.validar(estado,
                UnoAcao.JogarCarta.de(ANA, CartaUno.numero(Cor.VERMELHO, 3)))))
                .contains("voce nao tem essa carta");
    }

    @Test
    void a_carta_conhece_as_proprias_regras() {
        CartaUno azulTres = CartaUno.numero(Cor.AZUL, 3);
        CartaUno verdeTres = CartaUno.numero(Cor.VERDE, 3);
        CartaUno curinga = CartaUno.curinga(TipoDeCarta.CORINGA);

        assertThat(verdeTres.combinaCom(azulTres, Cor.AZUL)).isTrue();
        assertThat(CartaUno.numero(Cor.VERDE, 4).combinaCom(azulTres, Cor.AZUL)).isFalse();
        assertThat(curinga.combinaCom(azulTres, Cor.AZUL)).isTrue();
        assertThat(CartaUno.numero(Cor.VERDE, 4).combinaCom(curinga, Cor.VERDE)).isTrue();
        assertThat(CartaUno.numero(Cor.AZUL, 4).combinaCom(curinga, Cor.VERDE)).isFalse();
        assertThat(TipoDeCarta.MAIS_DOIS.compraForcada()).isEqualTo(2);
        assertThat(TipoDeCarta.CORINGA_MAIS_QUATRO.compraForcada()).isEqualTo(4);
        assertThat(TipoDeCarta.PULAR.compraForcada()).isZero();
        assertThat(CartaUno.baralhoCompleto()).hasSize(108);
    }

    @Test
    void identificador_e_faixa_de_jogadores() {
        assertThat(uno.identificador()).isEqualTo(UnoDefinicao.ID);
        assertThat(uno.configuracaoSuportada().suporta(2)).isTrue();
        assertThat(uno.configuracaoSuportada().suporta(6)).isTrue();
        assertThat(uno.configuracaoSuportada().suporta(7)).isFalse();
    }
}
