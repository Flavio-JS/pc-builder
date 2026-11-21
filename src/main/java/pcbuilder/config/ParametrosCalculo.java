package pcbuilder.config;

import pcbuilder.enums.Resolucao;

/**
 * Constantes de calculo usadas pelas regras de compatibilidade (secao 13 do modelo de
 * dados) e de limitacao (secao 14).
 *
 * <p>Nao sao dados de componente: ficam fora das regras para poderem ser ajustados sem
 * alterar a logica. Apenas os parametros ja usados pelas HUs implementadas estao
 * declarados aqui; os demais entram junto com a regra que os utiliza.</p>
 */
public final class ParametrosCalculo {

    /**
     * Folga de 40% sobre o consumo total do sistema (regras C14 e L06).
     *
     * <p>O consumo somado das pecas e o valor de pico; a fonte precisa entregar esse
     * valor com folga para nao trabalhar no limite.</p>
     */
    public static final double MARGEM_SEGURANCA_FONTE = 1.40;

    /**
     * Reserva de potencia para as pecas que o fluxo nao coleta (fans, cooler,
     * perifericos) ou que ainda nao foram escolhidas numa etapa parcial (regra L06).
     */
    public static final int CONSUMO_BASE_SISTEMA_WATTS = 50;

    /**
     * Tensao de referencia da CPU (regra L02) usada quando
     * {@code cpu.tensaoMaximaVolts} nao esta cadastrada.
     */
    public static final double TENSAO_REFERENCIA_CPU = 1.25;

    /** Uso do VRM a partir do qual a placa pode estrangular a CPU (regra L02). */
    public static final int LIMITE_ALERTA_USO_VRM_PERCENTUAL = 80;

    /** Mesmo limite quando o VRM nao tem dissipador (regra L02). */
    public static final int LIMITE_ALERTA_USO_VRM_SEM_DISSIPADOR_PERCENTUAL = 60;

    /** Diferenca tolerada entre o indice da CPU e o da GPU (regra L01). */
    public static final int TOLERANCIA_DESEQUILIBRIO_CPU_GPU_PERCENTUAL = 15;

    /** Faixa de carga em que a fonte alcanca a melhor eficiencia (regra L09). */
    public static final int FAIXA_EFICIENCIA_FONTE_MIN_PERCENTUAL = 40;
    /** Faixa de carga em que a fonte alcanca a melhor eficiencia (regra L09). */
    public static final int FAIXA_EFICIENCIA_FONTE_MAX_PERCENTUAL = 60;

    private ParametrosCalculo() {
        // classe utilitaria: nao deve ser instanciada
    }

    /** Margem de seguranca da fonte em pontos percentuais ({@code 40}). */
    public static int margemSegurancaPercentual() {
        return (int) Math.round((MARGEM_SEGURANCA_FONTE - 1.0) * 100);
    }

    /**
     * Banda de uma pista PCIe por geracao (regra L05).
     *
     * @param geracao versao do PCIe ({@code 3}, {@code 4}, {@code 5}...)
     * @return GB/s por pista; {@code 0} quando a geracao nao esta na tabela
     */
    public static double bandaPciePorPistaGBs(int geracao) {
        switch (geracao) {
            case 1:
                return 0.25;
            case 2:
                return 0.5;
            case 3:
                return 0.985;
            case 4:
                return 1.969;
            case 5:
                return 3.938;
            case 6:
                return 7.563;
            default:
                return 0.0;
        }
    }

    /**
     * VRAM minima recomendada para a resolucao-alvo da GPU (regra L07).
     *
     * @param resolucao resolucao em que a GPU e usada
     * @return GB minimos; {@code 0} quando a resolucao e desconhecida
     */
    public static int vramMinimaGB(Resolucao resolucao) {
        if (resolucao == null) {
            return 0;
        }
        switch (resolucao) {
            case FHD_1080P:
                return 8;
            case QHD_1440P:
                return 12;
            case UHD_4K:
                return 16;
            default:
                return 0;
        }
    }

    /**
     * Tolerancia de desequilibrio CPU x GPU conforme a resolucao (regra L01).
     *
     * <p>Quanto maior a resolucao, mais a GPU pesa e menor a chance de gargalo de CPU,
     * por isso a tolerancia cresce de 1080p para 4K.</p>
     *
     * @param resolucao resolucao-alvo da GPU
     * @return tolerancia em pontos percentuais
     */
    public static int toleranciaDesequilibrioCpuGpuPercentual(Resolucao resolucao) {
        if (resolucao == null) {
            return TOLERANCIA_DESEQUILIBRIO_CPU_GPU_PERCENTUAL;
        }
        switch (resolucao) {
            case FHD_1080P:
                return 10;
            case QHD_1440P:
                return 15;
            case UHD_4K:
                return 20;
            default:
                return TOLERANCIA_DESEQUILIBRIO_CPU_GPU_PERCENTUAL;
        }
    }
}
