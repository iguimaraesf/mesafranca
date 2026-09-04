package br.com.mesafranca.games.loveletter;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Love Letter")
class LoveLetterDefinicaoTest {

    private static final IdJogador ANA = new IdJogador("ana");
    private static final IdJogador BRUNO = new IdJogador("bruno");
    private static final IdJogador CARLA = new IdJogador("carla");

    private final LoveLetterDefinicao jogo = new LoveLetterDefinicao();

    private static List<Jogador> trio() {
        return List.of(
                Jogador.humano(ANA, new Assento(0)),
                Jogador.humano(BRUNO, new Assento(1)),
                Jogador.humano(CARLA, new Assento(2)));
    }

    private LoveLetterEstado inicial(long semente) {
        return (LoveLetterEstado) jogo.estadoInicial(
                ConfiguracaoDePartida.de(LoveLetterDefinicao.ID, 3),
                trio(), Sorteio.comSemente(semente));
    }

    /** Monta uma mesa de duas pessoas com mãos e baralho escolhidos a dedo. */
    private static LoveLetterEstado mesaDeDois(
            List<Carta> maoDeAna, List<Carta> maoDeBruno, List<Carta> baralho, Fase fase) {
        List<IdJogador> assentos = List.of(ANA, BRUNO);
        Map<IdJogador, List<Carta>> maos = new LinkedHashMap<>();
        maos.put(ANA, maoDeAna);
        maos.put(BRUNO, maoDeBruno);
        Map<IdJogador, List<Carta>> descartes = new LinkedHashMap<>();
        descartes.put(ANA, List.of());
        descartes.put(BRUNO, List.of());
        return new LoveLetterEstado(assentos, maos, baralho, Carta.GUARDA, descartes,
                Set.of(), new OrdemDeTurno(assentos, 0, OrdemDeTurno.Sentido.HORARIO, Set.of()),
                fase, List.of(), false);
    }

    private LoveLetterEstado aplicar(LoveLetterEstado estado, Acao acao) {
        assertThat(jogo.validar(estado, acao)).isInstanceOf(ResultadoDeValidacao.Aceita.class);
        return (LoveLetterEstado) jogo.aplicar(estado, acao, Sorteio.comSemente(1L)).estado();
    }

    private Transicao transicao(LoveLetterEstado estado, Acao acao) {
        assertThat(jogo.validar(estado, acao)).isInstanceOf(ResultadoDeValidacao.Aceita.class);
        return jogo.aplicar(estado, acao, Sorteio.comSemente(1L));
    }

    private static String motivo(ResultadoDeValidacao resultado) {
        return ((ResultadoDeValidacao.Recusada) resultado).motivo();
    }

    // ---------------------------------------------------------------- ciclo

    @Test
    @DisplayName("uma rodada inteira, da distribuição ao vencedor")
    void rodada_completa_termina_com_vencedor() {
        for (long semente = 1; semente <= 25; semente++) {
            LoveLetterEstado estado = inicial(semente);
            assertThat(estado.baralho()).hasSize(Carta.baralhoCompleto().size() - 1 - 3);

            int jogadas = 0;
            while (!estado.encerrada() && jogadas < 200) {
                List<Acao> disponiveis = jogo.acoesDisponiveis(estado, estado.jogadorDaVez());
                assertThat(disponiveis).isNotEmpty();
                estado = aplicar(estado, disponiveis.getFirst());
                jogadas++;
            }

            assertThat(estado.encerrada()).isTrue();
            assertThat(estado.vencedores()).isNotEmpty();
            assertThat(jogo.resultado(estado)).isInstanceOf(Resultado.Encerrado.class);
        }
    }

    @Test
    void as_dezesseis_cartas_permanecem_conservadas() {
        LoveLetterEstado estado = inicial(42L);

        while (!estado.encerrada()) {
            int naMesa = estado.baralho().size() + 1;
            for (IdJogador jogador : estado.assentos()) {
                naMesa += estado.mao(jogador).size() + estado.descartes().get(jogador).size();
            }
            assertThat(naMesa).isEqualTo(Carta.baralhoCompleto().size());
            estado = aplicar(estado, jogo.acoesDisponiveis(estado, estado.jogadorDaVez()).getFirst());
        }
    }

    // ------------------------------------------------------- informação oculta

    @Test
    @DisplayName("a visão de um jogador não carrega a mão de ninguém mais")
    void visao_nao_revela_a_mao_alheia() {
        LoveLetterEstado estado = inicial(5L);

        LoveLetterVisao deAna = (LoveLetterVisao) jogo.visaoDe(estado, ANA);
        LoveLetterVisao deBruno = (LoveLetterVisao) jogo.visaoDe(estado, BRUNO);

        assertThat(deAna.minhaMao()).isEqualTo(estado.mao(ANA));
        assertThat(deBruno.minhaMao()).isEqualTo(estado.mao(BRUNO));
        assertThat(deAna).isNotEqualTo(deBruno);
        // O que sai sobre os outros é só a contagem.
        assertThat(deAna.tamanhoDaMao()).hasSize(3);
        assertThat(deAna.tamanhoDaMao().get(BRUNO)).isEqualTo(1);
        assertThat(deAna.cartasNoBaralho()).isEqualTo(estado.baralho().size());
    }

    @Test
    void a_compra_sai_redigida_para_quem_nao_comprou() {
        LoveLetterEstado estado = inicial(5L);
        Transicao compra = transicao(estado, new LoveLetterAcao.ComprarCarta(ANA));
        Evento evento = compra.eventos().getFirst();

        Evento paraAna = jogo.eventoVisivelPara(compra.estado(), evento, ANA).orElseThrow();
        Evento paraBruno = jogo.eventoVisivelPara(compra.estado(), evento, BRUNO).orElseThrow();

        assertThat(((LoveLetterEvento.CartaComprada) paraAna).carta()).isPresent();
        // Bruno continua sabendo que Ana comprou: some o conteúdo, não o fato.
        assertThat(((LoveLetterEvento.CartaComprada) paraBruno).carta()).isEmpty();
        assertThat(((LoveLetterEvento.CartaComprada) paraBruno).jogador()).isEqualTo(ANA);
    }

    @Test
    void o_padre_espia_e_so_o_espiao_enxerga() {
        LoveLetterEstado estado = mesaDeDois(
                List.of(Carta.PADRE, Carta.AIA), List.of(Carta.PRINCESA),
                List.of(Carta.GUARDA, Carta.GUARDA), Fase.JOGAR);

        Transicao espiada = transicao(estado,
                LoveLetterAcao.JogarCarta.mirando(ANA, Carta.PADRE, BRUNO));
        LoveLetterEvento.MaoEspiada evento = espiada.eventos().stream()
                .filter(LoveLetterEvento.MaoEspiada.class::isInstance)
                .map(LoveLetterEvento.MaoEspiada.class::cast)
                .findFirst()
                .orElseThrow();

        assertThat(evento.carta()).contains(Carta.PRINCESA);
        assertThat(((LoveLetterEvento.MaoEspiada)
                jogo.eventoVisivelPara(espiada.estado(), evento, BRUNO).orElseThrow()).carta())
                .isEmpty();
        assertThat(((LoveLetterEvento.MaoEspiada)
                jogo.eventoVisivelPara(espiada.estado(), evento, ANA).orElseThrow()).carta())
                .contains(Carta.PRINCESA);
    }

    @Test
    void a_comparacao_do_barao_so_e_vista_pelos_dois_envolvidos() {
        List<IdJogador> assentos = List.of(ANA, BRUNO, CARLA);
        Map<IdJogador, List<Carta>> maos = new LinkedHashMap<>();
        maos.put(ANA, List.of(Carta.BARAO, Carta.REI));
        maos.put(BRUNO, List.of(Carta.GUARDA));
        maos.put(CARLA, List.of(Carta.AIA));
        Map<IdJogador, List<Carta>> descartes = new LinkedHashMap<>();
        assentos.forEach(jogador -> descartes.put(jogador, List.of()));
        LoveLetterEstado estado = new LoveLetterEstado(assentos, maos,
                List.of(Carta.GUARDA, Carta.GUARDA), Carta.PRINCESA, descartes, Set.of(),
                new OrdemDeTurno(assentos, 0, OrdemDeTurno.Sentido.HORARIO, Set.of()),
                Fase.JOGAR, List.of(), false);

        Transicao duelo = transicao(estado,
                LoveLetterAcao.JogarCarta.mirando(ANA, Carta.BARAO, BRUNO));
        LoveLetterEvento.MaosComparadas comparacao = duelo.eventos().stream()
                .filter(LoveLetterEvento.MaosComparadas.class::isInstance)
                .map(LoveLetterEvento.MaosComparadas.class::cast)
                .findFirst()
                .orElseThrow();

        assertThat(((LoveLetterEvento.MaosComparadas) jogo
                .eventoVisivelPara(duelo.estado(), comparacao, ANA).orElseThrow()).cartaDoAlvo())
                .contains(Carta.GUARDA);
        assertThat(((LoveLetterEvento.MaosComparadas) jogo
                .eventoVisivelPara(duelo.estado(), comparacao, BRUNO).orElseThrow()).cartaDoAutor())
                .contains(Carta.REI);
        LoveLetterEvento.MaosComparadas paraCarla = (LoveLetterEvento.MaosComparadas) jogo
                .eventoVisivelPara(duelo.estado(), comparacao, CARLA).orElseThrow();
        assertThat(paraCarla.cartaDoAutor()).isEmpty();
        assertThat(paraCarla.cartaDoAlvo()).isEmpty();
    }

    @Test
    void nenhum_evento_e_escondido_por_completo() {
        // Sumir com o evento entregaria pelo silêncio o que a redação esconde.
        LoveLetterEstado estado = inicial(9L);
        Transicao compra = transicao(estado, new LoveLetterAcao.ComprarCarta(ANA));

        for (Evento evento : compra.eventos()) {
            assertThat(jogo.eventoVisivelPara(compra.estado(), evento, CARLA)).isPresent();
        }
    }

    // ---------------------------------------------------------------- efeitos

    @Test
    void guarda_acerta_e_elimina() {
        LoveLetterEstado estado = mesaDeDois(
                List.of(Carta.GUARDA, Carta.AIA), List.of(Carta.PRINCIPE),
                List.of(Carta.REI, Carta.BARAO), Fase.JOGAR);

        LoveLetterEstado depois = aplicar(estado,
                LoveLetterAcao.JogarCarta.adivinhando(ANA, BRUNO, Carta.PRINCIPE));

        assertThat(depois.encerrada()).isTrue();
        assertThat(depois.vencedores()).containsExactly(ANA);
    }

    @Test
    void guarda_erra_e_ninguem_sai() {
        LoveLetterEstado estado = mesaDeDois(
                List.of(Carta.GUARDA, Carta.AIA), List.of(Carta.PRINCIPE),
                List.of(Carta.REI, Carta.BARAO), Fase.JOGAR);

        LoveLetterEstado depois = aplicar(estado,
                LoveLetterAcao.JogarCarta.adivinhando(ANA, BRUNO, Carta.REI));

        assertThat(depois.encerrada()).isFalse();
        assertThat(depois.jogadorDaVez()).isEqualTo(BRUNO);
        assertThat(depois.ativo(BRUNO)).isTrue();
    }

    @Test
    void barao_elimina_quem_tem_a_carta_menor() {
        LoveLetterEstado estado = mesaDeDois(
                List.of(Carta.BARAO, Carta.REI), List.of(Carta.GUARDA),
                List.of(Carta.AIA, Carta.AIA), Fase.JOGAR);

        LoveLetterEstado depois = aplicar(estado,
                LoveLetterAcao.JogarCarta.mirando(ANA, Carta.BARAO, BRUNO));

        assertThat(depois.vencedores()).containsExactly(ANA);
    }

    @Test
    void aia_protege_ate_a_propria_vez() {
        LoveLetterEstado estado = mesaDeDois(
                List.of(Carta.AIA, Carta.GUARDA), List.of(Carta.PADRE),
                List.of(Carta.REI, Carta.BARAO, Carta.CONDESSA), Fase.JOGAR);

        LoveLetterEstado protegida = aplicar(estado,
                LoveLetterAcao.JogarCarta.simples(ANA, Carta.AIA));
        assertThat(protegida.protegido(ANA)).isTrue();

        LoveLetterEstado brunoComprou = aplicar(protegida,
                new LoveLetterAcao.ComprarCarta(BRUNO));
        assertThat(LoveLetterDefinicao.alvosPossiveis(brunoComprou, BRUNO, Carta.PADRE)).isEmpty();
        assertThat(motivo(jogo.validar(brunoComprou,
                LoveLetterAcao.JogarCarta.mirando(BRUNO, Carta.PADRE, ANA))))
                .contains("alvo invalido");

        LoveLetterEstado semAlvo = aplicar(brunoComprou,
                LoveLetterAcao.JogarCarta.simples(BRUNO, Carta.PADRE));
        LoveLetterEstado anaJogaDeNovo = aplicar(semAlvo, new LoveLetterAcao.ComprarCarta(ANA));

        assertThat(anaJogaDeNovo.protegido(ANA)).isFalse();
    }

    @Test
    void principe_forca_descarte_e_recompra() {
        LoveLetterEstado estado = mesaDeDois(
                List.of(Carta.PRINCIPE, Carta.GUARDA), List.of(Carta.BARAO),
                List.of(Carta.REI, Carta.AIA), Fase.JOGAR);

        LoveLetterEstado depois = aplicar(estado,
                LoveLetterAcao.JogarCarta.mirando(ANA, Carta.PRINCIPE, BRUNO));

        assertThat(depois.descartes().get(BRUNO)).containsExactly(Carta.BARAO);
        assertThat(depois.mao(BRUNO)).containsExactly(Carta.REI);
    }

    @Test
    void principe_sobre_a_princesa_elimina() {
        LoveLetterEstado estado = mesaDeDois(
                List.of(Carta.PRINCIPE, Carta.GUARDA), List.of(Carta.PRINCESA),
                List.of(Carta.REI, Carta.AIA), Fase.JOGAR);

        Transicao golpe = transicao(estado,
                LoveLetterAcao.JogarCarta.mirando(ANA, Carta.PRINCIPE, BRUNO));
        LoveLetterEstado depois = (LoveLetterEstado) golpe.estado();

        assertThat(depois.vencedores()).containsExactly(ANA);
        assertThat(golpe.eventos()).anyMatch(e -> e instanceof LoveLetterEvento.JogadorEliminado);
    }

    @Test
    void principe_pode_mirar_em_si_mesmo() {
        LoveLetterEstado estado = mesaDeDois(
                List.of(Carta.PRINCIPE, Carta.GUARDA), List.of(Carta.BARAO),
                List.of(Carta.REI, Carta.AIA), Fase.JOGAR);

        LoveLetterEstado depois = aplicar(estado,
                LoveLetterAcao.JogarCarta.mirando(ANA, Carta.PRINCIPE, ANA));

        assertThat(depois.descartes().get(ANA)).containsExactly(Carta.PRINCIPE, Carta.GUARDA);
        assertThat(depois.mao(ANA)).containsExactly(Carta.REI);
    }

    @Test
    void rei_troca_as_maos() {
        LoveLetterEstado estado = mesaDeDois(
                List.of(Carta.REI, Carta.GUARDA), List.of(Carta.PRINCESA),
                List.of(Carta.AIA, Carta.AIA), Fase.JOGAR);

        LoveLetterEstado depois = aplicar(estado,
                LoveLetterAcao.JogarCarta.mirando(ANA, Carta.REI, BRUNO));

        assertThat(depois.mao(ANA)).containsExactly(Carta.PRINCESA);
        assertThat(depois.mao(BRUNO)).containsExactly(Carta.GUARDA);
    }

    @Test
    void jogar_a_princesa_elimina_quem_a_jogou() {
        LoveLetterEstado estado = mesaDeDois(
                List.of(Carta.PRINCESA, Carta.GUARDA), List.of(Carta.BARAO),
                List.of(Carta.AIA, Carta.AIA), Fase.JOGAR);

        LoveLetterEstado depois = aplicar(estado,
                LoveLetterAcao.JogarCarta.simples(ANA, Carta.PRINCESA));

        assertThat(depois.vencedores()).containsExactly(BRUNO);
    }

    @Test
    void condessa_e_obrigatoria_ao_lado_do_rei_ou_do_principe() {
        LoveLetterEstado comRei = mesaDeDois(
                List.of(Carta.CONDESSA, Carta.REI), List.of(Carta.BARAO),
                List.of(Carta.AIA, Carta.AIA), Fase.JOGAR);

        assertThat(LoveLetterDefinicao.jogaveis(comRei, ANA)).containsExactly(Carta.CONDESSA);
        assertThat(motivo(jogo.validar(comRei,
                LoveLetterAcao.JogarCarta.mirando(ANA, Carta.REI, BRUNO))))
                .contains("Condessa e obrigatoria");
        assertThat(jogo.acoesDisponiveis(comRei, ANA))
                .containsExactly(LoveLetterAcao.JogarCarta.simples(ANA, Carta.CONDESSA));

        LoveLetterEstado semRealeza = mesaDeDois(
                List.of(Carta.CONDESSA, Carta.AIA), List.of(Carta.BARAO),
                List.of(Carta.AIA, Carta.AIA), Fase.JOGAR);
        assertThat(LoveLetterDefinicao.jogaveis(semRealeza, ANA)).hasSize(2);
    }

    @Test
    void baralho_vazio_encerra_pela_maior_carta() {
        LoveLetterEstado estado = mesaDeDois(
                List.of(Carta.GUARDA, Carta.AIA), List.of(Carta.PRINCESA),
                List.of(), Fase.JOGAR);

        Transicao fim = transicao(estado,
                LoveLetterAcao.JogarCarta.adivinhando(ANA, BRUNO, Carta.REI));

        LoveLetterEstado depois = (LoveLetterEstado) fim.estado();
        assertThat(depois.vencedores()).containsExactly(BRUNO);
        assertThat(fim.eventos()).anyMatch(e -> e instanceof LoveLetterEvento.RodadaEncerrada);
    }

    // ------------------------------------------------------------- validação

    @Test
    void recusa_acao_fora_de_turno_de_outro_jogo_e_em_rodada_encerrada() {
        LoveLetterEstado estado = inicial(3L);

        assertThat(motivo(jogo.validar(estado, new LoveLetterAcao.ComprarCarta(BRUNO))))
                .contains("nao e a vez");
        assertThat(jogo.acoesDisponiveis(estado, BRUNO)).isEmpty();

        Acao estrangeira = new Acao() {
            @Override
            public IdJogador autor() {
                return ANA;
            }
        };
        assertThat(motivo(jogo.validar(estado, estrangeira))).contains("nao pertence ao Love Letter");

        LoveLetterEstado encerrada = estado.encerradaCom(List.of(ANA));
        assertThat(motivo(jogo.validar(encerrada, new LoveLetterAcao.ComprarCarta(ANA))))
                .contains("encerrada");
        assertThat(jogo.acoesDisponiveis(encerrada, ANA)).isEmpty();
    }

    @Test
    void recusa_comprar_duas_vezes_e_jogar_sem_comprar() {
        LoveLetterEstado estado = inicial(3L);

        assertThat(motivo(jogo.validar(estado,
                LoveLetterAcao.JogarCarta.simples(ANA, estado.cartaUnica(ANA)))))
                .contains("compre uma carta antes");

        LoveLetterEstado comprou = aplicar(estado, new LoveLetterAcao.ComprarCarta(ANA));
        assertThat(motivo(jogo.validar(comprou, new LoveLetterAcao.ComprarCarta(ANA))))
                .contains("ja comprou");
    }

    @Test
    void recusa_carta_que_nao_esta_na_mao_e_alvo_ou_palpite_incoerentes() {
        LoveLetterEstado estado = mesaDeDois(
                List.of(Carta.GUARDA, Carta.AIA), List.of(Carta.BARAO),
                List.of(Carta.REI, Carta.REI), Fase.JOGAR);

        assertThat(motivo(jogo.validar(estado,
                LoveLetterAcao.JogarCarta.simples(ANA, Carta.PRINCESA))))
                .contains("nao tem");
        assertThat(motivo(jogo.validar(estado,
                LoveLetterAcao.JogarCarta.simples(ANA, Carta.GUARDA))))
                .contains("exige um alvo");
        assertThat(motivo(jogo.validar(estado,
                LoveLetterAcao.JogarCarta.mirando(ANA, Carta.GUARDA, BRUNO))))
                .contains("exige um palpite");
        assertThat(motivo(jogo.validar(estado,
                LoveLetterAcao.JogarCarta.adivinhando(ANA, BRUNO, Carta.GUARDA))))
                .contains("palpitar Guarda");
        assertThat(motivo(jogo.validar(estado,
                LoveLetterAcao.JogarCarta.mirando(ANA, Carta.GUARDA, ANA))))
                .contains("alvo invalido");
        assertThat(motivo(jogo.validar(estado,
                new LoveLetterAcao.JogarCarta(ANA, Carta.AIA, Optional.of(BRUNO), Optional.empty()))))
                .contains("nao tem alvo");
        assertThat(motivo(jogo.validar(estado, new LoveLetterAcao.JogarCarta(
                ANA, Carta.AIA, Optional.empty(), Optional.of(Carta.REI)))))
                .contains("so a Guarda tem palpite");
    }

    @Test
    void identificador_e_faixa_de_jogadores() {
        assertThat(jogo.identificador()).isEqualTo(LoveLetterDefinicao.ID);
        assertThat(jogo.configuracaoSuportada().suporta(2)).isTrue();
        assertThat(jogo.configuracaoSuportada().suporta(4)).isTrue();
        assertThat(jogo.configuracaoSuportada().suporta(5)).isFalse();
    }

    @Test
    void cartas_conhecem_o_proprio_papel() {
        assertThat(Carta.GUARDA.exigeAlvo()).isTrue();
        assertThat(Carta.CONDESSA.exigeAlvo()).isFalse();
        assertThat(Carta.PRINCIPE.podeMirarEmSi()).isTrue();
        assertThat(Carta.REI.podeMirarEmSi()).isFalse();
        assertThat(Carta.PRINCESA.valor()).isEqualTo(8);
        assertThat(Carta.baralhoCompleto()).hasSize(16);
    }
}
