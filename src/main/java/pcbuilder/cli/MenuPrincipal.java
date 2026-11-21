package pcbuilder.cli;

import java.util.Optional;

import pcbuilder.analise.filtro.FiltroEtapa;
import pcbuilder.analise.filtro.RevalidadorMontagem;
import pcbuilder.analise.limitacao.AvaliadorLimitacoes;
import pcbuilder.cli.etapas.EtapaArmazenamento;
import pcbuilder.cli.etapas.EtapaFonte;
import pcbuilder.cli.etapas.EtapaMemoria;
import pcbuilder.cli.etapas.EtapaPlacaMae;
import pcbuilder.cli.etapas.EtapaPlacaVideo;
import pcbuilder.cli.etapas.EtapaProcessador;
import pcbuilder.cli.etapas.ResultadoEtapa;
import pcbuilder.enums.CategoriaComponente;
import pcbuilder.mock.CatalogoMock;
import pcbuilder.modelo.Montagem;
import pcbuilder.relatorio.ResumoMontagem;
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
 * <p>As seis etapas ja existem (HU-002 a HU-009). Ao concluir a etapa 6 o programa mostra
 * o resumo com pecas, precos e avisos (HU-010) e oferece {@code [N] Nova montagem},
 * {@code [T] Trocar uma peca} e {@code [0] Sair}.</p>
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

    private final CatalogoMock catalogo;
    private final Terminal terminal;
    private final FiltroEtapa filtro;
    private final AvaliadorLimitacoes avaliador = new AvaliadorLimitacoes();

    /**
     * @param catalogo base carregada uma unica vez pela aplicacao
     * @param terminal entrada e saida do programa
     */
    public MenuPrincipal(CatalogoMock catalogo, Terminal terminal) {
        this(catalogo, terminal, false);
    }

    /**
     * @param catalogo base carregada uma unica vez pela aplicacao
     * @param terminal entrada e saida do programa
     * @param explicarFiltro quando {@code true}, cada item oculto tambem vai para o
     *        {@code stderr} ({@code --explicar-filtro})
     */
    public MenuPrincipal(CatalogoMock catalogo, Terminal terminal, boolean explicarFiltro) {
        this.catalogo = catalogo;
        this.terminal = terminal;
        this.filtro = explicarFiltro
                ? FiltroEtapa.explicativo(catalogo, System.err::println)
                : new FiltroEtapa(catalogo);
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
                if (montarPc()) {
                    return SAIDA_SUCESSO;
                }
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
     * Executa uma montagem completa e mostra o resumo.
     *
     * <p>{@code [T] Trocar uma peca} reabre uma etapa preservando as demais;
     * {@code [N] Nova montagem} recomeca com uma montagem vazia; {@code [0] Sair}
     * encerra a aplicacao.</p>
     *
     * @return {@code true} quando o usuario escolheu sair no resumo
     */
    private boolean montarPc() {
        terminal.linhaEmBranco();
        while (true) {
            MontagemGuiada fluxo = new MontagemGuiada();
            if (!executarEtapas(fluxo)) {
                return false;
            }

            TelaResumo tela = new TelaResumo(terminal);
            boolean novaMontagem = false;
            while (!novaMontagem) {
                ResumoMontagem resumo = new ResumoMontagem(fluxo.getMontagem());
                if (!resumo.completa()) {
                    tela.imprimirPendencias(resumo);
                    return false;
                }
                TelaResumo.Resultado resultado = tela.executar(resumo);
                if (resultado == TelaResumo.Resultado.SAIR) {
                    return true;
                }
                if (resultado == TelaResumo.Resultado.NOVA_MONTAGEM) {
                    novaMontagem = true;
                } else if (resultado == TelaResumo.Resultado.TROCAR) {
                    trocarPeca(fluxo);
                }
            }
        }
    }

    /**
     * Executa as seis etapas do fluxo.
     *
     * @param fluxo montagem em andamento
     * @return {@code false} quando o usuario cancelou a montagem
     */
    private boolean executarEtapas(MontagemGuiada fluxo) {
        while (!fluxo.concluir()) {
            ResultadoEtapa resultado = executarEtapa(fluxo, false);
            if (resultado == ResultadoEtapa.CONCLUIDA) {
                fluxo.proximaEtapa();
                continue;
            }
            if (resultado == ResultadoEtapa.VOLTAR) {
                fluxo.etapaAnterior();
                continue;
            }
            return false;
        }
        return true;
    }

    /**
     * Executa a tela da etapa atual.
     *
     * @param fluxo montagem em andamento
     * @param aPartirDoResumo {@code true} quando a etapa foi reaberta pelo resumo
     * @return o resultado informado pela tela
     */
    private ResultadoEtapa executarEtapa(MontagemGuiada fluxo, boolean aPartirDoResumo) {
        CategoriaComponente categoria = fluxo.categoriaAtual();
        switch (categoria) {
            case CPU:
                return new EtapaProcessador(terminal, fluxo,
                        catalogo.porCategoria(CategoriaComponente.CPU), avaliador,
                        aPartirDoResumo).executar();
            case PLACA_MAE:
                return new EtapaPlacaMae(terminal, fluxo, filtro, avaliador).executar();
            case MEMORIA:
                return new EtapaMemoria(terminal, fluxo, filtro, avaliador).executar();
            case PLACA_VIDEO:
                return new EtapaPlacaVideo(terminal, fluxo, filtro, avaliador,
                        aPartirDoResumo).executar();
            case ARMAZENAMENTO:
                return new EtapaArmazenamento(terminal, fluxo, filtro, avaliador).executar();
            case FONTE:
                return new EtapaFonte(terminal, fluxo, filtro, avaliador).executar();
            default:
                throw new IllegalStateException("Categoria fora do fluxo guiado: " + categoria);
        }
    }

    /**
     * Troca uma peca a partir do resumo (HU-011).
     *
     * <p>Reabre a etapa escolhida, preserva as demais pecas e, se a troca invalidar
     * outra etapa, mostra a mensagem e leva o usuario direto a ela. O resumo e
     * recalculado por quem chamou.</p>
     *
     * @param fluxo montagem completa em andamento
     */
    private void trocarPeca(MontagemGuiada fluxo) {
        TelaResumo tela = new TelaResumo(terminal);
        int etapa = tela.escolherEtapa(new ResumoMontagem(fluxo.getMontagem()));
        if (etapa == 0) {
            return;
        }
        Montagem antesDaTroca = fluxo.getMontagem().copia();
        fluxo.reabrirEtapa(etapa);

        RevalidadorMontagem revalidador = new RevalidadorMontagem();
        while (true) {
            ResultadoEtapa resultado = executarEtapa(fluxo, true);
            if (resultado != ResultadoEtapa.CONCLUIDA) {
                fluxo.getMontagem().restaurar(antesDaTroca);
                return;
            }
            Optional<RevalidadorMontagem.EtapaInvalida> invalida =
                    revalidador.primeiraInvalida(fluxo.getMontagem());
            if (invalida.isEmpty()) {
                return;
            }
            RevalidadorMontagem.EtapaInvalida problema = invalida.get();
            terminal.linha("Configuracao invalida: a etapa " + problema.getEtapa()
                    + " (" + problema.getCategoria().getRotulo()
                    + ") precisa ser revista - " + problema.getMotivo());
            fluxo.reabrirEtapa(problema.getEtapa());
        }
    }
}
