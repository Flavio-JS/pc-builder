package pcbuilder.enums;

/**
 * Gravidade de uma limitacao entre duas pecas compativeis (secao 2.2 das historias de
 * usuario).
 *
 * <ul>
 *   <li>{@link #AVISO} - a peca funciona, mas entrega menos do que poderia (perda de
 *       desempenho: frequencia de memoria, banda de PCIe, VRAM, eficiencia);</li>
 *   <li>{@link #RISCO} - o conjunto funciona, mas em uma condicao de risco eletrico ou
 *       termico (VRM acima do limite, pico de energia acima do trilho de 12 V).</li>
 * </ul>
 *
 * <p>Nenhuma das duas severidades bloqueia a escolha: quem bloqueia e o filtro de
 * compatibilidade (HU-003).</p>
 */
public enum SeveridadeLimitacao {

    AVISO,
    RISCO
}
