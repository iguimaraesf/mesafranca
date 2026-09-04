package br.com.mesafranca.core.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("OrdemDeTurno")
class OrdemDeTurnoTest {

    private static final IdJogador ANA = new IdJogador("ana");
    private static final IdJogador BRUNO = new IdJogador("bruno");
    private static final IdJogador CARLA = new IdJogador("carla");
    private static final IdJogador DAVI = new IdJogador("davi");

    private static OrdemDeTurno mesaDeQuatro() {
        return OrdemDeTurno.de(List.of(
                Jogador.humano(DAVI, new Assento(3)),
                Jogador.humano(ANA, new Assento(0)),
                Jogador.humano(CARLA, new Assento(2)),
                Jogador.humano(BRUNO, new Assento(1))));
    }

    @Test
    void ordena_pelo_assento_e_nao_pela_ordem_de_chegada() {
        assertThat(mesaDeQuatro().assentos()).containsExactly(ANA, BRUNO, CARLA, DAVI);
        assertThat(mesaDeQuatro().daVez()).isEqualTo(ANA);
        assertThat(mesaDeQuatro().sentido()).isEqualTo(OrdemDeTurno.Sentido.HORARIO);
    }

    @Test
    void avanca_no_sentido_horario_e_da_a_volta() {
        OrdemDeTurno ordem = mesaDeQuatro();

        assertThat(ordem.proxima().daVez()).isEqualTo(BRUNO);
        assertThat(ordem.avancando(3).daVez()).isEqualTo(DAVI);
        assertThat(ordem.avancando(4).daVez()).isEqualTo(ANA);
        assertThat(ordem.avancando(5).daVez()).isEqualTo(BRUNO);
    }

    @Test
    void invertida_troca_o_sentido_sem_trocar_de_jogador() {
        OrdemDeTurno invertida = mesaDeQuatro().proxima().invertida();

        assertThat(invertida.daVez()).isEqualTo(BRUNO);
        assertThat(invertida.sentido()).isEqualTo(OrdemDeTurno.Sentido.ANTI_HORARIO);
        assertThat(invertida.proxima().daVez()).isEqualTo(ANA);
        assertThat(invertida.proxima().proxima().daVez()).isEqualTo(DAVI);
    }

    @Test
    void com_dois_jogadores_inverter_e_avancar_devolve_a_vez_a_quem_jogou() {
        // É por isso que o UNO não precisa de regra especial para o Inverter em
        // mesa de dois: a mecânica já produz o efeito de pular.
        OrdemDeTurno dupla = OrdemDeTurno.de(List.of(
                Jogador.humano(ANA, new Assento(0)),
                Jogador.humano(BRUNO, new Assento(1))));

        assertThat(dupla.invertida().proxima().daVez()).isEqualTo(BRUNO);
        assertThat(dupla.avancando(2).daVez()).isEqualTo(ANA);
    }

    @Test
    void pula_quem_saiu_da_mesa() {
        OrdemDeTurno ordem = mesaDeQuatro().semJogador(BRUNO).semJogador(CARLA);

        assertThat(ordem.daVez()).isEqualTo(ANA);
        assertThat(ordem.proxima().daVez()).isEqualTo(DAVI);
        assertThat(ordem.proxima().proxima().daVez()).isEqualTo(ANA);
        assertThat(ordem.ativos()).containsExactly(ANA, DAVI);
        assertThat(ordem.estaFora(BRUNO)).isTrue();
        assertThat(ordem.estaFora(ANA)).isFalse();
    }

    @Test
    void eliminar_quem_esta_jogando_nao_move_a_vez_sozinho() {
        // Quem decide se a eliminação encerra o turno é o jogo, não o core.
        OrdemDeTurno ordem = mesaDeQuatro().semJogador(ANA);

        assertThat(ordem.daVez()).isEqualTo(ANA);
        assertThat(ordem.proxima().daVez()).isEqualTo(BRUNO);
    }

    @Test
    void com_um_unico_ativo_a_vez_volta_para_ele() {
        OrdemDeTurno ordem = mesaDeQuatro().semJogador(BRUNO).semJogador(CARLA).semJogador(DAVI);

        assertThat(ordem.proxima().daVez()).isEqualTo(ANA);
        assertThat(ordem.ativos()).containsExactly(ANA);
    }

    @Test
    void sem_ninguem_ativo_a_ordem_nao_se_move() {
        OrdemDeTurno vazia = mesaDeQuatro()
                .semJogador(ANA).semJogador(BRUNO).semJogador(CARLA).semJogador(DAVI);

        assertThat(vazia.proxima()).isEqualTo(vazia);
        assertThat(vazia.ativos()).isEmpty();
    }

    @Test
    void responde_de_quem_e_a_vez() {
        OrdemDeTurno ordem = mesaDeQuatro();

        assertThat(ordem.eVezDe(ANA)).isTrue();
        assertThat(ordem.eVezDe(BRUNO)).isFalse();
    }

    @Test
    void recusa_mesa_invalida() {
        List<IdJogador> dois = List.of(ANA, BRUNO);
        Set<IdJogador> nenhum = Set.of();

        assertThatThrownBy(() -> new OrdemDeTurno(List.of(), 0, OrdemDeTurno.Sentido.HORARIO, nenhum))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ao menos um assento");
        assertThatThrownBy(() -> new OrdemDeTurno(
                List.of(ANA, ANA), 0, OrdemDeTurno.Sentido.HORARIO, nenhum))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("repetidos");
        assertThatThrownBy(() -> new OrdemDeTurno(dois, 2, OrdemDeTurno.Sentido.HORARIO, nenhum))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fora da mesa");
        // O lado negativo do intervalo tambem: um assento -1 nao existe, e a
        // guarda so vale se cobrir os dois lados.
        assertThatThrownBy(() -> new OrdemDeTurno(dois, -1, OrdemDeTurno.Sentido.HORARIO, nenhum))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fora da mesa");
        assertThatThrownBy(() -> new OrdemDeTurno(
                dois, 0, OrdemDeTurno.Sentido.HORARIO, Set.of(CARLA)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("eliminado");
        assertThatThrownBy(() -> new OrdemDeTurno(dois, 0, null, nenhum))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new OrdemDeTurno(null, 0, OrdemDeTurno.Sentido.HORARIO, nenhum))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void aceita_conjunto_de_eliminados_nulo() {
        assertThat(new OrdemDeTurno(
                List.of(ANA), 0, OrdemDeTurno.Sentido.HORARIO, null).fora()).isEmpty();
    }

    @Test
    void recusa_passo_nao_positivo_e_jogador_de_fora() {
        OrdemDeTurno ordem = mesaDeQuatro();

        assertThatThrownBy(() -> ordem.avancando(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positivo");
        assertThatThrownBy(() -> ordem.semJogador(new IdJogador("estranho")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nao esta na mesa");
        assertThatThrownBy(() -> ordem.semJogador(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void sentido_sabe_se_inverter() {
        assertThat(OrdemDeTurno.Sentido.HORARIO.invertido())
                .isEqualTo(OrdemDeTurno.Sentido.ANTI_HORARIO);
        assertThat(OrdemDeTurno.Sentido.ANTI_HORARIO.invertido())
                .isEqualTo(OrdemDeTurno.Sentido.HORARIO);
    }
}
