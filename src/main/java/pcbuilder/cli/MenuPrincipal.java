package pcbuilder.cli;

import java.util.Optional;

import pcbuilder.analise.filtro.FiltroEtapa;
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
     * <p>{@code [N] Nova montagem} recomeca o fluxo com uma montagem vazia;
     * {@code [0] Sair} encerra a aplicacao.</p>
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

            ResumoMontagem resumo = new ResumoMontagem(fluxo.getMontagem());
            TelaResumo telaResumo = new TelaResumo(terminal);
            if (!resumo.completa()) {
                telaResumo.imprimirPendencias(resumo);
                return false;
            }
            if (telaResumo.executar(resumo) == TelaResumo.Resultado.SAIR) {
                return true;
            }
            // NOVA_MONTAGEM: o laco recomeca com uma montagem vazia.
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
            CategoriaComponente categoria = fluxo.categoriaAtual();
            if (categoria == CategoriaComponente.CPU) {
                EtapaProcessador etapa = new EtapaProcessador(terminal, fluxo,
                        catalogo.porCategoria(CategoriaComponente.CPU), avaliador);
                if (etapa.executar() != ResultadoEtapa.CONCLUIDA) {
                    return false;
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
                    return false;
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
                    return false;
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
                    return false;
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
                    return false;
                }
                fluxo.proximaEtapa();
                continue;
            }
            if (categoria == CategoriaComponente.FONTE) {
                EtapaFonte etapa = new EtapaFonte(terminal, fluxo, filtro, avaliador);
                ResultadoEtapa resultado = etapa.executar();
                if (resultado == ResultadoEtapa.VOLTAR) {
                    fluxo.etapaAnterior();
                    continue;
                }
                if (resultado != ResultadoEtapa.CONCLUIDA) {
                    return false;
                }
                fluxo.proximaEtapa();
                continue;
            }
            throw new IllegalStateException("Categoria fora do fluxo guiado: " + categoria);
        }
        return true;
    }
}
