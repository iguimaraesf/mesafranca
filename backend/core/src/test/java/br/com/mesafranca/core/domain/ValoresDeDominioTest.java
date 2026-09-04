package br.com.mesafranca.core.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Valores do dominio")
class ValoresDeDominioTest {

    @Nested
    @DisplayName("IdentificadorDeJogo")
    class Identificador {

        @ParameterizedTest
        @ValueSource(strings = {"ludo", "love-letter", "uno", "jogo-2"})
        void aceita_formato_valido(String valor) {
            assertThat(new IdentificadorDeJogo(valor).valor()).isEqualTo(valor);
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "L", "Ludo", "com espaco", "-comeca-com-hifen", "1digito",
                "excessivamente-longo-para-um-identificador-de-jogo"})
        void recusa_formato_invalido(String valor) {
            assertThatThrownBy(() -> new IdentificadorDeJogo(valor))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("identificador de jogo invalido");
        }

        @Test
        void recusa_nulo() {
            assertThatThrownBy(() -> new IdentificadorDeJogo(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void expoe_o_valor_no_toString() {
            assertThat(new IdentificadorDeJogo("ludo")).hasToString("ludo");
        }
    }

    @Nested
    @DisplayName("IdJogador e IdAcao")
    class Identidades {

        @Test
        void removem_espacos_das_bordas() {
            assertThat(new IdJogador("  ana  ").valor()).isEqualTo("ana");
            assertThat(new IdAcao(" a-1 ").valor()).isEqualTo("a-1");
        }

        @Test
        void expoem_o_valor_no_toString() {
            assertThat(new IdJogador("ana")).hasToString("ana");
            assertThat(new IdAcao("a-1")).hasToString("a-1");
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        void recusam_vazio(String valor) {
            assertThatThrownBy(() -> new IdJogador(valor))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new IdAcao(valor))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void recusam_nulo() {
            assertThatThrownBy(() -> new IdJogador(null))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new IdAcao(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("IdPartida")
    class Partidas {

        @Test
        void constroi_a_partir_de_texto() {
            String texto = "11111111-2222-3333-4444-555555555555";
            assertThat(IdPartida.de(texto).valor()).isEqualTo(UUID.fromString(texto));
            assertThat(IdPartida.de(texto)).hasToString(texto);
        }

        @Test
        void recusa_nulo() {
            assertThatThrownBy(() -> new IdPartida(null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> IdPartida.de(null)).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Assento e Jogador")
    class Assentos {

        @Test
        void assento_comeca_em_zero() {
            assertThat(new Assento(0).indice()).isZero();
        }

        @Test
        void assento_recusa_negativo() {
            assertThatThrownBy(() -> new Assento(-1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("negativo");
        }

        @Test
        void jogador_humano_tem_atalho() {
            Jogador jogador = Jogador.humano(new IdJogador("ana"), new Assento(2));
            assertThat(jogador.tipo()).isEqualTo(TipoDeJogador.HUMANO);
            assertThat(jogador.assento().indice()).isEqualTo(2);
        }

        @Test
        void jogador_recusa_campos_nulos() {
            IdJogador id = new IdJogador("ana");
            Assento assento = new Assento(0);
            assertThatThrownBy(() -> new Jogador(null, assento, TipoDeJogador.BOT))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new Jogador(id, null, TipoDeJogador.BOT))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new Jogador(id, assento, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Configuracoes")
    class Configuracoes {

        private final IdentificadorDeJogo ludo = new IdentificadorDeJogo("ludo");

        @Test
        void configuracao_de_partida_copia_as_opcoes() {
            Map<String, String> original = new HashMap<>(Map.of("variante", "rapida"));
            ConfiguracaoDePartida configuracao = new ConfiguracaoDePartida(ludo, 4, original);
            original.put("intruso", "x");

            assertThat(configuracao.opcoes()).containsExactly(Map.entry("variante", "rapida"));
            assertThat(configuracao.opcao("variante", "padrao")).isEqualTo("rapida");
            assertThat(configuracao.opcao("ausente", "padrao")).isEqualTo("padrao");
        }

        @Test
        void configuracao_de_partida_aceita_opcoes_nulas() {
            assertThat(new ConfiguracaoDePartida(ludo, 2, null).opcoes()).isEmpty();
            assertThat(ConfiguracaoDePartida.de(ludo, 2).opcoes()).isEmpty();
        }

        @Test
        void configuracao_de_partida_exige_jogo_e_jogadores() {
            assertThatThrownBy(() -> new ConfiguracaoDePartida(null, 2, Map.of()))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new ConfiguracaoDePartida(ludo, 0, Map.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("positivo");
        }

        @Test
        void faixa_suportada_responde_sobre_os_extremos() {
            ConfiguracaoSuportada faixa = ConfiguracaoSuportada.de(2, 4);
            assertThat(faixa.suporta(1)).isFalse();
            assertThat(faixa.suporta(2)).isTrue();
            assertThat(faixa.suporta(4)).isTrue();
            assertThat(faixa.suporta(5)).isFalse();
        }

        @Test
        void faixa_suportada_recusa_limites_incoerentes() {
            assertThatThrownBy(() -> ConfiguracaoSuportada.de(0, 4))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("minimo");
            assertThatThrownBy(() -> ConfiguracaoSuportada.de(4, 2))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("menor que minimo");
        }
    }

    @Nested
    @DisplayName("Resultado")
    class Resultados {

        @Test
        void em_andamento_nao_esta_encerrado() {
            assertThat(Resultado.emAndamento().encerrada()).isFalse();
        }

        @Test
        void vencida_registra_o_vencedor() {
            IdJogador ana = new IdJogador("ana");
            Resultado resultado = Resultado.vencidaPor(ana);

            assertThat(resultado.encerrada()).isTrue();
            assertThat(((Resultado.Encerrado) resultado).vencedores()).containsExactly(ana);
        }

        @Test
        void empate_encerra_sem_vencedor() {
            assertThat(((Resultado.Encerrado) Resultado.empatada()).vencedores()).isEmpty();
            assertThat(new Resultado.Encerrado(null).vencedores()).isEmpty();
        }

        @Test
        void vencedor_nulo_e_recusado() {
            assertThatThrownBy(() -> Resultado.vencidaPor(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("ResultadoDeValidacao")
    class Validacoes {

        @Test
        void aceita_e_aprovada() {
            assertThat(ResultadoDeValidacao.aceita().aprovada()).isTrue();
        }

        @Test
        void recusada_carrega_motivo_e_nao_e_aprovada() {
            ResultadoDeValidacao recusada = ResultadoDeValidacao.recusada("nao e sua vez");
            assertThat(recusada.aprovada()).isFalse();
            assertThat(((ResultadoDeValidacao.Recusada) recusada).motivo()).isEqualTo("nao e sua vez");
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "  "})
        void recusa_sem_motivo_e_defeito(String motivo) {
            assertThatThrownBy(() -> ResultadoDeValidacao.recusada(motivo))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("motivo");
        }

        @Test
        void recusa_com_motivo_nulo_e_defeito() {
            assertThatThrownBy(() -> ResultadoDeValidacao.recusada(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Transicao e EventoRegistrado")
    class Transicoes {

        private final EstadoDeJogo estado = new EstadoDeJogo() {
        };
        private final Evento evento = new Evento() {
        };

        @Test
        void transicao_copia_a_lista_de_eventos() {
            Transicao transicao = Transicao.de(estado, evento);
            assertThat(transicao.eventos()).containsExactly(evento);
            assertThatThrownBy(() -> transicao.eventos().add(evento))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void transicao_aceita_lista_nula_como_vazia() {
            assertThat(new Transicao(estado, null).eventos()).isEmpty();
        }

        @Test
        void transicao_exige_estado() {
            assertThatThrownBy(() -> new Transicao(null, List.of()))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void evento_registrado_comeca_em_um() {
            assertThat(new EventoRegistrado(1L, evento).sequencia()).isEqualTo(1L);
            assertThatThrownBy(() -> new EventoRegistrado(0L, evento))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("sequencia comeca em 1");
            assertThatThrownBy(() -> new EventoRegistrado(1L, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}
