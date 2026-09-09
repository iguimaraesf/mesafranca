package br.com.mesafranca.games.ludo;

import br.com.mesafranca.core.domain.Acao;
import br.com.mesafranca.core.domain.Assento;
import br.com.mesafranca.core.domain.ConfiguracaoDePartida;
import br.com.mesafranca.core.domain.IdJogador;
import br.com.mesafranca.core.domain.Jogador;
import br.com.mesafranca.core.domain.Resultado;
import br.com.mesafranca.core.domain.ResultadoDeValidacao;
import br.com.mesafranca.core.domain.Transicao;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Teste manual para entender o fluxo do sistema sem IA.
 * Demonstra os conceitos do Java 17+ usados no projeto.
 */
@DisplayName("Teste Manual - Aprendendo o Sistema")
class TesteManual {

    // ================================================================
    // MÉTODO REUTILIZÁVEL - ESTE CÓDIGO SE REPETE PARA TODOS OS TESTES
    // ================================================================
    private void testarComJogadores(int numeroDeJogadores, int maxRodadas) {
        // 1. Cria a definição do jogo (implementação da SPI)
        LudoDefinicao ludo = new LudoDefinicao();

        // 2. Cria jogadores dinamicamente
        List<Jogador> jogadores = new ArrayList<>();
        for (int i = 0; i < numeroDeJogadores; i++) {
            jogadores.add(Jogador.humano(new IdJogador("jogador" + i), new Assento(i)));
        }

        // 3. Cria estado inicial do jogo
        LudoEstado estado = (LudoEstado) ludo.estadoInicial(
                ConfiguracaoDePartida.de(LudoDefinicao.ID, numeroDeJogadores),
                jogadores,
                Sorteio.comSemente(1L)
        );

        System.out.println("=== TESTE COM " + numeroDeJogadores + " JOGADORES ===");
        System.out.println("=== INFORMAÇÕES DO TABULEIRO ===");
        System.out.println("Formato: " + estado.formatoDoTabuleiro());
        System.out.println("Total de casas na pista: " + estado.totalDeCasasNaPista());
        System.out.println("Casas por jogador: " + estado.casasPorJogador());
        System.out.println("Posições no corredor final: " + estado.posicoesCorredorFinal());
        System.out.println("Jogador da vez: " + estado.jogadorDaVez());
        System.out.println();

        // 4. Simula jogadas completas (rolar dado + mover/passar)
        for (int i = 0; i < maxRodadas; i++) {
            IdJogador vez = estado.jogadorDaVez();
            Resultado resultado = ludo.resultado(estado);

            // Se partida encerrou, para o teste
            if (resultado instanceof Resultado.Encerrado) {
                System.out.println("=== PARTIDA ENCERRADA NA RODADA " + (i + 1) + " ===");
                System.out.println("Vencedor: " + ((Resultado.Encerrado) resultado).vencedores());
                System.out.println("Total de rodadas jogadas: " + (i + 1));
                return;
            }

            // Passo 1: Rolar o dado
            LudoAcao rolar = new LudoAcao.RolarDado(vez);
            ResultadoDeValidacao validacao = ludo.validar(estado, rolar);

            if (validacao instanceof ResultadoDeValidacao.Recusada recusada) {
                System.out.println("Rodada " + (i + 1) + ": Não pode rolar - " + recusada.motivo());
                continue;
            }

            Transicao transicao = ludo.aplicar(estado, rolar, Sorteio.comSemente(1L + i));
            estado = (LudoEstado) transicao.estado();

            // Passo 2: Ver o que pode fazer depois de rolar
            List<Acao> acoesAposRolagem = ludo.acoesDisponiveis(estado, vez);
            if (acoesAposRolagem.isEmpty()) {
                System.out.println("Rodada " + (i + 1) + ": Nenhuma ação disponível após rolar");
                continue;
            }

            // Passo 3: Aplica a primeira ação disponível (mover peça ou passar)
            Acao acaoAposRolagem = acoesAposRolagem.getFirst();
            validacao = ludo.validar(estado, acaoAposRolagem);

            if (validacao instanceof ResultadoDeValidacao.Aceita) {
                transicao = ludo.aplicar(estado, acaoAposRolagem, Sorteio.comSemente(1L + i));
                estado = (LudoEstado) transicao.estado();
            }

            // Mostra progresso a cada 50 rodadas
            if ((i + 1) % 50 == 0) {
                System.out.println("=== PROGRESSO: Rodada " + (i + 1) + " ===");
                System.out.println("Jogador da vez: " + estado.jogadorDaVez());
                System.out.println("Resultado: " + ludo.resultado(estado));
            }
        }

        System.out.println("=== LIMITE DE RODADAS ATINGIDO (" + maxRodadas + ") ===");
        System.out.println("Resultado final: " + ludo.resultado(estado));
    }

    // ================================================================
    // TESTES ESPECÍFICOS - USAM O MÉTODO REUTILIZÁVEL ACIMA
    // ================================================================

    @Test
    @DisplayName("teste com 3 jogadores (triângulo)")
    void teste_com_3_jogadores() {
        testarComJogadores(3, 500);
    }

    @Test
    @DisplayName("teste com 4 jogadores (quadrado clássico)")
    void teste_com_4_jogadores() {
        testarComJogadores(4, 500);
    }

    @Test
    @DisplayName("teste com 5 jogadores (pentágono)")
    void teste_com_5_jogadores() {
        testarComJogadores(5, 500);
    }

    @Test
    @DisplayName("teste com 6 jogadores (hexágono)")
    void teste_com_6_jogadores() {
        testarComJogadores(6, 1000); // Mais rodadas pois há mais jogadores e tabuleiro maior
    }

    @Test
    @DisplayName("entendendo records imutáveis")
    void entendendo_records() {
        // Records são classes imutáveis introduzidas no Java 14+
        // O projeto usa eles para representar estado que nunca muda

        IdJogador ana = new IdJogador("ana");
        IdJogador ana2 = new IdJogador("ana");

        System.out.println("=== RECORDS ===");
        System.out.println("ana.equals(ana2): " + ana.equals(ana2)); // true
        System.out.println("ana == ana2: " + (ana == ana2)); // false (objetos diferentes)
        System.out.println("ana: " + ana); // toString automático
        System.out.println();

        // Records são imutáveis por padrão
        // Não têm setters - para "mudar" um valor, cria-se um NOVO objeto
        IdJogador bruno = new IdJogador("bruno");
        System.out.println("=== IMUTABILIDADE ===");
        System.out.println("Ana: " + ana);
        System.out.println("Bruno: " + bruno);
        System.out.println("Records não têm setters - estado nunca muda");
        System.out.println("Para mudar, cria-se um NOVO objeto");
    }

    @Test
    @DisplayName("entendendo interfaces seladas")
    void entendendo_sealed() {
        // Sealed interfaces limitam quem pode implementar
        // O compilador sabe todos os casos possíveis

        Resultado emAndamento = Resultado.emAndamento();
        Resultado vencida = Resultado.vencidaPor(new IdJogador("ana"));
        Resultado empatada = Resultado.empatada();

        System.out.println("=== SEALED INTERFACES ===");
        System.out.println("Em andamento: " + emAndamento);
        System.out.println("Vencida: " + vencida);
        System.out.println("Empatada: " + empatada);
        System.out.println();

        // Pattern matching com switch exaustivo
        // O compilador exige que todos os casos sejam tratados
        System.out.println("=== SWITCH EXAUSTIVO ===");
        analisarResultado(emAndamento);
        analisarResultado(vencida);
        analisarResultado(empatada);
    }

    private void analisarResultado(Resultado resultado) {
        switch (resultado) {
            case Resultado.EmAndamento _ ->
                System.out.println("  " + resultado + " → partida continua");
            case Resultado.Encerrado encerrado -> {
                if (encerrado.vencedores().isEmpty()) {
                    System.out.println("  " + resultado + " → empate");
                } else {
                    System.out.println("  " + resultado + " → vencedor: " + encerrado.vencedores());
                }
            }
        }
    }
}
