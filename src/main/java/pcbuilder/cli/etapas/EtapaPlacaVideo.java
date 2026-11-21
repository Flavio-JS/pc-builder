package pcbuilder.cli.etapas;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import pcbuilder.analise.filtro.FiltroEtapa;
import pcbuilder.analise.limitacao.AvaliadorLimitacoes;
import pcbuilder.cli.TabelaComponentes;
import pcbuilder.cli.Terminal;
import pcbuilder.config.OrdemEtapas;
import pcbuilder.enums.CategoriaComponente;
import pcbuilder.modelo.Componente;
import pcbuilder.modelo.Montagem;
import pcbuilder.servico.MontagemGuiada;
import pcbuilder.servico.resumos.ResumoPlacaVideo;

/**
 * Etapa 4 da montagem guiada: escolha da placa de video (HU-007).
 *
 * <p>E a unica etapa <strong>condicional</strong>: quando o processador tem video
 * integrado a etapa e opcional e o rodape oferece {@code [0] Pular}; quando nao tem, a
 * etapa e obrigatoria, explica o motivo e nao oferece pular (regra C06).</p>
 *
 * <p>Ao ser reaberta a partir do resumo (HU-011), a etapa troca {@code [0] Pular} por
 * {@code [0] Voltar} (volta ao resumo) e, se ja houver GPU e a etapa for opcional,
 * oferece {@code [R] Remover peca}.</p>
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
    /** Prefixo da mensagem de remocao da GPU. */
    public static final String MENSAGEM_REMOVIDA = "Placa de video removida: ";
    /** Mensagem usada quando nenhuma GPU encaixa na placa-mae. */
    public static final String MENSAGEM_SEM_COMPATIVEIS =
            "Nenhuma placa de video compativel com a sua placa-mae.";
    /** Rodape da etapa opcional. */
    public static final String RODAPE_PULAR = "[0] Pular";
    /** Rodape da etapa obrigatoria. */
    public static final String RODAPE_VOLTAR = SelecaoFiltrada.RODAPE_VOLTAR;
    /** Comando de remocao da GPU (etapa opcional ja preenchida). */
    public static final String COMANDO_REMOVER = "R";
    /** Texto do comando de remocao no rodape. */
    public static final String RODAPE_REMOVER = "[R] Remover peca";

    private static final CategoriaComponente CATEGORIA = CategoriaComponente.PLACA_VIDEO;

    private final Terminal terminal;
    private final MontagemGuiada fluxo;
    private final FiltroEtapa filtro;
    private final AvaliadorLimitacoes avaliador;
    private final boolean aPartirDoResumo;

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
        this(terminal, fluxo, filtro, avaliador, false);
    }

    /**
     * @param terminal entrada e saida do programa
     * @param fluxo montagem em andamento
     * @param filtro filtro compartilhado pela aplicacao
     * @param avaliador avaliador de limitacoes
     * @param aPartirDoResumo {@code true} quando a etapa foi reaberta pela HU-011
     */
    public EtapaPlacaVideo(Terminal terminal, MontagemGuiada fluxo, FiltroEtapa filtro,
            AvaliadorLimitacoes avaliador, boolean aPartirDoResumo) {
        this.terminal = Objects.requireNonNull(terminal, "terminal");
        this.fluxo = Objects.requireNonNull(fluxo, "fluxo");
        this.filtro = Objects.requireNonNull(filtro, "filtro");
        this.avaliador = Objects.requireNonNull(avaliador, "avaliador");
        this.aPartirDoResumo = aPartirDoResumo;
    }

    /**
     * Exibe a etapa e espera a escolha ate concluir, pular, remover ou voltar.
     *
     * @return {@link ResultadoEtapa#CONCLUIDA} (inclusive quando a etapa opcional foi
     *         pulada ou a GPU foi removida), {@link ResultadoEtapa#VOLTAR} ou
     *         {@link ResultadoEtapa#CANCELADA} (entrada encerrada)
     */
    public ResultadoEtapa executar() {
        Montagem montagem = fluxo.getMontagem();
        boolean obrigatoria = OrdemEtapas.obrigatoria(OrdemEtapas.numeroDe(CATEGORIA),
                montagem);
        boolean temGpu = montagem.obter(CATEGORIA).isPresent();
        boolean podeRemover = aPartirDoResumo && !obrigatoria && temGpu;

        String rodape = (obrigatoria || aPartirDoResumo) ? RODAPE_VOLTAR : RODAPE_PULAR;
        if (podeRemover) {
            rodape = RODAPE_REMOVER + "   " + rodape;
        }
        Map<String, SelecaoFiltrada.ComandoExtra> extras = new LinkedHashMap<>();
        if (podeRemover) {
            extras.put(COMANDO_REMOVER, this::remover);
        }

        SelecaoFiltrada.Opcoes opcoes = new SelecaoFiltrada.Opcoes(rodape,
                MENSAGEM_SEM_COMPATIVEIS, rodape,
                obrigatoria ? MENSAGEM_OBRIGATORIA : null, extras);

        ResultadoEtapa resultado = new SelecaoFiltrada(terminal, filtro, avaliador, opcoes)
                .executar(fluxo, CATEGORIA, ResumoPlacaVideo::linhaDeApoio,
                        componente -> "Placa de video selecionada: "
                                + SelecaoFiltrada.descricao(componente));

        if (!aPartirDoResumo && !obrigatoria && resultado == ResultadoEtapa.VOLTAR) {
            terminal.linha(MENSAGEM_PULADA);
            return ResultadoEtapa.CONCLUIDA;
        }
        return resultado;
    }

    /** Comando {@code [R]}: remove a GPU escolhida (etapa opcional). */
    private Optional<ResultadoEtapa> remover() {
        Optional<Componente> removida = fluxo.getMontagem().remover(CATEGORIA);
        removida.ifPresent(componente -> terminal.linha(MENSAGEM_REMOVIDA
                + TabelaComponentes.nomeExibicao(componente) + "."));
        return Optional.of(ResultadoEtapa.CONCLUIDA);
    }
}
