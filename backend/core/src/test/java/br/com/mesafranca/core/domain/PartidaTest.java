package br.com.mesafranca.core.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.mesafranca.core.erro.RegraDePartidaViolada;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Partida")
class PartidaTest {

    private static final IdPartida ID = new IdPartida(UUID.randomUUID());
    private static final IdentificadorDeJogo LUDO = new IdentificadorDeJogo("ludo");
    private static final IdJogador ANA = new IdJogador("ana");
    private static final IdJogador BRUNO = new IdJogador("bruno");

    private static final EstadoDeJogo ESTADO = new EstadoDeJogo() {
    };

    private static Partida paraDoisJogadores() {
        return Partida.nova(ID, ConfiguracaoDePartida.de(LUDO, 2), 99L);
    }

    private static Partida cheiaEIniciada() {
        return paraDoisJogadores()
                .com(Jogador.humano(ANA, new Assento(0)))
                .com(Jogador.humano(BRUNO, new Assento(1)))
                .iniciada(ESTADO);
    }

    @Test
    void nasce_aguardando_sem_jogadores_e_sem_estado() {
        Partida partida = paraDoisJogadores();

        assertThat(partida.fase()).isEqualTo(FaseDaPartida.AGUARDANDO);
        assertThat(partida.jogadores()).isEmpty();
        assertThat(partida.estado()).isEmpty();
        assertThat(partida.jogo()).isEqualTo(LUDO);
        assertThat(partida.semente()).isEqualTo(99L);
        assertThat(partida.versao()).isZero();
        assertThat(partida.ultimaSequencia()).isZero();
    }

    @Test
    void distribui_assentos_em_ordem_de_chegada() {
        Partida partida = paraDoisJogadores();
        assertThat(partida.proximoAssento().indice()).isZero();

        partida = partida.com(Jogador.humano(ANA, partida.proximoAssento()));
        assertThat(partida.proximoAssento().indice()).isEqualTo(1);
        assertThat(partida.contem(ANA)).isTrue();
        assertThat(partida.contem(BRUNO)).isFalse();
        assertThat(partida.jogador(ANA)).isPresent();
        assertThat(partida.jogador(BRUNO)).isEmpty();
    }

    @Test
    void cada_alteracao_incrementa_a_versao() {
        Partida partida = paraDoisJogadores();
        long inicial = partida.versao();

        partida = partida.com(Jogador.humano(ANA, new Assento(0)));

        assertThat(partida.versao()).isEqualTo(inicial + 1);
    }

    @Test
    void fica_cheia_no_numero_configurado() {
        Partida partida = paraDoisJogadores().com(Jogador.humano(ANA, new Assento(0)));
        assertThat(partida.cheia()).isFalse();

        partida = partida.com(Jogador.humano(BRUNO, new Assento(1)));
        assertThat(partida.cheia()).isTrue();
    }

    @Test
    void recusa_jogador_repetido() {
        Partida partida = paraDoisJogadores().com(Jogador.humano(ANA, new Assento(0)));
        Jogador repetido = Jogador.humano(ANA, new Assento(1));

        assertThatThrownBy(() -> partida.com(repetido))
                .isInstanceOf(RegraDePartidaViolada.class)
                .hasMessageContaining("ja esta na partida");
    }

    @Test
    void recusa_assento_ocupado() {
        Partida partida = paraDoisJogadores().com(Jogador.humano(ANA, new Assento(0)));
        Jogador colidindo = Jogador.humano(BRUNO, new Assento(0));

        assertThatThrownBy(() -> partida.com(colidindo))
                .isInstanceOf(RegraDePartidaViolada.class)
                .hasMessageContaining("assento ocupado");
    }

    @Test
    void recusa_jogador_alem_da_capacidade() {
        Partida partida = paraDoisJogadores()
                .com(Jogador.humano(ANA, new Assento(0)))
                .com(Jogador.humano(BRUNO, new Assento(1)));
        Jogador terceiro = Jogador.humano(new IdJogador("carla"), new Assento(2));

        assertThatThrownBy(() -> partida.com(terceiro))
                .isInstanceOf(RegraDePartidaViolada.class)
                .hasMessageContaining("cheia");
    }

    @Test
    void recusa_jogador_depois_de_iniciada() {
        Partida partida = cheiaEIniciada();
        Jogador atrasado = Jogador.humano(new IdJogador("carla"), new Assento(2));

        assertThatThrownBy(() -> partida.com(atrasado))
                .isInstanceOf(RegraDePartidaViolada.class)
                .hasMessageContaining("nao aceita mais jogadores");
    }

    @Test
    void iniciar_exige_mesa_completa() {
        Partida incompleta = paraDoisJogadores().com(Jogador.humano(ANA, new Assento(0)));

        assertThatThrownBy(() -> incompleta.iniciada(ESTADO))
                .isInstanceOf(RegraDePartidaViolada.class)
                .hasMessageContaining("faltam jogadores");
    }

    @Test
    void iniciar_duas_vezes_e_recusado() {
        Partida partida = cheiaEIniciada();

        assertThat(partida.fase()).isEqualTo(FaseDaPartida.EM_ANDAMENTO);
        assertThat(partida.estado()).contains(ESTADO);
        assertThatThrownBy(() -> partida.iniciada(ESTADO))
                .isInstanceOf(RegraDePartidaViolada.class)
                .hasMessageContaining("ja iniciada");
    }

    @Test
    void avancar_reserva_a_faixa_de_sequencias() {
        Partida partida = cheiaEIniciada();
        long versao = partida.versao();

        Partida avancada = partida.apos(ESTADO, 3);

        assertThat(avancada.ultimaSequencia()).isEqualTo(3L);
        assertThat(avancada.versao()).isEqualTo(versao + 1);
        assertThat(avancada.apos(ESTADO, 2).ultimaSequencia()).isEqualTo(5L);
    }

    @Test
    void avancar_exige_partida_em_andamento_e_quantidade_valida() {
        Partida aguardando = paraDoisJogadores();
        assertThatThrownBy(() -> aguardando.apos(ESTADO, 1))
                .isInstanceOf(RegraDePartidaViolada.class)
                .hasMessageContaining("nao aceita acoes");

        Partida emAndamento = cheiaEIniciada();
        assertThatThrownBy(() -> emAndamento.apos(ESTADO, -1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> emAndamento.apos(null, 1))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void encerrar_so_vale_para_partida_em_andamento() {
        Partida encerrada = cheiaEIniciada().encerrada();
        assertThat(encerrada.fase()).isEqualTo(FaseDaPartida.ENCERRADA);

        assertThatThrownBy(encerrada::encerrada)
                .isInstanceOf(RegraDePartidaViolada.class)
                .hasMessageContaining("em andamento");
    }

    @Test
    void estado_obrigatorio_falha_antes_do_inicio() {
        Partida partida = paraDoisJogadores();

        assertThatThrownBy(partida::estadoObrigatorio)
                .isInstanceOf(RegraDePartidaViolada.class)
                .hasMessageContaining("ainda nao tem estado");
        assertThat(cheiaEIniciada().estadoObrigatorio()).isSameAs(ESTADO);
    }

    @Test
    void lista_de_jogadores_e_imutavel() {
        Partida partida = paraDoisJogadores().com(Jogador.humano(ANA, new Assento(0)));
        Jogador intruso = Jogador.humano(BRUNO, new Assento(1));

        assertThatThrownBy(() -> partida.jogadores().add(intruso))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void recusa_campos_invalidos_na_construcao() {
        ConfiguracaoDePartida configuracao = ConfiguracaoDePartida.de(LUDO, 2);

        assertThatThrownBy(() -> new Partida(null, configuracao, List.of(),
                FaseDaPartida.AGUARDANDO, Optional.empty(), 0L, 0L, 0L))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Partida(ID, configuracao, null,
                FaseDaPartida.AGUARDANDO, Optional.empty(), 0L, -1L, 0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("versao");
        assertThatThrownBy(() -> new Partida(ID, configuracao, List.of(),
                FaseDaPartida.AGUARDANDO, Optional.empty(), 0L, 0L, -1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sequencia");
        assertThatThrownBy(() -> new Partida(ID, configuracao, List.of(),
                null, Optional.empty(), 0L, 0L, 0L))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Partida(ID, configuracao, List.of(),
                FaseDaPartida.AGUARDANDO, null, 0L, 0L, 0L))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Partida(ID, null, List.of(),
                FaseDaPartida.AGUARDANDO, Optional.empty(), 0L, 0L, 0L))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void jogador_nulo_e_recusado_nas_consultas() {
        Partida partida = paraDoisJogadores();
        assertThatThrownBy(() -> partida.jogador(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> partida.com(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void aceita_lista_de_jogadores_nula_como_vazia() {
        Partida partida = new Partida(ID, ConfiguracaoDePartida.de(LUDO, 2), null,
                FaseDaPartida.AGUARDANDO, Optional.empty(), 0L, 0L, 0L);
        assertThat(partida.jogadores()).isEmpty();
    }
}
