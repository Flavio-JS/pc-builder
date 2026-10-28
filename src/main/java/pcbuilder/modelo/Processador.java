package pcbuilder.modelo;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import pcbuilder.enums.CategoriaComponente;
import pcbuilder.enums.Soquete;
import pcbuilder.enums.TipoMemoria;

/**
 * Processador (CPU) - secao 3 do modelo de dados.
 *
 * <p>Guarda apenas dados atomicos: nada de "4x slots DDR5" em um campo so, nada de
 * valor calculado (potencia, gargalo) gravado no objeto.</p>
 */
public final class Processador extends Componente {

    private final Soquete soquete;
    private final int nucleosFisicos;
    private final int threads;
    private final int frequenciaBaseMHz;
    private final int frequenciaBoostMHz;
    private final int tdpWatts;
    private final int consumoMaximoWatts;
    private final Double tensaoMaximaVolts;
    private final List<TipoMemoria> tiposMemoriaSuportados;
    private final int canaisMemoria;
    private final int frequenciaMemoriaNativaMTs;
    private final Integer frequenciaMaximaSincronaMTs;
    private final int capacidadeMaximaMemoriaGB;
    private final boolean suportaECC;
    private final int versaoPCIeMaxima;
    private final int pistasPCIeLivres;
    private final boolean possuiVideoIntegrado;
    private final boolean desbloqueadoOverclock;
    private final int indiceDesempenhoSingleThread;
    private final int indiceDesempenhoMultithread;

    private Processador(Builder construtor) {
        super(construtor);
        this.soquete = construtor.soquete;
        this.nucleosFisicos = construtor.nucleosFisicos;
        this.threads = construtor.threads;
        this.frequenciaBaseMHz = construtor.frequenciaBaseMHz;
        this.frequenciaBoostMHz = construtor.frequenciaBoostMHz;
        this.tdpWatts = construtor.tdpWatts;
        this.consumoMaximoWatts = construtor.consumoMaximoWatts;
        this.tensaoMaximaVolts = construtor.tensaoMaximaVolts;
        this.tiposMemoriaSuportados = List.copyOf(construtor.tiposMemoriaSuportados);
        this.canaisMemoria = construtor.canaisMemoria;
        this.frequenciaMemoriaNativaMTs = construtor.frequenciaMemoriaNativaMTs;
        this.frequenciaMaximaSincronaMTs = construtor.frequenciaMaximaSincronaMTs;
        this.capacidadeMaximaMemoriaGB = construtor.capacidadeMaximaMemoriaGB;
        this.suportaECC = construtor.suportaECC;
        this.versaoPCIeMaxima = construtor.versaoPCIeMaxima;
        this.pistasPCIeLivres = construtor.pistasPCIeLivres;
        this.possuiVideoIntegrado = construtor.possuiVideoIntegrado;
        this.desbloqueadoOverclock = construtor.desbloqueadoOverclock;
        this.indiceDesempenhoSingleThread = construtor.indiceDesempenhoSingleThread;
        this.indiceDesempenhoMultithread = construtor.indiceDesempenhoMultithread;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Soquete getSoquete() {
        return soquete;
    }

    public int getNucleosFisicos() {
        return nucleosFisicos;
    }

    public int getThreads() {
        return threads;
    }

    public int getFrequenciaBaseMHz() {
        return frequenciaBaseMHz;
    }

    public int getFrequenciaBoostMHz() {
        return frequenciaBoostMHz;
    }

    public int getTdpWatts() {
        return tdpWatts;
    }

    public int getConsumoMaximoWatts() {
        return consumoMaximoWatts;
    }

    public Double getTensaoMaximaVolts() {
        return tensaoMaximaVolts;
    }

    public List<TipoMemoria> getTiposMemoriaSuportados() {
        return tiposMemoriaSuportados;
    }

    public int getCanaisMemoria() {
        return canaisMemoria;
    }

    public int getFrequenciaMemoriaNativaMTs() {
        return frequenciaMemoriaNativaMTs;
    }

    public Integer getFrequenciaMaximaSincronaMTs() {
        return frequenciaMaximaSincronaMTs;
    }

    public int getCapacidadeMaximaMemoriaGB() {
        return capacidadeMaximaMemoriaGB;
    }

    public boolean isSuportaECC() {
        return suportaECC;
    }

    public int getVersaoPCIeMaxima() {
        return versaoPCIeMaxima;
    }

    public int getPistasPCIeLivres() {
        return pistasPCIeLivres;
    }

    public boolean isPossuiVideoIntegrado() {
        return possuiVideoIntegrado;
    }

    public boolean isDesbloqueadoOverclock() {
        return desbloqueadoOverclock;
    }

    public int getIndiceDesempenhoSingleThread() {
        return indiceDesempenhoSingleThread;
    }

    public int getIndiceDesempenhoMultithread() {
        return indiceDesempenhoMultithread;
    }

    @Override
    protected Map<String, Object> camposEspecificos() {
        Map<String, Object> campos = new LinkedHashMap<>();
        campos.put("soquete", soquete);
        campos.put("nucleosFisicos", nucleosFisicos);
        campos.put("threads", threads);
        campos.put("frequenciaBaseMHz", frequenciaBaseMHz);
        campos.put("frequenciaBoostMHz", frequenciaBoostMHz);
        campos.put("tdpWatts", tdpWatts);
        campos.put("consumoMaximoWatts", consumoMaximoWatts);
        campos.put("tensaoMaximaVolts", tensaoMaximaVolts);
        campos.put("tiposMemoriaSuportados", tiposMemoriaSuportados);
        campos.put("canaisMemoria", canaisMemoria);
        campos.put("frequenciaMemoriaNativaMTs", frequenciaMemoriaNativaMTs);
        campos.put("frequenciaMaximaSincronaMTs", frequenciaMaximaSincronaMTs);
        campos.put("capacidadeMaximaMemoriaGB", capacidadeMaximaMemoriaGB);
        campos.put("suportaECC", suportaECC);
        campos.put("versaoPCIeMaxima", versaoPCIeMaxima);
        campos.put("pistasPCIeLivres", pistasPCIeLivres);
        campos.put("possuiVideoIntegrado", possuiVideoIntegrado);
        campos.put("desbloqueadoOverclock", desbloqueadoOverclock);
        campos.put("indiceDesempenhoSingleThread", indiceDesempenhoSingleThread);
        campos.put("indiceDesempenhoMultithread", indiceDesempenhoMultithread);
        return campos;
    }

    /** Builder de {@link Processador}; a categoria ja sai fixa como {@code CPU}. */
    public static final class Builder extends Componente.Builder<Builder> {

        private Soquete soquete;
        private int nucleosFisicos;
        private int threads;
        private int frequenciaBaseMHz;
        private int frequenciaBoostMHz;
        private int tdpWatts;
        private int consumoMaximoWatts;
        private Double tensaoMaximaVolts;
        private List<TipoMemoria> tiposMemoriaSuportados = List.of();
        private int canaisMemoria;
        private int frequenciaMemoriaNativaMTs;
        private Integer frequenciaMaximaSincronaMTs;
        private int capacidadeMaximaMemoriaGB;
        private boolean suportaECC;
        private int versaoPCIeMaxima;
        private int pistasPCIeLivres;
        private boolean possuiVideoIntegrado;
        private boolean desbloqueadoOverclock;
        private int indiceDesempenhoSingleThread;
        private int indiceDesempenhoMultithread;

        private Builder() {
            categoria = CategoriaComponente.CPU;
        }

        @Override
        protected Builder eu() {
            return this;
        }

        public Builder soquete(Soquete valor) {
            this.soquete = valor;
            return this;
        }

        public Builder nucleosFisicos(int valor) {
            this.nucleosFisicos = valor;
            return this;
        }

        public Builder threads(int valor) {
            this.threads = valor;
            return this;
        }

        public Builder frequenciaBaseMHz(int valor) {
            this.frequenciaBaseMHz = valor;
            return this;
        }

        public Builder frequenciaBoostMHz(int valor) {
            this.frequenciaBoostMHz = valor;
            return this;
        }

        public Builder tdpWatts(int valor) {
            this.tdpWatts = valor;
            return this;
        }

        public Builder consumoMaximoWatts(int valor) {
            this.consumoMaximoWatts = valor;
            return this;
        }

        public Builder tensaoMaximaVolts(Double valor) {
            this.tensaoMaximaVolts = valor;
            return this;
        }

        public Builder tiposMemoriaSuportados(TipoMemoria... valores) {
            this.tiposMemoriaSuportados = List.of(valores);
            return this;
        }

        public Builder canaisMemoria(int valor) {
            this.canaisMemoria = valor;
            return this;
        }

        public Builder frequenciaMemoriaNativaMTs(int valor) {
            this.frequenciaMemoriaNativaMTs = valor;
            return this;
        }

        public Builder frequenciaMaximaSincronaMTs(Integer valor) {
            this.frequenciaMaximaSincronaMTs = valor;
            return this;
        }

        public Builder capacidadeMaximaMemoriaGB(int valor) {
            this.capacidadeMaximaMemoriaGB = valor;
            return this;
        }

        public Builder suportaECC(boolean valor) {
            this.suportaECC = valor;
            return this;
        }

        public Builder versaoPCIeMaxima(int valor) {
            this.versaoPCIeMaxima = valor;
            return this;
        }

        public Builder pistasPCIeLivres(int valor) {
            this.pistasPCIeLivres = valor;
            return this;
        }

        public Builder possuiVideoIntegrado(boolean valor) {
            this.possuiVideoIntegrado = valor;
            return this;
        }

        public Builder desbloqueadoOverclock(boolean valor) {
            this.desbloqueadoOverclock = valor;
            return this;
        }

        public Builder indiceDesempenhoSingleThread(int valor) {
            this.indiceDesempenhoSingleThread = valor;
            return this;
        }

        public Builder indiceDesempenhoMultithread(int valor) {
            this.indiceDesempenhoMultithread = valor;
            return this;
        }

        @Override
        public Processador construir() {
            return new Processador(this);
        }
    }
}
