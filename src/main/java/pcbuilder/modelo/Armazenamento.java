package pcbuilder.modelo;

import java.util.LinkedHashMap;
import java.util.Map;

import pcbuilder.enums.CategoriaComponente;
import pcbuilder.enums.FormatoArmazenamento;
import pcbuilder.enums.InterfaceArmazenamento;
import pcbuilder.enums.TipoMidia;

/** Armazenamento (SSD ou HD) - secao 7 do modelo de dados. */
public final class Armazenamento extends Componente {

    private final TipoMidia tipoMidia;
    /**
     * No modelo de dados o campo se chama {@code interface}; em Java esse nome e
     * palavra reservada, por isso o atributo ganhou o sufixo
     * ({@code interfaceArmazenamento}) e a chave da ficha continua "interface".
     */
    private final InterfaceArmazenamento interfaceArmazenamento;
    private final FormatoArmazenamento formato;
    private final int capacidadeGB;
    private final Integer versaoPCIe;
    private final Integer pistasPCIe;
    private final Integer velocidadeSATAGbps;
    private final int leituraSequencialMBs;
    private final int escritaSequencialMBs;
    private final Integer tbw;
    private final Boolean possuiDRAMCache;
    private final Double consumoMedioWatts;
    private final Double consumoPicoWatts;

    private Armazenamento(Builder construtor) {
        super(construtor);
        this.tipoMidia = construtor.tipoMidia;
        this.interfaceArmazenamento = construtor.interfaceArmazenamento;
        this.formato = construtor.formato;
        this.capacidadeGB = construtor.capacidadeGB;
        this.versaoPCIe = construtor.versaoPCIe;
        this.pistasPCIe = construtor.pistasPCIe;
        this.velocidadeSATAGbps = construtor.velocidadeSATAGbps;
        this.leituraSequencialMBs = construtor.leituraSequencialMBs;
        this.escritaSequencialMBs = construtor.escritaSequencialMBs;
        this.tbw = construtor.tbw;
        this.possuiDRAMCache = construtor.possuiDRAMCache;
        this.consumoMedioWatts = construtor.consumoMedioWatts;
        this.consumoPicoWatts = construtor.consumoPicoWatts;
    }

    public static Builder builder() {
        return new Builder();
    }

    public TipoMidia getTipoMidia() {
        return tipoMidia;
    }

    public InterfaceArmazenamento getInterfaceArmazenamento() {
        return interfaceArmazenamento;
    }

    public FormatoArmazenamento getFormato() {
        return formato;
    }

    public int getCapacidadeGB() {
        return capacidadeGB;
    }

    public Integer getVersaoPCIe() {
        return versaoPCIe;
    }

    public Integer getPistasPCIe() {
        return pistasPCIe;
    }

    public Integer getVelocidadeSATAGbps() {
        return velocidadeSATAGbps;
    }

    public int getLeituraSequencialMBs() {
        return leituraSequencialMBs;
    }

    public int getEscritaSequencialMBs() {
        return escritaSequencialMBs;
    }

    public Integer getTbw() {
        return tbw;
    }

    public Boolean getPossuiDRAMCache() {
        return possuiDRAMCache;
    }

    public Double getConsumoMedioWatts() {
        return consumoMedioWatts;
    }

    public Double getConsumoPicoWatts() {
        return consumoPicoWatts;
    }

    @Override
    protected Map<String, Object> camposEspecificos() {
        Map<String, Object> campos = new LinkedHashMap<>();
        campos.put("tipoMidia", tipoMidia);
        campos.put("interface", interfaceArmazenamento);
        campos.put("formato", formato);
        campos.put("capacidadeGB", capacidadeGB);
        campos.put("versaoPCIe", versaoPCIe);
        campos.put("pistasPCIe", pistasPCIe);
        campos.put("velocidadeSATAGbps", velocidadeSATAGbps);
        campos.put("leituraSequencialMBs", leituraSequencialMBs);
        campos.put("escritaSequencialMBs", escritaSequencialMBs);
        campos.put("tbw", tbw);
        campos.put("possuiDRAMCache", possuiDRAMCache);
        campos.put("consumoMedioWatts", consumoMedioWatts);
        campos.put("consumoPicoWatts", consumoPicoWatts);
        return campos;
    }

    /** Builder de {@link Armazenamento}; a categoria ja sai fixa como {@code ARMAZENAMENTO}. */
    public static final class Builder extends Componente.Builder<Builder> {

        private TipoMidia tipoMidia;
        private InterfaceArmazenamento interfaceArmazenamento;
        private FormatoArmazenamento formato;
        private int capacidadeGB;
        private Integer versaoPCIe;
        private Integer pistasPCIe;
        private Integer velocidadeSATAGbps;
        private int leituraSequencialMBs;
        private int escritaSequencialMBs;
        private Integer tbw;
        private Boolean possuiDRAMCache;
        private Double consumoMedioWatts;
        private Double consumoPicoWatts;

        private Builder() {
            categoria = CategoriaComponente.ARMAZENAMENTO;
        }

        @Override
        protected Builder eu() {
            return this;
        }

        public Builder tipoMidia(TipoMidia valor) {
            this.tipoMidia = valor;
            return this;
        }

        public Builder interfaceArmazenamento(InterfaceArmazenamento valor) {
            this.interfaceArmazenamento = valor;
            return this;
        }

        public Builder formato(FormatoArmazenamento valor) {
            this.formato = valor;
            return this;
        }

        public Builder capacidadeGB(int valor) {
            this.capacidadeGB = valor;
            return this;
        }

        public Builder versaoPCIe(Integer valor) {
            this.versaoPCIe = valor;
            return this;
        }

        public Builder pistasPCIe(Integer valor) {
            this.pistasPCIe = valor;
            return this;
        }

        public Builder velocidadeSATAGbps(Integer valor) {
            this.velocidadeSATAGbps = valor;
            return this;
        }

        public Builder leituraSequencialMBs(int valor) {
            this.leituraSequencialMBs = valor;
            return this;
        }

        public Builder escritaSequencialMBs(int valor) {
            this.escritaSequencialMBs = valor;
            return this;
        }

        public Builder tbw(Integer valor) {
            this.tbw = valor;
            return this;
        }

        public Builder possuiDRAMCache(Boolean valor) {
            this.possuiDRAMCache = valor;
            return this;
        }

        public Builder consumoMedioWatts(Double valor) {
            this.consumoMedioWatts = valor;
            return this;
        }

        public Builder consumoPicoWatts(Double valor) {
            this.consumoPicoWatts = valor;
            return this;
        }

        @Override
        public Armazenamento construir() {
            return new Armazenamento(this);
        }
    }
}
