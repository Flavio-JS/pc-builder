package pcbuilder.analise;

import pcbuilder.config.ParametrosCalculo;
import pcbuilder.modelo.Fonte;
import pcbuilder.modelo.Montagem;
import pcbuilder.modelo.PlacaVideo;

/**
 * Calculos da fonte usados pelos avisos <strong>L06</strong> (pico transiente) e
 * <strong>L09</strong> (eficiencia) da HU-004/HU-009.
 *
 * <p>A carga e {@code consumoTotalSistemaWatts / potenciaTotalWatts}; a faixa eficiente
 * vem de {@link ParametrosCalculo}. Nada e gravado no componente.</p>
 */
public final class CalculadoraFonte {

    private CalculadoraFonte() {
        // classe utilitaria: nao deve ser instanciada
    }

    /**
     * Percentual de carga da fonte para o consumo atual.
     *
     * @param montagem pecas escolhidas
     * @param fonte fonte candidata
     * @return a carga em pontos percentuais; {@code 0} quando a potencia nao e valida
     */
    public static double percentualCarga(Montagem montagem, Fonte fonte) {
        if (fonte.getPotenciaTotalWatts() <= 0) {
            return 0;
        }
        return CalculadoraConsumo.consumoTotalSistemaWatts(montagem) * 100.0
                / fonte.getPotenciaTotalWatts();
    }

    /**
     * A fonte esta na faixa de maior eficiencia?
     *
     * @param percentualCarga carga em pontos percentuais
     * @return {@code true} entre 40% e 60%
     */
    public static boolean dentroDaFaixaEficiente(double percentualCarga) {
        return percentualCarga >= ParametrosCalculo.FAIXA_EFICIENCIA_FONTE_MIN_PERCENTUAL
                && percentualCarga <= ParametrosCalculo.FAIXA_EFICIENCIA_FONTE_MAX_PERCENTUAL;
    }

    /**
     * O pico transiente da GPU passa do trilho de 12 V da fonte?
     *
     * @param gpu placa de video escolhida (pode ter pico nulo)
     * @param fonte fonte candidata
     * @return {@code true} quando ha risco de desligamento
     */
    public static boolean picoAcimaDoTrilho(PlacaVideo gpu, Fonte fonte) {
        Integer pico = gpu.getPicoTransienteWatts();
        int trilho = fonte.getPotenciaTrilho12VWatts();
        return pico != null && trilho > 0 && pico > trilho;
    }
}
