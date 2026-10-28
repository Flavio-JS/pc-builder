package pcbuilder.cli.etapas;

import java.util.List;
import java.util.Optional;

import pcbuilder.cli.TabelaComponentes;
import pcbuilder.cli.TelaEtapa;
import pcbuilder.cli.Terminal;
import pcbuilder.modelo.Componente;
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
 *   <li>{@code [0] Cancelar montagem} - pede confirmacao {@code S/N} e, se confirmado,
 *       descarta a montagem e volta ao menu principal.</li>
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

    /**
     * @param terminal entrada e saida do programa
     * @param fluxo montagem em andamento (a CPU escolhida e guardada nela)
     * @param processadores todos os processadores do catalogo, na ordem da base
     */
    public EtapaProcessador(Terminal terminal, MontagemGuiada fluxo,
            List<Componente> processadores) {
        this.terminal = terminal;
        this.fluxo = fluxo;
        this.processadores = processadores == null ? List.of() : List.copyOf(processadores);
        this.tela = new TelaEtapa(terminal);
    }

    /**
     * Exibe a etapa e espera a escolha do usuario ate concluir, cancelar ou descobrir
     * que a categoria esta vazia.
     *
     * @return o que aconteceu, para o menu principal decidir o proximo passo
     */
    public ResultadoEtapa executar() {
        tela.imprimirCabecalho(fluxo.etapaAtual());
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
                if (confirmarCancelamento()) {
                    return ResultadoEtapa.CANCELADA;
                }
                continue;
            }

            Optional<Componente> escolhido = escolher(comando);
            if (escolhido.isEmpty()) {
                terminal.linha(MENSAGEM_OPCAO_INVALIDA);
                continue;
            }

            selecionar(escolhido.get());
            return ResultadoEtapa.CONCLUIDA;
        }
    }

    private void listar() {
        tela.imprimirSeparador();
        terminal.linha(TabelaComponentes.cabecalho());
        tela.imprimirSeparador();
        for (int indice = 0; indice < processadores.size(); indice++) {
            Componente componente = processadores.get(indice);
            terminal.linha(TabelaComponentes.linha(
                    indice + 1,
                    TabelaComponentes.nomeExibicao(componente),
                    FormatadorMoeda.reais(componente.getPrecoMedioBRL()),
                    linhaDeApoio(componente)));
        }
        int quantidade = processadores.size();
        terminal.linha(quantidade + (quantidade == 1
                ? " processador disponivel."
                : " processadores disponiveis."));
        tela.imprimirRodape(RODAPE);
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
        fluxo.getMontagem().adicionar(componente);
        terminal.linha("Processador selecionado: " + TabelaComponentes.nomeExibicao(componente)
                + " - " + FormatadorMoeda.reais(componente.getPrecoMedioBRL()));
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
