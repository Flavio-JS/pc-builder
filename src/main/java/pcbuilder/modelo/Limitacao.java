package pcbuilder.modelo;

import java.util.Objects;

import pcbuilder.enums.SeveridadeLimitacao;

/**
 * Limitacao detectada entre duas pecas <strong>compativeis</strong> (secao 2.3 das
 * historias de usuario): a montagem encaixa, mas uma peca impede a outra de entregar
 * 100% do que promete.
 *
 * <p>E o oposto de {@code ItemOculto} (HU-003): limitacao nunca remove a peca da lista e
 * nunca impede a escolha. Ela apenas vira o marcador {@code [!]} e o bloco de aviso com
 * {@code A escolha continua valida.}.</p>
 *
 * <p>Os valores comparados ficam como texto ja formatado ({@code "8000 MT/s"},
 * {@code "80%"}) porque cada regra compara unidades diferentes; o
 * {@link #getPercentualPerda() percentual de perda} fica como numero para que o resumo
 * possa ordenar ou filtrar sem reinterpretar texto.</p>
 */
public final class Limitacao {

    private final String regraId;
    private final Componente componenteLimitante;
    private final Componente componenteAfetado;
    private final String campoComparado;
    private final String valorDoNovo;
    private final String valorSuportado;
    private final String consequencia;
    private final SeveridadeLimitacao severidade;
    private final Double percentualPerda;

    private Limitacao(Builder construtor) {
        this.regraId = Objects.requireNonNull(construtor.regraId, "regraId");
        this.componenteLimitante = Objects.requireNonNull(construtor.componenteLimitante,
                "componenteLimitante");
        this.componenteAfetado = Objects.requireNonNull(construtor.componenteAfetado,
                "componenteAfetado");
        this.campoComparado = construtor.campoComparado;
        this.valorDoNovo = construtor.valorDoNovo;
        this.valorSuportado = construtor.valorSuportado;
        this.consequencia = Objects.requireNonNull(construtor.consequencia, "consequencia");
        this.severidade = construtor.severidade == null
                ? SeveridadeLimitacao.AVISO
                : construtor.severidade;
        this.percentualPerda = construtor.percentualPerda;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Identificador da regra que gerou o aviso ({@code L01}, {@code L04}...). */
    public String getRegraId() {
        return regraId;
    }

    /** Peca ja escolhida (ou com menor capacidade) que esta segurando a outra. */
    public Componente getComponenteLimitante() {
        return componenteLimitante;
    }

    /** Peca que nao vai ser usada por completo (normalmente a que esta sendo escolhida). */
    public Componente getComponenteAfetado() {
        return componenteAfetado;
    }

    /** Campo comparado, com o nome usado no modelo de dados. */
    public String getCampoComparado() {
        return campoComparado;
    }

    /** Valor que a peca nova pede, ja formatado com a unidade. */
    public String getValorDoNovo() {
        return valorDoNovo;
    }

    /** Valor que a peca limitante consegue entregar, ja formatado com a unidade. */
    public String getValorSuportado() {
        return valorSuportado;
    }

    /** Consequencia pratica em linguagem simples (sem jargao nao explicado). */
    public String getConsequencia() {
        return consequencia;
    }

    public SeveridadeLimitacao getSeveridade() {
        return severidade;
    }

    /**
     * Perda estimada em pontos percentuais ({@code 25.0} = ~25%).
     *
     * @return a perda, ou {@code null} quando a regra nao tem como quantificar
     */
    public Double getPercentualPerda() {
        return percentualPerda;
    }

    @Override
    public String toString() {
        return regraId + ": " + componenteLimitante + " limita " + componenteAfetado;
    }

    /** Builder da {@link Limitacao}; a severidade padrao e {@code AVISO}. */
    public static final class Builder {

        private String regraId;
        private Componente componenteLimitante;
        private Componente componenteAfetado;
        private String campoComparado;
        private String valorDoNovo;
        private String valorSuportado;
        private String consequencia;
        private SeveridadeLimitacao severidade;
        private Double percentualPerda;

        private Builder() {
            // builder criado apenas por Limitacao.builder()
        }

        public Builder regra(String valor) {
            this.regraId = valor;
            return this;
        }

        public Builder limitante(Componente valor) {
            this.componenteLimitante = valor;
            return this;
        }

        public Builder afetado(Componente valor) {
            this.componenteAfetado = valor;
            return this;
        }

        public Builder campo(String valor) {
            this.campoComparado = valor;
            return this;
        }

        public Builder valorDoNovo(String valor) {
            this.valorDoNovo = valor;
            return this;
        }

        public Builder valorSuportado(String valor) {
            this.valorSuportado = valor;
            return this;
        }

        public Builder consequencia(String valor) {
            this.consequencia = valor;
            return this;
        }

        public Builder severidade(SeveridadeLimitacao valor) {
            this.severidade = valor;
            return this;
        }

        public Builder percentualPerda(Double valor) {
            this.percentualPerda = valor;
            return this;
        }

        /** Cria a limitacao imutavel. */
        public Limitacao construir() {
            return new Limitacao(this);
        }
    }
}
