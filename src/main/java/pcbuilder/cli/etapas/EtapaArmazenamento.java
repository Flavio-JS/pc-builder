package pcbuilder.cli.etapas;

import java.util.Objects;

import pcbuilder.analise.filtro.FiltroEtapa;
import pcbuilder.analise.limitacao.AvaliadorLimitacoes;
import pcbuilder.cli.Terminal;
import pcbuilder.enums.CategoriaComponente;
import pcbuilder.servico.MontagemGuiada;
import pcbuilder.servico.resumos.ResumoArmazenamento;

/**
 * Etapa 5 da montagem guiada: escolha do armazenamento - SSD ou HD (HU-008).
 *
 * <p>A lista une as duas midias e vem do filtro C04: NVMe exige slot M.2 na placa e
 * SATA exige porta SATA. O que nao encaixa fica em {@code [M] Ver ocultos} com o motivo
 * ({@code placa-mae sem slot M.2 livre} / {@code sem porta SATA livre}).</p>
 *
 * <p>Os avisos L10 aparecem com {@code [!]} sem bloquear: perda de banda quando o SSD e
 * de geracao maior que o slot e o informativo de midia lenta (HD mecanico ou SSD SATA em
 * relacao ao NVMe).</p>
 *
 * <p>O laco de lista, os comandos {@code [M]}, {@code [L]} e {@code [0] Voltar} e o bloco
 * de aviso ficam em {@link SelecaoFiltrada}.</p>
 */
public final class EtapaArmazenamento {

    /** Rodape de comandos da etapa. */
    public static final String RODAPE = SelecaoFiltrada.RODAPE_VOLTAR;

    private static final CategoriaComponente CATEGORIA = CategoriaComponente.ARMAZENAMENTO;

    private final MontagemGuiada fluxo;
    private final SelecaoFiltrada selecao;

    /**
     * @param terminal entrada e saida do programa
     * @param fluxo montagem em andamento (CPU, placa-mae, memoria e video ja definidos)
     * @param filtro filtro compartilhado pela aplicacao (com {@code --explicar-filtro})
     */
    public EtapaArmazenamento(Terminal terminal, MontagemGuiada fluxo, FiltroEtapa filtro) {
        this(terminal, fluxo, filtro, new AvaliadorLimitacoes());
    }

    /**
     * @param terminal entrada e saida do programa
     * @param fluxo montagem em andamento (CPU, placa-mae, memoria e video ja definidos)
     * @param filtro filtro compartilhado pela aplicacao (com {@code --explicar-filtro})
     * @param avaliador avaliador de limitacoes usado para marcar os itens com {@code [!]}
     */
    public EtapaArmazenamento(Terminal terminal, MontagemGuiada fluxo, FiltroEtapa filtro,
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
        return selecao.executar(fluxo, CATEGORIA, ResumoArmazenamento::linhaDeApoio,
                componente -> "Armazenamento selecionado: "
                        + SelecaoFiltrada.descricao(componente));
    }
}
