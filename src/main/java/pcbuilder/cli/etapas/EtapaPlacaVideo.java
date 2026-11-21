package pcbuilder.cli.etapas;

import java.util.Objects;

import pcbuilder.analise.filtro.FiltroEtapa;
import pcbuilder.analise.limitacao.AvaliadorLimitacoes;
import pcbuilder.cli.Terminal;
import pcbuilder.config.OrdemEtapas;
import pcbuilder.enums.CategoriaComponente;
import pcbuilder.servico.MontagemGuiada;
import pcbuilder.servico.resumos.ResumoPlacaVideo;

/**
 * Etapa 4 da montagem guiada: escolha da placa de video (HU-007).
 *
 * <p>E a unica etapa <strong>condicional</strong>: quando o processador tem video
 * integrado a etapa e opcional e o rodape oferece {@code [0] Pular}; quando nao tem, a
 * etapa e obrigatoria, explica o motivo e nao oferece pular (regra C06).</p>
 *
 * <p>A lista vem do filtro C05 (slot x16 livre) e os avisos de L05 (banda PCIe), L01
 * (gargalo de CPU) e L07 (VRAM x resolucao) aparecem com {@code [!]} sem bloquear a
 * escolha.</p>
 */
public final class EtapaPlacaVideo {

    /** Mensagem exibida quando a CPU nao tem video integrado. */
    public static final String MENSAGEM_OBRIGATORIA =
            "Seu processador nao tem video integrado, por isso a placa de video e obrigatoria.";
    /** Mensagem exibida ao pular a etapa opcional. */
    public static final String MENSAGEM_PULADA =
            "Placa de video pulada: o seu processador ja tem video integrado.";
    /** Mensagem usada quando nenhuma GPU encaixa na placa-mae. */
    public static final String MENSAGEM_SEM_COMPATIVEIS =
            "Nenhuma placa de video compativel com a sua placa-mae.";
    /** Rodape da etapa opcional. */
    public static final String RODAPE_PULAR = "[0] Pular";
    /** Rodape da etapa obrigatoria. */
    public static final String RODAPE_VOLTAR = SelecaoFiltrada.RODAPE_VOLTAR;

    private static final CategoriaComponente CATEGORIA = CategoriaComponente.PLACA_VIDEO;

    private final Terminal terminal;
    private final MontagemGuiada fluxo;
    private final FiltroEtapa filtro;
    private final AvaliadorLimitacoes avaliador;

    /**
     * @param terminal entrada e saida do programa
     * @param fluxo montagem em andamento (CPU, placa-mae e memoria ja estao nela)
     * @param filtro filtro compartilhado pela aplicacao (com {@code --explicar-filtro})
     */
    public EtapaPlacaVideo(Terminal terminal, MontagemGuiada fluxo, FiltroEtapa filtro) {
        this(terminal, fluxo, filtro, new AvaliadorLimitacoes());
    }

    /**
     * @param terminal entrada e saida do programa
     * @param fluxo montagem em andamento (CPU, placa-mae e memoria ja estao nela)
     * @param filtro filtro compartilhado pela aplicacao (com {@code --explicar-filtro})
     * @param avaliador avaliador de limitacoes usado para marcar os itens com {@code [!]}
     */
    public EtapaPlacaVideo(Terminal terminal, MontagemGuiada fluxo, FiltroEtapa filtro,
            AvaliadorLimitacoes avaliador) {
        this.terminal = Objects.requireNonNull(terminal, "terminal");
        this.fluxo = Objects.requireNonNull(fluxo, "fluxo");
        this.filtro = Objects.requireNonNull(filtro, "filtro");
        this.avaliador = Objects.requireNonNull(avaliador, "avaliador");
    }

    /**
     * Exibe a etapa e espera a escolha ate concluir, pular (quando opcional) ou voltar.
     *
     * @return {@link ResultadoEtapa#CONCLUIDA} (inclusive quando a etapa opcional foi
     *         pulada), {@link ResultadoEtapa#VOLTAR} ou
     *         {@link ResultadoEtapa#CANCELADA} (entrada encerrada)
     */
    public ResultadoEtapa executar() {
        boolean obrigatoria = OrdemEtapas.obrigatoria(OrdemEtapas.numeroDe(CATEGORIA),
                fluxo.getMontagem());
        String rodaZero = obrigatoria ? RODAPE_VOLTAR : RODAPE_PULAR;
        SelecaoFiltrada.Opcoes opcoes = new SelecaoFiltrada.Opcoes(rodaZero,
                MENSAGEM_SEM_COMPATIVEIS, rodaZero,
                obrigatoria ? MENSAGEM_OBRIGATORIA : null);

        ResultadoEtapa resultado = new SelecaoFiltrada(terminal, filtro, avaliador, opcoes)
                .executar(fluxo, CATEGORIA, ResumoPlacaVideo::linhaDeApoio,
                        componente -> "Placa de video selecionada: "
                                + SelecaoFiltrada.descricao(componente));

        if (!obrigatoria && resultado == ResultadoEtapa.VOLTAR) {
            terminal.linha(MENSAGEM_PULADA);
            return ResultadoEtapa.CONCLUIDA;
        }
        return resultado;
    }
}
