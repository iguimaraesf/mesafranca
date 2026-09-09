package br.com.mesafranca.games.ludo;

import br.com.mesafranca.core.domain.Acao;
import br.com.mesafranca.core.domain.ConfiguracaoDePartida;
import br.com.mesafranca.core.domain.ConfiguracaoSuportada;
import br.com.mesafranca.core.domain.EstadoDeJogo;
import br.com.mesafranca.core.domain.Evento;
import br.com.mesafranca.core.domain.IdJogador;
import br.com.mesafranca.core.domain.IdentificadorDeJogo;
import br.com.mesafranca.core.domain.Jogador;
import br.com.mesafranca.core.domain.OrdemDeTurno;
import br.com.mesafranca.core.domain.Resultado;
import br.com.mesafranca.core.domain.ResultadoDeValidacao;
import br.com.mesafranca.core.domain.Transicao;
import br.com.mesafranca.core.domain.Visao;
import br.com.mesafranca.core.port.out.FonteDeAleatoriedade;
import br.com.mesafranca.core.spi.DefinicaoDeJogo;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Ludo: dados, peças e captura.
 *
 * <p>Papel arquitetural (PRD §5.1): pressionar a aleatoriedade determinística.
 * Todo lance passa por {@link FonteDeAleatoriedade}, o que torna qualquer
 * partida reproduzível a partir da semente.
 *
 * <p><strong>Funcionalidades futuras (não implementadas):</strong>
 * <ul>
 *   <li>Casas protegidas (safe spots) - configurable por partida</li>
 *   <li>Opção de 2 ou 4 peças por jogador - configurable por partida</li>
 *   <li>Bloqueio (duas peças do mesmo jogador na mesma casa)</li>
 *   <li>Regra de bônus por chegada</li>
 *   <li>Tabuleiros especiais de 5 e 6 jogadores (já suportado pelo adaptativo)</li>
 * </ul>
 *
 * <p><strong>Implementado:</strong>
 * <ul>
 *   <li>Tabuleiro adaptativo (oval, triângulo, quadrado, pentágono, hexágono)</li>
 *   <li>Sair da base com 1 ou 6</li>
 *   <li>Seis dá outra vez, três seis seguidos perdem a vez</li>
 *   <li>Captura e chegada dão outra vez</li>
 *   <li>Ciclo completo: sair da base, percorrer, capturar, coluna final, vencer</li>
 * </ul>
 */
public final class LudoDefinicao implements DefinicaoDeJogo {

    public static final IdentificadorDeJogo ID = new IdentificadorDeJogo("ludo");

    @Override
    public IdentificadorDeJogo identificador() {
        return ID;
    }

    @Override
    public ConfiguracaoSuportada configuracaoSuportada() {
        return ConfiguracaoSuportada.de(2, 4);
    }

    @Override
    public EstadoDeJogo estadoInicial(
            ConfiguracaoDePartida configuracao,
            List<Jogador> jogadores,
            FonteDeAleatoriedade aleatoriedade) {
        OrdemDeTurno ordem = OrdemDeTurno.de(jogadores);
        return LudoEstado.inicial(ordem.assentos(), ordem);
    }

    @Override
    public List<Acao> acoesDisponiveis(EstadoDeJogo estado, IdJogador jogador) {
        LudoEstado atual = (LudoEstado) estado;
        if (atual.vencedor().isPresent() || !atual.ordem().eVezDe(jogador)) {
            return List.of();
        }
        if (atual.dado().isEmpty()) {
            return List.of(new LudoAcao.RolarDado(jogador));
        }
        List<Integer> movimentos = pecasQuePodemMover(atual, jogador, atual.dado().orElseThrow());
        if (movimentos.isEmpty()) {
            return List.of(new LudoAcao.PassarVez(jogador));
        }
        return movimentos.stream().<Acao>map(peca -> new LudoAcao.MoverPeca(jogador, peca)).toList();
    }

    @Override
    public ResultadoDeValidacao validar(EstadoDeJogo estado, Acao acao) {
        LudoEstado atual = (LudoEstado) estado;
        if (!(acao instanceof LudoAcao ludo)) {
            return ResultadoDeValidacao.recusada("acao nao pertence ao Ludo");
        }
        if (atual.vencedor().isPresent()) {
            return ResultadoDeValidacao.recusada("partida encerrada");
        }
        if (!atual.ordem().eVezDe(ludo.autor())) {
            return ResultadoDeValidacao.recusada("nao e a vez de " + ludo.autor());
        }
        return switch (ludo) {
            case LudoAcao.RolarDado ignorado -> atual.dado().isPresent()
                    ? ResultadoDeValidacao.recusada("o dado ja foi rolado nesta vez")
                    : ResultadoDeValidacao.aceita();
            case LudoAcao.MoverPeca mover -> validarMovimento(atual, mover);
            case LudoAcao.PassarVez ignorado -> validarPassagem(atual, ludo.autor());
        };
    }

    private static ResultadoDeValidacao validarMovimento(LudoEstado atual, LudoAcao.MoverPeca mover) {
        if (atual.dado().isEmpty()) {
            return ResultadoDeValidacao.recusada("role o dado antes de mover");
        }
        int dado = atual.dado().orElseThrow();
        if (!pecasQuePodemMover(atual, mover.autor(), dado).contains(mover.peca())) {
            return ResultadoDeValidacao.recusada(
                    "peca " + mover.peca() + " nao pode andar " + dado);
        }
        return ResultadoDeValidacao.aceita();
    }

    private static ResultadoDeValidacao validarPassagem(LudoEstado atual, IdJogador autor) {
        if (atual.dado().isEmpty()) {
            return ResultadoDeValidacao.recusada("role o dado antes de passar a vez");
        }
        if (!pecasQuePodemMover(atual, autor, atual.dado().orElseThrow()).isEmpty()) {
            return ResultadoDeValidacao.recusada("ha movimento possivel; nao pode passar");
        }
        return ResultadoDeValidacao.aceita();
    }

    @Override
    public Transicao aplicar(EstadoDeJogo estado, Acao acao, FonteDeAleatoriedade aleatoriedade) {
        LudoEstado atual = (LudoEstado) estado;
        LudoAcao ludo = (LudoAcao) acao;
        return switch (ludo) {
            case LudoAcao.RolarDado rolar -> rolar(atual, rolar.autor(), aleatoriedade);
            case LudoAcao.MoverPeca mover -> mover(atual, mover);
            case LudoAcao.PassarVez passar -> passarVez(atual, passar.autor(), List.of());
        };
    }

    private static Transicao rolar(
            LudoEstado atual, IdJogador jogador, FonteDeAleatoriedade aleatoriedade) {
        int valor = aleatoriedade.dado(LudoEstado.FACES_DO_DADO);
        // Só conta seis seguidos quando realmente sai 6 (não quando sai 1)
        int seguidos = valor == LudoEstado.FACES_DO_DADO ? atual.seisSeguidos() + 1 : 0;
        List<Evento> eventos = new ArrayList<>();
        eventos.add(new LudoEvento.DadoRolado(jogador, valor));

        if (seguidos >= LudoEstado.SEIS_SEGUIDOS_PARA_PERDER_A_VEZ) {
            // Três seis seguidos anulam a jogada inteira: é o freio que impede
            // um jogador de monopolizar a mesa com turnos extras.
            return passarVez(atual.comDado(Optional.empty(), 0), jogador, eventos);
        }
        return new Transicao(atual.comDado(Optional.of(valor), seguidos), eventos);
    }

    private static Transicao mover(LudoEstado atual, LudoAcao.MoverPeca mover) {
        IdJogador jogador = mover.autor();
        int dado = atual.dado().orElseThrow();
        int origem = atual.avancoDe(jogador, mover.peca());
        int destino = origem == LudoEstado.NA_BASE ? 0 : origem + dado;

        List<Evento> eventos = new ArrayList<>();
        LudoEstado depois = atual.comPeca(jogador, mover.peca(), destino);

        if (origem == LudoEstado.NA_BASE) {
            eventos.add(new LudoEvento.PecaSaiuDaBase(
                    jogador, mover.peca(), depois.casaNaPista(jogador, 0).orElseThrow()));
        } else {
            // Evento mostra casas absolutas para o front-end renderizar corretamente
            // Se estiver na coluna final, usa o avanço diretamente (para o front-end desenhar)
            int numeroDeJogadores = atual.assentos().size();
            int ultimoAvanco = LudoEstado.calcularUltimoAvancoNaPista(numeroDeJogadores);

            int casaOrigem, casaDestino;
            if (origem > ultimoAvanco) {
                // Na coluna final - usa avanço como posição para o front-end
                casaOrigem = origem;
            } else {
                casaOrigem = atual.casaNaPista(jogador, origem).orElseThrow();
            }

            if (destino > ultimoAvanco) {
                // Na coluna final - usa avanço como posição para o front-end
                casaDestino = destino;
            } else {
                casaDestino = depois.casaNaPista(jogador, destino).orElseThrow();
            }

            eventos.add(new LudoEvento.PecaMovida(jogador, mover.peca(), casaOrigem, casaDestino));
        }

        boolean capturou = false;
        Optional<Integer> casa = depois.casaNaPista(jogador, destino);
        if (casa.isPresent()) {
            for (IdJogador adversario : depois.assentos()) {
                if (adversario.equals(jogador)) {
                    continue;
                }
                List<Integer> avancos = depois.pecasDe(adversario);
                for (int i = 0; i < avancos.size(); i++) {
                    if (depois.casaNaPista(adversario, avancos.get(i)).equals(casa)) {
                        depois = depois.comPeca(adversario, i, LudoEstado.NA_BASE);
                        eventos.add(new LudoEvento.PecaCapturada(adversario, i, jogador));
                        capturou = true;
                    }
                }
            }
        }

        int numeroDeJogadores = atual.assentos().size();
        int avancoFinal = LudoEstado.calcularAvancoFinal(numeroDeJogadores);
        boolean chegou = destino == avancoFinal;
        if (chegou) {
            eventos.add(new LudoEvento.PecaCompletouPercurso(jogador, mover.peca()));
        }

        if (depois.todasChegaram(jogador)) {
            eventos.add(new LudoEvento.PartidaVencida(jogador));
            return new Transicao(depois.comVencedor(jogador), eventos);
        }

        // Seis, captura e chegada dão outra vez. É o que faz o Ludo ter turnos
        // encadeados e o que impede o motor de assumir "uma ação, um turno".
        // Nota: 1 sai da base mas não dá outra vez.
        boolean jogaDeNovo = dado == LudoEstado.FACES_DO_DADO || capturou || chegou;
        LudoEstado semDado = depois.comDado(
                Optional.empty(), dado == LudoEstado.FACES_DO_DADO ? depois.seisSeguidos() : 0);
        if (jogaDeNovo) {
            return new Transicao(semDado, eventos);
        }
        return passarVez(semDado, jogador, eventos);
    }

    private static Transicao passarVez(
            LudoEstado atual, IdJogador jogador, List<Evento> jaOcorridos) {
        List<Evento> eventos = new ArrayList<>(jaOcorridos);
        OrdemDeTurno proxima = atual.ordem().proxima();
        eventos.add(new LudoEvento.VezPassada(jogador, proxima.daVez()));
        return new Transicao(atual.comOrdem(proxima).comDado(Optional.empty(), 0), eventos);
    }

    @Override
    public Visao visaoDe(EstadoDeJogo estado, IdJogador jogador) {
        LudoEstado atual = (LudoEstado) estado;
        return new LudoVisao(
                atual.assentos(),
                atual.pecas(),
                atual.entradas(),
                atual.jogadorDaVez(),
                atual.dado(),
                atual.vencedor());
    }

    @Override
    public Resultado resultado(EstadoDeJogo estado) {
        LudoEstado atual = (LudoEstado) estado;
        return atual.vencedor().map(Resultado::vencidaPor).orElseGet(Resultado::emAndamento);
    }

    /**
     * Retorna informações sobre o formato do tabuleiro para o front-end renderizar.
     * Isso permite que o cliente desenhe o tabuleiro corretamente conforme a
     * quantidade de jogadores (oval, triângulo, quadrado, etc.).
     */
    public InformacoesDoTabuleiro informacoesDoTabuleiro(EstadoDeJogo estado) {
        LudoEstado atual = (LudoEstado) estado;
        return new InformacoesDoTabuleiro(
                atual.totalDeCasasNaPista(),
                atual.casasPorJogador(),
                atual.posicoesCorredorFinal(),
                atual.formatoDoTabuleiro(),
                atual.assentos().size()
        );
    }

    /**
     * DTO para enviar informações do tabuleiro ao front-end.
     * Futuramente isso pode ser exposto via Visao ou através de um endpoint específico.
     */
    public record InformacoesDoTabuleiro(
            int totalDeCasasNaPista,
            int casasPorJogador,
            int posicoesCorredorFinal,
            String formato,
            int numeroDeJogadores
    ) {}

    /**
     * Peças do jogador que podem andar {@code dado} casas.
     *
     * <p>Uma peça na base sai com 1 ou 6. Uma peça na pista ou na coluna só anda
     * se o resultado couber até a casa final — é assim que "precisa do número
     * exato para chegar" fica sendo uma comparação, e não uma regra à parte.
     * Duas peças do mesmo jogador não ocupam a mesma posição.
     */
    static List<Integer> pecasQuePodemMover(LudoEstado estado, IdJogador jogador, int dado) {
        List<Integer> avancos = estado.pecasDe(jogador);
        int numeroDeJogadores = estado.assentos().size();
        int avancoFinal = LudoEstado.calcularAvancoFinal(numeroDeJogadores);
        List<Integer> possiveis = new ArrayList<>();

        for (int peca = 0; peca < avancos.size(); peca++) {
            int origem = avancos.get(peca);
            if (origem == avancoFinal) {
                continue;
            }
            int destino = origem == LudoEstado.NA_BASE ? 0 : origem + dado;
            if (origem == LudoEstado.NA_BASE && dado != 1 && dado != LudoEstado.FACES_DO_DADO) {
                continue;
            }
            if (destino > avancoFinal) {
                continue;
            }
            if (destino < avancoFinal && ocupadaPorPropria(avancos, peca, destino)) {
                continue;
            }
            possiveis.add(peca);
        }
        return List.copyOf(possiveis);
    }

    private static boolean ocupadaPorPropria(List<Integer> avancos, int peca, int destino) {
        for (int outra = 0; outra < avancos.size(); outra++) {
            if (outra != peca && avancos.get(outra) == destino) {
                return true;
            }
        }
        return false;
    }
}
