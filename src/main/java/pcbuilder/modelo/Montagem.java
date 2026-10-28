package pcbuilder.modelo;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import pcbuilder.enums.CategoriaComponente;

/**
 * Montagem em andamento: no maximo uma peca por categoria (secao 2.1 das historias
 * de usuario).
 *
 * <p>E o contenedor que sobrevive ao avanco de etapa - a etapa 2 nao recebe a CPU
 * "por parametro", ela consulta a montagem. Trocar uma peca significa
 * {@link #adicionar(Componente)} de novo na mesma categoria, o que descarta a
 * anterior.</p>
 *
 * <p>Os componentes vem do catalogo e sao imutaveis; a montagem so guarda as
 * referencias escolhidas. Nenhuma peca e escolhida automaticamente: enquanto o
 * usuario nao escolher, {@link #estaVazia()} devolve {@code true}.</p>
 */
public final class Montagem {

    private final Map<CategoriaComponente, Componente> pecas =
            new EnumMap<>(CategoriaComponente.class);

    /** Acrescenta (ou substitui) a peca da categoria dela. */
    public void adicionar(Componente componente) {
        Objects.requireNonNull(componente, "componente");
        pecas.put(componente.getCategoria(), componente);
    }

    /** Peca escolhida para a categoria, quando ja existe. */
    public Optional<Componente> obter(CategoriaComponente categoria) {
        return Optional.ofNullable(pecas.get(categoria));
    }

    /** {@code true} enquanto nenhuma peca foi escolhida. */
    public boolean estaVazia() {
        return pecas.isEmpty();
    }

    /** Quantidade de categorias ja preenchidas. */
    public int quantidadePecas() {
        return pecas.size();
    }

    /** Visao somente leitura da montagem, na ordem das categorias. */
    public Map<CategoriaComponente, Componente> pecas() {
        Map<CategoriaComponente, Componente> copia = new EnumMap<>(CategoriaComponente.class);
        copia.putAll(pecas);
        return Collections.unmodifiableMap(copia);
    }

    /**
     * Soma dos precos das pecas escolhidas, em {@link BigDecimal}.
     *
     * <p>Peca sem preco no catalogo nao entra na soma (o resumo da HU-010 mostra
     * {@code R$ --} e avisa que o total esta incompleto).</p>
     *
     * @return total das pecas presentes; {@code ZERO} quando a montagem esta vazia
     */
    public BigDecimal custoTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (Componente componente : pecas.values()) {
            if (componente.getPrecoMedioBRL() != null) {
                total = total.add(componente.getPrecoMedioBRL());
            }
        }
        return total;
    }

    /** Descarta todas as pecas (montagem cancelada ou reiniciada). */
    public void limpar() {
        pecas.clear();
    }
}
