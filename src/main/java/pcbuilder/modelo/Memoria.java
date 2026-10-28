package pcbuilder.modelo;

import java.util.LinkedHashMap;
import java.util.Map;

import pcbuilder.enums.CategoriaComponente;
import pcbuilder.enums.TipoMemoria;

/** Memoria RAM - secao 5 do modelo de dados. */
public final class Memoria extends Componente {

    private final TipoMemoria tipo;
    private final int capacidadePorPenteGB;
    private final int quantidadePentes;
    private final int frequenciaMTs;
    private final Integer frequenciaJEDECBaseMTs;
    private final int latenciaCAS;
    private final Double tensaoVolts;
    private final boolean perfilEXPO;
    private final boolean perfilXMP;
    private final boolean possuiECC;
    private final Integer alturaMm;

    private Memoria(Builder construtor) {
        super(construtor);
        this.tipo = construtor.tipo;
        this.capacidadePorPenteGB = construtor.capacidadePorPenteGB;
        this.quantidadePentes = construtor.quantidadePentes;
        this.frequenciaMTs = construtor.frequenciaMTs;
        this.frequenciaJEDECBaseMTs = construtor.frequenciaJEDECBaseMTs;
        this.latenciaCAS = construtor.latenciaCAS;
        this.tensaoVolts = construtor.tensaoVolts;
        this.perfilEXPO = construtor.perfilEXPO;
        this.perfilXMP = construtor.perfilXMP;
        this.possuiECC = construtor.possuiECC;
        this.alturaMm = construtor.alturaMm;
    }

    public static Builder builder() {
        return new Builder();
    }

    public TipoMemoria getTipo() {
        return tipo;
    }

    public int getCapacidadePorPenteGB() {
        return capacidadePorPenteGB;
    }

    public int getQuantidadePentes() {
        return quantidadePentes;
    }

    public int getFrequenciaMTs() {
        return frequenciaMTs;
    }

    public Integer getFrequenciaJEDECBaseMTs() {
        return frequenciaJEDECBaseMTs;
    }

    public int getLatenciaCAS() {
        return latenciaCAS;
    }

    public Double getTensaoVolts() {
        return tensaoVolts;
    }

    public boolean isPerfilEXPO() {
        return perfilEXPO;
    }

    public boolean isPerfilXMP() {
        return perfilXMP;
    }

    public boolean isPossuiECC() {
        return possuiECC;
    }

    public Integer getAlturaMm() {
        return alturaMm;
    }

    @Override
    protected Map<String, Object> camposEspecificos() {
        Map<String, Object> campos = new LinkedHashMap<>();
        campos.put("tipo", tipo);
        campos.put("capacidadePorPenteGB", capacidadePorPenteGB);
        campos.put("quantidadePentes", quantidadePentes);
        campos.put("frequenciaMTs", frequenciaMTs);
        campos.put("frequenciaJEDECBaseMTs", frequenciaJEDECBaseMTs);
        campos.put("latenciaCAS", latenciaCAS);
        campos.put("tensaoVolts", tensaoVolts);
        campos.put("perfilEXPO", perfilEXPO);
        campos.put("perfilXMP", perfilXMP);
        campos.put("possuiECC", possuiECC);
        campos.put("alturaMm", alturaMm);
        return campos;
    }

    /** Builder de {@link Memoria}; a categoria ja sai fixa como {@code MEMORIA}. */
    public static final class Builder extends Componente.Builder<Builder> {

        private TipoMemoria tipo;
        private int capacidadePorPenteGB;
        private int quantidadePentes;
        private int frequenciaMTs;
        private Integer frequenciaJEDECBaseMTs;
        private int latenciaCAS;
        private Double tensaoVolts;
        private boolean perfilEXPO;
        private boolean perfilXMP;
        private boolean possuiECC;
        private Integer alturaMm;

        private Builder() {
            categoria = CategoriaComponente.MEMORIA;
        }

        @Override
        protected Builder eu() {
            return this;
        }

        public Builder tipo(TipoMemoria valor) {
            this.tipo = valor;
            return this;
        }

        public Builder capacidadePorPenteGB(int valor) {
            this.capacidadePorPenteGB = valor;
            return this;
        }

        public Builder quantidadePentes(int valor) {
            this.quantidadePentes = valor;
            return this;
        }

        public Builder frequenciaMTs(int valor) {
            this.frequenciaMTs = valor;
            return this;
        }

        public Builder frequenciaJEDECBaseMTs(Integer valor) {
            this.frequenciaJEDECBaseMTs = valor;
            return this;
        }

        public Builder latenciaCAS(int valor) {
            this.latenciaCAS = valor;
            return this;
        }

        public Builder tensaoVolts(Double valor) {
            this.tensaoVolts = valor;
            return this;
        }

        public Builder perfilEXPO(boolean valor) {
            this.perfilEXPO = valor;
            return this;
        }

        public Builder perfilXMP(boolean valor) {
            this.perfilXMP = valor;
            return this;
        }

        public Builder possuiECC(boolean valor) {
            this.possuiECC = valor;
            return this;
        }

        public Builder alturaMm(Integer valor) {
            this.alturaMm = valor;
            return this;
        }

        @Override
        public Memoria construir() {
            return new Memoria(this);
        }
    }
}
