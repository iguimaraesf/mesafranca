package br.com.mesafranca.core.port;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.mesafranca.core.domain.Acao;
import br.com.mesafranca.core.domain.Assento;
import br.com.mesafranca.core.domain.ConfiguracaoDePartida;
import br.com.mesafranca.core.domain.ConfiguracaoSuportada;
import br.com.mesafranca.core.domain.EstadoDeJogo;
import br.com.mesafranca.core.domain.Evento;
import br.com.mesafranca.core.domain.IdJogador;
import br.com.mesafranca.core.domain.IdentificadorDeJogo;
import br.com.mesafranca.core.domain.Jogador;
import br.com.mesafranca.core.domain.Resultado;
import br.com.mesafranca.core.domain.ResultadoDeValidacao;
import br.com.mesafranca.core.domain.Transicao;
import br.com.mesafranca.core.domain.Visao;
import br.com.mesafranca.core.port.in.CriarPartida;
import br.com.mesafranca.core.port.in.EntrarNaPartida;
import br.com.mesafranca.core.port.in.IniciarPartida;
import br.com.mesafranca.core.port.in.SubmeterAcao;
import br.com.mesafranca.core.port.out.FonteDeAleatoriedade;
import br.com.mesafranca.core.spi.DefinicaoDeJogo;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Guardas das respostas das portas de entrada e o padrão da SPI.
 *
 * <p>Estes testes nasceram de uma falha de cobertura, e não do contrário: o
 * portão do JaCoCo reprovou o build apontando que as quatro recusas nunca
 * tinham sido construídas com motivo inválido. Eram lacunas de verdade — uma
 * recusa sem motivo chegaria ao cliente como uma tela em branco.
 */
@DisplayName("Respostas das portas")
class RespostasDasPortasTest {

    @Nested
    @DisplayName("Toda recusa exige motivo")
    class Recusas {

        @Test
        void criar_partida() {
            assertThat(new CriarPartida.Recusado("jogo cheio").motivo()).isEqualTo("jogo cheio");
            assertThatThrownBy(() -> new CriarPartida.Recusado(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("motivo");
            assertThatThrownBy(() -> new CriarPartida.Recusado("   "))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void entrar_na_partida() {
            assertThat(new EntrarNaPartida.Recusado("cheia").motivo()).isEqualTo("cheia");
            assertThatThrownBy(() -> new EntrarNaPartida.Recusado(null))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new EntrarNaPartida.Recusado(""))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void iniciar_partida() {
            assertThat(new IniciarPartida.Recusado("faltam jogadores").motivo())
                    .isEqualTo("faltam jogadores");
            assertThatThrownBy(() -> new IniciarPartida.Recusado(null))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new IniciarPartida.Recusado(" "))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void submeter_acao() {
            assertThat(new SubmeterAcao.Recusada("nao e sua vez").motivo())
                    .isEqualTo("nao e sua vez");
            assertThatThrownBy(() -> new SubmeterAcao.Recusada(null))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new SubmeterAcao.Recusada("\t"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Respostas de sucesso")
    class Sucessos {

        @Test
        void sentou_exige_assento_e_aplicada_copia_os_eventos() {
            assertThat(new EntrarNaPartida.Sentou(new Assento(2)).assento().indice()).isEqualTo(2);
            assertThatThrownBy(() -> new EntrarNaPartida.Sentou(null))
                    .isInstanceOf(NullPointerException.class);

            SubmeterAcao.Aplicada aplicada = new SubmeterAcao.Aplicada(null, 3L);
            assertThat(aplicada.eventos()).isEmpty();
            assertThat(aplicada.versao()).isEqualTo(3L);
            assertThat(new IniciarPartida.Iniciada(7L).versao()).isEqualTo(7L);
        }
    }

    @Nested
    @DisplayName("SPI de jogo")
    class Spi {

        /**
         * Jogo que não sobrescreve nada além do obrigatório, só para exercitar
         * o comportamento padrão de {@code eventoVisivelPara}.
         */
        private final DefinicaoDeJogo semSegredo = new DefinicaoDeJogo() {
            @Override
            public IdentificadorDeJogo identificador() {
                return new IdentificadorDeJogo("sem-segredo");
            }

            @Override
            public ConfiguracaoSuportada configuracaoSuportada() {
                return ConfiguracaoSuportada.de(2, 2);
            }

            @Override
            public EstadoDeJogo estadoInicial(
                    ConfiguracaoDePartida configuracao,
                    List<Jogador> jogadores,
                    FonteDeAleatoriedade aleatoriedade) {
                return ESTADO;
            }

            @Override
            public List<Acao> acoesDisponiveis(EstadoDeJogo estado, IdJogador jogador) {
                return List.of();
            }

            @Override
            public ResultadoDeValidacao validar(EstadoDeJogo estado, Acao acao) {
                return ResultadoDeValidacao.aceita();
            }

            @Override
            public Transicao aplicar(
                    EstadoDeJogo estado, Acao acao, FonteDeAleatoriedade aleatoriedade) {
                return Transicao.de(estado);
            }

            @Override
            public Visao visaoDe(EstadoDeJogo estado, IdJogador jogador) {
                return VISAO;
            }

            @Override
            public Resultado resultado(EstadoDeJogo estado) {
                return Resultado.emAndamento();
            }
        };

        @Test
        @DisplayName("por padrao, todo evento e publico")
        void o_padrao_entrega_o_evento_a_qualquer_um() {
            Evento evento = new Evento() {
            };

            Optional<Evento> paraUm = semSegredo.eventoVisivelPara(
                    ESTADO, evento, new IdJogador("ana"));
            Optional<Evento> paraOutro = semSegredo.eventoVisivelPara(
                    ESTADO, evento, new IdJogador("bruno"));

            assertThat(paraUm).contains(evento);
            assertThat(paraOutro).contains(evento);
        }

        @Test
        void a_definicao_minima_responde_o_contrato_inteiro() {
            assertThat(semSegredo.identificador().valor()).isEqualTo("sem-segredo");
            assertThat(semSegredo.configuracaoSuportada().suporta(2)).isTrue();
            assertThat(semSegredo.estadoInicial(
                    ConfiguracaoDePartida.de(semSegredo.identificador(), 2), List.of(), null))
                    .isSameAs(ESTADO);
            assertThat(semSegredo.acoesDisponiveis(ESTADO, new IdJogador("ana"))).isEmpty();
            assertThat(semSegredo.validar(ESTADO, null).aprovada()).isTrue();
            assertThat(semSegredo.aplicar(ESTADO, null, null).eventos()).isEmpty();
            assertThat(semSegredo.visaoDe(ESTADO, new IdJogador("ana"))).isSameAs(VISAO);
            assertThat(semSegredo.resultado(ESTADO).encerrada()).isFalse();
        }
    }

    private static final EstadoDeJogo ESTADO = new EstadoDeJogo() {
    };

    private static final Visao VISAO = new Visao() {
    };
}
