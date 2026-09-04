package br.com.mesafranca.games.loveletter;

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
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Love Letter: mão de uma carta só, dedução e blefe.
 *
 * <p>Papel arquitetural (PRD §5.2): é este jogo que prova que a plataforma
 * sabe guardar segredo. Ele é o único dos três que sobrescreve
 * {@code eventoVisivelPara}, e o único cuja {@code visaoDe} muda de verdade
 * conforme quem pergunta.
 *
 * <p><strong>Fora do escopo desta implementação, deliberadamente:</strong> as
 * fichas de afeição e a sequência de várias rodadas (aqui uma rodada é a
 * partida), a variante de dois jogadores com três cartas viradas para cima, e
 * o modo de 5 a 6 jogadores. O ciclo completo — comprar, jogar, resolver
 * efeito, eliminar e apurar vencedor — está inteiro.
 */
public final class LoveLetterDefinicao implements DefinicaoDeJogo {

    public static final IdentificadorDeJogo ID = new IdentificadorDeJogo("love-letter");

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
        List<Carta> embaralhado = aleatoriedade.embaralhar(Carta.baralhoCompleto());
        return LoveLetterEstado.inicial(ordem.assentos(), ordem, embaralhado);
    }

    @Override
    public List<Acao> acoesDisponiveis(EstadoDeJogo estado, IdJogador jogador) {
        LoveLetterEstado atual = (LoveLetterEstado) estado;
        if (atual.encerrada() || !atual.ordem().eVezDe(jogador)) {
            return List.of();
        }
        if (atual.fase() == Fase.COMPRAR) {
            return List.of(new LoveLetterAcao.ComprarCarta(jogador));
        }
        List<Acao> acoes = new ArrayList<>();
        for (Carta carta : jogaveis(atual, jogador)) {
            List<IdJogador> alvos = alvosPossiveis(atual, jogador, carta);
            if (!carta.exigeAlvo() || alvos.isEmpty()) {
                acoes.add(LoveLetterAcao.JogarCarta.simples(jogador, carta));
                continue;
            }
            for (IdJogador alvo : alvos) {
                if (carta == Carta.GUARDA) {
                    for (Carta palpite : Carta.values()) {
                        if (palpite != Carta.GUARDA) {
                            acoes.add(LoveLetterAcao.JogarCarta.adivinhando(jogador, alvo, palpite));
                        }
                    }
                } else {
                    acoes.add(LoveLetterAcao.JogarCarta.mirando(jogador, carta, alvo));
                }
            }
        }
        return List.copyOf(acoes);
    }

    @Override
    public ResultadoDeValidacao validar(EstadoDeJogo estado, Acao acao) {
        LoveLetterEstado atual = (LoveLetterEstado) estado;
        if (!(acao instanceof LoveLetterAcao carta)) {
            return ResultadoDeValidacao.recusada("acao nao pertence ao Love Letter");
        }
        if (atual.encerrada()) {
            return ResultadoDeValidacao.recusada("rodada encerrada");
        }
        if (!atual.ordem().eVezDe(carta.autor())) {
            return ResultadoDeValidacao.recusada("nao e a vez de " + carta.autor());
        }
        return switch (carta) {
            case LoveLetterAcao.ComprarCarta ignorado -> atual.fase() == Fase.COMPRAR
                    ? ResultadoDeValidacao.aceita()
                    : ResultadoDeValidacao.recusada("voce ja comprou nesta vez");
            case LoveLetterAcao.JogarCarta jogada -> validarJogada(atual, jogada);
        };
    }

    private static ResultadoDeValidacao validarJogada(
            LoveLetterEstado atual, LoveLetterAcao.JogarCarta jogada) {
        if (atual.fase() != Fase.JOGAR) {
            return ResultadoDeValidacao.recusada("compre uma carta antes de jogar");
        }
        if (!atual.mao(jogada.autor()).contains(jogada.carta())) {
            return ResultadoDeValidacao.recusada("voce nao tem " + jogada.carta());
        }
        if (!jogaveis(atual, jogada.autor()).contains(jogada.carta())) {
            return ResultadoDeValidacao.recusada(
                    "com Rei ou Principe na mao, a Condessa e obrigatoria");
        }

        List<IdJogador> alvos = alvosPossiveis(atual, jogada.autor(), jogada.carta());
        boolean precisaDeAlvo = jogada.carta().exigeAlvo() && !alvos.isEmpty();
        if (precisaDeAlvo && jogada.alvo().isEmpty()) {
            return ResultadoDeValidacao.recusada(jogada.carta() + " exige um alvo");
        }
        if (jogada.alvo().isPresent()) {
            if (!jogada.carta().exigeAlvo()) {
                return ResultadoDeValidacao.recusada(jogada.carta() + " nao tem alvo");
            }
            if (!alvos.contains(jogada.alvo().orElseThrow())) {
                return ResultadoDeValidacao.recusada(
                        "alvo invalido: eliminado, protegido ou voce mesmo");
            }
        }

        if (jogada.carta() == Carta.GUARDA && !alvos.isEmpty()) {
            if (jogada.palpite().isEmpty()) {
                return ResultadoDeValidacao.recusada("a Guarda exige um palpite");
            }
            if (jogada.palpite().orElseThrow() == Carta.GUARDA) {
                return ResultadoDeValidacao.recusada("nao se pode palpitar Guarda");
            }
        } else if (jogada.palpite().isPresent()) {
            return ResultadoDeValidacao.recusada("so a Guarda tem palpite");
        }
        return ResultadoDeValidacao.aceita();
    }

    @Override
    public Transicao aplicar(EstadoDeJogo estado, Acao acao, FonteDeAleatoriedade aleatoriedade) {
        LoveLetterEstado atual = (LoveLetterEstado) estado;
        return switch ((LoveLetterAcao) acao) {
            case LoveLetterAcao.ComprarCarta comprar -> comprar(atual, comprar.autor());
            case LoveLetterAcao.JogarCarta jogada -> jogar(atual, jogada);
        };
    }

    private static Transicao comprar(LoveLetterEstado atual, IdJogador jogador) {
        Carta comprada = atual.baralho().getFirst();
        List<Carta> mao = new ArrayList<>(atual.mao(jogador));
        mao.add(comprada);

        // A proteção da Aia dura até o começo da própria vez, e é aqui que a
        // vez começa de verdade.
        LoveLetterEstado depois = atual
                .semProtecao(jogador)
                .comBaralho(atual.baralho().subList(1, atual.baralho().size()))
                .comMao(jogador, mao)
                .comFase(Fase.JOGAR);

        return Transicao.de(depois,
                new LoveLetterEvento.CartaComprada(jogador, Optional.of(comprada)));
    }

    private static Transicao jogar(LoveLetterEstado atual, LoveLetterAcao.JogarCarta jogada) {
        IdJogador autor = jogada.autor();
        List<Evento> eventos = new ArrayList<>();
        eventos.add(new LoveLetterEvento.CartaJogada(
                autor, jogada.carta(), jogada.alvo(), jogada.palpite()));

        List<Carta> mao = new ArrayList<>(atual.mao(autor));
        mao.remove(jogada.carta());
        LoveLetterEstado depois = atual.comMao(autor, mao).comDescarte(autor, jogada.carta());
        depois = resolverEfeito(depois, jogada, eventos);

        return encerrarVez(depois, autor, eventos);
    }

    private static LoveLetterEstado resolverEfeito(
            LoveLetterEstado estado, LoveLetterAcao.JogarCarta jogada, List<Evento> eventos) {
        IdJogador autor = jogada.autor();
        Optional<IdJogador> alvo = jogada.alvo();

        return switch (jogada.carta()) {
            case GUARDA -> alvo
                    .map(vitima -> jogada.palpite().orElseThrow() == estado.cartaUnica(vitima)
                            ? eliminar(estado, vitima, "palpite certeiro da Guarda", eventos)
                            : estado)
                    .orElse(estado);

            case PADRE -> {
                alvo.ifPresent(vitima -> eventos.add(new LoveLetterEvento.MaoEspiada(
                        autor, vitima, Optional.of(estado.cartaUnica(vitima)))));
                yield estado;
            }

            case BARAO -> alvo.map(rival -> compararMaos(estado, autor, rival, eventos))
                    .orElse(estado);

            case AIA -> {
                eventos.add(new LoveLetterEvento.JogadorProtegido(autor));
                yield estado.comProtegido(autor);
            }

            case PRINCIPE -> alvo.map(vitima -> forcarDescarte(estado, vitima, eventos))
                    .orElse(estado);

            case REI -> alvo.map(rival -> trocarMaos(estado, autor, rival, eventos))
                    .orElse(estado);

            case CONDESSA -> estado;

            case PRINCESA -> eliminar(estado, autor, "descartou a Princesa", eventos);
        };
    }

    private static LoveLetterEstado compararMaos(
            LoveLetterEstado estado, IdJogador autor, IdJogador rival, List<Evento> eventos) {
        Carta doAutor = estado.cartaUnica(autor);
        Carta doRival = estado.cartaUnica(rival);
        eventos.add(new LoveLetterEvento.MaosComparadas(
                autor, rival, Optional.of(doAutor), Optional.of(doRival)));

        if (doAutor.valor() > doRival.valor()) {
            return eliminar(estado, rival, "perdeu a comparacao do Barao", eventos);
        }
        if (doRival.valor() > doAutor.valor()) {
            return eliminar(estado, autor, "perdeu a comparacao do Barao", eventos);
        }
        return estado;
    }

    private static LoveLetterEstado trocarMaos(
            LoveLetterEstado estado, IdJogador autor, IdJogador rival, List<Evento> eventos) {
        List<Carta> doAutor = estado.mao(autor);
        List<Carta> doRival = estado.mao(rival);
        eventos.add(new LoveLetterEvento.MaosTrocadas(autor, rival));
        return estado.comMao(autor, doRival).comMao(rival, doAutor);
    }

    /**
     * Efeito do Príncipe: o alvo descarta e compra outra.
     *
     * <p>Descartar a Princesa por ordem do Príncipe elimina do mesmo jeito —
     * é o que torna o Príncipe uma arma e não só um incômodo.
     *
     * <p>Com o baralho vazio, o alvo recebe a carta que fora retirada no
     * começo da rodada, conforme a regra oficial. Como o baralho vazio encerra
     * a rodada logo em seguida, essa carta não precisa ser marcada como
     * consumida: ninguém mais a compra.
     */
    private static LoveLetterEstado forcarDescarte(
            LoveLetterEstado estado, IdJogador vitima, List<Evento> eventos) {
        Carta descartada = estado.cartaUnica(vitima);
        LoveLetterEstado depois = estado.comMao(vitima, List.of()).comDescarte(vitima, descartada);

        if (descartada == Carta.PRINCESA) {
            return eliminarSemRevelar(depois, vitima, "descartou a Princesa pelo Principe", eventos);
        }
        if (depois.baralho().isEmpty()) {
            return depois.comMao(vitima, List.of(depois.removida()));
        }
        Carta nova = depois.baralho().getFirst();
        eventos.add(new LoveLetterEvento.CartaComprada(vitima, Optional.of(nova)));
        return depois
                .comBaralho(depois.baralho().subList(1, depois.baralho().size()))
                .comMao(vitima, List.of(nova));
    }

    /**
     * Elimina quem ainda segura a carta que vai ser revelada.
     *
     * <p>Aqui o jogador <strong>sempre</strong> tem exatamente uma carta: a
     * Guarda e o Barão agem sobre quem não jogou, e quem joga a Princesa fica
     * com a outra carta na mão. O único caso de mão vazia é o descarte forçado
     * pelo Príncipe, e esse segue por {@link #eliminarSemRevelar}.
     *
     * <p>A versão anterior tinha um ramo defensivo para mão vazia. Ele era
     * inalcançável, e um ramo inalcançável não é segurança: é código que
     * ninguém nunca vai testar nem manter. Se a invariante quebrar,
     * {@code cartaUnica} falha alto — que é o comportamento certo para
     * defeito de programação.
     */
    private static LoveLetterEstado eliminar(
            LoveLetterEstado estado, IdJogador jogador, String motivo, List<Evento> eventos) {
        Carta revelada = estado.cartaUnica(jogador);
        eventos.add(new LoveLetterEvento.JogadorEliminado(jogador, revelada, motivo));
        return estado.comMao(jogador, List.of())
                .comDescarte(jogador, revelada)
                .comOrdem(estado.ordem().semJogador(jogador));
    }

    /** O descarte já foi feito; só falta tirar o jogador da mesa. */
    private static LoveLetterEstado eliminarSemRevelar(
            LoveLetterEstado estado, IdJogador jogador, String motivo, List<Evento> eventos) {
        eventos.add(new LoveLetterEvento.JogadorEliminado(jogador, Carta.PRINCESA, motivo));
        return estado.comOrdem(estado.ordem().semJogador(jogador));
    }

    /**
     * Fecha o turno: uma rodada acaba quando sobra um jogador ou quando o
     * baralho seca.
     */
    private static Transicao encerrarVez(
            LoveLetterEstado estado, IdJogador autor, List<Evento> eventos) {
        List<IdJogador> ativos = estado.ordem().ativos();
        if (ativos.size() <= 1) {
            eventos.add(new LoveLetterEvento.RodadaEncerrada(ativos, "ultimo de pe"));
            return new Transicao(estado.encerradaCom(ativos), eventos);
        }
        if (estado.baralho().isEmpty()) {
            List<IdJogador> campeoes = maioresCartas(estado, ativos);
            eventos.add(new LoveLetterEvento.RodadaEncerrada(campeoes, "baralho vazio"));
            return new Transicao(estado.encerradaCom(campeoes), eventos);
        }
        OrdemDeTurno proxima = estado.ordem().proxima();
        eventos.add(new LoveLetterEvento.VezPassada(autor, proxima.daVez()));
        return new Transicao(estado.comOrdem(proxima).comFase(Fase.COMPRAR), eventos);
    }

    private static List<IdJogador> maioresCartas(LoveLetterEstado estado, List<IdJogador> ativos) {
        int maior = ativos.stream()
                .mapToInt(jogador -> estado.cartaUnica(jogador).valor())
                .max()
                .orElseThrow();
        return ativos.stream()
                .filter(jogador -> estado.cartaUnica(jogador).valor() == maior)
                .sorted(Comparator.comparing(IdJogador::valor))
                .toList();
    }

    @Override
    public Visao visaoDe(EstadoDeJogo estado, IdJogador jogador) {
        LoveLetterEstado atual = (LoveLetterEstado) estado;
        return new LoveLetterVisao(
                atual.assentos(),
                atual.mao(jogador),
                atual.tamanhoDasMaos(),
                atual.descartes(),
                atual.protegidos(),
                atual.eliminados(),
                atual.baralho().size(),
                atual.jogadorDaVez(),
                atual.fase(),
                atual.vencedores(),
                atual.encerrada());
    }

    @Override
    public Resultado resultado(EstadoDeJogo estado) {
        LoveLetterEstado atual = (LoveLetterEstado) estado;
        if (!atual.encerrada()) {
            return Resultado.emAndamento();
        }
        return new Resultado.Encerrado(atual.vencedores());
    }

    /**
     * Redige o que o destinatário não pode saber.
     *
     * <p>Repare que nenhum evento é escondido por completo: a mesa vê que
     * alguém comprou, espiou ou comparou. Só o conteúdo some. Esconder o
     * evento inteiro daria a informação oposta pelo silêncio.
     */
    @Override
    public Optional<Evento> eventoVisivelPara(
            EstadoDeJogo estado, Evento evento, IdJogador jogador) {
        if (!(evento instanceof LoveLetterEvento)) {
            return Optional.of(evento);
        }
        return Optional.of(switch ((LoveLetterEvento) evento) {
            case LoveLetterEvento.CartaComprada comprada -> comprada.jogador().equals(jogador)
                    ? comprada
                    : new LoveLetterEvento.CartaComprada(comprada.jogador(), Optional.empty());

            case LoveLetterEvento.MaoEspiada espiada -> espiada.espiao().equals(jogador)
                    ? espiada
                    : new LoveLetterEvento.MaoEspiada(
                            espiada.espiao(), espiada.alvo(), Optional.empty());

            case LoveLetterEvento.MaosComparadas comparadas ->
                    comparadas.autor().equals(jogador) || comparadas.alvo().equals(jogador)
                            ? comparadas
                            : new LoveLetterEvento.MaosComparadas(
                                    comparadas.autor(), comparadas.alvo(),
                                    Optional.empty(), Optional.empty());

            case LoveLetterEvento.CartaJogada publico -> publico;
            case LoveLetterEvento.MaosTrocadas publico -> publico;
            case LoveLetterEvento.JogadorProtegido publico -> publico;
            case LoveLetterEvento.JogadorEliminado publico -> publico;
            case LoveLetterEvento.VezPassada publico -> publico;
            case LoveLetterEvento.RodadaEncerrada publico -> publico;
        });
    }

    /** Cartas que o jogador pode jogar agora, já aplicada a obrigação da Condessa. */
    static List<Carta> jogaveis(LoveLetterEstado estado, IdJogador jogador) {
        List<Carta> mao = estado.mao(jogador);
        boolean temCondessa = mao.contains(Carta.CONDESSA);
        boolean temRealeza = mao.contains(Carta.REI) || mao.contains(Carta.PRINCIPE);
        if (temCondessa && temRealeza) {
            return List.of(Carta.CONDESSA);
        }
        return mao.stream().distinct().toList();
    }

    /** Quem pode ser alvo: ativo, desprotegido, e nunca o próprio autor — salvo o Príncipe. */
    static List<IdJogador> alvosPossiveis(
            LoveLetterEstado estado, IdJogador autor, Carta carta) {
        if (!carta.exigeAlvo()) {
            return List.of();
        }
        List<IdJogador> alvos = new ArrayList<>();
        for (IdJogador candidato : estado.ordem().ativos()) {
            boolean ehOAutor = candidato.equals(autor);
            if (ehOAutor && !carta.podeMirarEmSi()) {
                continue;
            }
            if (!ehOAutor && estado.protegido(candidato)) {
                continue;
            }
            alvos.add(candidato);
        }
        return List.copyOf(alvos);
    }
}
