package pcbuilder.cli.etapas;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import pcbuilder.analise.filtro.FiltroEtapa;
import pcbuilder.analise.filtro.ListaFiltrada;
import pcbuilder.analise.limitacao.AvaliadorLimitacoes;
import pcbuilder.analise.limitacao.TextosLimitacao;
import pcbuilder.cli.TabelaComponentes;
import pcbuilder.cli.TelaEtapa;
import pcbuilder.cli.Terminal;
import pcbuilder.enums.CategoriaComponente;
import pcbuilder.modelo.Componente;
import pcbuilder.modelo.Limitacao;
import pcbuilder.modelo.Montagem;
import pcbuilder.relatorio.FormatadorMoeda;
import pcbuilder.servico.MontagemGuiada;

/**
 * Laco de selecao de uma etapa com lista filtrada (HU-003) e avisos de limitacao
 * (HU-004), compartilhado pelas etapas 2 a 6.
 *
 * <p>O que muda de etapa para etapa e: a categoria, a linha de apoio e o texto de
 * confirmacao. Ficam aqui, em um unico lugar, o cabecalho, a barra de progresso, os
 * comandos {@code [M] Ver ocultos}, {@code [L] Avisos} e {@code [0] Voltar}, a escolha
 * por numero, o bloco de aviso com {@code [Enter] para continuar} e o acumulo dos avisos
 * na montagem.</p>
 *
 * <p>A tela <strong>nao</strong> cria o fluxo nem decide para onde voltar: devolve
 * {@link ResultadoEtapa#VOLTAR} e quem chamou (o menu principal) usa
 * {@code MontagemGuiada.etapaAnterior()}.</p>
 */
public final class SelecaoFiltrada {

    /** Comando que retorna a etapa anterior. */
    public static final String COMANDO_VOLTAR = "0";
    /** Rodape de comandos da etapa. */
    public static final String RODAPE_VOLTAR = "[0] Voltar";
    /** Mensagem de entrada que nao e um numero da lista. */
    public static final String MENSAGEM_OPCAO_INVALIDA = "Digite o numero de uma das opcoes.";

    private final Terminal terminal;
    private final FiltroEtapa filtro;
    private final AvaliadorLimitacoes avaliador;
    private final TelaEtapa tela;
    private final Opcoes opcoes;

    /**
     * @param terminal entrada e saida do programa
     * @param filtro filtro compartilhado pela aplicacao (com {@code --explicar-filtro})
     * @param avaliador avaliador de limitacoes usado para marcar os itens com {@code [!]}
     */
    public SelecaoFiltrada(Terminal terminal, FiltroEtapa filtro,
            AvaliadorLimitacoes avaliador) {
        this(terminal, filtro, avaliador, Opcoes.padrao());
    }

    /**
     * @param terminal entrada e saida do programa
     * @param filtro filtro compartilhado pela aplicacao
     * @param avaliador avaliador de limitacoes usado para marcar os itens com {@code [!]}
     * @param opcoes textos/comandos proprios da etapa (por exemplo, {@code [0] Pular})
     */
    public SelecaoFiltrada(Terminal terminal, FiltroEtapa filtro,
            AvaliadorLimitacoes avaliador, Opcoes opcoes) {
        this.terminal = Objects.requireNonNull(terminal, "terminal");
        this.filtro = Objects.requireNonNull(filtro, "filtro");
        this.avaliador = Objects.requireNonNull(avaliador, "avaliador");
        this.opcoes = Objects.requireNonNull(opcoes, "opcoes");
        this.tela = new TelaEtapa(terminal);
    }

    /**
     * Exibe a etapa e processa a escolha ate concluir, voltar ou a entrada terminar.
     *
     * @param fluxo montagem em andamento (as pecas anteriores ja estao nela)
     * @param categoria categoria coletada nesta etapa
     * @param linhaDeApoio texto curto por componente (nucleos, chipset, watts...)
     * @param mensagemSelecionada texto impresso depois de guardar a peca
     * @return {@link ResultadoEtapa#CONCLUIDA}, {@link ResultadoEtapa#VOLTAR} ou
     *         {@link ResultadoEtapa#CANCELADA}
     */
    public ResultadoEtapa executar(MontagemGuiada fluxo, CategoriaComponente categoria,
            Function<Componente, String> linhaDeApoio,
            Function<Componente, String> mensagemSelecionada) {
        Objects.requireNonNull(fluxo, "fluxo");
        Objects.requireNonNull(categoria, "categoria");
        Objects.requireNonNull(mensagemSelecionada, "mensagemSelecionada");

        terminal.limparTela();
        tela.imprimirPecasEscolhidas(fluxo.getMontagem(), fluxo.etapaAtual());
        tela.imprimirCabecalho(fluxo.etapaAtual(), fluxo.getMontagem());
        tela.imprimirBarraProgresso(fluxo.etapaAtual());
        if (opcoes.getMensagemInicial() != null) {
            terminal.linha(opcoes.getMensagemInicial());
        }

        ListaFiltrada lista = filtro.filtrar(categoria, fluxo.getMontagem());
        if (lista.estaVazia()) {
            return executarSemCompativeis(lista);
        }
        Map<Componente, List<Limitacao>> avisos = avaliador.avaliarLista(
                fluxo.getMontagem(), lista.getCompativeis());

        while (true) {
            tela.imprimirListaFiltrada(lista, linhaDeApoio,
                    componente -> avisos.getOrDefault(componente, List.of()));
            tela.imprimirContexto(fluxo.getMontagem(), fluxo.etapaAtual());
            tela.imprimirRodape(opcoes.getRodapeZero());

            Optional<String> entrada = terminal.perguntar(">");
            if (entrada.isEmpty()) {
                return ResultadoEtapa.CANCELADA;
            }
            String comando = entrada.get();

            if (COMANDO_VOLTAR.equals(comando)) {
                return ResultadoEtapa.VOLTAR;
            }
            if (TelaEtapa.COMANDO_OCULTOS.equalsIgnoreCase(comando)) {
                tela.imprimirOcultos(lista);
                continue;
            }
            if (TelaEtapa.COMANDO_AVISOS.equalsIgnoreCase(comando)) {
                tela.imprimirAvisosDaMontagem(fluxo.getMontagem());
                continue;
            }
            ComandoExtra extra = opcoes.comandoExtra(comando);
            if (extra != null) {
                Optional<ResultadoEtapa> resultadoExtra = extra.executar();
                if (resultadoExtra.isPresent()) {
                    return resultadoExtra.get();
                }
                continue;
            }

            Optional<Componente> escolhido = escolher(lista, comando);
            if (escolhido.isEmpty()) {
                terminal.linha(MENSAGEM_OPCAO_INVALIDA);
                continue;
            }
            Optional<ResultadoEtapa> resultadoSelecao = selecionarComConfirmacao(fluxo,
                    categoria, lista, avisos, escolhido.get(), mensagemSelecionada);
            if (resultadoSelecao.isPresent()) {
                return resultadoSelecao.get();
            }
        }
    }

    /**
     * Confirma a escolha: mostra os avisos (podendo trocar por outro numero), pede
     * {@code S/N} quando a categoria ja tem peca e guarda a peca na montagem.
     *
     * @return {@link ResultadoEtapa#CONCLUIDA} quando a peca foi guardada,
     *         {@link ResultadoEtapa#CANCELADA} no fim da entrada, ou vazio quando o
     *         usuario recusou a troca e deve voltar para a lista
     */
    private Optional<ResultadoEtapa> selecionarComConfirmacao(MontagemGuiada fluxo,
            CategoriaComponente categoria, ListaFiltrada lista,
            Map<Componente, List<Limitacao>> avisos, Componente escolhido,
            Function<Componente, String> mensagemSelecionada) {
        Componente novo = escolhido;
        while (true) {
            List<Limitacao> avisosNovo = avisos.getOrDefault(novo, List.of());
            if (!avisosNovo.isEmpty()) {
                tela.imprimirAviso(avisosNovo);
                Optional<String> resposta = terminal.perguntar(
                        TelaEtapa.MENSAGEM_CONFIRMAR_OU_TROCAR);
                if (resposta.isEmpty()) {
                    return Optional.of(ResultadoEtapa.CANCELADA);
                }
                if (!resposta.get().isEmpty()) {
                    Optional<Componente> outro = escolher(lista, resposta.get());
                    if (outro.isEmpty()) {
                        terminal.linha(MENSAGEM_OPCAO_INVALIDA);
                        continue;
                    }
                    novo = outro.get();
                    continue;
                }
            }

            Optional<Componente> atual = fluxo.getMontagem().obter(categoria);
            if (atual.isPresent() && !Objects.equals(atual.get(), novo)
                    && !terminal.confirmar("Trocar "
                            + TabelaComponentes.nomeExibicao(atual.get()) + " por "
                            + TabelaComponentes.nomeExibicao(novo) + "?")) {
                return Optional.empty();
            }

            selecionar(fluxo.getMontagem(), categoria, novo, avisosNovo,
                    mensagemSelecionada);
            return Optional.of(ResultadoEtapa.CONCLUIDA);
        }
    }

    /**
     * Nome e preco formatados da peca ({@code ASUS Prime B650M-A - R$ 949,00}), para a
     * mensagem de confirmacao de cada etapa.
     *
     * @param componente peca escolhida
     * @return o texto base da mensagem de selecao
     */
    public static String descricao(Componente componente) {
        return TabelaComponentes.nomeExibicao(componente) + " - "
                + FormatadorMoeda.reais(componente.getPrecoMedioBRL());
    }

    /** Caminho sem nenhum item compativel: explica o motivo e aceita {@code [0]}/{@code [M]}. */
    private ResultadoEtapa executarSemCompativeis(ListaFiltrada lista) {
        tela.imprimirSemCompativeis(lista, opcoes.getMensagemSemCompativeis(),
                opcoes.getLinhaZeroSemCompativeis());
        while (true) {
            Optional<String> entrada = terminal.perguntar(">");
            if (entrada.isEmpty()) {
                return ResultadoEtapa.CANCELADA;
            }
            String comando = entrada.get();
            if (COMANDO_VOLTAR.equals(comando)) {
                return ResultadoEtapa.VOLTAR;
            }
            if (TelaEtapa.COMANDO_OCULTOS.equalsIgnoreCase(comando)) {
                tela.imprimirOcultos(lista);
                continue;
            }
            terminal.linha(MENSAGEM_OPCAO_INVALIDA);
        }
    }

    /** Converte o texto digitado em um item da lista visivel. */
    private static Optional<Componente> escolher(ListaFiltrada lista, String comando) {
        int numero;
        try {
            numero = Integer.parseInt(comando);
        } catch (NumberFormatException causa) {
            return Optional.empty();
        }
        if (numero < 1 || numero > lista.getCompativeis().size()) {
            return Optional.empty();
        }
        return Optional.of(lista.getCompativeis().get(numero - 1));
    }

    /**
     * Guarda a peca na montagem, acumula os avisos dela e imprime a confirmacao.
     *
     * <p>Os avisos da peca substituida sao descartados antes, para que trocar a peca nao
     * deixe aviso antigo valendo.</p>
     */
    private void selecionar(Montagem montagem, CategoriaComponente categoria,
            Componente componente, List<Limitacao> avisos,
            Function<Componente, String> mensagemSelecionada) {
        montagem.limparLimitacoesDe(categoria);
        montagem.registrarLimitacoes(avisos);
        montagem.adicionar(componente);
        terminal.linha(mensagemSelecionada.apply(componente));
        if (terminal.isDetalharLimitacoes()) {
            for (String pendencia : avaliador.pendencias(montagem, componente)) {
                terminal.linha("    " + TextosLimitacao.SEM_DADOS + ": " + pendencia);
            }
        }
    }

    /** O que um comando extra da etapa faz: imprimir algo e continuar, ou encerrar a etapa. */
    @FunctionalInterface
    public interface ComandoExtra {

        /**
         * Executa o comando.
         *
         * @return o resultado da etapa quando o comando encerra a selecao; vazio para
         *         apenas imprimir e continuar na lista
         */
        Optional<ResultadoEtapa> executar();
    }

    /**
     * Textos e comandos proprios de uma etapa.
     *
     * <p>O padrao e o das etapas obrigatorias: {@code [0] Voltar} e a mensagem generica
     * de "nenhum compativel". A etapa 4 opcional usa {@code [0] Pular} e a etapa 4
     * obrigatoria explica por que a placa de video e necessaria.</p>
     */
    public static final class Opcoes {

        private final String rodapeZero;
        private final String mensagemSemCompativeis;
        private final String linhaZeroSemCompativeis;
        private final String mensagemInicial;
        private final Map<String, ComandoExtra> comandosExtras;

        /**
         * @param rodapeZero comando {@code [0] ...} exibido no rodape da lista
         * @param mensagemSemCompativeis primeira linha quando o filtro nao deixa item
         * @param linhaZeroSemCompativeis comando exibido quando o filtro nao deixa item
         * @param mensagemInicial linha extra depois do cabecalho (pode ser {@code null})
         */
        public Opcoes(String rodapeZero, String mensagemSemCompativeis,
                String linhaZeroSemCompativeis, String mensagemInicial) {
            this(rodapeZero, mensagemSemCompativeis, linhaZeroSemCompativeis, mensagemInicial,
                    Map.of());
        }

        /**
         * @param rodapeZero comando {@code [0] ...} exibido no rodape da lista
         * @param mensagemSemCompativeis primeira linha quando o filtro nao deixa item
         * @param linhaZeroSemCompativeis comando exibido quando o filtro nao deixa item
         * @param mensagemInicial linha extra depois do cabecalho (pode ser {@code null})
         * @param comandosExtras comandos adicionais (tecla em maiuscula -&gt; acao)
         */
        public Opcoes(String rodapeZero, String mensagemSemCompativeis,
                String linhaZeroSemCompativeis, String mensagemInicial,
                Map<String, ComandoExtra> comandosExtras) {
            this.rodapeZero = Objects.requireNonNull(rodapeZero, "rodapeZero");
            this.mensagemSemCompativeis = Objects.requireNonNull(mensagemSemCompativeis,
                    "mensagemSemCompativeis");
            this.linhaZeroSemCompativeis = Objects.requireNonNull(linhaZeroSemCompativeis,
                    "linhaZeroSemCompativeis");
            this.mensagemInicial = mensagemInicial;
            this.comandosExtras = Map.copyOf(comandosExtras);
        }

        /** Opcoes das etapas obrigatorias (voltar e mensagem generica). */
        public static Opcoes padrao() {
            return new Opcoes(RODAPE_VOLTAR, TelaEtapa.MENSAGEM_SEM_COMPATIVEIS,
                    "[0] Voltar para trocar a peca anterior", null);
        }

        public String getRodapeZero() {
            return rodapeZero;
        }

        public String getMensagemSemCompativeis() {
            return mensagemSemCompativeis;
        }

        public String getLinhaZeroSemCompativeis() {
            return linhaZeroSemCompativeis;
        }

        public String getMensagemInicial() {
            return mensagemInicial;
        }

        /**
         * Acao extra para a tecla digitada, quando existir.
         *
         * @param comando texto digitado (comparado sem diferenciar maiusculas)
         * @return a acao, ou {@code null} quando a tecla nao tem comando extra
         */
        public ComandoExtra comandoExtra(String comando) {
            if (comando == null) {
                return null;
            }
            return comandosExtras.get(comando.trim().toUpperCase(Locale.ROOT));
        }
    }
}
