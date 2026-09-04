package br.com.mesafranca.core.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.mesafranca.core.domain.IdJogador;
import br.com.mesafranca.core.domain.TipoDeJogador;
import br.com.mesafranca.core.duble.RepositorioDeTeste;
import br.com.mesafranca.core.erro.PartidaNaoEncontrada;
import br.com.mesafranca.core.port.in.EntrarNaPartida;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("EntrarNaPartidaService")
class EntrarNaPartidaServiceTest {

    private final RepositorioDeTeste repositorio = new RepositorioDeTeste();
    private final EntrarNaPartida servico = new EntrarNaPartidaService(repositorio);

    private EntrarNaPartida.Resposta entrar(IdJogador jogador, TipoDeJogador tipo) {
        return servico.entrar(
                new EntrarNaPartida.Comando(FabricaDeCenario.PARTIDA, jogador, tipo));
    }

    @Test
    void senta_o_jogador_no_proximo_assento_livre() {
        repositorio.comPartida(FabricaDeCenario.aguardandoJogadores());

        var primeira = entrar(FabricaDeCenario.ANA, TipoDeJogador.HUMANO);
        var segunda = entrar(FabricaDeCenario.BRUNO, TipoDeJogador.CONVIDADO);

        assertThat(((EntrarNaPartida.Sentou) primeira).assento().indice()).isZero();
        assertThat(((EntrarNaPartida.Sentou) segunda).assento().indice()).isEqualTo(1);

        var guardada = repositorio.carregar(FabricaDeCenario.PARTIDA).orElseThrow();
        assertThat(guardada.jogadores()).hasSize(2);
        assertThat(guardada.jogador(FabricaDeCenario.BRUNO).orElseThrow().tipo())
                .isEqualTo(TipoDeJogador.CONVIDADO);
    }

    @Test
    void recusa_quem_ja_esta_na_partida() {
        repositorio.comPartida(FabricaDeCenario.comAna());

        var resposta = entrar(FabricaDeCenario.ANA, TipoDeJogador.HUMANO);

        assertThat(((EntrarNaPartida.Recusado) resposta).motivo()).contains("ja esta na partida");
    }

    @Test
    void recusa_quando_a_mesa_esta_cheia() {
        repositorio.comPartida(FabricaDeCenario.cheia());

        var resposta = entrar(new IdJogador("carla"), TipoDeJogador.HUMANO);

        assertThat(((EntrarNaPartida.Recusado) resposta).motivo()).contains("cheia");
    }

    @Test
    void recusa_depois_de_a_partida_comecar() {
        repositorio.comPartida(FabricaDeCenario.emAndamento());

        var resposta = entrar(new IdJogador("carla"), TipoDeJogador.BOT);

        assertThat(((EntrarNaPartida.Recusado) resposta).motivo()).contains("ja iniciada");
    }

    @Test
    void partida_inexistente_e_erro() {
        assertThatThrownBy(() -> entrar(FabricaDeCenario.ANA, TipoDeJogador.HUMANO))
                .isInstanceOf(PartidaNaoEncontrada.class)
                .hasMessageContaining(FabricaDeCenario.PARTIDA.toString());
    }

    @Test
    void exige_comando_e_repositorio() {
        assertThatThrownBy(() -> servico.entrar(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new EntrarNaPartidaService(null))
                .isInstanceOf(NullPointerException.class);
    }
}
