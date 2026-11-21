package pcbuilder.servico.resumos;

import java.util.ArrayList;
import java.util.List;

import pcbuilder.analise.CalculadoraPcie;
import pcbuilder.modelo.Componente;
import pcbuilder.modelo.PlacaVideo;

/**
 * Linha de apoio da etapa 4 (HU-007): resumo da placa de video para quem nao conhece as
 * especificacoes.
 *
 * <p>Formato da historia de usuario:</p>
 * <pre>
 * 12 GB GDDR6X | PCIe 4.0 x16 | 220 W | 1x 16-pinos
 * </pre>
 *
 * <p>Tudo vem dos campos da GPU - a descricao do enlace usa a mesma tabela de banda da
 * regra L05 e os conectores sao listados apenas quando existem.</p>
 */
public final class ResumoPlacaVideo {

    private ResumoPlacaVideo() {
        // classe utilitaria: nao deve ser instanciada
    }

    /**
     * Linha de apoio exibida ao lado do preco na lista da etapa 4.
     *
     * @param componente candidato da etapa (deve ser uma {@link PlacaVideo})
     * @return o resumo em uma linha; vazio quando o componente nao e placa de video
     */
    public static String linhaDeApoio(Componente componente) {
        if (!(componente instanceof PlacaVideo gpu)) {
            return "";
        }
        return memoria(gpu) + " | "
                + CalculadoraPcie.descricao(gpu.getVersaoPCIe(), gpu.getPistasPCIe())
                + " | " + gpu.getTdpWatts() + " W"
                + " | " + conectores(gpu);
    }

    private static String memoria(PlacaVideo gpu) {
        String tipo = gpu.getTipoMemoriaVRAM() == null
                ? ""
                : " " + gpu.getTipoMemoriaVRAM().name();
        return gpu.getMemoriaVRAMGB() + " GB" + tipo;
    }

    /** Conectores de energia exigidos pela GPU, por exemplo {@code 1x 16-pinos}. */
    private static String conectores(PlacaVideo gpu) {
        List<String> partes = new ArrayList<>();
        if (gpu.getQuantidadeConectores16pin() > 0) {
            partes.add(gpu.getQuantidadeConectores16pin() + "x 16-pinos");
        }
        if (gpu.getQuantidadeConectores8pin() > 0) {
            partes.add(gpu.getQuantidadeConectores8pin() + "x 8-pinos");
        }
        if (gpu.getQuantidadeConectores6pin() > 0) {
            partes.add(gpu.getQuantidadeConectores6pin() + "x 6-pinos");
        }
        return partes.isEmpty() ? "sem conector extra" : String.join(" + ", partes);
    }
}
