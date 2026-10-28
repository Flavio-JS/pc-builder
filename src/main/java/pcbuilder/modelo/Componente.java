package pcbuilder.modelo;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import pcbuilder.enums.CategoriaComponente;

/**
 * Superclasse de todos os componentes do catalogo (secao 2 do modelo de dados).
 *
 * <p>Os objetos sao <strong>imutaveis</strong>: os campos recebem valor apenas no
 * construtor, que copia os dados do {@link Builder}. Depois de criado, o
 * componente nunca muda - trocar uma peca significa criar outro objeto.</p>
 *
 * <p>{@link #campos()} devolve a "ficha" do registro (nome do campo -&gt; valor).
 * Ela existe para que a validacao do cadastro (HU-001) e os resumos possam
 * percorrer o registro sem depender de qual subclasse esta em maos. Cada nome de
 * chave e exatamente o nome do campo no modelo de dados.</p>
 */
public abstract class Componente {

    private final String id;
    private final String modelo;
    private final String fabricante;
    private final CategoriaComponente categoria;
    private final Integer consumoEstimadoWatts;
    private final BigDecimal precoMedioBRL;
    private final Integer garantiaMeses;
    private final Integer anoLancamento;
    private final String observacoes;

    /**
     * Construtor usado apenas pelas subclasses, que repassam o proprio builder.
     *
     * @param construtor builder ja preenchido pelo {@code construir()} da subclasse
     */
    protected Componente(Builder<?> construtor) {
        this.id = construtor.id;
        this.modelo = construtor.modelo;
        this.fabricante = construtor.fabricante;
        this.categoria = construtor.categoria;
        this.consumoEstimadoWatts = construtor.consumoEstimadoWatts;
        this.precoMedioBRL = construtor.precoMedioBRL;
        this.garantiaMeses = construtor.garantiaMeses;
        this.anoLancamento = construtor.anoLancamento;
        this.observacoes = construtor.observacoes;
    }

    public String getId() {
        return id;
    }

    public String getModelo() {
        return modelo;
    }

    public String getFabricante() {
        return fabricante;
    }

    public CategoriaComponente getCategoria() {
        return categoria;
    }

    public Integer getConsumoEstimadoWatts() {
        return consumoEstimadoWatts;
    }

    public BigDecimal getPrecoMedioBRL() {
        return precoMedioBRL;
    }

    public Integer getGarantiaMeses() {
        return garantiaMeses;
    }

    public Integer getAnoLancamento() {
        return anoLancamento;
    }

    public String getObservacoes() {
        return observacoes;
    }

    /**
     * Ficha completa do registro: campos comuns (secao 2) + campos da subclasse.
     *
     * @return mapa ordenado com todos os campos; valores ausentes ficam como {@code null}
     */
    public final Map<String, Object> campos() {
        Map<String, Object> campos = new LinkedHashMap<>();
        campos.put("id", id);
        campos.put("modelo", modelo);
        campos.put("fabricante", fabricante);
        campos.put("categoria", categoria);
        campos.put("consumoEstimadoWatts", consumoEstimadoWatts);
        campos.put("precoMedioBRL", precoMedioBRL);
        campos.put("garantiaMeses", garantiaMeses);
        campos.put("anoLancamento", anoLancamento);
        campos.put("observacoes", observacoes);
        campos.putAll(camposEspecificos());
        return campos;
    }

    /** Campos proprios da subclasse, na ordem das secoes 3 a 8 do modelo de dados. */
    protected abstract Map<String, Object> camposEspecificos();

    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (!(outro instanceof Componente componente)) {
            return false;
        }
        return Objects.equals(id, componente.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return modelo + " (" + fabricante + ")";
    }

    /**
     * Builder comum a todos os componentes.
     *
     * <p>Cada subclasse cria o proprio builder ({@code Processador.builder()},
     * {@code Fonte.builder()} etc.) e acrescenta os setters dos campos
     * especificos. A categoria nunca e digitada: o builder de cada subclasse fixa
     * a sua no construtor, por isso dois componentes do mesmo tipo nao podem sair
     * com categorias diferentes.</p>
     *
     * @param <T> tipo do builder concreto (devolvido pelos setters, para encadear)
     */
    public abstract static class Builder<T extends Builder<T>> {

        private String id;
        private String modelo;
        private String fabricante;
        /** Fixado pelo builder da subclasse; nao existe setter publico. */
        protected CategoriaComponente categoria;
        private Integer consumoEstimadoWatts;
        private BigDecimal precoMedioBRL;
        private Integer garantiaMeses;
        private Integer anoLancamento;
        private String observacoes;

        /** Devolve o proprio builder concreto (padrao "self type"). */
        protected abstract T eu();

        public T id(String valor) {
            this.id = valor;
            return eu();
        }

        public T modelo(String valor) {
            this.modelo = valor;
            return eu();
        }

        public T fabricante(String valor) {
            this.fabricante = valor;
            return eu();
        }

        public T consumoEstimadoWatts(Integer valor) {
            this.consumoEstimadoWatts = valor;
            return eu();
        }

        public T precoMedioBRL(BigDecimal valor) {
            this.precoMedioBRL = valor;
            return eu();
        }

        /** Atalho para os mocks, que declaram o preco como texto ("2199.00"). */
        public T precoMedioBRL(String valor) {
            this.precoMedioBRL = valor == null ? null : new BigDecimal(valor);
            return eu();
        }

        public T garantiaMeses(Integer valor) {
            this.garantiaMeses = valor;
            return eu();
        }

        public T anoLancamento(Integer valor) {
            this.anoLancamento = valor;
            return eu();
        }

        public T observacoes(String valor) {
            this.observacoes = valor;
            return eu();
        }

        /** Cria o componente imutavel a partir dos valores acumulados. */
        public abstract Componente construir();
    }
}
