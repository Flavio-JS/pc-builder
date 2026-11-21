package pcbuilder.cli.etapas;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import pcbuilder.analise.CalculadoraConsumo;
import pcbuilder.analise.RequisitosFonte;
import pcbuilder.analise.filtro.FiltroEtapa;
import pcbuilder.analise.limitacao.AvaliadorLimitacoes;
import pcbuilder.cli.TelaEtapa;
import pcbuilder.cli.Terminal;
import pcbuilder.config.ParametrosCalculo;
import pcbuilder.enums.CategoriaComponente;
import pcbuilder.modelo.Montagem;
import pcbuilder.servico.MontagemGuiada;
import pcbuilder.servico.resumos.ResumoFonte;

/**
 * Etapa 6 da montagem guiada: escolha da fonte (HU-009).
 *
 * <p>Antes da lista a tela mostra o que a configuracao exige - consumo com a margem de
 * 40% e os conectores exigidos - e o comando {@code [C]} detalha a soma componente por
 * componente. A lista vem do filtro C10/C14 (potencia e conectores) e os avisos L06
 * (pico transiente) e L09 (eficiencia) aparecem com {@code [!]} sem bloquear.</p>
 *
 * <p>Ao escolher, imprime {@code Fonte selecionada: ...} e o menu principal encerra o
 * fluxo; o resumo completo entra na HU-010.</p>
 */
public final class EtapaFonte {

    /** Comando que detalha o calculo do consumo. */
    public static final String COMANDO_CALCULO = "C";
    /** Rodape da etapa: calculo do consumo + voltar. */
    public static final String RODAPE = "[C] Ver calculo do consumo   "
            + SelecaoFiltrada.RODAPE_VOLTAR;

    private static final CategoriaComponente CATEGORIA = CategoriaComponente.FONTE;

    private final Terminal terminal;
    private final MontagemGuiada fluxo;
    private final FiltroEtapa filtro;
    private final AvaliadorLimitacoes avaliador;

    /**
     * @param terminal entrada e saida do programa
     * @param fluxo montagem em andamento (as cinco pecas anteriores ja estao nela)
     * @param filtro filtro compartilhado pela aplicacao (com {@code --explicar-filtro})
     */
    public EtapaFonte(Terminal terminal, MontagemGuiada fluxo, FiltroEtapa filtro) {
        this(terminal, fluxo, filtro, new AvaliadorLimitacoes());
    }

    /**
     * @param terminal entrada e saida do programa
     * @param fluxo montagem em andamento (as cinco pecas anteriores ja estao nela)
     * @param filtro filtro compartilhado pela aplicacao (com {@code --explicar-filtro})
     * @param avaliador avaliador de limitacoes usado para marcar os itens com {@code [!]}
     */
    public EtapaFonte(Terminal terminal, MontagemGuiada fluxo, FiltroEtapa filtro,
            AvaliadorLimitacoes avaliador) {
        this.terminal = Objects.requireNonNull(terminal, "terminal");
        this.fluxo = Objects.requireNonNull(fluxo, "fluxo");
        this.filtro = Objects.requireNonNull(filtro, "filtro");
        this.avaliador = Objects.requireNonNull(avaliador, "avaliador");
    }

    /**
     * Exibe a etapa e espera a escolha ate concluir ou pedir para voltar.
     *
     * @return {@link ResultadoEtapa#CONCLUIDA}, {@link ResultadoEtapa#VOLTAR} ou
     *         {@link ResultadoEtapa#CANCELADA} (entrada encerrada)
     */
    public ResultadoEtapa executar() {
        Montagem montagem = fluxo.getMontagem();
        int consumo = CalculadoraConsumo.consumoTotalSistemaWatts(montagem);
        int margem = RequisitosFonte.potenciaNecessariaWatts(montagem);
        String conectores = RequisitosFonte.descricaoConectores(montagem);

        String bloco = "Consumo estimado da sua configuracao: " + consumo
                + " W (com margem de " + ParametrosCalculo.margemSegurancaPercentual()
                + "%: " + margem + " W)\n"
                + "Conectores exigidos: "
                + (conectores.isEmpty() ? "nenhum" : conectores);
        String semFonte = "Nenhuma fonte compativel. Sua configuracao precisa de " + margem
                + " W" + (conectores.isEmpty() ? "." : " e de " + conectores + ".");

        SelecaoFiltrada.Opcoes opcoes = new SelecaoFiltrada.Opcoes(RODAPE, semFonte,
                SelecaoFiltrada.RODAPE_VOLTAR, bloco,
                Map.of(COMANDO_CALCULO, this::imprimirCalculo));
        return new SelecaoFiltrada(terminal, filtro, avaliador, opcoes)
                .executar(fluxo, CATEGORIA, ResumoFonte::linhaDeApoio,
                        componente -> "Fonte selecionada: "
                                + SelecaoFiltrada.descricao(componente));
    }

    /** Comando {@code [C]}: detalha a soma do consumo componente por componente. */
    private Optional<ResultadoEtapa> imprimirCalculo() {
        Montagem montagem = fluxo.getMontagem();
        TelaEtapa tela = new TelaEtapa(terminal);
        tela.imprimirSeparador();
        terminal.linha("Calculo do consumo:");
        for (CalculadoraConsumo.ItemConsumo item : CalculadoraConsumo.detalhar(montagem)) {
            terminal.linha("  " + item.getDescricao() + " - " + item.getWatts() + " W");
        }
        terminal.linha("  Total - " + CalculadoraConsumo.consumoTotalSistemaWatts(montagem)
                + " W (com margem de " + ParametrosCalculo.margemSegurancaPercentual()
                + "%: " + RequisitosFonte.potenciaNecessariaWatts(montagem) + " W)");
        return Optional.empty();
    }
}
