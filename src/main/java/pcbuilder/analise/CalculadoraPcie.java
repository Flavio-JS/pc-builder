package pcbuilder.analise;

import pcbuilder.config.ParametrosCalculo;

/**
 * Calculos de banda PCIe usados pelas regras <strong>L05</strong> (GPU x slot x16),
 * <strong>L10</strong> (armazenamento x slot) e <strong>L11</strong> (pistas da CPU).
 *
 * <p>A banda por pista vem de {@link ParametrosCalculo#bandaPciePorPistaGBs(int)}. O
 * resultado e sempre uma estimativa para explicar a perda em linguagem simples; nada e
 * gravado no componente (secao 16 do modelo de dados).</p>
 */
public final class CalculadoraPcie {

    private CalculadoraPcie() {
        // classe utilitaria: nao deve ser instanciada
    }

    /**
     * Banda total de um enlace PCIe.
     *
     * @param geracao versao do PCIe
     * @param pistas quantidade de pistas usadas
     * @return GB/s; {@code 0} quando a geracao nao esta na tabela
     */
    public static double bandaGBs(int geracao, int pistas) {
        return ParametrosCalculo.bandaPciePorPistaGBs(geracao) * Math.max(0, pistas);
    }

    /**
     * Banda efetiva quando a GPU usa um slot de geracao menor.
     *
     * @param geracaoGpu versao da GPU
     * @param pistasGpu pistas que a GPU usa
     * @param geracaoSlot versao do slot da placa-mae
     * @param pistasSlot pistas fisicas do slot ({@code 0} quando nao cadastrado)
     * @return GB/s efetivos
     */
    public static double bandaEfetivaGBs(int geracaoGpu, int pistasGpu, int geracaoSlot,
            int pistasSlot) {
        int pistasEfetivas = pistasEfetivas(pistasGpu, pistasSlot);
        return bandaGBs(Math.min(geracaoGpu, geracaoSlot), pistasEfetivas);
    }

    /**
     * Pistas realmente usadas pelo enlace: o menor valor entre o que a peca pede e o que
     * o slot oferece. Slot sem o dado cadastrado nao reduz as pistas da peca.
     *
     * @param pistasPeca pistas da GPU/armazenamento
     * @param pistasSlot pistas do slot/porta ({@code 0} quando nao cadastrado)
     * @return pistas efetivas
     */
    public static int pistasEfetivas(int pistasPeca, int pistasSlot) {
        if (pistasSlot <= 0) {
            return Math.max(0, pistasPeca);
        }
        return Math.min(Math.max(0, pistasPeca), pistasSlot);
    }

    /**
     * Teto de leitura de um slot M.2, em MB/s.
     *
     * @param geracao geracao PCIe do slot
     * @param pistas pistas do SSD (normalmente 4)
     * @return o teto aproximado (PCIe 4.0 x4 = ~7.876 MB/s); {@code 0} sem dados
     */
    public static int tetoSlotM2MBs(int geracao, int pistas) {
        double banda = bandaGBs(geracao, pistas);
        if (banda <= 0) {
            return 0;
        }
        return (int) Math.round(banda * 1000);
    }

    /**
     * Perda percentual de banda.
     *
     * @param bandaNominal banda que a peca pediria
     * @param bandaEfetiva banda que o slot entrega
     * @return a perda em pontos percentuais ({@code 50.0} = metade)
     */
    public static double perdaPercentual(double bandaNominal, double bandaEfetiva) {
        if (bandaNominal <= 0) {
            return 0;
        }
        double perda = (1 - bandaEfetiva / bandaNominal) * 100;
        return Math.max(0, Math.min(100, perda));
    }

    /**
     * Descricao curta do enlace, usada no aviso ({@code PCIe 4.0 x16}).
     *
     * @param geracao versao do PCIe
     * @param pistas pistas do enlace
     * @return o texto pronto para a tela
     */
    public static String descricao(int geracao, int pistas) {
        return "PCIe " + geracao + ".0 x" + pistas;
    }
}
