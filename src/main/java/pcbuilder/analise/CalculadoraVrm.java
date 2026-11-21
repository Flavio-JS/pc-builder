package pcbuilder.analise;

import pcbuilder.config.ParametrosCalculo;
import pcbuilder.modelo.PlacaMae;
import pcbuilder.modelo.Processador;

/**
 * Calculos do VRM da placa-mae usados pela regra <strong>L02</strong> (HU-004).
 *
 * <p>A corrente exigida pela CPU vem de {@code consumoMaximoWatts / tensaoMaximaVolts}
 * (140 W / 1,25 V = 112 A, conforme a analise tecnica) e a capacidade do VRM de
 * {@code vrmFasesCPU x vrmCorrentePorFaseA} (12 x 60 A = 720 A). Acima do limite de
 * alerta a placa pode reduzir a frequencia da CPU para se proteger.</p>
 */
public final class CalculadoraVrm {

    private CalculadoraVrm() {
        // classe utilitaria: nao deve ser instanciada
    }

    /**
     * Corrente que a CPU pede ao VRM, em amperes.
     *
     * @param cpu processador escolhido
     * @return a corrente exigida (usa {@code TENSAO_REFERENCIA_CPU} quando a tensao nao
     *         esta cadastrada); {@code 0} quando o consumo maximo nao esta cadastrado
     */
    public static double correnteExigidaCPUAmperes(Processador cpu) {
        int consumo = cpu.getConsumoMaximoWatts();
        if (consumo <= 0) {
            return 0;
        }
        Double tensao = cpu.getTensaoMaximaVolts();
        double referencia = tensao == null || tensao <= 0
                ? ParametrosCalculo.TENSAO_REFERENCIA_CPU
                : tensao;
        return consumo / referencia;
    }

    /**
     * Capacidade teorica do VRM, em amperes.
     *
     * @param placaMae placa-mae escolhida
     * @return {@code vrmFasesCPU x vrmCorrentePorFaseA}
     */
    public static int capacidadeVRMAmperes(PlacaMae placaMae) {
        return placaMae.getVrmFasesCPU() * placaMae.getVrmCorrentePorFaseA();
    }

    /**
     * Percentual do VRM usado pela CPU.
     *
     * @param cpu processador escolhido
     * @param placaMae placa-mae escolhida
     * @return o uso em pontos percentuais; {@code 0} quando nao ha dados para calcular
     */
    public static double percentualUsoVRM(Processador cpu, PlacaMae placaMae) {
        int capacidade = capacidadeVRMAmperes(placaMae);
        if (capacidade <= 0) {
            return 0;
        }
        return correnteExigidaCPUAmperes(cpu) * 100.0 / capacidade;
    }

    /**
     * Limite de uso a partir do qual o aviso e gerado.
     *
     * @param placaMae placa-mae escolhida
     * @return 60% para VRM sem dissipador; 80% nos demais casos
     */
    public static int limiteAlertaPercentual(PlacaMae placaMae) {
        return Boolean.FALSE.equals(placaMae.getVrmPossuiDissipador())
                ? ParametrosCalculo.LIMITE_ALERTA_USO_VRM_SEM_DISSIPADOR_PERCENTUAL
                : ParametrosCalculo.LIMITE_ALERTA_USO_VRM_PERCENTUAL;
    }
}
