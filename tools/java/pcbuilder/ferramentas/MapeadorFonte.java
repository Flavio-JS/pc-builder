package pcbuilder.ferramentas;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import pcbuilder.enums.Certificado80Plus;
import pcbuilder.enums.Modularidade;
import pcbuilder.enums.PadraoATX;
import pcbuilder.modelo.Componente;
import pcbuilder.modelo.Fonte;

/**
 * Converte os registros de {@code open-db/PSU} para o modelo do projeto.
 *
 * <p>O catalogo informa potencia, certificacao, modularidade, comprimento e os
 * conectores. Quando a lista de conectores vem zerada (falha comum), ela e
 * derivada da potencia. A potencia do trilho 12 V, a eficiencia e o padrao ATX
 * tambem sao derivados - ver {@code docs/base-mockada.md}.</p>
 */
final class MapeadorFonte {

    private static final Map<String, Certificado80Plus> CERTIFICACOES = Map.ofEntries(
            Map.entry("80+", Certificado80Plus.WHITE),
            Map.entry("80+ WHITE", Certificado80Plus.WHITE),
            Map.entry("80+ STANDARD", Certificado80Plus.WHITE),
            Map.entry("80+ BRONZE", Certificado80Plus.BRONZE),
            Map.entry("80+ SILVER", Certificado80Plus.SILVER),
            Map.entry("80+ GOLD", Certificado80Plus.GOLD),
            Map.entry("80+ PLATINUM", Certificado80Plus.PLATINUM),
            Map.entry("80+ TITANIUM", Certificado80Plus.TITANIUM));

    private MapeadorFonte() {
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
        estatisticas.incluir("FONTE", componentes.size());
        return componentes;
    }

    private static Componente mapear(Registro registro, Estatisticas estatisticas) {
        int potencia = registro.inteiro("wattage");
        if (potencia <= 0) {
            estatisticas.ignorar("Fonte: sem potencia");
            return null;
        }

        Certificado80Plus certificacao = certificacaoDe(registro.texto("efficiency_rating"));
        Modularidade modularidade = modularidadeDe(registro.texto("modular"));

        int conectores16 = registro.inteiro("connectors.pcie_12vhpwr");
        int conectores8 = registro.inteiro("connectors.pcie_6_plus_2_pin");
        int conectoresCpu8 = registro.inteiro("connectors.eps_8_pin");
        int conectoresSata = registro.inteiro("connectors.sata");
        Integer conectoresMolex = registro.inteiroNulo("connectors.molex_4_pin");

        if (conectores8 == 0 && potencia >= 400) {
            conectores8 = (int) Math.max(1, Math.min(8, Math.round(potencia / 250.0)));
        }
        if (conectoresCpu8 == 0) {
            conectoresCpu8 = potencia >= 700 ? 2 : 1;
        }
        if (conectoresSata == 0) {
            conectoresSata = 4;
        }

        int comprimento = registro.inteiro("length");
        double precoBase = 0.55 * potencia * fatorPreco(certificacao)
                + (modularidade == Modularidade.TOTALMENTE_MODULAR ? 80
                        : modularidade == Modularidade.SEMI_MODULAR ? 40 : 0);

        return Fonte.builder()
                .id(registro.id("fonte"))
                .modelo(registro.nomeLimpo())
                .fabricante(registro.fabricante() == null ? "Desconhecido" : registro.fabricante())
                .precoMedioBRL(Heuristicas.preco(precoBase))
                .garantiaMeses(garantia(certificacao))
                .potenciaTotalWatts(potencia)
                .potenciaTrilho12VWatts(potencia)
                .certificacao80Plus(certificacao)
                .eficienciaPercentualCarga50(Double.valueOf(eficiencia(certificacao)))
                .modularidade(modularidade)
                .padraoATX(conectores16 > 0 ? PadraoATX.ATX_3_0 : PadraoATX.ATX_2_4)
                .quantidadeConectores16pin12VHPWR(conectores16)
                .quantidadeConectoresPCIe8pin(conectores8)
                .quantidadeConectoresPCIe6pin(0)
                .quantidadeConectoresCPU8pin(conectoresCpu8)
                .quantidadeConectoresCPU4pin(0)
                .quantidadeConectoresSATA(conectoresSata)
                .quantidadeConectoresMolex(conectoresMolex)
                .comprimentoMm(comprimento > 0 ? comprimento : 140)
                .alturaMm(Integer.valueOf(86))
                .possuiPFCAtivo(Boolean.TRUE)
                .construir();
    }

    private static int garantia(Certificado80Plus certificacao) {
        return switch (certificacao) {
            case GOLD, PLATINUM, TITANIUM -> 120;
            default -> 36;
        };
    }

    private static Certificado80Plus certificacaoDe(String certificacao) {
        if (certificacao == null) {
            return Certificado80Plus.NENHUM;
        }
        String chave = certificacao.trim().toUpperCase().replaceAll("\\s+", " ");
        return CERTIFICACOES.getOrDefault(chave, Certificado80Plus.NENHUM);
    }

    private static Modularidade modularidadeDe(String modularidade) {
        if (modularidade == null) {
            return Modularidade.NAO_MODULAR;
        }
        return switch (modularidade.trim().toLowerCase()) {
            case "full", "totalmente modular" -> Modularidade.TOTALMENTE_MODULAR;
            case "semi-modular", "semi modular" -> Modularidade.SEMI_MODULAR;
            default -> Modularidade.NAO_MODULAR;
        };
    }

    private static double eficiencia(Certificado80Plus certificacao) {
        return switch (certificacao) {
            case WHITE -> 82;
            case BRONZE -> 85;
            case SILVER -> 87;
            case GOLD -> 90;
            case PLATINUM -> 92;
            case TITANIUM -> 94;
            default -> 80;
        };
    }

    private static double fatorPreco(Certificado80Plus certificacao) {
        return switch (certificacao) {
            case WHITE -> 0.95;
            case BRONZE -> 1.0;
            case SILVER -> 1.05;
            case GOLD -> 1.15;
            case PLATINUM -> 1.35;
            case TITANIUM -> 1.6;
            default -> 0.9;
        };
    }
}
