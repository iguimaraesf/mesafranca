package br.com.mesafranca.games.ludo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.mesafranca.core.domain.IdJogador;
import br.com.mesafranca.core.domain.OrdemDeTurno;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Guardas de construção do Ludo.
 *
 * <p>São as invariantes que protegem o estado de entradas impossíveis. Estes
 * testes existem porque o portão de cobertura mostrou, no {@code core}, que
 * guarda sem teste é guarda que ninguém sabe se funciona.
 */
@DisplayName("Ludo - guardas de construcao")
class LudoGuardasTest {

    private static final IdJogador ANA = new IdJogador("ana");
    private static final IdJogador BRUNO = new IdJogador("bruno");

    @ParameterizedTest
    @ValueSource(ints = {-1, 4, 99})
    void nao_existe_peca_fora_das_quatro(int peca) {
        assertThatThrownBy(() -> new LudoAcao.MoverPeca(ANA, peca))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("peca inexistente");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3})
    void as_quatro_pecas_sao_validas(int peca) {
        assertThat(new LudoAcao.MoverPeca(ANA, peca).peca()).isEqualTo(peca);
    }

    @Test
    void acoes_exigem_autor() {
        assertThatThrownBy(() -> new LudoAcao.RolarDado(null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new LudoAcao.MoverPeca(null, 0))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new LudoAcao.PassarVez(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void contagem_de_seis_nao_pode_ser_negativa() {
        List<IdJogador> assentos = List.of(ANA, BRUNO);
        Map<IdJogador, Integer> entradas = new LinkedHashMap<>();
        entradas.put(ANA, 0);
        entradas.put(BRUNO, 26);
        Map<IdJogador, List<Integer>> pecas = new LinkedHashMap<>();
        pecas.put(ANA, List.of(-1, -1, -1, -1));
        pecas.put(BRUNO, List.of(-1, -1, -1, -1));
        OrdemDeTurno ordem = new OrdemDeTurno(assentos, 0, OrdemDeTurno.Sentido.HORARIO, Set.of());

        assertThatThrownBy(() -> new LudoEstado(assentos, entradas, pecas, ordem,
                Optional.empty(), -1, Optional.empty()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("contagem de seis negativa");
    }

    @Test
    void casa_na_pista_so_existe_para_quem_esta_na_pista() {
        List<IdJogador> assentos = List.of(ANA, BRUNO);
        int numeroDeJogadores = assentos.size();
        int ultimoAvanco = LudoEstado.calcularUltimoAvancoNaPista(numeroDeJogadores);
        int avancoFinal = LudoEstado.calcularAvancoFinal(numeroDeJogadores);
        int casasNaPista = LudoEstado.calcularCasasNaPista(numeroDeJogadores);

        Map<IdJogador, Integer> entradas = new LinkedHashMap<>();
        entradas.put(ANA, 0);
        entradas.put(BRUNO, casasNaPista / 2);
        Map<IdJogador, List<Integer>> pecas = new LinkedHashMap<>();
        pecas.put(ANA, List.of(LudoEstado.NA_BASE, 0, ultimoAvanco, avancoFinal));
        pecas.put(BRUNO, List.of(-1, -1, -1, -1));
        LudoEstado estado = new LudoEstado(assentos, entradas, pecas,
                new OrdemDeTurno(assentos, 0, OrdemDeTurno.Sentido.HORARIO, Set.of()),
                Optional.empty(), 0, Optional.empty());

        assertThat(estado.casaNaPista(ANA, LudoEstado.NA_BASE)).isEmpty();
        assertThat(estado.casaNaPista(ANA, 0)).contains(0);
        assertThat(estado.casaNaPista(ANA, ultimoAvanco)).contains(ultimoAvanco);
        assertThat(estado.casaNaPista(ANA, ultimoAvanco + 1)).isEmpty();
        assertThat(estado.casaNaPista(ANA, avancoFinal)).isEmpty();
        // Entrada 13 e avanço 15 dão a volta no tabuleiro de 26 casas.
        assertThat(estado.casaNaPista(BRUNO, 15)).contains(2);

        assertThat(estado.chegou(ANA, 3)).isTrue();
        assertThat(estado.chegou(ANA, 0)).isFalse();
        assertThat(estado.todasChegaram(ANA)).isFalse();
        assertThat(estado.pecasDe(new IdJogador("estranho"))).isEmpty();
    }
}
