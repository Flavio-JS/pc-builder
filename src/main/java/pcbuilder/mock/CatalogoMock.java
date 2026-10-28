package pcbuilder.mock;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import pcbuilder.enums.CategoriaComponente;
import pcbuilder.modelo.Componente;
import pcbuilder.servico.ResultadoValidacao;
import pcbuilder.servico.ValidadorCadastro;

/**
 * Catalogo em memoria carregado a partir do arquivo {@code base/componentes.json}.
 *
 * <p>A base e montada <strong>uma unica vez por execucao</strong> e compartilhada
 * por todas as etapas: {@link #criar()} devolve sempre a mesma instancia. Registros
 * com problema de nivel {@code ERRO} sao descartados; registros com {@code AVISO}
 * entram no catalogo normalmente.</p>
 *
 * <p>Os componentes sao agregados em {@code Map<CategoriaComponente,
 * List<Componente>>}, que e a estrutura usada pelas etapas do fluxo guiado.</p>
 */
public final class CatalogoMock {

    private static CatalogoMock instancia;

    private final Map<CategoriaComponente, List<Componente>> componentesPorCategoria;
    private final ResultadoValidacao resultadoValidacao;

    private CatalogoMock(Map<CategoriaComponente, List<Componente>> componentesPorCategoria,
            ResultadoValidacao resultadoValidacao) {
        this.componentesPorCategoria = componentesPorCategoria;
        this.resultadoValidacao = resultadoValidacao;
    }

    /**
     * Carrega (na primeira chamada) e devolve o catalogo da execucao.
     *
     * @return instancia unica do catalogo, ja validada
     */
    public static synchronized CatalogoMock criar() {
        if (instancia == null) {
            instancia = carregar();
        }
        return instancia;
    }

    private static CatalogoMock carregar() {
        List<Componente> registros = LeitorBaseMock.carregar();
        ResultadoValidacao validacao = new ValidadorCadastro().validar(registros);

        Map<CategoriaComponente, List<Componente>> agrupado = new EnumMap<>(CategoriaComponente.class);
        for (CategoriaComponente categoria : CategoriaComponente.values()) {
            agrupado.put(categoria, new ArrayList<>());
        }
        for (Componente componente : validacao.getRegistrosValidos()) {
            List<Componente> lista = agrupado.get(componente.getCategoria());
            if (lista != null) {
                lista.add(componente);
            }
        }

        Map<CategoriaComponente, List<Componente>> imutavel = new EnumMap<>(CategoriaComponente.class);
        for (Map.Entry<CategoriaComponente, List<Componente>> entrada : agrupado.entrySet()) {
            imutavel.put(entrada.getKey(), List.copyOf(entrada.getValue()));
        }
        return new CatalogoMock(imutavel, validacao);
    }

    /** Componentes validos de uma categoria (lista vazia quando nao ha nenhum). */
    public List<Componente> porCategoria(CategoriaComponente categoria) {
        List<Componente> lista = componentesPorCategoria.get(categoria);
        return lista == null ? List.of() : lista;
    }

    /** Todos os componentes validos, na ordem dos registros carregados. */
    public List<Componente> todos() {
        return resultadoValidacao.getRegistrosValidos();
    }

    public int quantidadeTotal() {
        return todos().size();
    }

    /** Quantidade de itens validos por categoria, para o resumo da carga. */
    public Map<CategoriaComponente, Integer> quantidadePorCategoria() {
        Map<CategoriaComponente, Integer> quantidades = new EnumMap<>(CategoriaComponente.class);
        for (Map.Entry<CategoriaComponente, List<Componente>> entrada : componentesPorCategoria.entrySet()) {
            quantidades.put(entrada.getKey(), entrada.getValue().size());
        }
        return quantidades;
    }

    /** Resultado da validacao aplicada na carga (problemas e registros descartados). */
    public ResultadoValidacao getResultadoValidacao() {
        return resultadoValidacao;
    }
}
