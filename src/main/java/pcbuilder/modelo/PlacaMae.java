package pcbuilder.modelo;

import java.util.LinkedHashMap;
import java.util.Map;

import pcbuilder.enums.CategoriaComponente;
import pcbuilder.enums.Chipset;
import pcbuilder.enums.FormatoPlacaMae;
import pcbuilder.enums.Soquete;
import pcbuilder.enums.TipoMemoria;

/** Placa-mae - secao 4 do modelo de dados. */
public final class PlacaMae extends Componente {

    private final Soquete soqueteCPU;
    private final Chipset chipset;
    private final FormatoPlacaMae formato;
    private final int comprimentoMm;
    private final int larguraMm;
    private final boolean suportaAtualizacaoBIOSSemCPU;
    private final int vrmFasesCPU;
    private final int vrmCorrentePorFaseA;
    private final Boolean vrmPossuiDissipador;
    private final int quantidadeSlotsMemoria;
    private final TipoMemoria tipoMemoria;
    private final int canaisMemoria;
    private final int capacidadeMaximaMemoriaGB;
    private final int frequenciaMemoriaMaximaMTs;
    private final boolean suportaPerfilEXPO;
    private final boolean suportaPerfilXMP;
    private final boolean suportaECC;
    private final int quantidadeSlotsM2;
    private final int geracaoMaximaSlotM2;
    private final int geracaoMinimaSlotM2;
    private final int quantidadeSlotsPCIe16;
    private final int versaoSlotPCIe16;
    private final int pistasSlotPCIe16;
    private final int quantidadePortasSATA;
    private final int velocidadeSATAGbps;
    private final int quantidadeConectoresCPU8pin;
    private final int quantidadeConectoresCPU4pin;
    private final int quantidadeConectoresATX24pin;

    private PlacaMae(Builder construtor) {
        super(construtor);
        this.soqueteCPU = construtor.soqueteCPU;
        this.chipset = construtor.chipset;
        this.formato = construtor.formato;
        this.comprimentoMm = construtor.comprimentoMm;
        this.larguraMm = construtor.larguraMm;
        this.suportaAtualizacaoBIOSSemCPU = construtor.suportaAtualizacaoBIOSSemCPU;
        this.vrmFasesCPU = construtor.vrmFasesCPU;
        this.vrmCorrentePorFaseA = construtor.vrmCorrentePorFaseA;
        this.vrmPossuiDissipador = construtor.vrmPossuiDissipador;
        this.quantidadeSlotsMemoria = construtor.quantidadeSlotsMemoria;
        this.tipoMemoria = construtor.tipoMemoria;
        this.canaisMemoria = construtor.canaisMemoria;
        this.capacidadeMaximaMemoriaGB = construtor.capacidadeMaximaMemoriaGB;
        this.frequenciaMemoriaMaximaMTs = construtor.frequenciaMemoriaMaximaMTs;
        this.suportaPerfilEXPO = construtor.suportaPerfilEXPO;
        this.suportaPerfilXMP = construtor.suportaPerfilXMP;
        this.suportaECC = construtor.suportaECC;
        this.quantidadeSlotsM2 = construtor.quantidadeSlotsM2;
        this.geracaoMaximaSlotM2 = construtor.geracaoMaximaSlotM2;
        this.geracaoMinimaSlotM2 = construtor.geracaoMinimaSlotM2;
        this.quantidadeSlotsPCIe16 = construtor.quantidadeSlotsPCIe16;
        this.versaoSlotPCIe16 = construtor.versaoSlotPCIe16;
        this.pistasSlotPCIe16 = construtor.pistasSlotPCIe16;
        this.quantidadePortasSATA = construtor.quantidadePortasSATA;
        this.velocidadeSATAGbps = construtor.velocidadeSATAGbps;
        this.quantidadeConectoresCPU8pin = construtor.quantidadeConectoresCPU8pin;
        this.quantidadeConectoresCPU4pin = construtor.quantidadeConectoresCPU4pin;
        this.quantidadeConectoresATX24pin = construtor.quantidadeConectoresATX24pin;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Soquete getSoqueteCPU() {
        return soqueteCPU;
    }

    public Chipset getChipset() {
        return chipset;
    }

    public FormatoPlacaMae getFormato() {
        return formato;
    }

    public int getComprimentoMm() {
        return comprimentoMm;
    }

    public int getLarguraMm() {
        return larguraMm;
    }

    public boolean isSuportaAtualizacaoBIOSSemCPU() {
        return suportaAtualizacaoBIOSSemCPU;
    }

    public int getVrmFasesCPU() {
        return vrmFasesCPU;
    }

    public int getVrmCorrentePorFaseA() {
        return vrmCorrentePorFaseA;
    }

    public Boolean getVrmPossuiDissipador() {
        return vrmPossuiDissipador;
    }

    public int getQuantidadeSlotsMemoria() {
        return quantidadeSlotsMemoria;
    }

    public TipoMemoria getTipoMemoria() {
        return tipoMemoria;
    }

    public int getCanaisMemoria() {
        return canaisMemoria;
    }

    public int getCapacidadeMaximaMemoriaGB() {
        return capacidadeMaximaMemoriaGB;
    }

    public int getFrequenciaMemoriaMaximaMTs() {
        return frequenciaMemoriaMaximaMTs;
    }

    public boolean isSuportaPerfilEXPO() {
        return suportaPerfilEXPO;
    }

    public boolean isSuportaPerfilXMP() {
        return suportaPerfilXMP;
    }

    public boolean isSuportaECC() {
        return suportaECC;
    }

    public int getQuantidadeSlotsM2() {
        return quantidadeSlotsM2;
    }

    public int getGeracaoMaximaSlotM2() {
        return geracaoMaximaSlotM2;
    }

    public int getGeracaoMinimaSlotM2() {
        return geracaoMinimaSlotM2;
    }

    public int getQuantidadeSlotsPCIe16() {
        return quantidadeSlotsPCIe16;
    }

    public int getVersaoSlotPCIe16() {
        return versaoSlotPCIe16;
    }

    public int getPistasSlotPCIe16() {
        return pistasSlotPCIe16;
    }

    public int getQuantidadePortasSATA() {
        return quantidadePortasSATA;
    }

    public int getVelocidadeSATAGbps() {
        return velocidadeSATAGbps;
    }

    public int getQuantidadeConectoresCPU8pin() {
        return quantidadeConectoresCPU8pin;
    }

    public int getQuantidadeConectoresCPU4pin() {
        return quantidadeConectoresCPU4pin;
    }

    public int getQuantidadeConectoresATX24pin() {
        return quantidadeConectoresATX24pin;
    }

    @Override
    protected Map<String, Object> camposEspecificos() {
        Map<String, Object> campos = new LinkedHashMap<>();
        campos.put("soqueteCPU", soqueteCPU);
        campos.put("chipset", chipset);
        campos.put("formato", formato);
        campos.put("comprimentoMm", comprimentoMm);
        campos.put("larguraMm", larguraMm);
        campos.put("suportaAtualizacaoBIOSSemCPU", suportaAtualizacaoBIOSSemCPU);
        campos.put("vrmFasesCPU", vrmFasesCPU);
        campos.put("vrmCorrentePorFaseA", vrmCorrentePorFaseA);
        campos.put("vrmPossuiDissipador", vrmPossuiDissipador);
        campos.put("quantidadeSlotsMemoria", quantidadeSlotsMemoria);
        campos.put("tipoMemoria", tipoMemoria);
        campos.put("canaisMemoria", canaisMemoria);
        campos.put("capacidadeMaximaMemoriaGB", capacidadeMaximaMemoriaGB);
        campos.put("frequenciaMemoriaMaximaMTs", frequenciaMemoriaMaximaMTs);
        campos.put("suportaPerfilEXPO", suportaPerfilEXPO);
        campos.put("suportaPerfilXMP", suportaPerfilXMP);
        campos.put("suportaECC", suportaECC);
        campos.put("quantidadeSlotsM2", quantidadeSlotsM2);
        campos.put("geracaoMaximaSlotM2", geracaoMaximaSlotM2);
        campos.put("geracaoMinimaSlotM2", geracaoMinimaSlotM2);
        campos.put("quantidadeSlotsPCIe16", quantidadeSlotsPCIe16);
        campos.put("versaoSlotPCIe16", versaoSlotPCIe16);
        campos.put("pistasSlotPCIe16", pistasSlotPCIe16);
        campos.put("quantidadePortasSATA", quantidadePortasSATA);
        campos.put("velocidadeSATAGbps", velocidadeSATAGbps);
        campos.put("quantidadeConectoresCPU8pin", quantidadeConectoresCPU8pin);
        campos.put("quantidadeConectoresCPU4pin", quantidadeConectoresCPU4pin);
        campos.put("quantidadeConectoresATX24pin", quantidadeConectoresATX24pin);
        return campos;
    }

    /** Builder de {@link PlacaMae}; a categoria ja sai fixa como {@code PLACA_MAE}. */
    public static final class Builder extends Componente.Builder<Builder> {

        private Soquete soqueteCPU;
        private Chipset chipset;
        private FormatoPlacaMae formato;
        private int comprimentoMm;
        private int larguraMm;
        private boolean suportaAtualizacaoBIOSSemCPU;
        private int vrmFasesCPU;
        private int vrmCorrentePorFaseA;
        private Boolean vrmPossuiDissipador;
        private int quantidadeSlotsMemoria;
        private TipoMemoria tipoMemoria;
        private int canaisMemoria;
        private int capacidadeMaximaMemoriaGB;
        private int frequenciaMemoriaMaximaMTs;
        private boolean suportaPerfilEXPO;
        private boolean suportaPerfilXMP;
        private boolean suportaECC;
        private int quantidadeSlotsM2;
        private int geracaoMaximaSlotM2;
        private int geracaoMinimaSlotM2;
        private int quantidadeSlotsPCIe16;
        private int versaoSlotPCIe16;
        private int pistasSlotPCIe16;
        private int quantidadePortasSATA;
        private int velocidadeSATAGbps;
        private int quantidadeConectoresCPU8pin;
        private int quantidadeConectoresCPU4pin;
        private int quantidadeConectoresATX24pin;

        private Builder() {
            categoria = CategoriaComponente.PLACA_MAE;
        }

        @Override
        protected Builder eu() {
            return this;
        }

        public Builder soqueteCPU(Soquete valor) {
            this.soqueteCPU = valor;
            return this;
        }

        public Builder chipset(Chipset valor) {
            this.chipset = valor;
            return this;
        }

        public Builder formato(FormatoPlacaMae valor) {
            this.formato = valor;
            return this;
        }

        public Builder comprimentoMm(int valor) {
            this.comprimentoMm = valor;
            return this;
        }

        public Builder larguraMm(int valor) {
            this.larguraMm = valor;
            return this;
        }

        public Builder suportaAtualizacaoBIOSSemCPU(boolean valor) {
            this.suportaAtualizacaoBIOSSemCPU = valor;
            return this;
        }

        public Builder vrmFasesCPU(int valor) {
            this.vrmFasesCPU = valor;
            return this;
        }

        public Builder vrmCorrentePorFaseA(int valor) {
            this.vrmCorrentePorFaseA = valor;
            return this;
        }

        public Builder vrmPossuiDissipador(Boolean valor) {
            this.vrmPossuiDissipador = valor;
            return this;
        }

        public Builder quantidadeSlotsMemoria(int valor) {
            this.quantidadeSlotsMemoria = valor;
            return this;
        }

        public Builder tipoMemoria(TipoMemoria valor) {
            this.tipoMemoria = valor;
            return this;
        }

        public Builder canaisMemoria(int valor) {
            this.canaisMemoria = valor;
            return this;
        }

        public Builder capacidadeMaximaMemoriaGB(int valor) {
            this.capacidadeMaximaMemoriaGB = valor;
            return this;
        }

        public Builder frequenciaMemoriaMaximaMTs(int valor) {
            this.frequenciaMemoriaMaximaMTs = valor;
            return this;
        }

        public Builder suportaPerfilEXPO(boolean valor) {
            this.suportaPerfilEXPO = valor;
            return this;
        }

        public Builder suportaPerfilXMP(boolean valor) {
            this.suportaPerfilXMP = valor;
            return this;
        }

        public Builder suportaECC(boolean valor) {
            this.suportaECC = valor;
            return this;
        }

        public Builder quantidadeSlotsM2(int valor) {
            this.quantidadeSlotsM2 = valor;
            return this;
        }

        public Builder geracaoMaximaSlotM2(int valor) {
            this.geracaoMaximaSlotM2 = valor;
            return this;
        }

        public Builder geracaoMinimaSlotM2(int valor) {
            this.geracaoMinimaSlotM2 = valor;
            return this;
        }

        public Builder quantidadeSlotsPCIe16(int valor) {
            this.quantidadeSlotsPCIe16 = valor;
            return this;
        }

        public Builder versaoSlotPCIe16(int valor) {
            this.versaoSlotPCIe16 = valor;
            return this;
        }

        public Builder pistasSlotPCIe16(int valor) {
            this.pistasSlotPCIe16 = valor;
            return this;
        }

        public Builder quantidadePortasSATA(int valor) {
            this.quantidadePortasSATA = valor;
            return this;
        }

        public Builder velocidadeSATAGbps(int valor) {
            this.velocidadeSATAGbps = valor;
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

        public Builder quantidadeConectoresATX24pin(int valor) {
            this.quantidadeConectoresATX24pin = valor;
            return this;
        }

        @Override
        public PlacaMae construir() {
            return new PlacaMae(this);
        }
    }
}
