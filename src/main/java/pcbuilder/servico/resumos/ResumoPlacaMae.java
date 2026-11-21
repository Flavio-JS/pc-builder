package pcbuilder.servico.resumos;

import pcbuilder.modelo.Componente;
import pcbuilder.modelo.PlacaMae;

/**
 * Linha de apoio da etapa 2 (HU-005): resume o que a placa-mae oferece sem obrigar o
 * usuario a interpretar a ficha tecnica.
 *
 * <p>Formato da historia de usuario:</p>
 * <pre>
 * chipset B650 | MATX | 4 slots DDR5 (max 6000 MT/s) | VRM 8x50A
 * </pre>
 *
 * <p>O texto e montado <strong>somente</strong> a partir dos campos da placa: nada de
 * valor fixo no codigo da tela. Chipset, formato ou tipo de memoria ausentes aparecem
 * como {@code --}, porque a etapa precisa continuar legivel com cadastro incompleto.</p>
 */
public final class ResumoPlacaMae {

    private ResumoPlacaMae() {
        // classe utilitaria: nao deve ser instanciada
    }

    /**
     * Linha de apoio exibida ao lado do preco na lista da etapa 2.
     *
     * @param componente candidato da etapa (deve ser uma {@link PlacaMae})
     * @return o resumo em uma linha; vazio quando o componente nao e uma placa-mae
     */
    public static String linhaDeApoio(Componente componente) {
        if (!(componente instanceof PlacaMae placaMae)) {
            return "";
        }
        return "chipset " + texto(placaMae.getChipset())
                + " | " + texto(placaMae.getFormato())
                + " | " + placaMae.getQuantidadeSlotsMemoria() + " slots "
                + texto(placaMae.getTipoMemoria())
                + " (max " + placaMae.getFrequenciaMemoriaMaximaMTs() + " MT/s)"
                + " | VRM " + placaMae.getVrmFasesCPU() + "x"
                + placaMae.getVrmCorrentePorFaseA() + "A";
    }

    private static String texto(Enum<?> valor) {
        return valor == null ? "--" : valor.name();
    }
}
