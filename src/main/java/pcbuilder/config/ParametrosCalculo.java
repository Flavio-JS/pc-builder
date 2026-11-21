package pcbuilder.config;

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

    private ParametrosCalculo() {
        // classe utilitaria: nao deve ser instanciada
    }

    /** Margem de seguranca da fonte em pontos percentuais ({@code 40}). */
    public static int margemSegurancaPercentual() {
        return (int) Math.round((MARGEM_SEGURANCA_FONTE - 1.0) * 100);
    }
}
