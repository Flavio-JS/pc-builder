package pcbuilder.cli;

import java.util.Optional;

import pcbuilder.cli.etapas.EtapaProcessador;
import pcbuilder.cli.etapas.ResultadoEtapa;
import pcbuilder.enums.CategoriaComponente;
import pcbuilder.mock.CatalogoMock;
import pcbuilder.servico.MontagemGuiada;

/**
 * Menu principal da aplicacao: unica porta de entrada do fluxo guiado (secao 2.1 das
 * historias de usuario).
 *
 * <p>Opcoes: {@code [1] Montar PC (guiado)} e {@code [0] Sair}. Cada {@code [1]} cria
 * uma {@link MontagemGuiada} nova com montagem vazia e entra na etapa 1. Quando o
 * usuario sai do fluxo (cancelou ou a base nao tem a categoria), o menu volta a
 * aparecer.</p>
 *
 * <p>As etapas 2 a 6 ainda nao existem nesta HU: ao avancar da etapa 1 o programa
 * mostra o cabecalho da proxima etapa e informa em qual HU ela entra, voltando ao
 * menu sem manter a montagem parcial.</p>
 */
public final class MenuPrincipal {

    /** Codigo de saida de uma execucao normal. */
    public static final int SAIDA_SUCESSO = 0;

    /** Opcao que inicia o fluxo guiado. */
    public static final String OPCAO_MONTAR = "1";
    /** Opcao que encerra o programa. */
    public static final String OPCAO_SAIR = "0";
    /** Menu impresso a cada volta do laco. */
    public static final String MENU = "[1] Montar PC (guiado)   [0] Sair";
    /** Mensagem de opcao fora do menu. */
    public static final String MENSAGEM_OPCAO_INVALIDA = "Digite o numero de uma das opcoes.";

    private static final String AVISO_ETAPAS_PENDENTES =
            "As etapas 2 a 6 entram nas HUs 005 a 009. Voltando ao menu principal.";

    private final CatalogoMock catalogo;
    private final Terminal terminal;

    /**
     * @param catalogo base carregada uma unica vez pela aplicacao
     * @param terminal entrada e saida do programa
     */
    public MenuPrincipal(CatalogoMock catalogo, Terminal terminal) {
        this.catalogo = catalogo;
        this.terminal = terminal;
    }

    /**
     * Mostra o menu e processa as opcoes ate o usuario sair (ou a entrada terminar).
     *
     * @return {@link #SAIDA_SUCESSO}
     */
    public int executar() {
        while (true) {
            imprimirMenu();
            Optional<String> entrada = terminal.perguntar(">");
            if (entrada.isEmpty()) {
                return SAIDA_SUCESSO;
            }

            String opcao = entrada.get();
            if (OPCAO_SAIR.equals(opcao)) {
                return SAIDA_SUCESSO;
            }
            if (OPCAO_MONTAR.equals(opcao)) {
                montarPc();
                continue;
            }
            terminal.linha(MENSAGEM_OPCAO_INVALIDA);
        }
    }

    private void imprimirMenu() {
        terminal.linhaEmBranco();
        terminal.linha(MENU);
    }

    /**
     * Executa as etapas do fluxo ate a etapa atual nao estar implementada, o usuario
     * cancelar ou a montagem terminar.
     */
    private void montarPc() {
        terminal.linhaEmBranco();
        MontagemGuiada fluxo = new MontagemGuiada();

        while (!fluxo.concluir()) {
            if (fluxo.categoriaAtual() == CategoriaComponente.CPU) {
                EtapaProcessador etapa = new EtapaProcessador(terminal, fluxo,
                        catalogo.porCategoria(CategoriaComponente.CPU));
                if (etapa.executar() != ResultadoEtapa.CONCLUIDA) {
                    return;
                }
                fluxo.proximaEtapa();
                continue;
            }
            informarEtapaPendente(fluxo);
            return;
        }

        terminal.linha("Montagem concluida.");
    }

    /**
     * Exibe o cabecalho da etapa seguinte e avisa que ela pertence a outra HU.
     *
     * <p>Serve para provar que a montagem avancou de etapa preservando a escolha
     * anterior, enquanto as etapas 2 a 6 nao estao implementadas.</p>
     */
    private void informarEtapaPendente(MontagemGuiada fluxo) {
        terminal.linhaEmBranco();
        TelaEtapa tela = new TelaEtapa(terminal);
        tela.imprimirCabecalho(fluxo.etapaAtual());
        tela.imprimirBarraProgresso(fluxo.etapaAtual());
        terminal.linha(AVISO_ETAPAS_PENDENTES);
    }
}
