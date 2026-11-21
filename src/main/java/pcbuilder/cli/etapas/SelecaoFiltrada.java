package pcbuilder.cli.etapas;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import pcbuilder.analise.filtro.FiltroEtapa;
import pcbuilder.analise.filtro.ListaFiltrada;
import pcbuilder.analise.limitacao.AvaliadorLimitacoes;
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

    /**
     * @param terminal entrada e saida do programa
     * @param filtro filtro compartilhado pela aplicacao (com {@code --explicar-filtro})
     * @param avaliador avaliador de limitacoes usado para marcar os itens com {@code [!]}
     */
    public SelecaoFiltrada(Terminal terminal, FiltroEtapa filtro,
            AvaliadorLimitacoes avaliador) {
        this.terminal = Objects.requireNonNull(terminal, "terminal");
        this.filtro = Objects.requireNonNull(filtro, "filtro");
        this.avaliador = Objects.requireNonNull(avaliador, "avaliador");
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

        tela.imprimirCabecalho(fluxo.etapaAtual());
        tela.imprimirBarraProgresso(fluxo.etapaAtual());

        ListaFiltrada lista = filtro.filtrar(categoria, fluxo.getMontagem());
        if (lista.estaVazia()) {
            return executarSemCompativeis(lista);
        }
        Map<Componente, List<Limitacao>> avisos = avaliador.avaliarLista(
                fluxo.getMontagem(), lista.getCompativeis());

        while (true) {
            tela.imprimirListaFiltrada(lista, linhaDeApoio,
                    componente -> avisos.getOrDefault(componente, List.of()));
            tela.imprimirRodape(RODAPE_VOLTAR);

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

            Optional<Componente> escolhido = escolher(lista, comando);
            if (escolhido.isEmpty()) {
                terminal.linha(MENSAGEM_OPCAO_INVALIDA);
                continue;
            }
            selecionar(fluxo.getMontagem(), categoria, escolhido.get(),
                    avisos.getOrDefault(escolhido.get(), List.of()), mensagemSelecionada);
            return ResultadoEtapa.CONCLUIDA;
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
        tela.imprimirSemCompativeis(lista);
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
        tela.imprimirAvisoSelecao(avisos);
        montagem.adicionar(componente);
        terminal.linha(mensagemSelecionada.apply(componente));
    }
}
