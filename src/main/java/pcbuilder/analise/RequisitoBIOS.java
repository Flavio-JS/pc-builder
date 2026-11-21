package pcbuilder.analise;

import pcbuilder.enums.Chipset;
import pcbuilder.enums.Soquete;
import pcbuilder.modelo.PlacaMae;
import pcbuilder.modelo.Processador;

/**
 * Requisito de BIOS da CPU na placa-mae (regra <strong>C09</strong>).
 *
 * <p>O modelo de dados descreve uma tabela {@code (idPlacaMae, idProcessador)} com a
 * versao minima de BIOS de cada par. Essa tabela <strong>nao existe</strong> na base
 * convertida do OpenDB e o catalogo nao traz o ano de lancamento das placas. Para a
 * HU-005 o requisito e estimado por familia de chipset:</p>
 *
 * <table border="1">
 *   <caption>Familias que podem exigir atualizacao de BIOS</caption>
 *   <tr><th>Soquete</th><th>Familia da placa</th><th>CPUs que podem exigir update</th></tr>
 *   <tr><td>AM5</td><td>A620, B650, B650E, X670, X670E</td><td>lancadas em 2024+</td></tr>
 *   <tr><td>AM4</td><td>A320, B350, B450, X570</td><td>lancadas em 2020+ (Ryzen 5000)</td></tr>
 *   <tr><td>LGA1700</td><td>H610, B660, Z690</td><td>lancadas em 2023+ (13a/14a geracao)</td></tr>
 *   <tr><td>LGA1200</td><td>H410, B460, Z490</td><td>lancadas em 2021+ (11a geracao)</td></tr>
 * </table>
 *
 * <p>As demais familias (B550/A520, B760/Z790, H510/B560/Z590, B850/X870, B860/Z890)
 * sairam de fabrica com suporte a geracao correspondente e nao geram aviso. E uma
 * estimativa: no fluxo guiado a C09 <strong>nunca oculta</strong> a placa (HU-005) - ela
 * apenas gera o aviso {@code pode exigir atualizacao de BIOS} com a dica de BIOS
 * Flashback quando {@code suportaAtualizacaoBIOSSemCPU = true}.</p>
 */
public final class RequisitoBIOS {

    private RequisitoBIOS() {
        // classe utilitaria: nao deve ser instanciada
    }

    /**
     * Ano de lancamento do chipset (0 quando nao esta na tabela).
     *
     * @param chipset chipset da placa-mae
     * @return o ano, ou {@code 0} quando desconhecido
     */
    public static int anoDoChipset(Chipset chipset) {
        if (chipset == null) {
            return 0;
        }
        switch (chipset) {
            // AMD AM4
            case A320:
            case B350:
                return 2017;
            case B450:
                return 2018;
            case X570:
                return 2019;
            case A520:
            case B550:
                return 2020;
            // AMD AM5
            case B650:
            case B650E:
            case X670:
            case X670E:
                return 2022;
            case A620:
                return 2023;
            case X870:
            case X870E:
                return 2024;
            case B850:
                return 2025;
            // Intel LGA1200
            case H410:
            case B460:
            case Z490:
                return 2020;
            case H510:
            case B560:
            case Z590:
                return 2021;
            // Intel LGA1700
            case Z690:
                return 2021;
            case H610:
            case B660:
            case Z790:
                return 2022;
            case B760:
                return 2023;
            // Intel LGA1851
            case Z890:
                return 2024;
            case B860:
                return 2025;
            default:
                return 0;
        }
    }

    /**
     * O par CPU + placa <em>pode</em> exigir atualizacao de BIOS?
     *
     * @param cpu processador escolhido
     * @param placaMae placa-mae candidata
     * @return {@code true} quando a familia da placa antecede a geracao da CPU
     */
    public static boolean podeExigirAtualizacao(Processador cpu, PlacaMae placaMae) {
        Chipset chipset = placaMae.getChipset();
        Integer ano = cpu.getAnoLancamento();
        Soquete soquete = cpu.getSoquete();
        if (chipset == null || ano == null || soquete == null) {
            return false;
        }
        switch (soquete) {
            case AM5:
                return ano >= 2024 && chipsetAm5Serie600(chipset);
            case AM4:
                return ano >= 2020 && chipsetAm4AnteriorB550(chipset);
            case LGA1700:
                return ano >= 2023 && chipsetIntelSerie600(chipset);
            case LGA1200:
                return ano >= 2021 && chipsetIntelSerie400(chipset);
            default:
                // LGA1851: as 800-series sairam junto com as CPUs suportadas.
                return false;
        }
    }

    /** AM5: 600-series lancadas para Ryzen 7000 (2022/2023). */
    private static boolean chipsetAm5Serie600(Chipset chipset) {
        switch (chipset) {
            case A620:
            case B650:
            case B650E:
            case X670:
            case X670E:
                return true;
            default:
                return false;
        }
    }

    /** AM4: placas anteriores a B550/A520, lancadas antes do Ryzen 5000. */
    private static boolean chipsetAm4AnteriorB550(Chipset chipset) {
        switch (chipset) {
            case A320:
            case B350:
            case B450:
            case X570:
                return true;
            default:
                return false;
        }
    }

    /** Intel LGA1700: 600-series lancadas para a 12a geracao. */
    private static boolean chipsetIntelSerie600(Chipset chipset) {
        switch (chipset) {
            case H610:
            case B660:
            case Z690:
                return true;
            default:
                return false;
        }
    }

    /** Intel LGA1200: 400-series lancadas para a 10a geracao. */
    private static boolean chipsetIntelSerie400(Chipset chipset) {
        switch (chipset) {
            case H410:
            case B460:
            case Z490:
                return true;
            default:
                return false;
        }
    }
}
