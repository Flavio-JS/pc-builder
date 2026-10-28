package pcbuilder.modelo;

import java.util.LinkedHashMap;
import java.util.Map;

import pcbuilder.enums.CategoriaComponente;
import pcbuilder.enums.FabricanteChip;
import pcbuilder.enums.Resolucao;
import pcbuilder.enums.TipoMemoriaVRAM;

/** Placa de video (GPU) - secao 6 do modelo de dados. */
public final class PlacaVideo extends Componente {

    private final FabricanteChip fabricanteChip;
    private final int memoriaVRAMGB;
    private final TipoMemoriaVRAM tipoMemoriaVRAM;
    private final int versaoPCIe;
    private final int pistasPCIe;
    private final int comprimentoMm;
    private final int espessuraSlots;
    private final int tdpWatts;
    private final Integer picoTransienteWatts;
    private final Integer fonteRecomendadaWatts;
    private final int quantidadeConectores16pin;
    private final int quantidadeConectores8pin;
    private final int quantidadeConectores6pin;
    private final Resolucao resolucaoAlvo;
    private final int indiceDesempenhoGPU;
    private final Boolean possuiRayTracing;

    private PlacaVideo(Builder construtor) {
        super(construtor);
        this.fabricanteChip = construtor.fabricanteChip;
        this.memoriaVRAMGB = construtor.memoriaVRAMGB;
        this.tipoMemoriaVRAM = construtor.tipoMemoriaVRAM;
        this.versaoPCIe = construtor.versaoPCIe;
        this.pistasPCIe = construtor.pistasPCIe;
        this.comprimentoMm = construtor.comprimentoMm;
        this.espessuraSlots = construtor.espessuraSlots;
        this.tdpWatts = construtor.tdpWatts;
        this.picoTransienteWatts = construtor.picoTransienteWatts;
        this.fonteRecomendadaWatts = construtor.fonteRecomendadaWatts;
        this.quantidadeConectores16pin = construtor.quantidadeConectores16pin;
        this.quantidadeConectores8pin = construtor.quantidadeConectores8pin;
        this.quantidadeConectores6pin = construtor.quantidadeConectores6pin;
        this.resolucaoAlvo = construtor.resolucaoAlvo;
        this.indiceDesempenhoGPU = construtor.indiceDesempenhoGPU;
        this.possuiRayTracing = construtor.possuiRayTracing;
    }

    public static Builder builder() {
        return new Builder();
    }

    public FabricanteChip getFabricanteChip() {
        return fabricanteChip;
    }

    public int getMemoriaVRAMGB() {
        return memoriaVRAMGB;
    }

    public TipoMemoriaVRAM getTipoMemoriaVRAM() {
        return tipoMemoriaVRAM;
    }

    public int getVersaoPCIe() {
        return versaoPCIe;
    }

    public int getPistasPCIe() {
        return pistasPCIe;
    }

    public int getComprimentoMm() {
        return comprimentoMm;
    }

    public int getEspessuraSlots() {
        return espessuraSlots;
    }

    public int getTdpWatts() {
        return tdpWatts;
    }

    public Integer getPicoTransienteWatts() {
        return picoTransienteWatts;
    }

    public Integer getFonteRecomendadaWatts() {
        return fonteRecomendadaWatts;
    }

    public int getQuantidadeConectores16pin() {
        return quantidadeConectores16pin;
    }

    public int getQuantidadeConectores8pin() {
        return quantidadeConectores8pin;
    }

    public int getQuantidadeConectores6pin() {
        return quantidadeConectores6pin;
    }

    public Resolucao getResolucaoAlvo() {
        return resolucaoAlvo;
    }

    public int getIndiceDesempenhoGPU() {
        return indiceDesempenhoGPU;
    }

    public Boolean getPossuiRayTracing() {
        return possuiRayTracing;
    }

    @Override
    protected Map<String, Object> camposEspecificos() {
        Map<String, Object> campos = new LinkedHashMap<>();
        campos.put("fabricanteChip", fabricanteChip);
        campos.put("memoriaVRAMGB", memoriaVRAMGB);
        campos.put("tipoMemoriaVRAM", tipoMemoriaVRAM);
        campos.put("versaoPCIe", versaoPCIe);
        campos.put("pistasPCIe", pistasPCIe);
        campos.put("comprimentoMm", comprimentoMm);
        campos.put("espessuraSlots", espessuraSlots);
        campos.put("tdpWatts", tdpWatts);
        campos.put("picoTransienteWatts", picoTransienteWatts);
        campos.put("fonteRecomendadaWatts", fonteRecomendadaWatts);
        campos.put("quantidadeConectores16pin", quantidadeConectores16pin);
        campos.put("quantidadeConectores8pin", quantidadeConectores8pin);
        campos.put("quantidadeConectores6pin", quantidadeConectores6pin);
        campos.put("resolucaoAlvo", resolucaoAlvo);
        campos.put("indiceDesempenhoGPU", indiceDesempenhoGPU);
        campos.put("possuiRayTracing", possuiRayTracing);
        return campos;
    }

    /** Builder de {@link PlacaVideo}; a categoria ja sai fixa como {@code PLACA_VIDEO}. */
    public static final class Builder extends Componente.Builder<Builder> {

        private FabricanteChip fabricanteChip;
        private int memoriaVRAMGB;
        private TipoMemoriaVRAM tipoMemoriaVRAM;
        private int versaoPCIe;
        private int pistasPCIe;
        private int comprimentoMm;
        private int espessuraSlots;
        private int tdpWatts;
        private Integer picoTransienteWatts;
        private Integer fonteRecomendadaWatts;
        private int quantidadeConectores16pin;
        private int quantidadeConectores8pin;
        private int quantidadeConectores6pin;
        private Resolucao resolucaoAlvo;
        private int indiceDesempenhoGPU;
        private Boolean possuiRayTracing;

        private Builder() {
            categoria = CategoriaComponente.PLACA_VIDEO;
        }

        @Override
        protected Builder eu() {
            return this;
        }

        public Builder fabricanteChip(FabricanteChip valor) {
            this.fabricanteChip = valor;
            return this;
        }

        public Builder memoriaVRAMGB(int valor) {
            this.memoriaVRAMGB = valor;
            return this;
        }

        public Builder tipoMemoriaVRAM(TipoMemoriaVRAM valor) {
            this.tipoMemoriaVRAM = valor;
            return this;
        }

        public Builder versaoPCIe(int valor) {
            this.versaoPCIe = valor;
            return this;
        }

        public Builder pistasPCIe(int valor) {
            this.pistasPCIe = valor;
            return this;
        }

        public Builder comprimentoMm(int valor) {
            this.comprimentoMm = valor;
            return this;
        }

        public Builder espessuraSlots(int valor) {
            this.espessuraSlots = valor;
            return this;
        }

        public Builder tdpWatts(int valor) {
            this.tdpWatts = valor;
            return this;
        }

        public Builder picoTransienteWatts(Integer valor) {
            this.picoTransienteWatts = valor;
            return this;
        }

        public Builder fonteRecomendadaWatts(Integer valor) {
            this.fonteRecomendadaWatts = valor;
            return this;
        }

        public Builder quantidadeConectores16pin(int valor) {
            this.quantidadeConectores16pin = valor;
            return this;
        }

        public Builder quantidadeConectores8pin(int valor) {
            this.quantidadeConectores8pin = valor;
            return this;
        }

        public Builder quantidadeConectores6pin(int valor) {
            this.quantidadeConectores6pin = valor;
            return this;
        }

        public Builder resolucaoAlvo(Resolucao valor) {
            this.resolucaoAlvo = valor;
            return this;
        }

        public Builder indiceDesempenhoGPU(int valor) {
            this.indiceDesempenhoGPU = valor;
            return this;
        }

        public Builder possuiRayTracing(Boolean valor) {
            this.possuiRayTracing = valor;
            return this;
        }

        @Override
        public PlacaVideo construir() {
            return new PlacaVideo(this);
        }
    }
}
