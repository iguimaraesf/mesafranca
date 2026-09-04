package br.com.mesafranca.games.uno;

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
 * UNO: efeitos encadeados, sentido reversível e ação fora de turno.
 *
 * <p>Papel arquitetural (PRD §5.3): é o jogo que quebra a suposição de que
 * "ação válida" implica "ação de quem está jogando". {@code DeclararUno} e
 * {@code AcusarUno} valem para qualquer jogador, a qualquer momento — e foi
 * isso que obrigou a validação de turno a descer para dentro de cada caso, em
 * vez de ficar numa guarda única no começo.
 *
 * <p><strong>Fora do escopo desta implementação, deliberadamente:</strong>
 * desafio do +4, empilhamento de +2 e +4, jump-in, pontuação entre rodadas, a
 * regra 7-0 e o efeito da carta inicial virada. O ciclo completo — distribuir,
 * jogar, comprar, aplicar efeito, declarar, punir e vencer — está inteiro.
 */
public final class UnoDefinicao implements DefinicaoDeJogo {

    public static final IdentificadorDeJogo ID = new IdentificadorDeJogo("uno");

    @Override
    public IdentificadorDeJogo identificador() {
        return ID;
    }

    @Override
    public ConfiguracaoSuportada configuracaoSuportada() {
        return ConfiguracaoSuportada.de(2, 6);
    }

    @Override
    public EstadoDeJogo estadoInicial(
            ConfiguracaoDePartida configuracao,
            List<Jogador> jogadores,
            FonteDeAleatoriedade aleatoriedade) {
        OrdemDeTurno ordem = OrdemDeTurno.de(jogadores);
        return UnoEstado.inicial(
                ordem.assentos(), ordem, aleatoriedade.embaralhar(CartaUno.baralhoCompleto()));
    }

    @Override
    public List<Acao> acoesDisponiveis(EstadoDeJogo estado, IdJogador jogador) {
        UnoEstado atual = (UnoEstado) estado;
        if (atual.vencedor().isPresent()) {
            return List.of();
        }
        List<Acao> acoes = new ArrayList<>();

        // Fora de turno: valem para qualquer jogador, mesmo quem não está jogando.
        if (atual.mao(jogador).size() == 1 && !atual.declararamUno().contains(jogador)) {
            acoes.add(new UnoAcao.DeclararUno(jogador));
        }
        for (IdJogador alvo : atual.assentos()) {
            if (!alvo.equals(jogador) && atual.vulneravelAAcusacao(alvo)) {
                acoes.add(new UnoAcao.AcusarUno(jogador, alvo));
            }
        }

        if (!atual.ordem().eVezDe(jogador)) {
            return List.copyOf(acoes);
        }
        for (CartaUno carta : atual.mao(jogador).stream().distinct().toList()) {
            if (!carta.combinaCom(atual.topo(), atual.corAtiva())) {
                continue;
            }
            if (carta.curinga()) {
                for (Cor cor : Cor.values()) {
                    if (cor.jogavel()) {
                        acoes.add(UnoAcao.JogarCarta.escolhendo(jogador, carta, cor));
                    }
                }
            } else {
                acoes.add(UnoAcao.JogarCarta.de(jogador, carta));
            }
        }
        if (!atual.comprouNestaVez()) {
            acoes.add(new UnoAcao.Comprar(jogador));
        } else if (!atual.podeJogar(jogador)) {
            acoes.add(new UnoAcao.PassarVez(jogador));
        }
        return List.copyOf(acoes);
    }

    @Override
    public ResultadoDeValidacao validar(EstadoDeJogo estado, Acao acao) {
        UnoEstado atual = (UnoEstado) estado;
        if (!(acao instanceof UnoAcao uno)) {
            return ResultadoDeValidacao.recusada("acao nao pertence ao UNO");
        }
        if (atual.vencedor().isPresent()) {
            return ResultadoDeValidacao.recusada("partida encerrada");
        }
        return switch (uno) {
            case UnoAcao.JogarCarta jogada -> validarJogada(atual, jogada);
            case UnoAcao.Comprar comprar -> exigirVez(atual, comprar.autor())
                    .orElseGet(() -> atual.comprouNestaVez()
                            ? ResultadoDeValidacao.recusada("voce ja comprou nesta vez")
                            : ResultadoDeValidacao.aceita());
            case UnoAcao.PassarVez passar -> validarPassagem(atual, passar.autor());
            case UnoAcao.DeclararUno declarar -> validarDeclaracao(atual, declarar.autor());
            case UnoAcao.AcusarUno acusar -> validarAcusacao(atual, acusar);
        };
    }

    /** Guarda de turno usada só pelas ações que exigem a vez. */
    private static Optional<ResultadoDeValidacao> exigirVez(UnoEstado atual, IdJogador autor) {
        if (!atual.ordem().eVezDe(autor)) {
            return Optional.of(ResultadoDeValidacao.recusada("nao e a vez de " + autor));
        }
        return Optional.empty();
    }

    private static ResultadoDeValidacao validarJogada(
            UnoEstado atual, UnoAcao.JogarCarta jogada) {
        Optional<ResultadoDeValidacao> foraDeVez = exigirVez(atual, jogada.autor());
        if (foraDeVez.isPresent()) {
            return foraDeVez.get();
        }
        if (!atual.mao(jogada.autor()).contains(jogada.carta())) {
            return ResultadoDeValidacao.recusada("voce nao tem essa carta");
        }
        if (!jogada.carta().combinaCom(atual.topo(), atual.corAtiva())) {
            return ResultadoDeValidacao.recusada(
                    "carta nao combina com " + atual.corAtiva() + " nem com o topo");
        }
        if (jogada.carta().curinga()) {
            if (jogada.corEscolhida().isEmpty()) {
                return ResultadoDeValidacao.recusada("curinga exige escolher a cor");
            }
            if (!jogada.corEscolhida().orElseThrow().jogavel()) {
                return ResultadoDeValidacao.recusada("preto nao e cor jogavel");
            }
        } else if (jogada.corEscolhida().isPresent()) {
            return ResultadoDeValidacao.recusada("so curinga escolhe cor");
        }
        return ResultadoDeValidacao.aceita();
    }

    private static ResultadoDeValidacao validarPassagem(UnoEstado atual, IdJogador autor) {
        Optional<ResultadoDeValidacao> foraDeVez = exigirVez(atual, autor);
        if (foraDeVez.isPresent()) {
            return foraDeVez.get();
        }
        if (!atual.comprouNestaVez()) {
            return ResultadoDeValidacao.recusada("compre antes de passar a vez");
        }
        if (atual.podeJogar(autor)) {
            return ResultadoDeValidacao.recusada("voce tem carta jogavel");
        }
        return ResultadoDeValidacao.aceita();
    }

    private static ResultadoDeValidacao validarDeclaracao(UnoEstado atual, IdJogador autor) {
        if (atual.mao(autor).size() != 1) {
            return ResultadoDeValidacao.recusada("so declara UNO quem esta com uma carta");
        }
        if (atual.declararamUno().contains(autor)) {
            return ResultadoDeValidacao.recusada("voce ja declarou UNO");
        }
        return ResultadoDeValidacao.aceita();
    }

    private static ResultadoDeValidacao validarAcusacao(UnoEstado atual, UnoAcao.AcusarUno acusar) {
        if (acusar.acusado().equals(acusar.autor())) {
            return ResultadoDeValidacao.recusada("ninguem se acusa");
        }
        if (!atual.assentos().contains(acusar.acusado())) {
            return ResultadoDeValidacao.recusada("acusado nao esta na mesa");
        }
        if (!atual.vulneravelAAcusacao(acusar.acusado())) {
            return ResultadoDeValidacao.recusada("acusado nao esta com uma carta por declarar");
        }
        return ResultadoDeValidacao.aceita();
    }

    @Override
    public Transicao aplicar(EstadoDeJogo estado, Acao acao, FonteDeAleatoriedade aleatoriedade) {
        UnoEstado atual = (UnoEstado) estado;
        return switch ((UnoAcao) acao) {
            case UnoAcao.JogarCarta jogada -> jogar(atual, jogada, aleatoriedade);
            case UnoAcao.Comprar comprar -> comprarNaVez(atual, comprar.autor(), aleatoriedade);
            case UnoAcao.PassarVez passar -> passarVez(atual, passar.autor(), new ArrayList<>());
            case UnoAcao.DeclararUno declarar -> Transicao.de(
                    atual.comDeclaracao(declarar.autor()),
                    new UnoEvento.UnoDeclarado(declarar.autor()));
            case UnoAcao.AcusarUno acusar -> punir(atual, acusar, aleatoriedade);
        };
    }

    private static Transicao jogar(
            UnoEstado atual, UnoAcao.JogarCarta jogada, FonteDeAleatoriedade aleatoriedade) {
        IdJogador autor = jogada.autor();
        List<Evento> eventos = new ArrayList<>();
        eventos.add(new UnoEvento.CartaJogada(autor, jogada.carta()));

        List<CartaUno> mao = new ArrayList<>(atual.mao(autor));
        mao.remove(jogada.carta());
        List<CartaUno> descarte = new ArrayList<>(atual.descarte());
        descarte.add(jogada.carta());

        Cor cor = jogada.corEscolhida().orElse(jogada.carta().cor());
        jogada.corEscolhida().ifPresent(escolhida ->
                eventos.add(new UnoEvento.CorEscolhida(autor, escolhida)));

        UnoEstado depois = atual
                .comMao(autor, mao)
                .comDescarte(descarte, cor)
                .comCompraRegistrada(false);

        if (mao.isEmpty()) {
            eventos.add(new UnoEvento.PartidaVencida(autor));
            return new Transicao(depois.comVencedor(autor), eventos);
        }
        return aplicarEfeito(depois, jogada.carta(), autor, eventos, aleatoriedade);
    }

    private static Transicao aplicarEfeito(
            UnoEstado estado,
            CartaUno carta,
            IdJogador autor,
            List<Evento> eventos,
            FonteDeAleatoriedade aleatoriedade) {

        UnoEstado atual = estado;
        if (carta.tipo() == TipoDeCarta.INVERTER) {
            atual = atual.comOrdem(atual.ordem().invertida());
            eventos.add(new UnoEvento.SentidoInvertido(atual.ordem().sentido()));
        }

        int compras = carta.tipo().compraForcada();
        // Com dois jogadores, inverter o sentido devolveria a vez ao adversario;
        // a regra oficial manda que o Inverter valha como Pular. Sem esta linha,
        // a carta viraria um "passe a vez" com nome pomposo.
        boolean inverterValeComoPular = carta.tipo() == TipoDeCarta.INVERTER
                && atual.ordem().ativos().size() == 2;
        boolean pula = carta.tipo() == TipoDeCarta.PULAR || compras > 0 || inverterValeComoPular;

        if (compras > 0) {
            IdJogador vitima = atual.ordem().proxima().daVez();
            atual = distribuir(atual, vitima, compras, eventos, aleatoriedade);
        }
        if (pula) {
            IdJogador pulado = atual.ordem().proxima().daVez();
            eventos.add(new UnoEvento.JogadorPulado(pulado));
            OrdemDeTurno adiante = atual.ordem().avancando(2);
            eventos.add(new UnoEvento.VezPassada(autor, adiante.daVez()));
            return new Transicao(atual.comOrdem(adiante).comCompraRegistrada(false), eventos);
        }
        return passarVez(atual, autor, eventos);
    }

    private static Transicao comprarNaVez(
            UnoEstado atual, IdJogador autor, FonteDeAleatoriedade aleatoriedade) {
        List<Evento> eventos = new ArrayList<>();
        UnoEstado depois = distribuir(atual, autor, 1, eventos, aleatoriedade)
                .comCompraRegistrada(true);
        // Comprar não passa a vez: quem comprou pode jogar a carta comprada.
        return new Transicao(depois, eventos);
    }

    private static Transicao punir(
            UnoEstado atual, UnoAcao.AcusarUno acusar, FonteDeAleatoriedade aleatoriedade) {
        List<Evento> eventos = new ArrayList<>();
        UnoEstado depois = distribuir(
                atual, acusar.acusado(), UnoEstado.PUNICAO_POR_ESQUECER_UNO,
                eventos, aleatoriedade);

        eventos.add(new UnoEvento.UnoPunido(
                acusar.acusado(), acusar.autor(), UnoEstado.PUNICAO_POR_ESQUECER_UNO));
        // Punido, o assunto esta encerrado: sem isto, com o monte seco a punicao
        // nao entrega carta alguma e a mesma acusacao poderia ser repetida para
        // sempre, sem mudar nada.
        depois = depois.comDeclaracao(acusar.acusado());
        // A acusação não mexe no turno: é interrupção, não jogada.
        return new Transicao(depois, eventos);
    }

    /**
     * Entrega cartas do monte, remontando a partir do descarte quando acaba.
     *
     * <p>Remontar exige embaralhar, e embaralhar exige a porta de
     * aleatoriedade — é o ponto em que o UNO prova que o determinismo por
     * semente vale também para o que acontece no meio de uma jogada.
     */
    private static UnoEstado distribuir(
            UnoEstado estado,
            IdJogador jogador,
            int quantidade,
            List<Evento> eventos,
            FonteDeAleatoriedade aleatoriedade) {

        UnoEstado atual = estado;
        List<CartaUno> compradas = new ArrayList<>();
        for (int i = 0; i < quantidade; i++) {
            if (atual.monte().isEmpty()) {
                atual = remontar(atual, eventos, aleatoriedade);
                if (atual.monte().isEmpty()) {
                    break;
                }
            }
            compradas.add(atual.monte().getFirst());
            atual = atual.comMonte(atual.monte().subList(1, atual.monte().size()));
        }
        if (compradas.isEmpty()) {
            return atual;
        }
        List<CartaUno> mao = new ArrayList<>(atual.mao(jogador));
        mao.addAll(compradas);
        eventos.add(new UnoEvento.CartaComprada(
                jogador, compradas.size(), Optional.of(List.copyOf(compradas))));
        return atual.comMao(jogador, mao);
    }

    private static UnoEstado remontar(
            UnoEstado atual, List<Evento> eventos, FonteDeAleatoriedade aleatoriedade) {
        if (atual.descarte().size() <= 1) {
            return atual;
        }
        List<CartaUno> paraEmbaralhar = atual.descarte().subList(0, atual.descarte().size() - 1);
        List<CartaUno> novoMonte = aleatoriedade.embaralhar(paraEmbaralhar);
        eventos.add(new UnoEvento.MonteRemontado(novoMonte.size()));
        return atual
                .comMonte(novoMonte)
                .comDescarte(List.of(atual.topo()), atual.corAtiva());
    }

    private static Transicao passarVez(UnoEstado atual, IdJogador autor, List<Evento> jaOcorridos) {
        List<Evento> eventos = new ArrayList<>(jaOcorridos);
        OrdemDeTurno proxima = atual.ordem().proxima();
        eventos.add(new UnoEvento.VezPassada(autor, proxima.daVez()));
        return new Transicao(atual.comOrdem(proxima).comCompraRegistrada(false), eventos);
    }

    @Override
    public Visao visaoDe(EstadoDeJogo estado, IdJogador jogador) {
        UnoEstado atual = (UnoEstado) estado;
        return new UnoVisao(
                atual.assentos(),
                atual.mao(jogador),
                atual.tamanhoDasMaos(),
                atual.topo(),
                atual.corAtiva(),
                atual.jogadorDaVez(),
                atual.ordem().sentido(),
                atual.monte().size(),
                atual.declararamUno(),
                atual.vencedor());
    }

    @Override
    public Resultado resultado(EstadoDeJogo estado) {
        UnoEstado atual = (UnoEstado) estado;
        return atual.vencedor().map(Resultado::vencidaPor).orElseGet(Resultado::emAndamento);
    }

    /** Só a compra é segredo: a mesa vê quantas cartas, não quais. */
    @Override
    public Optional<Evento> eventoVisivelPara(
            EstadoDeJogo estado, Evento evento, IdJogador jogador) {
        if (evento instanceof UnoEvento.CartaComprada comprada
                && !comprada.jogador().equals(jogador)) {
            return Optional.of(new UnoEvento.CartaComprada(
                    comprada.jogador(), comprada.quantidade(), Optional.empty()));
        }
        return Optional.of(evento);
    }
}
