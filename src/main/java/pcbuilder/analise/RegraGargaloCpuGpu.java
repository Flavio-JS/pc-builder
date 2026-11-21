package pcbuilder.analise;

import pcbuilder.config.ParametrosCalculo;
import pcbuilder.modelo.PlacaVideo;
import pcbuilder.modelo.Processador;

/**
 * Calculo de equilibrio CPU x GPU usado pela regra <strong>L01</strong> (HU-004).
 *
 * <p>Sem perfil de uso, o desequilibrio e
 * {@code (indiceSingleThread da CPU - indiceDesempenhoGPU) / indiceDesempenhoGPU}. Um
 * valor muito negativo significa que a CPU e fraca para a GPU: a placa de video nao vai
 * ser usada por completo (gargalo de CPU). Um valor positivo e o cenario saudavel - a CPU
 * sobra. A tolerancia depende da resolucao-alvo da GPU.</p>
 */
public final class RegraGargaloCpuGpu {

    private RegraGargaloCpuGpu() {
        // classe utilitaria: nao deve ser instanciada
    }

    /**
     * Desequilibrio entre a CPU e a GPU, em pontos percentuais.
     *
     * @param cpu processador escolhido
     * @param gpu placa de video escolhida
     * @return negativo quando a CPU e mais fraca; {@code 0} sem dados para comparar
     */
    public static double desequilibrioPercentual(Processador cpu, PlacaVideo gpu) {
        int indiceGpu = gpu.getIndiceDesempenhoGPU();
        if (indiceGpu <= 0) {
            return 0;
        }
        int indiceCpu = cpu.getIndiceDesempenhoSingleThread();
        return (indiceCpu - indiceGpu) * 100.0 / indiceGpu;
    }

    /**
     * Tolerancia de desequilibrio para a resolucao-alvo da GPU.
     *
     * @param gpu placa de video escolhida
     * @return tolerancia em pontos percentuais (10 em 1080p, 15 em 1440p, 20 em 4K)
     */
    public static int toleranciaPercentual(PlacaVideo gpu) {
        return ParametrosCalculo.toleranciaDesequilibrioCpuGpuPercentual(gpu.getResolucaoAlvo());
    }

    /**
     * Ha gargalo de CPU na combinacao?
     *
     * @param cpu processador escolhido
     * @param gpu placa de video escolhida
     * @return {@code true} quando a CPU e mais fraca que a GPU alem da tolerancia
     */
    public static boolean haGargaloDeCpu(Processador cpu, PlacaVideo gpu) {
        return desequilibrioPercentual(cpu, gpu) < -toleranciaPercentual(gpu);
    }
}
