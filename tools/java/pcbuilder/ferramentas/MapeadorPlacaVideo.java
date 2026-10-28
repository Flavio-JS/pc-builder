package pcbuilder.ferramentas;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pcbuilder.enums.FabricanteChip;
import pcbuilder.enums.Resolucao;
import pcbuilder.enums.TipoMemoriaVRAM;
import pcbuilder.modelo.Componente;
import pcbuilder.modelo.PlacaVideo;

/**
 * Converte os registros de {@code open-db/GPU} para o modelo do projeto.
 *
 * <p>O catalogo informa VRAM, TDP, dimensoes e (em parte dos registros) os
 * conectores de energia. Quando os conectores vem zerados eles sao derivados do
 * TDP; o pico transiente, a fonte recomendada e o indice de desempenho tambem sao
 * calculados - ver {@code docs/base-mockada.md}.</p>
 */
final class MapeadorPlacaVideo {

    private static final Pattern LANES = Pattern.compile("x(\\d+)");
    private static final Pattern GERACAO = Pattern.compile("PCIe\\s*(\\d)");
    private static final Pattern RAY_TRACING = Pattern.compile("(?i)(RTX|Radeon RX (6|7|9)|Arc)");

    private MapeadorPlacaVideo() {
        // classe utilitaria: nao deve ser instanciada
    }

    static List<Componente> converter(List<Object> brutos, Estatisticas estatisticas) {
        List<Candidato> candidatos = new ArrayList<>();
        for (Object bruto : brutos) {
            Candidato candidato = mapear(new Registro(bruto), estatisticas);
            if (candidato != null) {
                candidatos.add(candidato);
            }
        }
        return montar(candidatos, estatisticas);
    }

    private static Candidato mapear(Registro registro, Estatisticas estatisticas) {
        FabricanteChip fabricanteChip = fabricanteDe(registro.texto("chipset_manufacturer"));
        if (fabricanteChip == null) {
            estatisticas.ignorar("Placa de video: fabricante fora do dominio");
            return null;
        }
        int vram = registro.inteiro("memory");
        int tdp = registro.inteiro("tdp");
        if (vram <= 0 || tdp <= 0) {
            estatisticas.ignorar("Placa de video: sem VRAM/TDP");
            return null;
        }

        String interfacePcie = registro.texto("interface");
        int[] pcie = pcie(interfacePcie);

        Candidato candidato = new Candidato();
        candidato.id = registro.id("gpu");
        candidato.modelo = registro.nomeLimpo();
        candidato.fabricante = registro.fabricante() == null ? "Desconhecido" : registro.fabricante();
        candidato.ano = registro.inteiroNulo("metadata.releaseYear");
        candidato.fabricanteChip = fabricanteChip;
        candidato.vram = vram;
        candidato.tipoVram = tipoVramDe(registro.texto("memory_type"));
        candidato.versaoPcie = pcie[0];
        candidato.pistasPcie = pcie[1];
        candidato.tdp = tdp;
        candidato.comprimento = registro.inteiro("length") > 0
                ? registro.inteiro("length") : comprimentoPadrao(tdp);
        candidato.slots = registro.inteiro("total_slot_width") > 0
                ? registro.inteiro("total_slot_width") : 2;
        candidato.pico = (int) (Math.ceil(tdp * 1.6 / 10.0) * 10);
        candidato.fonteRecomendada = Math.max(400, Heuristicas.multiplo50(tdp * 2.6));
        candidato.resolucao = vram >= 16 ? Resolucao.UHD_4K
                : vram >= 12 ? Resolucao.QHD_1440P : Resolucao.FHD_1080P;

        int conectores16 = registro.inteiro("power_connectors.pcie_12VHPWR")
                + registro.inteiro("power_connectors.pcie_12V_2x6");
        int conectores8 = registro.inteiro("power_connectors.pcie_8_pin");
        int conectores6 = registro.inteiro("power_connectors.pcie_6_pin");
        if (conectores16 + conectores8 + conectores6 == 0) {
            if (tdp >= 250) {
                conectores16 = 1;
            } else if (tdp >= 150) {
                conectores8 = 2;
            } else if (tdp >= 75) {
                conectores8 = 1;
            }
        }
        candidato.conectores16 = conectores16;
        candidato.conectores8 = conectores8;
        candidato.conectores6 = conectores6;
        candidato.rayTracing = RAY_TRACING.matcher(
                registro.texto("chipset") + " " + candidato.modelo).find();
        candidato.proxy = Math.max(1, registro.inteiro("core_count"))
                * Math.max(1, registro.inteiro("core_boost_clock"));
        candidato.preco = Heuristicas.preco(100 + 80.0 * vram + 9.0 * tdp);
        return candidato;
    }

    private static List<Componente> montar(List<Candidato> candidatos, Estatisticas estatisticas) {
        double menor = Double.MAX_VALUE;
        double maior = -Double.MAX_VALUE;
        for (Candidato candidato : candidatos) {
            menor = Math.min(menor, candidato.proxy);
            maior = Math.max(maior, candidato.proxy);
        }

        List<Componente> componentes = new ArrayList<>();
        for (Candidato candidato : candidatos) {
            componentes.add(PlacaVideo.builder()
                    .id(candidato.id)
                    .modelo(candidato.modelo)
                    .fabricante(candidato.fabricante)
                    .precoMedioBRL(candidato.preco)
                    .garantiaMeses(36)
                    .anoLancamento(candidato.ano)
                    .fabricanteChip(candidato.fabricanteChip)
                    .memoriaVRAMGB(candidato.vram)
                    .tipoMemoriaVRAM(candidato.tipoVram)
                    .versaoPCIe(candidato.versaoPcie)
                    .pistasPCIe(candidato.pistasPcie)
                    .comprimentoMm(candidato.comprimento)
                    .espessuraSlots(candidato.slots)
                    .tdpWatts(candidato.tdp)
                    .picoTransienteWatts(candidato.pico)
                    .fonteRecomendadaWatts(candidato.fonteRecomendada)
                    .quantidadeConectores16pin(candidato.conectores16)
                    .quantidadeConectores8pin(candidato.conectores8)
                    .quantidadeConectores6pin(candidato.conectores6)
                    .resolucaoAlvo(candidato.resolucao)
                    .indiceDesempenhoGPU(Heuristicas.normalizar(candidato.proxy, menor, maior))
                    .possuiRayTracing(candidato.rayTracing)
                    .construir());
        }
        estatisticas.incluir("PLACA_VIDEO", componentes.size());
        return componentes;
    }

    private static FabricanteChip fabricanteDe(String fabricante) {
        if (fabricante == null) {
            return null;
        }
        return switch (fabricante.trim().toUpperCase()) {
            case "NVIDIA" -> FabricanteChip.NVIDIA;
            case "AMD" -> FabricanteChip.AMD;
            case "INTEL" -> FabricanteChip.INTEL;
            default -> null;
        };
    }

    private static TipoMemoriaVRAM tipoVramDe(String tipo) {
        if (tipo == null) {
            return null;
        }
        return switch (tipo.trim().toUpperCase()) {
            case "GDDR6" -> TipoMemoriaVRAM.GDDR6;
            case "GDDR6X" -> TipoMemoriaVRAM.GDDR6X;
            case "GDDR7" -> TipoMemoriaVRAM.GDDR7;
            case "HBM2" -> TipoMemoriaVRAM.HBM2;
            default -> null;
        };
    }

    /** {versao PCIe, pistas} a partir de "PCIe 4.0 x16" (versao 4 e 16 pistas no padrao). */
    private static int[] pcie(String interfacePcie) {
        int versao = 4;
        int pistas = 16;
        if (interfacePcie != null) {
            Matcher geracao = GERACAO.matcher(interfacePcie);
            if (geracao.find()) {
                versao = Integer.parseInt(geracao.group(1));
            }
            Matcher lanes = LANES.matcher(interfacePcie);
            if (lanes.find()) {
                pistas = Integer.parseInt(lanes.group(1));
            }
        }
        return new int[] {Math.max(1, versao), Math.max(1, pistas)};
    }

    private static int comprimentoPadrao(int tdp) {
        if (tdp >= 250) {
            return 300;
        }
        return tdp >= 150 ? 250 : 200;
    }

    /** Dados derivados de uma placa de video antes de normalizar o indice. */
    private static final class Candidato {
        private String id;
        private String modelo;
        private String fabricante;
        private Integer ano;
        private FabricanteChip fabricanteChip;
        private int vram;
        private TipoMemoriaVRAM tipoVram;
        private int versaoPcie;
        private int pistasPcie;
        private int comprimento;
        private int slots;
        private int tdp;
        private int pico;
        private int fonteRecomendada;
        private int conectores16;
        private int conectores8;
        private int conectores6;
        private Resolucao resolucao;
        private boolean rayTracing;
        private double proxy;
        private BigDecimal preco;
    }
}
