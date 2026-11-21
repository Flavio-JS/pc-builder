package pcbuilder.cli.etapas;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

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
import pcbuilder.servico.resumos.ResumoPlacaMae;

/**
 * Etapa 2 da montagem guiada: escolha da placa-mae (HU-005).
 *
 * <p>A lista vem do filtro de compatibilidade (HU-003): com uma CPU AM5 so aparecem
 * placas AM5, e as demais ficam em {@code [M] Ver ocultos} com o motivo
 * ({@code soquete LGA1700 x processador AM5}). Os itens compativeis que geram limitacao
 * (VRM apertado - L02, BIOS que pode precisar de atualizacao - C09) aparecem com
 * {@code [!]} e continuam selecionaveis (HU-004); ao escolher, o bloco de aviso e
 * exibido e o programa espera {@code [Enter] para continuar}.</p>
 *
 * <p>Comandos desta etapa:</p>
 * <ul>
 *   <li>numero da lista - escolhe a placa e guarda na montagem;</li>
 *   <li>{@code [0] Voltar} - retorna a etapa 1 preservando a CPU; trocar a CPU faz o
 *       filtro desta etapa ser recalculado;</li>
 *   <li>{@code [M] Ver ocultos} - lista os itens de outro soquete com o motivo;</li>
 *   <li>{@code [L] Avisos} - rele os avisos de limitacao acumulados.</li>
 * </ul>
 */
public final class EtapaPlacaMae {

    /** Comando que volta para a etapa 1. */
    public static final String COMANDO_VOLTAR = "0";
    /** Rodape de comandos da etapa. */
    public static final String RODAPE = "[0] Voltar";
    /** Mensagem de entrada que nao e um numero da lista. */
    public static final String MENSAGEM_OPCAO_INVALIDA = "Digite o numero de uma das opcoes.";

    private static final CategoriaComponente CATEGORIA = CategoriaComponente.PLACA_MAE;

    private final Terminal terminal;
    private final MontagemGuiada fluxo;
    private final FiltroEtapa filtro;
    private final AvaliadorLimitacoes avaliador;
    private final TelaEtapa tela;

    /**
     * @param terminal entrada e saida do programa
     * @param fluxo montagem em andamento (a CPU ja esta nela)
     * @param filtro filtro compartilhado pela aplicacao (com {@code --explicar-filtro})
     */
    public EtapaPlacaMae(Terminal terminal, MontagemGuiada fluxo, FiltroEtapa filtro) {
        this(terminal, fluxo, filtro, new AvaliadorLimitacoes());
    }

    /**
     * @param terminal entrada e saida do programa
     * @param fluxo montagem em andamento (a CPU ja esta nela)
     * @param filtro filtro compartilhado pela aplicacao (com {@code --explicar-filtro})
     * @param avaliador avaliador de limitacoes usado para marcar os itens com {@code [!]}
     */
    public EtapaPlacaMae(Terminal terminal, MontagemGuiada fluxo, FiltroEtapa filtro,
            AvaliadorLimitacoes avaliador) {
        this.terminal = Objects.requireNonNull(terminal, "terminal");
        this.fluxo = Objects.requireNonNull(fluxo, "fluxo");
        this.filtro = Objects.requireNonNull(filtro, "filtro");
        this.avaliador = Objects.requireNonNull(avaliador, "avaliador");
        this.tela = new TelaEtapa(terminal);
    }

    /**
     * Exibe a etapa e espera a escolha do usuario ate concluir ou pedir para voltar.
     *
     * @return {@link ResultadoEtapa#CONCLUIDA}, {@link ResultadoEtapa#VOLTAR} ou
     *         {@link ResultadoEtapa#CANCELADA} (entrada encerrada)
     */
    public ResultadoEtapa executar() {
        tela.imprimirCabecalho(fluxo.etapaAtual());
        tela.imprimirBarraProgresso(fluxo.etapaAtual());

        ListaFiltrada lista = filtro.filtrar(CATEGORIA, fluxo.getMontagem());
        if (lista.estaVazia()) {
            return executarSemCompativeis(lista);
        }
        Map<Componente, List<Limitacao>> avisos = avaliador.avaliarLista(
                fluxo.getMontagem(), lista.getCompativeis());

        while (true) {
            tela.imprimirListaFiltrada(lista, ResumoPlacaMae::linhaDeApoio,
                    componente -> avisos.getOrDefault(componente, List.of()));
            tela.imprimirRodape(RODAPE);

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
            selecionar(escolhido.get(), avisos.getOrDefault(escolhido.get(), List.of()));
            return ResultadoEtapa.CONCLUIDA;
        }
    }

    /**
     * Caminho sem nenhum item compativel: explica o motivo predominante e aceita apenas
     * {@code [0] Voltar} (ou {@code [M]}, para o usuario entender o que esta travando).
     */
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

    /**
     * Converte o texto digitado em uma placa da lista visivel.
     *
     * @return a placa escolhida, ou vazio quando o texto nao e um numero valido
     */
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
     * Guarda a placa na montagem, acumula os avisos dela e imprime a confirmacao.
     *
     * <p>Os avisos da placa substituida sao descartados antes, para que trocar de placa
     * nao deixe aviso antigo valendo.</p>
     */
    private void selecionar(Componente componente, List<Limitacao> avisos) {
        Montagem montagem = fluxo.getMontagem();
        montagem.limparLimitacoesDe(CATEGORIA);
        montagem.registrarLimitacoes(avisos);
        tela.imprimirAvisoSelecao(avisos);
        montagem.adicionar(componente);
        terminal.linha("Placa-mae selecionada: " + TabelaComponentes.nomeExibicao(componente)
                + " - " + FormatadorMoeda.reais(componente.getPrecoMedioBRL()));
    }
}
