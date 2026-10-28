package pcbuilder.modelo;

import java.util.LinkedHashMap;
import java.util.Map;

import pcbuilder.enums.CategoriaComponente;
import pcbuilder.enums.Certificado80Plus;
import pcbuilder.enums.Modularidade;
import pcbuilder.enums.PadraoATX;

/** Fonte de alimentacao (PSU) - secao 8 do modelo de dados. */
public final class Fonte extends Componente {

    private final int potenciaTotalWatts;
    private final int potenciaTrilho12VWatts;
    private final Certificado80Plus certificacao80Plus;
    private final Double eficienciaPercentualCarga50;
    private final Modularidade modularidade;
    private final PadraoATX padraoATX;
    private final int quantidadeConectores16pin12VHPWR;
    private final int quantidadeConectoresPCIe8pin;
    private final int quantidadeConectoresPCIe6pin;
    private final int quantidadeConectoresCPU8pin;
    private final int quantidadeConectoresCPU4pin;
    private final int quantidadeConectoresSATA;
    private final Integer quantidadeConectoresMolex;
    private final int comprimentoMm;
    private final Integer alturaMm;
    private final Boolean possuiPFCAtivo;

    private Fonte(Builder construtor) {
        super(construtor);
        this.potenciaTotalWatts = construtor.potenciaTotalWatts;
        this.potenciaTrilho12VWatts = construtor.potenciaTrilho12VWatts;
        this.certificacao80Plus = construtor.certificacao80Plus;
        this.eficienciaPercentualCarga50 = construtor.eficienciaPercentualCarga50;
        this.modularidade = construtor.modularidade;
        this.padraoATX = construtor.padraoATX;
        this.quantidadeConectores16pin12VHPWR = construtor.quantidadeConectores16pin12VHPWR;
        this.quantidadeConectoresPCIe8pin = construtor.quantidadeConectoresPCIe8pin;
        this.quantidadeConectoresPCIe6pin = construtor.quantidadeConectoresPCIe6pin;
        this.quantidadeConectoresCPU8pin = construtor.quantidadeConectoresCPU8pin;
        this.quantidadeConectoresCPU4pin = construtor.quantidadeConectoresCPU4pin;
        this.quantidadeConectoresSATA = construtor.quantidadeConectoresSATA;
        this.quantidadeConectoresMolex = construtor.quantidadeConectoresMolex;
        this.comprimentoMm = construtor.comprimentoMm;
        this.alturaMm = construtor.alturaMm;
        this.possuiPFCAtivo = construtor.possuiPFCAtivo;
    }

    public static Builder builder() {
        return new Builder();
    }

    public int getPotenciaTotalWatts() {
        return potenciaTotalWatts;
    }

    public int getPotenciaTrilho12VWatts() {
        return potenciaTrilho12VWatts;
    }

    public Certificado80Plus getCertificacao80Plus() {
        return certificacao80Plus;
    }

    public Double getEficienciaPercentualCarga50() {
        return eficienciaPercentualCarga50;
    }

    public Modularidade getModularidade() {
        return modularidade;
    }

    public PadraoATX getPadraoATX() {
        return padraoATX;
    }

    public int getQuantidadeConectores16pin12VHPWR() {
        return quantidadeConectores16pin12VHPWR;
    }

    public int getQuantidadeConectoresPCIe8pin() {
        return quantidadeConectoresPCIe8pin;
    }

    public int getQuantidadeConectoresPCIe6pin() {
        return quantidadeConectoresPCIe6pin;
    }

    public int getQuantidadeConectoresCPU8pin() {
        return quantidadeConectoresCPU8pin;
    }

    public int getQuantidadeConectoresCPU4pin() {
        return quantidadeConectoresCPU4pin;
    }

    public int getQuantidadeConectoresSATA() {
        return quantidadeConectoresSATA;
    }

    public Integer getQuantidadeConectoresMolex() {
        return quantidadeConectoresMolex;
    }

    public int getComprimentoMm() {
        return comprimentoMm;
    }

    public Integer getAlturaMm() {
        return alturaMm;
    }

    public Boolean getPossuiPFCAtivo() {
        return possuiPFCAtivo;
    }

    @Override
    protected Map<String, Object> camposEspecificos() {
        Map<String, Object> campos = new LinkedHashMap<>();
        campos.put("potenciaTotalWatts", potenciaTotalWatts);
        campos.put("potenciaTrilho12VWatts", potenciaTrilho12VWatts);
        campos.put("certificacao80Plus", certificacao80Plus);
        campos.put("eficienciaPercentualCarga50", eficienciaPercentualCarga50);
        campos.put("modularidade", modularidade);
        campos.put("padraoATX", padraoATX);
        campos.put("quantidadeConectores16pin12VHPWR", quantidadeConectores16pin12VHPWR);
        campos.put("quantidadeConectoresPCIe8pin", quantidadeConectoresPCIe8pin);
        campos.put("quantidadeConectoresPCIe6pin", quantidadeConectoresPCIe6pin);
        campos.put("quantidadeConectoresCPU8pin", quantidadeConectoresCPU8pin);
        campos.put("quantidadeConectoresCPU4pin", quantidadeConectoresCPU4pin);
        campos.put("quantidadeConectoresSATA", quantidadeConectoresSATA);
        campos.put("quantidadeConectoresMolex", quantidadeConectoresMolex);
        campos.put("comprimentoMm", comprimentoMm);
        campos.put("alturaMm", alturaMm);
        campos.put("possuiPFCAtivo", possuiPFCAtivo);
        return campos;
    }

    /** Builder de {@link Fonte}; a categoria ja sai fixa como {@code FONTE}. */
    public static final class Builder extends Componente.Builder<Builder> {

        private int potenciaTotalWatts;
        private int potenciaTrilho12VWatts;
        private Certificado80Plus certificacao80Plus;
        private Double eficienciaPercentualCarga50;
        private Modularidade modularidade;
        private PadraoATX padraoATX;
        private int quantidadeConectores16pin12VHPWR;
        private int quantidadeConectoresPCIe8pin;
        private int quantidadeConectoresPCIe6pin;
        private int quantidadeConectoresCPU8pin;
        private int quantidadeConectoresCPU4pin;
        private int quantidadeConectoresSATA;
        private Integer quantidadeConectoresMolex;
        private int comprimentoMm;
        private Integer alturaMm;
        private Boolean possuiPFCAtivo;

        private Builder() {
            categoria = CategoriaComponente.FONTE;
        }

        @Override
        protected Builder eu() {
            return this;
        }

        public Builder potenciaTotalWatts(int valor) {
            this.potenciaTotalWatts = valor;
            return this;
        }

        public Builder potenciaTrilho12VWatts(int valor) {
            this.potenciaTrilho12VWatts = valor;
            return this;
        }

        public Builder certificacao80Plus(Certificado80Plus valor) {
            this.certificacao80Plus = valor;
            return this;
        }

        public Builder eficienciaPercentualCarga50(Double valor) {
            this.eficienciaPercentualCarga50 = valor;
            return this;
        }

        public Builder modularidade(Modularidade valor) {
            this.modularidade = valor;
            return this;
        }

        public Builder padraoATX(PadraoATX valor) {
            this.padraoATX = valor;
            return this;
        }

        public Builder quantidadeConectores16pin12VHPWR(int valor) {
            this.quantidadeConectores16pin12VHPWR = valor;
            return this;
        }

        public Builder quantidadeConectoresPCIe8pin(int valor) {
            this.quantidadeConectoresPCIe8pin = valor;
            return this;
        }

        public Builder quantidadeConectoresPCIe6pin(int valor) {
            this.quantidadeConectoresPCIe6pin = valor;
            return this;
        }

        public Builder quantidadeConectoresCPU8pin(int valor) {
            this.quantidadeConectoresCPU8pin = valor;
            return this;
        }

        public Builder quantidadeConectoresCPU4pin(int valor) {
            this.quantidadeConectoresCPU4pin = valor;
            return this;
        }

        public Builder quantidadeConectoresSATA(int valor) {
            this.quantidadeConectoresSATA = valor;
            return this;
        }

        public Builder quantidadeConectoresMolex(Integer valor) {
            this.quantidadeConectoresMolex = valor;
            return this;
        }

        public Builder comprimentoMm(int valor) {
            this.comprimentoMm = valor;
            return this;
        }

        public Builder alturaMm(Integer valor) {
            this.alturaMm = valor;
            return this;
        }

        public Builder possuiPFCAtivo(Boolean valor) {
            this.possuiPFCAtivo = valor;
            return this;
        }

        @Override
        public Fonte construir() {
            return new Fonte(this);
        }
    }
}
