package pcbuilder.teste;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import pcbuilder.enums.CategoriaComponente;
import pcbuilder.enums.Soquete;
import pcbuilder.enums.TipoMemoria;
import pcbuilder.modelo.Componente;

/**
 * Componente artificial usado <strong>somente nos testes</strong>.
 *
 * <p>Serve para simular registros "sujos" que as classes de dominio tipadas nao
 * deixam criar no codigo de producao - por exemplo {@code tdpWatts = "65W"} ou um
 * campo calculado gravado. Sem ele, as verificacoes 2, 3, 4, 9 e 10 nao teriam como
 * ser exercitadas.</p>
 *
 * <p>A ficha padrao e a de um processador valido; cada teste troca um unico campo
 * com {@link Construtor#campo(String, Object)}.</p>
 */
final class ComponenteFake extends Componente {

    private final Map<String, Object> especificos;

    private ComponenteFake(Construtor construtor) {
        super(construtor);
        this.especificos = new LinkedHashMap<>(construtor.especificos);
    }

    /** Construtor de uma CPU valida, pronta para receber um campo "sujo". */
    static Construtor cpuValida() {
        return new Construtor();
    }

    @Override
    protected Map<String, Object> camposEspecificos() {
        return especificos;
    }

    @Override
    public String toString() {
        return "ComponenteFake[" + getId() + "]";
    }

    /** Builder do fake: preenche a ficha de CPU com valores que passam no checklist. */
    static final class Construtor extends Componente.Builder<Construtor> {

        private final Map<String, Object> especificos = new LinkedHashMap<>();

        private Construtor() {
            categoria = CategoriaComponente.CPU;
            id("cpu-fake");
            modelo("CPU Fake");
            fabricante("Teste");
            precoMedioBRL("100.00");
            especificos.put("soquete", Soquete.AM5);
            especificos.put("nucleosFisicos", 8);
            especificos.put("threads", 16);
            especificos.put("frequenciaBaseMHz", 3800);
            especificos.put("frequenciaBoostMHz", 5500);
            especificos.put("tdpWatts", 65);
            especificos.put("consumoMaximoWatts", 140);
            especificos.put("tensaoMaximaVolts", 1.25);
            especificos.put("tiposMemoriaSuportados", List.of(TipoMemoria.DDR5));
            especificos.put("canaisMemoria", 2);
            especificos.put("frequenciaMemoriaNativaMTs", 5200);
            especificos.put("frequenciaMaximaSincronaMTs", 6000);
            especificos.put("capacidadeMaximaMemoriaGB", 192);
            especificos.put("suportaECC", true);
            especificos.put("versaoPCIeMaxima", 5);
            especificos.put("pistasPCIeLivres", 24);
            especificos.put("possuiVideoIntegrado", true);
            especificos.put("desbloqueadoOverclock", true);
            especificos.put("indiceDesempenhoSingleThread", 90);
            especificos.put("indiceDesempenhoMultithread", 85);
        }

        /** Troca (ou acrescenta) um campo da ficha do fake. */
        Construtor campo(String nome, Object valor) {
            especificos.put(nome, valor);
            return this;
        }

        @Override
        protected Construtor eu() {
            return this;
        }

        @Override
        public ComponenteFake construir() {
            return new ComponenteFake(this);
        }
    }
}
