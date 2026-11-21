package pcbuilder.cli.etapas;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import pcbuilder.analise.limitacao.AvaliadorLimitacoes;
import pcbuilder.analise.limitacao.TextosLimitacao;
import pcbuilder.cli.TabelaComponentes;
import pcbuilder.cli.TelaEtapa;
import pcbuilder.cli.Terminal;
import pcbuilder.enums.CategoriaComponente;
import pcbuilder.modelo.Componente;
import pcbuilder.modelo.Limitacao;
import pcbuilder.modelo.Montagem;
import pcbuilder.modelo.Processador;
import pcbuilder.relatorio.FormatadorMoeda;
import pcbuilder.servico.MontagemGuiada;

/**
 * Etapa 1 da montagem guiada: escolha do processador (HU-002).
 *
 * <p>E a unica etapa sem filtro: nao existe peca anterior para limitar a lista, entao
 * todos os processadores do catalogo aparecem. A escolha e feita <strong>somente pelo
 * numero</strong> da opcao - o usuario leigo nunca digita especificacao tecnica - e
 * nada e escolhido automaticamente.</p>
 *
 * <p>Comandos desta etapa:</p>
 * <ul>
 *   <li>numero da lista - escolhe o processador e guarda na montagem;</li>
 *   <li>{@code [L] Avisos} - rele os avisos de limitacao ja acumulados (HU-004);</li>
 *   <li>{@code [0] Cancelar montagem} - pede confirmacao {@code S/N} e, se confirmado,
 *       descarta a montagem e volta ao menu principal. Ao ser reaberta pelo resumo
 *       (HU-011), a etapa mostra {@code [0] Voltar} e nao descarta nada.</li>
 * </ul>
 */
public final class EtapaProcessador {

    /** Comando de cancelamento da montagem. */
    public static final String COMANDO_CANCELAR = "0";
    /** Rodape de comandos da etapa. */
    public static final String RODAPE = "[0] Cancelar montagem";
    /** Mensagem de entrada que nao e um numero da lista. */
    public static final String MENSAGEM_OPCAO_INVALIDA = "Digite o numero de uma das opcoes.";
    /** Mensagem de categoria vazia no catalogo. */
    public static final String MENSAGEM_SEM_COMPONENTES = "Nenhum processador cadastrado.";

    private final Terminal terminal;
    private final MontagemGuiada fluxo;
    private final List<Componente> processadores;
    private final TelaEtapa tela;
    private final AvaliadorLimitacoes avaliador;
    private final boolean aPartirDoResumo;

    /**
     * @param terminal entrada e saida do programa
     * @param fluxo montagem em andamento (a CPU escolhida e guardada nela)
     * @param processadores todos os processadores do catalogo, na ordem da base
     */
    public EtapaProcessador(Terminal terminal, MontagemGuiada fluxo,
            List<Componente> processadores) {
        this(terminal, fluxo, processadores, new AvaliadorLimitacoes());
    }

    /**
     * @param terminal entrada e saida do programa
     * @param fluxo montagem em andamento (a CPU escolhida e guardada nela)
     * @param processadores todos os processadores do catalogo, na ordem da base
     * @param avaliador avaliador de limitacoes usado ao selecionar a peca
     */
    public EtapaProcessador(Terminal terminal, MontagemGuiada fluxo,
            List<Componente> processadores, AvaliadorLimitacoes avaliador) {
        this(terminal, fluxo, processadores, avaliador, false);
    }

    /**
     * @param terminal entrada e saida do programa
     * @param fluxo montagem em andamento (a CPU escolhida e guardada nela)
     * @param processadores todos os processadores do catalogo, na ordem da base
     * @param avaliador avaliador de limitacoes usado ao selecionar a peca
     * @param aPartirDoResumo {@code true} quando a etapa foi reaberta pela HU-011
     */
    public EtapaProcessador(Terminal terminal, MontagemGuiada fluxo,
            List<Componente> processadores, AvaliadorLimitacoes avaliador,
            boolean aPartirDoResumo) {
        this.terminal = terminal;
        this.fluxo = fluxo;
        List<Componente> ordenados = new ArrayList<>(
                processadores == null ? List.of() : processadores);
        ordenados.sort(Componente.POR_PRECO);
        this.processadores = List.copyOf(ordenados);
        this.tela = new TelaEtapa(terminal);
        this.avaliador = Objects.requireNonNull(avaliador, "avaliador");
        this.aPartirDoResumo = aPartirDoResumo;
    }

    /**
     * Exibe a etapa e espera a escolha do usuario ate concluir, cancelar ou descobrir
     * que a categoria esta vazia.
     *
     * @return o que aconteceu, para o menu principal decidir o proximo passo
     */
    public ResultadoEtapa executar() {
        terminal.limparTela();
        tela.imprimirPecasEscolhidas(fluxo.getMontagem(), fluxo.etapaAtual());
        tela.imprimirCabecalho(fluxo.etapaAtual(), fluxo.getMontagem());
        tela.imprimirBarraProgresso(fluxo.etapaAtual());

        if (processadores.isEmpty()) {
            terminal.linha(MENSAGEM_SEM_COMPONENTES);
            return ResultadoEtapa.SEM_COMPONENTES;
        }

        while (true) {
            listar();

            Optional<String> entrada = terminal.perguntar(">");
            if (entrada.isEmpty()) {
                // Entrada encerrada (EOF): nao ha como continuar a montagem.
                return ResultadoEtapa.CANCELADA;
            }

            String comando = entrada.get();
            if (COMANDO_CANCELAR.equals(comando)) {
                if (aPartirDoResumo) {
                    return ResultadoEtapa.VOLTAR;
                }
                if (confirmarCancelamento()) {
                    return ResultadoEtapa.CANCELADA;
                }
                continue;
            }
            if (TelaEtapa.COMANDO_AVISOS.equalsIgnoreCase(comando)) {
                tela.imprimirAvisosDaMontagem(fluxo.getMontagem());
                continue;
            }

            Optional<Componente> escolhido = escolher(comando);
            if (escolhido.isEmpty()) {
                terminal.linha(MENSAGEM_OPCAO_INVALIDA);
                continue;
            }

            Componente novo = escolhido.get();
            Optional<Componente> atual = fluxo.getMontagem().obter(CategoriaComponente.CPU);
            if (atual.isPresent() && !Objects.equals(atual.get(), novo)
                    && !terminal.confirmar("Trocar "
                            + TabelaComponentes.nomeExibicao(atual.get()) + " por "
                            + TabelaComponentes.nomeExibicao(novo) + "?")) {
                continue;
            }

            selecionar(novo);
            return ResultadoEtapa.CONCLUIDA;
        }
    }

    private void listar() {
        tela.imprimirSeparador();
        terminal.linha(TabelaComponentes.cabecalho());
        tela.imprimirSeparador();
        List<String> apoios = new ArrayList<>();
        for (Componente componente : processadores) {
            apoios.add(linhaDeApoio(componente));
        }
        apoios = TabelaComponentes.alinharApoios(apoios);
        for (int indice = 0; indice < processadores.size(); indice++) {
            Componente componente = processadores.get(indice);
            terminal.linha(TabelaComponentes.linha(
                    indice + 1,
                    TabelaComponentes.nomeExibicao(componente),
                    FormatadorMoeda.reais(componente.getPrecoMedioBRL()),
                    apoios.get(indice)));
        }
        int quantidade = processadores.size();
        terminal.linha(quantidade + (quantidade == 1
                ? " processador disponivel."
                : " processadores disponiveis."));
        tela.imprimirContexto(fluxo.getMontagem(), fluxo.etapaAtual());
        tela.imprimirRodape(aPartirDoResumo ? SelecaoFiltrada.RODAPE_VOLTAR : RODAPE);
    }

    /**
     * Linha de apoio do processador: nucleos, soquete e se existe video integrado
     * (o que decide a obrigatoriedade da placa de video na etapa 4).
     */
    private static String linhaDeApoio(Componente componente) {
        if (!(componente instanceof Processador cpu)) {
            return "";
        }
        return cpu.getNucleosFisicos() + " nucleos | " + cpu.getSoquete()
                + " | video integrado: " + (cpu.isPossuiVideoIntegrado() ? "sim" : "nao");
    }

    /**
     * Converte o texto digitado em um componente da lista.
     *
     * @return o componente escolhido, ou vazio quando o texto nao e um numero valido
     */
    private Optional<Componente> escolher(String comando) {
        int numero;
        try {
            numero = Integer.parseInt(comando);
        } catch (NumberFormatException causa) {
            return Optional.empty();
        }
        if (numero < 1 || numero > processadores.size()) {
            return Optional.empty();
        }
        return Optional.of(processadores.get(numero - 1));
    }

    private void selecionar(Componente componente) {
        Montagem montagem = fluxo.getMontagem();
        List<Limitacao> avisos = avaliador.avaliar(montagem, componente);
        montagem.limparLimitacoesDe(componente.getCategoria());
        montagem.registrarLimitacoes(avisos);
        tela.imprimirAvisoSelecao(avisos);
        montagem.adicionar(componente);
        terminal.linha("Processador selecionado: " + TabelaComponentes.nomeExibicao(componente)
                + " - " + FormatadorMoeda.reais(componente.getPrecoMedioBRL()));
        if (terminal.isDetalharLimitacoes()) {
            for (String pendencia : avaliador.pendencias(montagem, componente)) {
                terminal.linha("    " + TextosLimitacao.SEM_DADOS + ": " + pendencia);
            }
        }
    }

    /**
     * Pede a confirmacao {@code S/N} do cancelamento e descarta a montagem quando o
     * usuario confirma.
     *
     * @return {@code true} quando a montagem foi cancelada
     */
    private boolean confirmarCancelamento() {
        if (!terminal.confirmar("Cancelar a montagem?")) {
            return false;
        }
        fluxo.getMontagem().limpar();
        terminal.linha("Montagem cancelada.");
        return true;
    }
}
