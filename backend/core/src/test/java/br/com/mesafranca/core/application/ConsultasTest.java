package br.com.mesafranca.core.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.mesafranca.core.domain.EventoRegistrado;
import br.com.mesafranca.core.domain.Resultado;
import br.com.mesafranca.core.duble.CatalogoFixo;
import br.com.mesafranca.core.duble.JogoDeContagem;
import br.com.mesafranca.core.duble.RepositorioDeTeste;
import br.com.mesafranca.core.erro.PartidaNaoEncontrada;
import br.com.mesafranca.core.erro.RegraDePartidaViolada;
import br.com.mesafranca.core.port.in.ConsultarEstado;
import br.com.mesafranca.core.port.in.ListarAcoesDisponiveis;
import br.com.mesafranca.core.port.in.Reconectar;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Consultas de partida")
class ConsultasTest {

    private final RepositorioDeTeste repositorio = new RepositorioDeTeste();
    private final CatalogoFixo catalogo = new CatalogoFixo(new JogoDeContagem());

    @Nested
    @DisplayName("ConsultarEstado")
    class Estado {

        private final ConsultarEstado servico = new ConsultarEstadoService(repositorio, catalogo);

        @Test
        void devolve_a_visao_do_jogador_e_esconde_o_segredo_alheio() {
            var comSegredoDeAna = new JogoDeContagem.Estado(
                    1, Map.of(FabricaDeCenario.ANA, 77), Optional.of(FabricaDeCenario.ANA));
            repositorio.comPartida(FabricaDeCenario.cheia().iniciada(comSegredoDeAna));

            var deAna = servico.consultar(
                    new ConsultarEstado.Consulta(FabricaDeCenario.PARTIDA, FabricaDeCenario.ANA));
            var deBruno = servico.consultar(
                    new ConsultarEstado.Consulta(FabricaDeCenario.PARTIDA, FabricaDeCenario.BRUNO));

            assertThat(((JogoDeContagem.VisaoDoJogador) deAna.visao()).meuSegredo()).contains(77);
            assertThat(((JogoDeContagem.VisaoDoJogador) deBruno.visao()).meuSegredo()).isEmpty();
            assertThat(((JogoDeContagem.VisaoDoJogador) deBruno.visao()).contagem()).isEqualTo(1);
            assertThat(deAna.resultado()).isInstanceOf(Resultado.EmAndamento.class);
            assertThat(deAna.ultimaSequencia()).isZero();
        }

        @Test
        void consultar_antes_do_inicio_e_erro_de_estado() {
            repositorio.comPartida(FabricaDeCenario.cheia());
            var consulta = new ConsultarEstado.Consulta(
                    FabricaDeCenario.PARTIDA, FabricaDeCenario.ANA);

            assertThatThrownBy(() -> servico.consultar(consulta))
                    .isInstanceOf(RegraDePartidaViolada.class);
        }

        @Test
        void partida_inexistente_e_erro() {
            var consulta = new ConsultarEstado.Consulta(
                    FabricaDeCenario.PARTIDA, FabricaDeCenario.ANA);

            assertThatThrownBy(() -> servico.consultar(consulta))
                    .isInstanceOf(PartidaNaoEncontrada.class);
            assertThatThrownBy(() -> servico.consultar(null))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new ConsultarEstadoService(null, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("ListarAcoesDisponiveis")
    class Acoes {

        private final ListarAcoesDisponiveis servico =
                new ListarAcoesDisponiveisService(repositorio, catalogo);

        @Test
        void lista_o_que_o_jogo_permite_agora() {
            repositorio.comPartida(FabricaDeCenario.emAndamento());

            var acoes = servico.listar(new ListarAcoesDisponiveis.Consulta(
                    FabricaDeCenario.PARTIDA, FabricaDeCenario.ANA));

            assertThat(acoes).containsExactly(
                    new JogoDeContagem.Incrementar(FabricaDeCenario.ANA),
                    new JogoDeContagem.Sussurrar(FabricaDeCenario.ANA));
        }

        @Test
        void partida_fora_de_andamento_nao_oferece_acao() {
            repositorio.comPartida(FabricaDeCenario.cheia());

            var acoes = servico.listar(new ListarAcoesDisponiveis.Consulta(
                    FabricaDeCenario.PARTIDA, FabricaDeCenario.ANA));

            assertThat(acoes).isEmpty();
        }

        @Test
        void partida_encerrada_pelo_jogo_nao_oferece_acao() {
            repositorio.comPartida(FabricaDeCenario.cheia()
                    .iniciada(FabricaDeCenario.estadoNaContagem(JogoDeContagem.ALVO)));

            var acoes = servico.listar(new ListarAcoesDisponiveis.Consulta(
                    FabricaDeCenario.PARTIDA, FabricaDeCenario.ANA));

            assertThat(acoes).isEmpty();
        }

        @Test
        void partida_inexistente_e_erro() {
            var consulta = new ListarAcoesDisponiveis.Consulta(
                    FabricaDeCenario.PARTIDA, FabricaDeCenario.ANA);

            assertThatThrownBy(() -> servico.listar(consulta))
                    .isInstanceOf(PartidaNaoEncontrada.class);
        }

        @Test
        void exige_consulta_e_dependencias() {
            assertThatThrownBy(() -> servico.listar(null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new ListarAcoesDisponiveisService(null, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Reconectar")
    class Reconexao {

        private final Reconectar servico = new ReconectarService(repositorio, catalogo);

        @Test
        void devolve_visao_atual_e_apenas_os_eventos_perdidos() {
            var partida = FabricaDeCenario.emAndamento();
            repositorio.comPartida(partida);
            repositorio.salvar(partida, List.of(
                    new EventoRegistrado(1L, new JogoDeContagem.Incrementado(FabricaDeCenario.ANA, 1, 3)),
                    new EventoRegistrado(2L, new JogoDeContagem.Incrementado(FabricaDeCenario.BRUNO, 2, 5))));

            var resposta = servico.reconectar(new Reconectar.Comando(
                    FabricaDeCenario.PARTIDA, FabricaDeCenario.ANA, 1L));

            assertThat(resposta.eventosPerdidos()).hasSize(1);
            assertThat(resposta.eventosPerdidos().getFirst().sequencia()).isEqualTo(2L);
            assertThat(resposta.visao()).isInstanceOf(JogoDeContagem.VisaoDoJogador.class);
        }

        @Test
        void sequencia_zero_traz_todo_o_log() {
            var partida = FabricaDeCenario.emAndamento();
            repositorio.comPartida(partida);
            repositorio.salvar(partida, List.of(
                    new EventoRegistrado(1L, new JogoDeContagem.Incrementado(FabricaDeCenario.ANA, 1, 3))));

            var resposta = servico.reconectar(new Reconectar.Comando(
                    FabricaDeCenario.PARTIDA, FabricaDeCenario.ANA, 0L));

            assertThat(resposta.eventosPerdidos()).hasSize(1);
        }

        @Test
        void recusa_sequencia_negativa_e_exige_dependencias() {
            assertThatThrownBy(() -> new Reconectar.Comando(
                    FabricaDeCenario.PARTIDA, FabricaDeCenario.ANA, -1L))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> servico.reconectar(null))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new ReconectarService(null, null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void partida_inexistente_e_erro() {
            var comando = new Reconectar.Comando(
                    FabricaDeCenario.PARTIDA, FabricaDeCenario.ANA, 0L);

            assertThatThrownBy(() -> servico.reconectar(comando))
                    .isInstanceOf(PartidaNaoEncontrada.class);
        }
    }
}
