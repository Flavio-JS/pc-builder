package pcbuilder.cli;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import pcbuilder.analise.filtro.FiltroEtapa;
import pcbuilder.analise.filtro.ListaFiltrada;
import pcbuilder.analise.limitacao.AvaliadorLimitacoes;
import pcbuilder.analise.limitacao.TextosLimitacao;
import pcbuilder.cli.etapas.EtapaArmazenamento;
import pcbuilder.cli.etapas.EtapaMemoria;
import pcbuilder.cli.etapas.EtapaPlacaMae;
import pcbuilder.cli.etapas.EtapaPlacaVideo;
import pcbuilder.cli.etapas.EtapaProcessador;
import pcbuilder.cli.etapas.ResultadoEtapa;
import pcbuilder.enums.CategoriaComponente;
import pcbuilder.mock.CatalogoMock;
import pcbuilder.modelo.Componente;
import pcbuilder.modelo.Limitacao;
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
 * <p>As etapas 1 a 5 (processador, placa-mae, memoria, placa de video e armazenamento)
 * ja existem. Ao avancar da etapa 5 o programa aplica o filtro de compatibilidade
 * (HU-003) e mostra quantas fontes encaixam na escolha feita, voltando ao menu sem manter
 * a montagem parcial. A tela completa da etapa 6 entra na HU-009.</p>
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
            "A etapa 6 (fonte) entra na HU-009. Voltando ao menu principal.";
    private static final String PREFIXO_PREVIA_FILTRO = "Filtro da proxima etapa: ";

    private final CatalogoMock catalogo;
    private final Terminal terminal;
    private final FiltroEtapa filtro;
    private final AvaliadorLimitacoes avaliador = new AvaliadorLimitacoes();
    private final boolean detalharLimitacoes;

    /**
     * @param catalogo base carregada uma unica vez pela aplicacao
     * @param terminal entrada e saida do programa
     */
    public MenuPrincipal(CatalogoMock catalogo, Terminal terminal) {
        this(catalogo, terminal, false, false);
    }

    /**
     * @param catalogo base carregada uma unica vez pela aplicacao
     * @param terminal entrada e saida do programa
     * @param explicarFiltro quando {@code true}, cada item oculto tambem vai para o
     *        {@code stderr} ({@code --explicar-filtro})
     */
    public MenuPrincipal(CatalogoMock catalogo, Terminal terminal, boolean explicarFiltro) {
        this(catalogo, terminal, explicarFiltro, false);
    }

    /**
     * @param catalogo base carregada uma unica vez pela aplicacao
     * @param terminal entrada e saida do programa
     * @param explicarFiltro quando {@code true}, cada item oculto tambem vai para o
     *        {@code stderr} ({@code --explicar-filtro})
     * @param detalharLimitacoes quando {@code true}, a previa da proxima etapa mostra o
     *        bloco completo de cada aviso e os dados que faltam para calcular
     *        ({@code --detalhar-limitacoes})
     */
    public MenuPrincipal(CatalogoMock catalogo, Terminal terminal, boolean explicarFiltro,
            boolean detalharLimitacoes) {
        this.catalogo = catalogo;
        this.terminal = terminal;
        this.filtro = explicarFiltro
                ? FiltroEtapa.explicativo(catalogo, System.err::println)
                : new FiltroEtapa(catalogo);
        this.detalharLimitacoes = detalharLimitacoes;
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
            CategoriaComponente categoria = fluxo.categoriaAtual();
            if (categoria == CategoriaComponente.CPU) {
                EtapaProcessador etapa = new EtapaProcessador(terminal, fluxo,
                        catalogo.porCategoria(CategoriaComponente.CPU), avaliador);
                if (etapa.executar() != ResultadoEtapa.CONCLUIDA) {
                    return;
                }
                fluxo.proximaEtapa();
                continue;
            }
            if (categoria == CategoriaComponente.PLACA_MAE) {
                EtapaPlacaMae etapa = new EtapaPlacaMae(terminal, fluxo, filtro, avaliador);
                ResultadoEtapa resultado = etapa.executar();
                if (resultado == ResultadoEtapa.VOLTAR) {
                    fluxo.etapaAnterior();
                    continue;
                }
                if (resultado != ResultadoEtapa.CONCLUIDA) {
                    return;
                }
                fluxo.proximaEtapa();
                continue;
            }
            if (categoria == CategoriaComponente.MEMORIA) {
                EtapaMemoria etapa = new EtapaMemoria(terminal, fluxo, filtro, avaliador);
                ResultadoEtapa resultado = etapa.executar();
                if (resultado == ResultadoEtapa.VOLTAR) {
                    fluxo.etapaAnterior();
                    continue;
                }
                if (resultado != ResultadoEtapa.CONCLUIDA) {
                    return;
                }
                fluxo.proximaEtapa();
                continue;
            }
            if (categoria == CategoriaComponente.PLACA_VIDEO) {
                EtapaPlacaVideo etapa = new EtapaPlacaVideo(terminal, fluxo, filtro, avaliador);
                ResultadoEtapa resultado = etapa.executar();
                if (resultado == ResultadoEtapa.VOLTAR) {
                    fluxo.etapaAnterior();
                    continue;
                }
                if (resultado != ResultadoEtapa.CONCLUIDA) {
                    return;
                }
                fluxo.proximaEtapa();
                continue;
            }
            if (categoria == CategoriaComponente.ARMAZENAMENTO) {
                EtapaArmazenamento etapa = new EtapaArmazenamento(terminal, fluxo, filtro,
                        avaliador);
                ResultadoEtapa resultado = etapa.executar();
                if (resultado == ResultadoEtapa.VOLTAR) {
                    fluxo.etapaAnterior();
                    continue;
                }
                if (resultado != ResultadoEtapa.CONCLUIDA) {
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
     * Exibe o cabecalho da etapa seguinte, a contagem do filtro (HU-003), o resumo dos
     * avisos de limitacao (HU-004) e avisa que a tela completa pertence a outra HU.
     *
     * <p>Serve para provar que a montagem avancou preservando a escolha anterior, que o
     * filtro ja esta ligado e que o avaliador de limitacoes ja marca os itens que teriam
     * {@code [!]}, enquanto as etapas 2 a 6 nao estao implementadas.</p>
     */
    private void informarEtapaPendente(MontagemGuiada fluxo) {
        terminal.linhaEmBranco();
        TelaEtapa tela = new TelaEtapa(terminal);
        tela.imprimirCabecalho(fluxo.etapaAtual(), fluxo.getMontagem());
        tela.imprimirBarraProgresso(fluxo.etapaAtual());

        ListaFiltrada lista = filtro.filtrar(fluxo.categoriaAtual(), fluxo.getMontagem());
        terminal.linha(PREFIXO_PREVIA_FILTRO + TelaEtapa.textoContagem(lista) + ".");

        Map<Componente, List<Limitacao>> avisos = avaliador.avaliarLista(
                fluxo.getMontagem(), lista.getCompativeis());
        if (avisos.isEmpty()) {
            terminal.linha(AVISO_ETAPAS_PENDENTES);
            return;
        }

        int quantidade = avisos.size();
        terminal.linha(quantidade + (quantidade == 1
                ? " componente da proxima etapa com aviso [!]."
                : " componentes da proxima etapa com aviso [!]."));
        if (detalharLimitacoes) {
            for (Map.Entry<Componente, List<Limitacao>> entrada : avisos.entrySet()) {
                terminal.linhaEmBranco();
                terminal.linha(TabelaComponentes.nomeExibicao(entrada.getKey()) + ":");
                for (Limitacao limitacao : entrada.getValue()) {
                    tela.imprimirLimitacao(limitacao);
                }
                for (String pendencia
                        : avaliador.pendencias(fluxo.getMontagem(), entrada.getKey())) {
                    terminal.linha("    " + TextosLimitacao.SEM_DADOS + ": " + pendencia);
                }
            }
        } else {
            terminal.linha("Use --detalhar-limitacoes para ver os detalhes.");
        }
        terminal.linha(AVISO_ETAPAS_PENDENTES);
    }
}
