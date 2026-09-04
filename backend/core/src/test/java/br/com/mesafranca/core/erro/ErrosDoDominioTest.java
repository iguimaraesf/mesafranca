package br.com.mesafranca.core.erro;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.mesafranca.core.domain.IdPartida;
import br.com.mesafranca.core.domain.IdentificadorDeJogo;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * As mensagens dos erros de dominio.
 *
 * <p>Parece teste de getter, mas nao e: estas mensagens sao o que aparece no
 * log quando algo da errado em producao. Uma mensagem que nao diz qual partida
 * nem qual versao e um erro que custa uma hora de investigacao.
 */
@DisplayName("Erros do dominio")
class ErrosDoDominioTest {

    private static final IdPartida PARTIDA =
            new IdPartida(UUID.fromString("00000000-0000-0000-0000-00000000000c"));

    @Test
    void conflito_de_versao_diz_qual_partida_e_quais_versoes() {
        ConflitoDeVersao erro = new ConflitoDeVersao(PARTIDA, 4L, 7L);

        assertThat(erro).hasMessageContaining(PARTIDA.toString())
                .hasMessageContaining("esperada 4")
                .hasMessageContaining("encontrada 7");
    }

    @Test
    void partida_nao_encontrada_diz_qual_identificador() {
        assertThat(new PartidaNaoEncontrada(PARTIDA))
                .hasMessageContaining(PARTIDA.toString());
    }

    @Test
    void jogo_desconhecido_diz_qual_jogo() {
        assertThat(new JogoDesconhecido(new IdentificadorDeJogo("xadrez")))
                .hasMessageContaining("xadrez");
    }

    @Test
    void regra_violada_preserva_a_mensagem() {
        assertThat(new RegraDePartidaViolada("mesa cheia")).hasMessageContaining("mesa cheia");
    }
}
