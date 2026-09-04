package br.com.mesafranca.core.port;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.mesafranca.core.domain.IdentificadorDeJogo;
import br.com.mesafranca.core.duble.AleatoriedadeRoteirizada;
import br.com.mesafranca.core.duble.CatalogoFixo;
import br.com.mesafranca.core.duble.JogoDeContagem;
import br.com.mesafranca.core.erro.JogoDesconhecido;
import br.com.mesafranca.core.port.out.FonteDeAleatoriedade;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Comportamento padrao das portas")
class PortasComPadraoTest {

    @Nested
    @DisplayName("FonteDeAleatoriedade")
    class Aleatoriedade {

        @Test
        void dado_delega_para_inteiro_no_intervalo_das_faces() {
            FonteDeAleatoriedade fonte = AleatoriedadeRoteirizada.de(6, 1);

            assertThat(fonte.dado(6)).isEqualTo(6);
            assertThat(fonte.dado(6)).isEqualTo(1);
        }

        @Test
        void dado_com_menos_de_duas_faces_e_defeito() {
            FonteDeAleatoriedade fonte = AleatoriedadeRoteirizada.de();

            assertThatThrownBy(() -> fonte.dado(1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("2 faces");
        }

        @Test
        void embaralhar_usa_fisher_yates_sobre_inteiro() {
            // Com i=3 sorteando 0 e i=2,1 sorteando o proprio indice, apenas
            // as pontas trocam: a permutacao e previsivel e verificavel.
            FonteDeAleatoriedade fonte = AleatoriedadeRoteirizada.de(0, 2, 1);

            List<String> embaralhado = fonte.embaralhar(List.of("a", "b", "c", "d"));

            assertThat(embaralhado).containsExactly("d", "b", "c", "a");
        }

        @Test
        void embaralhar_devolve_copia_imutavel_e_nao_toca_na_origem() {
            FonteDeAleatoriedade fonte = AleatoriedadeRoteirizada.de(0, 0, 0);
            List<String> original = List.of("a", "b", "c", "d");

            List<String> embaralhado = fonte.embaralhar(original);

            assertThat(original).containsExactly("a", "b", "c", "d");
            assertThatThrownBy(() -> embaralhado.add("e"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void embaralhar_lista_de_um_elemento_nao_sorteia_nada() {
            AleatoriedadeRoteirizada fonte = AleatoriedadeRoteirizada.de();

            assertThat(fonte.embaralhar(List.of("unico"))).containsExactly("unico");
            assertThat(fonte.embaralhar(List.of())).isEmpty();
            assertThat(fonte.consumidoPorCompleto()).isTrue();
        }

        @Test
        void embaralhar_recusa_nulo() {
            FonteDeAleatoriedade fonte = AleatoriedadeRoteirizada.de();

            assertThatThrownBy(() -> fonte.embaralhar(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("CatalogoDeJogos")
    class Catalogo {

        @Test
        void resolver_devolve_a_definicao_registrada() {
            CatalogoFixo catalogo = new CatalogoFixo(new JogoDeContagem());

            assertThat(catalogo.resolver(JogoDeContagem.ID).identificador())
                    .isEqualTo(JogoDeContagem.ID);
            assertThat(catalogo.disponiveis()).containsExactly(JogoDeContagem.ID);
        }

        @Test
        void resolver_jogo_ausente_falha_com_erro_de_dominio() {
            CatalogoFixo catalogo = new CatalogoFixo();
            IdentificadorDeJogo ausente = new IdentificadorDeJogo("xadrez");

            assertThatThrownBy(() -> catalogo.resolver(ausente))
                    .isInstanceOf(JogoDesconhecido.class)
                    .hasMessageContaining("xadrez");
        }
    }
}
