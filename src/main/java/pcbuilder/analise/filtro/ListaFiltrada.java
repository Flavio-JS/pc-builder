package pcbuilder.analise.filtro;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import pcbuilder.enums.CategoriaComponente;
import pcbuilder.modelo.Componente;

/**
 * Resultado do filtro de uma etapa: o que continua na lista e o que foi ocultado.
 *
 * <p>Os itens compativeis mantem a ordem do catalogo (menor preco primeiro). Os ocultos
 * guardam o motivo para o
 * comando {@code [M] Ver ocultos} e para a mensagem de "nenhum compativel", que usa o
 * {@link #motivoPredominante() motivo mais frequente} para explicar ao usuario o que
 * esta travando aquela etapa.</p>
 */
public final class ListaFiltrada {

    private final CategoriaComponente categoria;
    private final List<Componente> compativeis;
    private final List<ItemOculto> ocultos;
    private final int totalOriginal;

    /**
     * @param categoria categoria que estava sendo listada
     * @param compativeis itens que encaixam na configuracao, na ordem da base
     * @param ocultos itens removidos, cada um com o motivo
     */
    public ListaFiltrada(CategoriaComponente categoria, List<Componente> compativeis,
            List<ItemOculto> ocultos) {
        this(categoria, compativeis, ocultos,
                Objects.requireNonNull(compativeis, "compativeis").size()
                        + Objects.requireNonNull(ocultos, "ocultos").size());
    }

    /**
     * @param categoria categoria que estava sendo listada
     * @param compativeis itens que encaixam na configuracao, na ordem do catalogo
     * @param ocultos itens removidos, cada um com o motivo
     * @param totalOriginal quantidade de itens do catalogo antes do filtro (sem contar
     *        variantes sinteticas, como os kits {@code 2x} da memoria)
     */
    public ListaFiltrada(CategoriaComponente categoria, List<Componente> compativeis,
            List<ItemOculto> ocultos, int totalOriginal) {
        this.categoria = Objects.requireNonNull(categoria, "categoria");
        this.compativeis = List.copyOf(compativeis);
        this.ocultos = List.copyOf(ocultos);
        this.totalOriginal = totalOriginal;
    }

    public CategoriaComponente getCategoria() {
        return categoria;
    }

    /** Itens que continuam selecionaveis. */
    public List<Componente> getCompativeis() {
        return compativeis;
    }

    /** Itens removidos, com o motivo de cada um. */
    public List<ItemOculto> getOcultos() {
        return ocultos;
    }

    /** Quantidade de itens que a categoria tinha antes do filtro. */
    public int getTotalOriginal() {
        return totalOriginal;
    }

    /** Quantidade de itens ocultados por incompatibilidade. */
    public int quantidadeOcultos() {
        return ocultos.size();
    }

    /** {@code true} quando nenhum item da categoria encaixa na configuracao. */
    public boolean estaVazia() {
        return compativeis.isEmpty();
    }

    /**
     * Motivo que mais aparece entre os itens ocultos.
     *
     * <p>E o que a etapa usa para explicar uma lista vazia ("o que esta impedindo").
     * Empates ficam com o primeiro motivo encontrado, na ordem da base.</p>
     *
     * @return o motivo predominante, ou vazio quando nada foi ocultado
     */
    public Optional<String> motivoPredominante() {
        Map<String, Integer> contagem = new LinkedHashMap<>();
        for (ItemOculto oculto : ocultos) {
            contagem.merge(oculto.getMotivo(), 1, Integer::sum);
        }
        String predominante = null;
        int maiorContagem = 0;
        for (Map.Entry<String, Integer> entrada : contagem.entrySet()) {
            if (entrada.getValue() > maiorContagem) {
                maiorContagem = entrada.getValue();
                predominante = entrada.getKey();
            }
        }
        return Optional.ofNullable(predominante);
    }
}
