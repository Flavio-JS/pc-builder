package pcbuilder.analise.filtro;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import pcbuilder.enums.CategoriaComponente;
import pcbuilder.mock.CatalogoMock;
import pcbuilder.modelo.Componente;
import pcbuilder.modelo.Montagem;

/**
 * Filtra a lista de uma etapa aplicando as regras duras de compatibilidade.
 *
 * <p>O filtro e recalculado <strong>a cada chamada</strong> (sem cache): se o usuario
 * volta e troca uma peca, a proxima etapa ja enxerga a montagem nova. As regras duras
 * estao na secao 13 do modelo de dados; nenhuma delas esconde item por desempenho.</p>
 *
 * <p>Para depuracao existe a fabrica {@link #explicativo(CatalogoMock, Consumer)}: cada
 * item oculto e enviado ao destino informado (o {@code --explicar-filtro} da CLI usa o
 * {@code stderr}).</p>
 */
public final class FiltroEtapa {

    /** Regras na ordem em que sao avaliadas; a primeira que falhar define o motivo. */
    private static final List<RegraFiltro> REGRAS_PADRAO = List.of(
            new FiltroSoquete(),
            new FiltroTipoMemoria(),
            new FiltroSlotsMemoria(),
            new FiltroMemoriaECC(),
            new FiltroSlotArmazenamento(),
            new FiltroSlotPcie(),
            new FiltroVideoIntegrado(),
            new FiltroConectoresFonte(),
            new FiltroFonteSuficiente());

    private final CatalogoMock catalogo;
    private final List<RegraFiltro> regras;
    private final Consumer<String> registro;

    /**
     * Filtro normal: candidatos vindos do catalogo, sem registro de depuracao.
     *
     * @param catalogo base carregada uma unica vez pela aplicacao
     */
    public FiltroEtapa(CatalogoMock catalogo) {
        this(catalogo, REGRAS_PADRAO, linha -> { });
    }

    /**
     * @param catalogo base carregada uma unica vez pela aplicacao
     * @param regras regras aplicadas, na ordem de avaliacao
     * @param registro destino de uma linha por item oculto (depuracao)
     */
    public FiltroEtapa(CatalogoMock catalogo, List<RegraFiltro> regras,
            Consumer<String> registro) {
        this.catalogo = Objects.requireNonNull(catalogo, "catalogo");
        this.regras = List.copyOf(regras);
        this.registro = Objects.requireNonNull(registro, "registro");
    }

    /**
     * Filtro que tambem registra cada item oculto (usado por {@code --explicar-filtro}).
     *
     * @param catalogo base carregada pela aplicacao
     * @param destino recebe uma linha por item oculto
     * @return filtro com as regras padrao e o registro ligado
     */
    public static FiltroEtapa explicativo(CatalogoMock catalogo, Consumer<String> destino) {
        return new FiltroEtapa(catalogo, REGRAS_PADRAO,
                Objects.requireNonNull(destino, "destino"));
    }

    /**
     * Separa os componentes da categoria em compativeis e ocultos.
     *
     * @param categoria categoria que a etapa esta listando
     * @param montagem pecas ja escolhidas
     * @return lista filtrada com os motivos dos itens ocultos
     */
    public ListaFiltrada filtrar(CategoriaComponente categoria, Montagem montagem) {
        Objects.requireNonNull(categoria, "categoria");
        Objects.requireNonNull(montagem, "montagem");

        List<Componente> compativeis = new ArrayList<>();
        List<ItemOculto> ocultos = new ArrayList<>();
        for (Componente candidato : catalogo.porCategoria(categoria)) {
            Optional<String> motivo = motivoParaOcultar(categoria, montagem, candidato);
            if (motivo.isEmpty()) {
                compativeis.add(candidato);
                continue;
            }
            ocultos.add(new ItemOculto(candidato, motivo.get()));
            registro.accept("oculto: " + candidato + " - incompativel: " + motivo.get());
        }
        return new ListaFiltrada(categoria, compativeis, ocultos);
    }

    /** Primeira regra da categoria que reprovar o candidato define o motivo. */
    private Optional<String> motivoParaOcultar(CategoriaComponente categoria, Montagem montagem,
            Componente candidato) {
        for (RegraFiltro regra : regras) {
            if (regra.categoriaAfetada() != categoria) {
                continue;
            }
            Optional<String> motivo = regra.motivoIncompatibilidade(montagem, candidato);
            if (motivo.isPresent()) {
                return motivo;
            }
        }
        return Optional.empty();
    }
}
