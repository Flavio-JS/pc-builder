package pcbuilder.cli.etapas;

import java.util.Objects;

import pcbuilder.analise.filtro.FiltroEtapa;
import pcbuilder.analise.limitacao.AvaliadorLimitacoes;
import pcbuilder.cli.Terminal;
import pcbuilder.enums.CategoriaComponente;
import pcbuilder.servico.MontagemGuiada;
import pcbuilder.servico.resumos.ResumoPlacaMae;

/**
 * Etapa 2 da montagem guiada: escolha da placa-mae (HU-005).
 *
 * <p>A lista vem do filtro de compatibilidade (HU-003): com uma CPU AM5 so aparecem
 * placas AM5, e as demais ficam em {@code [M] Ver ocultos} com o motivo
 * ({@code soquete LGA1700 x processador AM5}). Os itens compativeis que geram limitacao
 * (VRM apertado - L02, BIOS que pode precisar de atualizacao - C09) aparecem com
 * {@code [!]} e continuam selecionaveis (HU-004).</p>
 *
 * <p>O laco de lista, os comandos {@code [M]}, {@code [L]} e {@code [0] Voltar} e o
 * bloco de aviso ficam em {@link SelecaoFiltrada}; aqui ficam apenas a categoria, a linha
 * de apoio e a mensagem de confirmacao.</p>
 */
public final class EtapaPlacaMae {

    /** Comando que volta para a etapa 1. */
    public static final String COMANDO_VOLTAR = SelecaoFiltrada.COMANDO_VOLTAR;
    /** Rodape de comandos da etapa. */
    public static final String RODAPE = SelecaoFiltrada.RODAPE_VOLTAR;

    private static final CategoriaComponente CATEGORIA = CategoriaComponente.PLACA_MAE;

    private final MontagemGuiada fluxo;
    private final SelecaoFiltrada selecao;

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
        this.fluxo = Objects.requireNonNull(fluxo, "fluxo");
        this.selecao = new SelecaoFiltrada(terminal, filtro, avaliador);
    }

    /**
     * Exibe a etapa e espera a escolha ate concluir ou pedir para voltar.
     *
     * @return {@link ResultadoEtapa#CONCLUIDA}, {@link ResultadoEtapa#VOLTAR} ou
     *         {@link ResultadoEtapa#CANCELADA} (entrada encerrada)
     */
    public ResultadoEtapa executar() {
        return selecao.executar(fluxo, CATEGORIA, ResumoPlacaMae::linhaDeApoio,
                componente -> "Placa-mae selecionada: "
                        + SelecaoFiltrada.descricao(componente));
    }
}
