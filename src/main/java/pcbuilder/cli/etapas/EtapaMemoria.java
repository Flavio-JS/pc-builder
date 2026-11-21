package pcbuilder.cli.etapas;

import java.util.Objects;

import pcbuilder.analise.CalculadoraMemoria;
import pcbuilder.analise.filtro.FiltroEtapa;
import pcbuilder.analise.limitacao.AvaliadorLimitacoes;
import pcbuilder.cli.Terminal;
import pcbuilder.enums.CategoriaComponente;
import pcbuilder.modelo.Memoria;
import pcbuilder.servico.MontagemGuiada;
import pcbuilder.servico.resumos.ResumoMemoria;

/**
 * Etapa 3 da montagem guiada: escolha da memoria (HU-006).
 *
 * <p>A lista e filtrada pelo tipo DDR da placa (C02) e pelos slots/capacidade aceitos
 * pela placa e pela CPU (C03): kits de outro tipo ou grandes demais ficam em
 * {@code [M] Ver ocultos} com o motivo. Os kits compativeis que rendem menos do que o
 * anunciado (frequencia limitada pela placa/CPU - L04, single channel - L04, perfil
 * EXPO/XMP nao suportado - C02) aparecem com {@code [!]} e continuam selecionaveis.</p>
 *
 * <p>O laco de lista, os comandos {@code [M]}, {@code [L]} e {@code [0] Voltar} e o bloco
 * de aviso ficam em {@link SelecaoFiltrada}.</p>
 */
public final class EtapaMemoria {

    /** Comando que volta para a etapa 2. */
    public static final String COMANDO_VOLTAR = SelecaoFiltrada.COMANDO_VOLTAR;
    /** Rodape de comandos da etapa. */
    public static final String RODAPE = SelecaoFiltrada.RODAPE_VOLTAR;

    private static final CategoriaComponente CATEGORIA = CategoriaComponente.MEMORIA;

    private final MontagemGuiada fluxo;
    private final SelecaoFiltrada selecao;

    /**
     * @param terminal entrada e saida do programa
     * @param fluxo montagem em andamento (CPU e placa-mae ja estao nela)
     * @param filtro filtro compartilhado pela aplicacao (com {@code --explicar-filtro})
     */
    public EtapaMemoria(Terminal terminal, MontagemGuiada fluxo, FiltroEtapa filtro) {
        this(terminal, fluxo, filtro, new AvaliadorLimitacoes());
    }

    /**
     * @param terminal entrada e saida do programa
     * @param fluxo montagem em andamento (CPU e placa-mae ja estao nela)
     * @param filtro filtro compartilhado pela aplicacao (com {@code --explicar-filtro})
     * @param avaliador avaliador de limitacoes usado para marcar os itens com {@code [!]}
     */
    public EtapaMemoria(Terminal terminal, MontagemGuiada fluxo, FiltroEtapa filtro,
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
        return selecao.executar(fluxo, CATEGORIA, ResumoMemoria::linhaDeApoio,
                componente -> mensagemSelecionada((Memoria) componente));
    }

    /** {@code Memoria selecionada: ... - R$ ... (2x16 GB = 32 GB, Dual Channel)}. */
    private String mensagemSelecionada(Memoria memoria) {
        int canais = CalculadoraMemoria.canaisSuportados(fluxo.getMontagem());
        return "Memoria selecionada: " + SelecaoFiltrada.descricao(memoria)
                + " (" + ResumoMemoria.descricaoKit(memoria) + ", "
                + ResumoMemoria.canal(memoria, canais) + ")";
    }
}
