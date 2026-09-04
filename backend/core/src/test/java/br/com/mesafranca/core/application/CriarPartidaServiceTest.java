package br.com.mesafranca.core.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.mesafranca.core.domain.IdentificadorDeJogo;
import br.com.mesafranca.core.duble.CatalogoFixo;
import br.com.mesafranca.core.duble.FabricaRoteirizada;
import br.com.mesafranca.core.duble.IdentidadeFixa;
import br.com.mesafranca.core.duble.JogoDeContagem;
import br.com.mesafranca.core.duble.RepositorioDeTeste;
import br.com.mesafranca.core.erro.JogoDesconhecido;
import br.com.mesafranca.core.port.in.CriarPartida;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CriarPartidaService")
class CriarPartidaServiceTest {

    private final RepositorioDeTeste repositorio = new RepositorioDeTeste();
    private final FabricaRoteirizada aleatoriedade = FabricaRoteirizada.com();
    private final CriarPartida servico = new CriarPartidaService(
            repositorio, new CatalogoFixo(new JogoDeContagem()), aleatoriedade, new IdentidadeFixa());

    @Test
    void cria_partida_aguardando_com_semente_sorteada_uma_unica_vez() {
        CriarPartida.Resposta resposta =
                servico.criar(CriarPartida.Comando.de(JogoDeContagem.ID, 2));

        assertThat(resposta).isInstanceOf(CriarPartida.Criada.class);
        var criada = (CriarPartida.Criada) resposta;
        assertThat(criada.partida()).isEqualTo(IdentidadeFixa.ID);

        var guardada = repositorio.carregar(IdentidadeFixa.ID).orElseThrow();
        assertThat(guardada.semente()).isEqualTo(aleatoriedade.novaSemente());
        assertThat(guardada.jogadores()).isEmpty();
        assertThat(guardada.versao()).isZero();
    }

    @Test
    void preserva_as_opcoes_escolhidas_no_lobby() {
        servico.criar(new CriarPartida.Comando(JogoDeContagem.ID, 3, Map.of("modo", "rapido")));

        var guardada = repositorio.carregar(IdentidadeFixa.ID).orElseThrow();
        assertThat(guardada.configuracao().opcao("modo", "?")).isEqualTo("rapido");
        assertThat(guardada.configuracao().numeroDeJogadores()).isEqualTo(3);
    }

    @Test
    void recusa_numero_de_jogadores_que_o_jogo_nao_aceita() {
        CriarPartida.Resposta resposta =
                servico.criar(CriarPartida.Comando.de(JogoDeContagem.ID, 9));

        assertThat(resposta).isInstanceOf(CriarPartida.Recusado.class);
        assertThat(((CriarPartida.Recusado) resposta).motivo()).contains("9 jogadores");
        assertThat(repositorio.carregar(IdentidadeFixa.ID)).isEmpty();
    }

    @Test
    void jogo_fora_do_catalogo_e_erro_e_nao_recusa() {
        var comando = CriarPartida.Comando.de(new IdentificadorDeJogo("xadrez"), 2);

        assertThatThrownBy(() -> servico.criar(comando)).isInstanceOf(JogoDesconhecido.class);
    }

    @Test
    void exige_comando_e_dependencias() {
        assertThatThrownBy(() -> servico.criar(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CriarPartidaService(null, null, null, null))
                .isInstanceOf(NullPointerException.class);
    }
}
