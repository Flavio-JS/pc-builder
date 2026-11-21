package pcbuilder.servico.resumos;

import java.util.ArrayList;
import java.util.List;

import pcbuilder.enums.Certificado80Plus;
import pcbuilder.modelo.Componente;
import pcbuilder.modelo.Fonte;

/**
 * Linha de apoio da etapa 6 (HU-009): resume a fonte para quem nao sabe o que e selo
 * 80 Plus nem trilho de 12 V.
 *
 * <p>Formato da historia de usuario:</p>
 * <pre>
 * 750 W | 80 Plus Gold | 12V: 744 W | 1x 16-pinos | 2x 8-pinos
 * </pre>
 *
 * <p>Tudo vem dos campos da fonte; a ausencia de selo aparece como
 * {@code sem selo 80 Plus} e a de conectores PCIe como {@code sem conector PCIe}.</p>
 */
public final class ResumoFonte {

    private ResumoFonte() {
        // classe utilitaria: nao deve ser instanciada
    }

    /**
     * Linha de apoio exibida ao lado do preco na lista da etapa 6.
     *
     * @param componente candidato da etapa (deve ser uma {@link Fonte})
     * @return o resumo em uma linha; vazio quando o componente nao e fonte
     */
    public static String linhaDeApoio(Componente componente) {
        if (!(componente instanceof Fonte fonte)) {
            return "";
        }
        List<String> partes = new ArrayList<>();
        partes.add(fonte.getPotenciaTotalWatts() + " W");
        partes.add(certificacao(fonte));
        if (fonte.getPotenciaTrilho12VWatts() > 0) {
            partes.add("12V: " + fonte.getPotenciaTrilho12VWatts() + " W");
        }
        partes.add(conectores(fonte));
        return String.join(" | ", partes);
    }

    private static String certificacao(Fonte fonte) {
        Certificado80Plus certificacao = fonte.getCertificacao80Plus();
        if (certificacao == null || certificacao == Certificado80Plus.NENHUM) {
            return "sem selo 80 Plus";
        }
        String nome = certificacao.name();
        return "80 Plus " + nome.charAt(0) + nome.substring(1).toLowerCase();
    }

    private static String conectores(Fonte fonte) {
        List<String> partes = new ArrayList<>();
        if (fonte.getQuantidadeConectores16pin12VHPWR() > 0) {
            partes.add(fonte.getQuantidadeConectores16pin12VHPWR() + "x 16-pinos");
        }
        if (fonte.getQuantidadeConectoresPCIe8pin() > 0) {
            partes.add(fonte.getQuantidadeConectoresPCIe8pin() + "x 8-pinos");
        }
        if (fonte.getQuantidadeConectoresPCIe6pin() > 0) {
            partes.add(fonte.getQuantidadeConectoresPCIe6pin() + "x 6-pinos");
        }
        return partes.isEmpty() ? "sem conector PCIe" : String.join(" | ", partes);
    }
}
