package pcbuilder.servico.resumos;

import java.util.ArrayList;
import java.util.List;

import pcbuilder.enums.InterfaceArmazenamento;
import pcbuilder.enums.TipoMidia;
import pcbuilder.modelo.Armazenamento;
import pcbuilder.modelo.Componente;

/**
 * Linha de apoio da etapa 5 (HU-008): resume a midia para quem nao diferencia SSD de HD.
 *
 * <p>Formatos da historia de usuario:</p>
 * <pre>
 * SSD NVMe M.2 2280 | 2 TB | 7000 MB/s
 * HD SATA 3.5" | 2 TB | 200 MB/s
 * </pre>
 *
 * <p>Capacidade, interface, formato e velocidade vem dos campos do componente; a
 * capacidade e convertida para TB quando e multipla de 1000 GB.</p>
 */
public final class ResumoArmazenamento {

    private ResumoArmazenamento() {
        // classe utilitaria: nao deve ser instanciada
    }

    /**
     * Linha de apoio exibida ao lado do preco na lista da etapa 5.
     *
     * @param componente candidato da etapa (deve ser um {@link Armazenamento})
     * @return o resumo em uma linha; vazio quando o componente nao e armazenamento
     */
    public static String linhaDeApoio(Componente componente) {
        if (!(componente instanceof Armazenamento armazenamento)) {
            return "";
        }
        List<String> partes = new ArrayList<>();
        String midia = descricaoMidia(armazenamento);
        if (!midia.isEmpty()) {
            partes.add(midia);
        }
        if (armazenamento.getCapacidadeGB() > 0) {
            partes.add(capacidade(armazenamento.getCapacidadeGB()));
        }
        if (armazenamento.getLeituraSequencialMBs() > 0) {
            partes.add(armazenamento.getLeituraSequencialMBs() + " MB/s");
        }
        return String.join(" | ", partes);
    }

    /** {@code SSD NVMe M.2 2280} ou {@code HD SATA 3.5"}. */
    private static String descricaoMidia(Armazenamento armazenamento) {
        List<String> partes = new ArrayList<>();
        if (armazenamento.getTipoMidia() != null) {
            partes.add(armazenamento.getTipoMidia() == TipoMidia.HDD ? "HD" : "SSD");
        }
        if (armazenamento.getInterfaceArmazenamento() != null) {
            partes.add(armazenamento.getInterfaceArmazenamento()
                    == InterfaceArmazenamento.NVME ? "NVMe" : "SATA");
        }
        String formato = formato(armazenamento);
        if (!formato.isEmpty()) {
            partes.add(formato);
        }
        return String.join(" ", partes);
    }

    private static String formato(Armazenamento armazenamento) {
        if (armazenamento.getFormato() == null) {
            return "";
        }
        switch (armazenamento.getFormato()) {
            case M2_2280:
                return "M.2 2280";
            case M2_2242:
                return "M.2 2242";
            case M2_22110:
                return "M.2 22110";
            case SATA_2_5:
                return "2.5\"";
            case SATA_3_5:
                return "3.5\"";
            default:
                return "";
        }
    }

    /** {@code 2 TB} para 2000 GB; {@code 512 GB} para 512. */
    private static String capacidade(int gigabytes) {
        if (gigabytes >= 1000 && gigabytes % 1000 == 0) {
            return (gigabytes / 1000) + " TB";
        }
        return gigabytes + " GB";
    }
}
