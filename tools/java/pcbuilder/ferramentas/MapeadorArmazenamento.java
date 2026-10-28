package pcbuilder.ferramentas;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pcbuilder.enums.FormatoArmazenamento;
import pcbuilder.enums.InterfaceArmazenamento;
import pcbuilder.enums.TipoMidia;
import pcbuilder.modelo.Armazenamento;
import pcbuilder.modelo.Componente;

/**
 * Converte os registros de {@code open-db/Storage} para o modelo do projeto.
 *
 * <p>O catalogo traz capacidade, tipo de midia, formato e interface. Velocidades,
 * consumo e TBW nao existem no catalogo e sao derivados da interface/geracao - sao
 * valores tipicos de cada classe de produto (ver {@code docs/base-mockada.md}).</p>
 */
final class MapeadorArmazenamento {

    private static final Pattern PCIE = Pattern.compile("PCIe\\s*(\\d)\\.\\d\\s*x(\\d)");
    private static final Pattern SATA = Pattern.compile("SATA\\s*(\\d)");

    private MapeadorArmazenamento() {
        // classe utilitaria: nao deve ser instanciada
    }

    static List<Componente> converter(List<Object> brutos, Estatisticas estatisticas) {
        List<Componente> componentes = new ArrayList<>();
        for (Object bruto : brutos) {
            Componente componente = mapear(new Registro(bruto), estatisticas);
            if (componente != null) {
                componentes.add(componente);
            }
        }
        estatisticas.incluir("ARMAZENAMENTO", componentes.size());
        return componentes;
    }

    private static Componente mapear(Registro registro, Estatisticas estatisticas) {
        TipoMidia tipo = tipoDe(registro.texto("storage_type"));
        if (tipo == null) {
            estatisticas.ignorar("Armazenamento: tipo de midia fora do dominio");
            return null;
        }
        FormatoArmazenamento formato = formatoDe(registro.texto("form_factor"));
        if (formato == null) {
            estatisticas.ignorar("Armazenamento: formato fora do dominio");
            return null;
        }

        String textoInterface = registro.texto("interface");
        boolean nvme;
        Integer versaoPcie = null;
        Integer pistasPcie = null;
        Integer velocidadeSata = null;
        if (textoInterface != null && textoInterface.toUpperCase().contains("PCIE")) {
            nvme = true;
            Matcher pcie = PCIE.matcher(textoInterface);
            if (pcie.find()) {
                versaoPcie = Integer.valueOf(pcie.group(1));
                pistasPcie = Integer.valueOf(pcie.group(2));
            } else {
                versaoPcie = Integer.valueOf(4);
                pistasPcie = Integer.valueOf(4);
            }
        } else if (textoInterface != null && textoInterface.toUpperCase().contains("SATA")) {
            nvme = false;
            Matcher sata = SATA.matcher(textoInterface);
            velocidadeSata = Integer.valueOf(
                    sata.find() ? Math.max(1, Integer.parseInt(sata.group(1))) : 6);
        } else {
            estatisticas.ignorar("Armazenamento: interface fora do dominio");
            return null;
        }

        boolean formatoM2 = formato == FormatoArmazenamento.M2_2280
                || formato == FormatoArmazenamento.M2_2242
                || formato == FormatoArmazenamento.M2_22110;
        if (nvme && !formatoM2) {
            estatisticas.ignorar("Armazenamento: NVMe sem formato M.2");
            return null;
        }

        int capacidade = registro.inteiro("capacity");
        if (capacidade <= 0) {
            estatisticas.ignorar("Armazenamento: sem capacidade");
            return null;
        }

        int leitura;
        int escrita;
        double consumo;
        double pico;
        Integer tbw;
        if (nvme) {
            int geracao = versaoPcie.intValue();
            leitura = geracao >= 5 ? 12000 : geracao >= 4 ? 7000 : geracao >= 3 ? 3400 : 1600;
            escrita = geracao >= 5 ? 11000 : geracao >= 4 ? 6000 : geracao >= 3 ? 2400 : 1400;
            consumo = 5.0;
            pico = 8.0;
            tbw = Integer.valueOf(capacidade / 2);
        } else if (tipo == TipoMidia.SSD) {
            leitura = 550;
            escrita = 520;
            consumo = 3.0;
            pico = 5.0;
            tbw = Integer.valueOf(capacidade / 2);
        } else {
            leitura = 190;
            escrita = 180;
            consumo = 6.0;
            pico = 9.0;
            tbw = null;
        }

        double precoBase = nvme ? 0.30 * capacidade + (versaoPcie.intValue() >= 4 ? 60 : 0)
                : tipo == TipoMidia.SSD ? 0.35 * capacidade : 0.16 * capacidade;

        return Armazenamento.builder()
                .id(registro.id(tipo == TipoMidia.SSD ? "ssd" : "hd"))
                .modelo(registro.nomeLimpo())
                .fabricante(registro.fabricante() == null ? "Desconhecido" : registro.fabricante())
                .precoMedioBRL(Heuristicas.preco(precoBase))
                .garantiaMeses(tipo == TipoMidia.SSD ? 60 : 24)
                .tipoMidia(tipo)
                .interfaceArmazenamento(nvme
                        ? InterfaceArmazenamento.NVME : InterfaceArmazenamento.SATA)
                .formato(formato)
                .capacidadeGB(capacidade)
                .versaoPCIe(versaoPcie)
                .pistasPCIe(pistasPcie)
                .velocidadeSATAGbps(velocidadeSata)
                .leituraSequencialMBs(leitura)
                .escritaSequencialMBs(escrita)
                .tbw(tbw)
                .consumoMedioWatts(Double.valueOf(consumo))
                .consumoPicoWatts(Double.valueOf(pico))
                .construir();
    }

    private static TipoMidia tipoDe(String tipo) {
        if (tipo == null) {
            return null;
        }
        return switch (tipo.trim().toUpperCase()) {
            case "SSD" -> TipoMidia.SSD;
            case "HDD" -> TipoMidia.HDD;
            default -> null;
        };
    }

    private static FormatoArmazenamento formatoDe(String formato) {
        if (formato == null) {
            return null;
        }
        return switch (formato.trim()) {
            case "M.2-2280" -> FormatoArmazenamento.M2_2280;
            case "M.2-2242" -> FormatoArmazenamento.M2_2242;
            case "M.2-22110" -> FormatoArmazenamento.M2_22110;
            case "2.5\"" -> FormatoArmazenamento.SATA_2_5;
            case "3.5\"" -> FormatoArmazenamento.SATA_3_5;
            default -> null;
        };
    }
}
