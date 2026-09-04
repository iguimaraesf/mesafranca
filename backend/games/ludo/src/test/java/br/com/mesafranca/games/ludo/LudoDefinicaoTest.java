package br.com.mesafranca.games.ludo;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.mesafranca.core.domain.Acao;
import br.com.mesafranca.core.domain.Assento;
import br.com.mesafranca.core.domain.ConfiguracaoDePartida;
import br.com.mesafranca.core.domain.EstadoDeJogo;
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

@DisplayName("Ludo")
class LudoDefinicaoTest {

    private static final IdJogador ANA = new IdJogador("ana");
    private static final IdJogador BRUNO = new IdJogador("bruno");

    private final LudoDefinicao ludo = new LudoDefinicao();

    private static List<Jogador> dupla() {
        return List.of(Jogador.humano(ANA, new Assento(0)), Jogador.humano(BRUNO, new Assento(1)));
    }

    private LudoEstado inicial() {
        return (LudoEstado) ludo.estadoInicial(
                ConfiguracaoDePartida.de(LudoDefinicao.ID, 2), dupla(), Sorteio.comSemente(1L));
    }

    private static LudoEstado comPecas(Map<IdJogador, List<Integer>> pecas) {
        List<IdJogador> assentos = List.of(ANA, BRUNO);
        Map<IdJogador, Integer> entradas = new LinkedHashMap<>();
        entradas.put(ANA, 0);
        entradas.put(BRUNO, 26);
        return new LudoEstado(assentos, entradas, pecas,
                new OrdemDeTurno(assentos, 0, OrdemDeTurno.Sentido.HORARIO, Set.of()),
                Optional.empty(), 0, Optional.empty());
    }

    private LudoEstado aplicar(LudoEstado estado, Acao acao, Sorteio sorteio) {
        assertThat(ludo.validar(estado, acao)).isInstanceOf(ResultadoDeValidacao.Aceita.class);
        return (LudoEstado) ludo.aplicar(estado, acao, sorteio).estado();
    }

    // ---------------------------------------------------------------- ciclo

    @Test
    @DisplayName("uma partida inteira, do primeiro lance ao vencedor")
    void partida_completa_termina_com_vencedor() {
        // Joga sempre a primeira ação oferecida pelo motor. Se o Ludo tivesse
        // um beco sem saída — nenhuma ação disponível com a partida em aberto —
        // o laço estouraria o limite e o teste falharia.
        Sorteio sorteio = Sorteio.comSemente(20260831L);
        LudoEstado estado = inicial();

        int jogadas = 0;
        while (ludo.resultado(estado) instanceof Resultado.EmAndamento && jogadas < 20_000) {
            IdJogador vez = estado.jogadorDaVez();
            List<Acao> disponiveis = ludo.acoesDisponiveis(estado, vez);
            assertThat(disponiveis).isNotEmpty();
            estado = aplicar(estado, disponiveis.getFirst(), sorteio);
            jogadas++;
        }

        assertThat(ludo.resultado(estado)).isInstanceOf(Resultado.Encerrado.class);
        IdJogador campeao = estado.vencedor().orElseThrow();
        assertThat(estado.pecasDe(campeao))
                .containsExactly(LudoEstado.AVANCO_FINAL, LudoEstado.AVANCO_FINAL,
                        LudoEstado.AVANCO_FINAL, LudoEstado.AVANCO_FINAL);
        assertThat(jogadas).isGreaterThan(50);
    }

    @Test
    void nenhuma_peca_fica_com_avanco_invalido_durante_a_partida() {
        Sorteio sorteio = Sorteio.comSemente(7L);
        LudoEstado estado = inicial();

        for (int i = 0; i < 3_000 && ludo.resultado(estado) instanceof Resultado.EmAndamento; i++) {
            List<Acao> disponiveis = ludo.acoesDisponiveis(estado, estado.jogadorDaVez());
            estado = aplicar(estado, disponiveis.getFirst(), sorteio);
            for (IdJogador jogador : estado.assentos()) {
                assertThat(estado.pecasDe(jogador)).hasSize(LudoEstado.PECAS_POR_JOGADOR);
                assertThat(estado.pecasDe(jogador))
                        .allMatch((Integer avanco) ->
                                avanco >= LudoEstado.NA_BASE && avanco <= LudoEstado.AVANCO_FINAL);
            }
        }
    }

    // ---------------------------------------------------------------- regras

    @Test
    void so_sai_da_base_com_seis() {
        LudoEstado depoisDoTres = aplicar(inicial(), new LudoAcao.RolarDado(ANA),
                Sorteio.roteirizado(3));

        assertThat(ludo.acoesDisponiveis(depoisDoTres, ANA))
                .containsExactly(new LudoAcao.PassarVez(ANA));
        assertThat(ludo.validar(depoisDoTres, new LudoAcao.MoverPeca(ANA, 0)))
                .isInstanceOf(ResultadoDeValidacao.Recusada.class);

        LudoEstado depoisDoSeis = aplicar(inicial(), new LudoAcao.RolarDado(ANA),
                Sorteio.roteirizado(6));

        // Com as quatro peças na base, qualquer uma pode sair.
        assertThat(ludo.acoesDisponiveis(depoisDoSeis, ANA))
                .hasSize(LudoEstado.PECAS_POR_JOGADOR);
        LudoEstado saiu = aplicar(depoisDoSeis, new LudoAcao.MoverPeca(ANA, 0),
                Sorteio.roteirizado());
        assertThat(saiu.avancoDe(ANA, 0)).isZero();
    }

    @Test
    void seis_da_direito_a_outra_jogada() {
        LudoEstado rolou = aplicar(inicial(), new LudoAcao.RolarDado(ANA), Sorteio.roteirizado(6));
        LudoEstado moveu = aplicar(rolou, new LudoAcao.MoverPeca(ANA, 0), Sorteio.roteirizado());

        assertThat(moveu.jogadorDaVez()).isEqualTo(ANA);
        assertThat(moveu.dado()).isEmpty();
    }

    @Test
    void tres_seis_seguidos_anulam_a_jogada() {
        LudoEstado estado = inicial();
        for (int i = 0; i < 2; i++) {
            estado = aplicar(estado, new LudoAcao.RolarDado(ANA), Sorteio.roteirizado(6));
            estado = aplicar(estado, new LudoAcao.MoverPeca(ANA, 0), Sorteio.roteirizado());
        }
        assertThat(estado.seisSeguidos()).isEqualTo(2);

        Transicao terceiro = ludo.aplicar(
                estado, new LudoAcao.RolarDado(ANA), Sorteio.roteirizado(6));
        LudoEstado depois = (LudoEstado) terceiro.estado();

        assertThat(depois.jogadorDaVez()).isEqualTo(BRUNO);
        assertThat(depois.seisSeguidos()).isZero();
        assertThat(terceiro.eventos()).anyMatch(e -> e instanceof LudoEvento.VezPassada);
    }

    @Test
    void capturar_manda_a_peca_adversaria_para_a_base_e_da_outra_vez() {
        // Ana entra na casa 30; Bruno tem uma peça lá (entrada 26, avanço 4).
        Map<IdJogador, List<Integer>> pecas = new LinkedHashMap<>();
        pecas.put(ANA, List.of(28, -1, -1, -1));
        pecas.put(BRUNO, List.of(4, -1, -1, -1));
        LudoEstado antes = comPecas(pecas);

        LudoEstado rolou = aplicar(antes, new LudoAcao.RolarDado(ANA), Sorteio.roteirizado(2));
        Transicao captura = ludo.aplicar(
                rolou, new LudoAcao.MoverPeca(ANA, 0), Sorteio.roteirizado());
        LudoEstado depois = (LudoEstado) captura.estado();

        assertThat(depois.avancoDe(BRUNO, 0)).isEqualTo(LudoEstado.NA_BASE);
        assertThat(depois.avancoDe(ANA, 0)).isEqualTo(30);
        assertThat(captura.eventos())
                .anyMatch(e -> e.equals(new LudoEvento.PecaCapturada(BRUNO, 0, ANA)));
        assertThat(depois.jogadorDaVez()).isEqualTo(ANA);
    }

    @Test
    void nao_captura_na_coluna_final() {
        Map<IdJogador, List<Integer>> pecas = new LinkedHashMap<>();
        pecas.put(ANA, List.of(51, -1, -1, -1));
        pecas.put(BRUNO, List.of(51, -1, -1, -1));
        LudoEstado antes = comPecas(pecas);

        LudoEstado rolou = aplicar(antes, new LudoAcao.RolarDado(ANA), Sorteio.roteirizado(1));
        LudoEstado depois = aplicar(rolou, new LudoAcao.MoverPeca(ANA, 0), Sorteio.roteirizado());

        assertThat(depois.avancoDe(BRUNO, 0)).isEqualTo(51);
    }

    @Test
    void chegar_exige_o_numero_exato() {
        Map<IdJogador, List<Integer>> pecas = new LinkedHashMap<>();
        pecas.put(ANA, List.of(54, LudoEstado.AVANCO_FINAL, LudoEstado.AVANCO_FINAL,
                LudoEstado.AVANCO_FINAL));
        pecas.put(BRUNO, List.of(-1, -1, -1, -1));
        LudoEstado antes = comPecas(pecas);

        LudoEstado demais = aplicar(antes, new LudoAcao.RolarDado(ANA), Sorteio.roteirizado(3));
        assertThat(ludo.acoesDisponiveis(demais, ANA))
                .containsExactly(new LudoAcao.PassarVez(ANA));

        LudoEstado exato = aplicar(antes, new LudoAcao.RolarDado(ANA), Sorteio.roteirizado(2));
        Transicao chegada = ludo.aplicar(
                exato, new LudoAcao.MoverPeca(ANA, 0), Sorteio.roteirizado());

        assertThat(chegada.eventos()).anyMatch(e -> e instanceof LudoEvento.PartidaVencida);
        assertThat(ludo.resultado(chegada.estado())).isInstanceOf(Resultado.Encerrado.class);
        assertThat(((Resultado.Encerrado) ludo.resultado(chegada.estado())).vencedores())
                .containsExactly(ANA);
    }

    @Test
    void duas_pecas_proprias_nao_dividem_a_mesma_posicao() {
        Map<IdJogador, List<Integer>> pecas = new LinkedHashMap<>();
        pecas.put(ANA, List.of(10, 13, -1, -1));
        pecas.put(BRUNO, List.of(-1, -1, -1, -1));
        LudoEstado rolou = aplicar(comPecas(pecas), new LudoAcao.RolarDado(ANA),
                Sorteio.roteirizado(3));

        List<Integer> moveis = new ArrayList<>();
        for (Acao acao : ludo.acoesDisponiveis(rolou, ANA)) {
            moveis.add(((LudoAcao.MoverPeca) acao).peca());
        }

        assertThat(moveis).containsExactly(1);
    }

    // ------------------------------------------------------------- validação

    @Test
    void recusa_acao_fora_de_turno_e_de_outro_jogo() {
        LudoEstado estado = inicial();

        assertThat(ludo.validar(estado, new LudoAcao.RolarDado(BRUNO)))
                .isInstanceOf(ResultadoDeValidacao.Recusada.class);
        assertThat(ludo.acoesDisponiveis(estado, BRUNO)).isEmpty();

        Acao estrangeira = new Acao() {
            @Override
            public IdJogador autor() {
                return ANA;
            }
        };
        assertThat(((ResultadoDeValidacao.Recusada) ludo.validar(estado, estrangeira)).motivo())
                .contains("nao pertence ao Ludo");
    }

    @Test
    void recusa_mover_antes_de_rolar_e_rolar_duas_vezes() {
        LudoEstado estado = inicial();
        assertThat(((ResultadoDeValidacao.Recusada)
                ludo.validar(estado, new LudoAcao.MoverPeca(ANA, 0))).motivo())
                .contains("role o dado");
        assertThat(((ResultadoDeValidacao.Recusada)
                ludo.validar(estado, new LudoAcao.PassarVez(ANA))).motivo())
                .contains("role o dado");

        LudoEstado rolou = aplicar(estado, new LudoAcao.RolarDado(ANA), Sorteio.roteirizado(6));
        assertThat(((ResultadoDeValidacao.Recusada)
                ludo.validar(rolou, new LudoAcao.RolarDado(ANA))).motivo())
                .contains("ja foi rolado");
        assertThat(((ResultadoDeValidacao.Recusada)
                ludo.validar(rolou, new LudoAcao.PassarVez(ANA))).motivo())
                .contains("ha movimento possivel");
    }

    @Test
    void partida_encerrada_nao_aceita_mais_nada() {
        Map<IdJogador, List<Integer>> pecas = new LinkedHashMap<>();
        int fim = LudoEstado.AVANCO_FINAL;
        pecas.put(ANA, List.of(fim, fim, fim, fim));
        pecas.put(BRUNO, List.of(-1, -1, -1, -1));
        LudoEstado encerrada = comPecas(pecas).comVencedor(ANA);

        assertThat(((ResultadoDeValidacao.Recusada)
                ludo.validar(encerrada, new LudoAcao.RolarDado(ANA))).motivo())
                .contains("encerrada");
        assertThat(ludo.acoesDisponiveis(encerrada, ANA)).isEmpty();
    }

    // --------------------------------------------------------------- projeção

    @Test
    void tabuleiro_se_ajusta_ao_numero_de_jogadores() {
        Map<Integer, List<Integer>> esperado = Map.of(
                2, List.of(0, 26),
                3, List.of(0, 17, 34),
                4, List.of(0, 13, 26, 39));

        for (Map.Entry<Integer, List<Integer>> caso : esperado.entrySet()) {
            List<Jogador> mesa = new ArrayList<>();
            for (int i = 0; i < caso.getKey(); i++) {
                mesa.add(Jogador.humano(new IdJogador("j" + i), new Assento(i)));
            }
            LudoEstado estado = (LudoEstado) ludo.estadoInicial(
                    ConfiguracaoDePartida.de(LudoDefinicao.ID, caso.getKey()),
                    mesa, Sorteio.comSemente(1L));

            List<Integer> entradas = new ArrayList<>();
            estado.assentos().forEach(jogador -> entradas.add(estado.entradas().get(jogador)));
            assertThat(entradas).containsExactly(caso.getValue().toArray(new Integer[0]));
        }
    }

    @Test
    void a_visao_e_a_mesma_para_todos_porque_nao_ha_segredo() {
        LudoEstado estado = inicial();

        LudoVisao deAna = (LudoVisao) ludo.visaoDe(estado, ANA);
        LudoVisao deBruno = (LudoVisao) ludo.visaoDe(estado, BRUNO);

        assertThat(deAna).isEqualTo(deBruno);
        assertThat(deAna.jogadorDaVez()).isEqualTo(ANA);
        assertThat(deAna.pecas()).hasSize(2);
    }

    @Test
    void todo_evento_do_ludo_e_publico() {
        LudoEstado estado = inicial();
        EstadoDeJogo qualquer = estado;
        LudoEvento evento = new LudoEvento.DadoRolado(ANA, 6);

        assertThat(ludo.eventoVisivelPara(qualquer, evento, BRUNO)).contains(evento);
    }

    @Test
    void identificador_e_faixa_de_jogadores() {
        assertThat(ludo.identificador()).isEqualTo(LudoDefinicao.ID);
        assertThat(ludo.configuracaoSuportada().suporta(2)).isTrue();
        assertThat(ludo.configuracaoSuportada().suporta(4)).isTrue();
        assertThat(ludo.configuracaoSuportada().suporta(5)).isFalse();
    }
}
