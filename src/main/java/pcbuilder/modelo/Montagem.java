package pcbuilder.modelo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
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
 *
 * <p>A partir da HU-004 a montagem tambem acumula as {@link Limitacao limitacoes} ja
 * aceitas pelo usuario, para que o resumo final (HU-010) possa repete-las. Os avisos
 * nao alteram as pecas nem o resultado do filtro.</p>
 */
public final class Montagem {

    private final Map<CategoriaComponente, Componente> pecas =
            new EnumMap<>(CategoriaComponente.class);
    private final List<Limitacao> limitacoes = new ArrayList<>();

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

    /**
     * Remove a peca da categoria (etapa opcional) e descarta os avisos ligados a ela.
     *
     * @param categoria categoria da peca a remover
     * @return a peca removida, quando existia
     */
    public Optional<Componente> remover(CategoriaComponente categoria) {
        Objects.requireNonNull(categoria, "categoria");
        Componente removida = pecas.remove(categoria);
        limparLimitacoesDe(categoria);
        return Optional.ofNullable(removida);
    }

    /**
     * Copia das pecas e dos avisos acumulados (HU-011).
     *
     * <p>Usada para desfazer uma troca que invalidou outra etapa quando o usuario
     * escolhe {@code [0] Voltar}: o resumo volta exatamente ao estado anterior.</p>
     *
     * @return copia independente da montagem
     */
    public Montagem copia() {
        Montagem copia = new Montagem();
        copia.pecas.putAll(pecas);
        copia.limitacoes.addAll(limitacoes);
        return copia;
    }

    /**
     * Restaura pecas e avisos a partir de uma copia (HU-011).
     *
     * @param copia estado anterior obtido com {@link #copia()}
     */
    public void restaurar(Montagem copia) {
        Objects.requireNonNull(copia, "copia");
        pecas.clear();
        pecas.putAll(copia.pecas);
        limitacoes.clear();
        limitacoes.addAll(copia.limitacoes);
    }

    /**
     * Registra os avisos aceitos pelo usuario (HU-004).
     *
     * @param novas limitacoes a acumular, na ordem em que foram exibidas
     */
    public void registrarLimitacoes(Collection<Limitacao> novas) {
        Objects.requireNonNull(novas, "novas");
        limitacoes.addAll(novas);
    }

    /** Avisos acumulados, na ordem em que foram aceitos. */
    public List<Limitacao> getLimitacoes() {
        return List.copyOf(limitacoes);
    }

    /**
     * Descarta os avisos ligados a uma categoria.
     *
     * <p>Usado ao trocar uma peca (HU-011): os avisos da peca substituida deixam de
     * valer. O aviso sai tanto quando a peca era a limitante quanto quando era a afetada.</p>
     *
     * @param categoria categoria da peca substituida
     */
    public void limparLimitacoesDe(CategoriaComponente categoria) {
        Objects.requireNonNull(categoria, "categoria");
        limitacoes.removeIf(limitacao ->
                limitacao.getComponenteLimitante().getCategoria() == categoria
                        || limitacao.getComponenteAfetado().getCategoria() == categoria);
    }

    /** Descarta os avisos acumulados. */
    public void limparLimitacoes() {
        limitacoes.clear();
    }

    /** Descarta todas as pecas e todos os avisos (montagem cancelada ou reiniciada). */
    public void limpar() {
        pecas.clear();
        limitacoes.clear();
    }
}
