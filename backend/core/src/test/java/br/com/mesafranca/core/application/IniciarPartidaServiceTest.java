package br.com.mesafranca.core.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.mesafranca.core.domain.FaseDaPartida;
import br.com.mesafranca.core.duble.CatalogoFixo;
import br.com.mesafranca.core.duble.FabricaRoteirizada;
import br.com.mesafranca.core.duble.JogoDeContagem;
import br.com.mesafranca.core.duble.RepositorioDeTeste;
import br.com.mesafranca.core.erro.PartidaNaoEncontrada;
import br.com.mesafranca.core.port.in.IniciarPartida;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("IniciarPartidaService")
class IniciarPartidaServiceTest {

    private final RepositorioDeTeste repositorio = new RepositorioDeTeste();
    private final FabricaRoteirizada aleatoriedade = FabricaRoteirizada.com();
    private final IniciarPartida servico = new IniciarPartidaService(
            repositorio, new CatalogoFixo(new JogoDeContagem()), aleatoriedade);

    private IniciarPartida.Resposta iniciar() {
        return servico.iniciar(new IniciarPartida.Comando(FabricaDeCenario.PARTIDA));
    }

    @Test
    void cria_o_estado_inicial_e_passa_para_em_andamento() {
        var cheia = FabricaDeCenario.cheia();
        repositorio.comPartida(cheia);

        var resposta = iniciar();

        assertThat(((IniciarPartida.Iniciada) resposta).versao()).isEqualTo(cheia.versao() + 1);
        var guardada = repositorio.carregar(FabricaDeCenario.PARTIDA).orElseThrow();
        assertThat(guardada.fase()).isEqualTo(FaseDaPartida.EM_ANDAMENTO);
        assertThat(guardada.estado()).isPresent();
    }

    @Test
    void deriva_a_aleatoriedade_a_partir_da_versao_corrente() {
        var cheia = FabricaDeCenario.cheia();
        repositorio.comPartida(cheia);

        iniciar();

        assertThat(aleatoriedade.passosPedidos()).containsExactly(cheia.versao());
    }

    @Test
    void recusa_com_mesa_incompleta() {
        repositorio.comPartida(FabricaDeCenario.comAna());

        var resposta = iniciar();

        assertThat(((IniciarPartida.Recusado) resposta).motivo()).contains("faltam jogadores: 1 de 2");
    }

    @Test
    void recusa_partida_ja_iniciada() {
        repositorio.comPartida(FabricaDeCenario.emAndamento());

        assertThat(((IniciarPartida.Recusado) iniciar()).motivo()).contains("ja iniciada");
    }

    @Test
    void partida_inexistente_e_erro() {
        assertThatThrownBy(this::iniciar).isInstanceOf(PartidaNaoEncontrada.class);
    }

    @Test
    void exige_comando_e_dependencias() {
        assertThatThrownBy(() -> servico.iniciar(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new IniciarPartidaService(null, null, null))
                .isInstanceOf(NullPointerException.class);
    }
}
