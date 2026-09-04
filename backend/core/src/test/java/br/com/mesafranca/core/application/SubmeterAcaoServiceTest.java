package br.com.mesafranca.core.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.mesafranca.core.domain.Acao;
import br.com.mesafranca.core.domain.FaseDaPartida;
import br.com.mesafranca.core.domain.IdAcao;
import br.com.mesafranca.core.domain.IdJogador;
import br.com.mesafranca.core.duble.AleatoriedadeRoteirizada;
import br.com.mesafranca.core.duble.CatalogoFixo;
import br.com.mesafranca.core.duble.FabricaRoteirizada;
import br.com.mesafranca.core.duble.JogoDeContagem;
import br.com.mesafranca.core.duble.PublicadorDeTeste;
import br.com.mesafranca.core.duble.RepositorioDeTeste;
import br.com.mesafranca.core.erro.PartidaNaoEncontrada;
import br.com.mesafranca.core.port.in.SubmeterAcao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SubmeterAcaoService")
class SubmeterAcaoServiceTest {

    private final RepositorioDeTeste repositorio = new RepositorioDeTeste();
    private final PublicadorDeTeste publicador = new PublicadorDeTeste();

    private SubmeterAcao servicoCom(int... valoresSorteados) {
        return new SubmeterAcaoService(
                repositorio,
                new CatalogoFixo(new JogoDeContagem()),
                new FabricaRoteirizada(AleatoriedadeRoteirizada.de(valoresSorteados), 1L),
                publicador);
    }

    private static SubmeterAcao.Comando comando(String idAcao, Acao acao) {
        return new SubmeterAcao.Comando(FabricaDeCenario.PARTIDA, new IdAcao(idAcao), acao);
    }

    @Test
    void aplica_a_acao_numera_o_evento_e_avanca_a_partida() {
        var emAndamento = FabricaDeCenario.emAndamento();
        repositorio.comPartida(emAndamento);

        var resposta = servicoCom(4)
                .submeter(comando("a-1", new JogoDeContagem.Incrementar(FabricaDeCenario.ANA)));

        var aplicada = (SubmeterAcao.Aplicada) resposta;
        assertThat(aplicada.eventos()).hasSize(1);
        assertThat(aplicada.eventos().getFirst().sequencia()).isEqualTo(1L);
        assertThat(aplicada.eventos().getFirst().evento())
                .isEqualTo(new JogoDeContagem.Incrementado(FabricaDeCenario.ANA, 1, 4));
        assertThat(aplicada.versao()).isEqualTo(emAndamento.versao() + 1);

        var guardada = repositorio.carregar(FabricaDeCenario.PARTIDA).orElseThrow();
        assertThat(guardada.ultimaSequencia()).isEqualTo(1L);
        assertThat(repositorio.log(FabricaDeCenario.PARTIDA)).hasSize(1);
    }

    @Test
    void numera_eventos_continuando_a_sequencia_anterior() {
        repositorio.comPartida(FabricaDeCenario.emAndamento());
        servicoCom(4).submeter(comando("a-1", new JogoDeContagem.Incrementar(FabricaDeCenario.ANA)));

        var segunda = (SubmeterAcao.Aplicada) servicoCom(2)
                .submeter(comando("a-2", new JogoDeContagem.Incrementar(FabricaDeCenario.BRUNO)));

        assertThat(segunda.eventos().getFirst().sequencia()).isEqualTo(2L);
    }

    @Test
    void reenvio_da_mesma_acao_nao_rola_o_dado_de_novo() {
        repositorio.comPartida(FabricaDeCenario.emAndamento());
        var acao = new JogoDeContagem.Incrementar(FabricaDeCenario.ANA);
        var servico = servicoCom(4);

        var primeira = servico.submeter(comando("a-1", acao));
        var repetida = servico.submeter(comando("a-1", acao));

        assertThat(primeira).isInstanceOf(SubmeterAcao.Aplicada.class);
        assertThat(repetida).isInstanceOf(SubmeterAcao.Repetida.class);
        assertThat(repositorio.log(FabricaDeCenario.PARTIDA)).hasSize(1);
        assertThat(repositorio.carregar(FabricaDeCenario.PARTIDA).orElseThrow().ultimaSequencia())
                .isEqualTo(1L);
    }

    @Test
    void devolve_a_recusa_que_o_jogo_produziu() {
        repositorio.comPartida(
                FabricaDeCenario.cheia().iniciada(FabricaDeCenario.estadoNaContagem(JogoDeContagem.ALVO)));

        var resposta = servicoCom()
                .submeter(comando("a-1", new JogoDeContagem.Incrementar(FabricaDeCenario.ANA)));

        assertThat(((SubmeterAcao.Recusada) resposta).motivo()).isEqualTo("partida encerrada");
        assertThat(repositorio.log(FabricaDeCenario.PARTIDA)).isEmpty();
    }

    @Test
    void recusa_acao_de_quem_nao_esta_na_partida() {
        repositorio.comPartida(FabricaDeCenario.emAndamento());

        var resposta = servicoCom()
                .submeter(comando("a-1", new JogoDeContagem.Incrementar(new IdJogador("intruso"))));

        assertThat(((SubmeterAcao.Recusada) resposta).motivo()).contains("nao participa");
    }

    @Test
    void recusa_acao_em_partida_que_ainda_nao_comecou() {
        repositorio.comPartida(FabricaDeCenario.cheia());

        var resposta = servicoCom()
                .submeter(comando("a-1", new JogoDeContagem.Incrementar(FabricaDeCenario.ANA)));

        assertThat(((SubmeterAcao.Recusada) resposta).motivo()).contains("nao esta em andamento");
    }

    @Test
    void encerra_a_partida_quando_o_jogo_declara_vencedor() {
        repositorio.comPartida(FabricaDeCenario.cheia()
                .iniciada(FabricaDeCenario.estadoNaContagem(JogoDeContagem.ALVO - 1)));

        servicoCom(6).submeter(comando("a-1", new JogoDeContagem.Incrementar(FabricaDeCenario.ANA)));

        assertThat(repositorio.carregar(FabricaDeCenario.PARTIDA).orElseThrow().fase())
                .isEqualTo(FaseDaPartida.ENCERRADA);
    }

    @Test
    void evento_privado_nao_chega_a_outro_jogador() {
        repositorio.comPartida(FabricaDeCenario.emAndamento());

        servicoCom(77).submeter(comando("a-1", new JogoDeContagem.Sussurrar(FabricaDeCenario.ANA)));

        assertThat(publicador.recebidosPor(FabricaDeCenario.ANA)).hasSize(1);
        assertThat(publicador.recebidosPor(FabricaDeCenario.ANA).getFirst().evento())
                .isEqualTo(new JogoDeContagem.Sussurrado(FabricaDeCenario.ANA, 77));
        assertThat(publicador.recebidosPor(FabricaDeCenario.BRUNO)).isEmpty();
    }

    @Test
    void evento_publico_chega_a_todos_os_jogadores() {
        repositorio.comPartida(FabricaDeCenario.emAndamento());

        servicoCom(4).submeter(comando("a-1", new JogoDeContagem.Incrementar(FabricaDeCenario.ANA)));

        assertThat(publicador.recebidosPor(FabricaDeCenario.ANA)).hasSize(1);
        assertThat(publicador.recebidosPor(FabricaDeCenario.BRUNO)).hasSize(1);
    }

    @Test
    void partida_inexistente_e_erro() {
        var servico = servicoCom();
        var comando = comando("a-1", new JogoDeContagem.Incrementar(FabricaDeCenario.ANA));

        assertThatThrownBy(() -> servico.submeter(comando))
                .isInstanceOf(PartidaNaoEncontrada.class);
    }

    @Test
    void exige_comando_e_dependencias() {
        var servico = servicoCom();
        assertThatThrownBy(() -> servico.submeter(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new SubmeterAcaoService(null, null, null, null))
                .isInstanceOf(NullPointerException.class);
    }
}
