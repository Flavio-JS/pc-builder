package pcbuilder.servico.resumos;

import pcbuilder.analise.CalculadoraMemoria;
import pcbuilder.modelo.Componente;
import pcbuilder.modelo.Memoria;

/**
 * Linha de apoio e texto de confirmacao da etapa 3 (HU-006).
 *
 * <p>A linha de apoio mostra capacidade total, frequencia, latencia e perfil:
 * {@code 2x16 GB = 32 GB | 8000 MT/s | CL38 | EXPO: sim}. Tudo e calculado a partir dos
 * campos do kit - capacidade total nunca e digitada no codigo da tela.</p>
 */
public final class ResumoMemoria {

    private ResumoMemoria() {
        // classe utilitaria: nao deve ser instanciada
    }

    /**
     * Linha de apoio exibida ao lado do preco na lista da etapa 3.
     *
     * @param componente candidato da etapa (deve ser uma {@link Memoria})
     * @return o resumo em uma linha; vazio quando o componente nao e memoria
     */
    public static String linhaDeApoio(Componente componente) {
        if (!(componente instanceof Memoria memoria)) {
            return "";
        }
        return descricaoKit(memoria) + " | " + memoria.getFrequenciaMTs() + " MT/s"
                + " | CL" + memoria.getLatenciaCAS()
                + " | EXPO: " + (memoria.isPerfilEXPO() ? "sim" : "nao");
    }

    /**
     * Descricao do kit: {@code 2x16 GB = 32 GB}.
     *
     * @param memoria kit escolhido
     * @return a descricao com a capacidade total calculada
     */
    public static String descricaoKit(Memoria memoria) {
        return memoria.getQuantidadePentes() + "x" + memoria.getCapacidadePorPenteGB()
                + " GB = " + CalculadoraMemoria.capacidadeTotalGB(memoria) + " GB";
    }

    /**
     * Canal em que o kit vai trabalhar.
     *
     * @param memoria kit escolhido
     * @param canais canais oferecidos pela placa-mae (ou pela CPU)
     * @return {@code Dual Channel} quando o kit preenche todos os canais;
     *         {@code Single Channel} caso contrario
     */
    public static String canal(Memoria memoria, int canais) {
        return CalculadoraMemoria.memoriaEmDualChannel(memoria, canais)
                ? "Dual Channel"
                : "Single Channel";
    }
}
