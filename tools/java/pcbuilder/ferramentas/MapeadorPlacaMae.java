package pcbuilder.ferramentas;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import pcbuilder.enums.Chipset;
import pcbuilder.enums.FormatoPlacaMae;
import pcbuilder.enums.Soquete;
import pcbuilder.enums.TipoMemoria;
import pcbuilder.modelo.Componente;
import pcbuilder.modelo.PlacaMae;

/**
 * Converte os registros de {@code open-db/Motherboard} para o modelo do projeto.
 *
 * <p>O catalogo nao traz VRM, perfis de overclock nem a potencia do slot x16; esses
 * valores sao derivados do chipset (ver {@link #PERFIS}) e do formato da placa. Os
 * chipsets fora do dominio do projeto sao descartados.</p>
 */
final class MapeadorPlacaMae {

    private static final Map<String, Soquete> SOQUETES = Map.of(
            "AM4", Soquete.AM4,
            "AM5", Soquete.AM5,
            "LGA 1200", Soquete.LGA1200,
            "LGA 1700", Soquete.LGA1700,
            "LGA 1851", Soquete.LGA1851);

    /** fases do VRM, corrente por fase, dissipador no VRM e preco base. */
    private record Perfil(int fases, int corrente, boolean dissipador, double preco) {
    }

    private static final Map<String, Perfil> PERFIS = Map.ofEntries(
            Map.entry("A320", new Perfil(6, 40, false, 420)),
            Map.entry("B350", new Perfil(6, 45, true, 480)),
            Map.entry("B450", new Perfil(6, 50, true, 550)),
            Map.entry("A520", new Perfil(6, 45, false, 500)),
            Map.entry("B550", new Perfil(8, 50, true, 750)),
            Map.entry("X570", new Perfil(12, 60, true, 1100)),
            Map.entry("A620", new Perfil(6, 40, false, 650)),
            Map.entry("B650", new Perfil(8, 50, true, 950)),
            Map.entry("B650E", new Perfil(12, 60, true, 1300)),
            Map.entry("X670", new Perfil(14, 60, true, 1600)),
            Map.entry("X670E", new Perfil(16, 70, true, 2000)),
            Map.entry("B850", new Perfil(12, 60, true, 1500)),
            Map.entry("X870", new Perfil(14, 70, true, 2100)),
            Map.entry("X870E", new Perfil(16, 80, true, 2800)),
            Map.entry("H410", new Perfil(5, 40, false, 450)),
            Map.entry("B460", new Perfil(6, 50, true, 600)),
            Map.entry("H510", new Perfil(5, 40, false, 550)),
            Map.entry("B560", new Perfil(8, 50, true, 750)),
            Map.entry("Z490", new Perfil(12, 55, true, 1200)),
            Map.entry("Z590", new Perfil(12, 60, true, 1400)),
            Map.entry("H610", new Perfil(6, 40, false, 600)),
            Map.entry("B660", new Perfil(8, 50, true, 800)),
            Map.entry("B760", new Perfil(8, 50, true, 950)),
            Map.entry("Z690", new Perfil(14, 60, true, 1400)),
            Map.entry("Z790", new Perfil(16, 60, true, 1800)),
            Map.entry("B860", new Perfil(12, 60, true, 1300)),
            Map.entry("Z890", new Perfil(16, 70, true, 2400)));

    /** Chipsets que o catalogo associa a kits de memoria mais rapidos. */
    private static final Set<String> DDR5_RAPIDA = Set.of(
            "B650E", "X670E", "B850", "X870", "X870E", "Z690", "Z790", "B860", "Z890");

    private MapeadorPlacaMae() {
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
        estatisticas.incluir("PLACA_MAE", componentes.size());
        return componentes;
    }

    private static Componente mapear(Registro registro, Estatisticas estatisticas) {
        String nomeSoquete = registro.texto("socket");
        Soquete soquete = nomeSoquete == null ? null : SOQUETES.get(nomeSoquete);
        if (soquete == null) {
            estatisticas.ignorar("Placa-mae: soquete fora do dominio");
            return null;
        }

        String nomeChipset = semFabricante(registro.texto("chipset"));
        Chipset chipset = chipsetDe(nomeChipset);
        Perfil perfil = nomeChipset == null ? null : PERFIS.get(nomeChipset);
        if (chipset == null || perfil == null) {
            estatisticas.ignorar("Placa-mae: chipset fora do dominio");
            return null;
        }

        FormatoPlacaMae formato = formatoDe(registro.texto("form_factor"));
        if (formato == null) {
            estatisticas.ignorar("Placa-mae: formato fora do dominio");
            return null;
        }

        int slots = registro.inteiro("memory.slots");
        TipoMemoria tipoMemoria = tipoMemoriaDe(registro.texto("memory.ram_type"));
        if (slots <= 0) {
            estatisticas.ignorar("Placa-mae: sem slots de memoria");
            return null;
        }
        if (tipoMemoria == null) {
            estatisticas.ignorar("Placa-mae: tipo de memoria fora do dominio");
            return null;
        }

        List<Integer> geracoesM2 = geracoesM2(registro);
        if (geracoesM2.isEmpty()) {
            estatisticas.ignorar("Placa-mae: sem slot M.2 PCIe");
            return null;
        }
        int[] pcie16 = slotPcie16(registro);
        if (pcie16 == null) {
            estatisticas.ignorar("Placa-mae: sem slot PCIe x16");
            return null;
        }

        int sata6 = registro.inteiro("storage_devices.sata_6_gb_s");
        int sata3 = registro.inteiro("storage_devices.sata_3_gb_s");
        if (sata6 + sata3 == 0) {
            // falha comum no catalogo: a placa tem portas, mas o campo veio zerado
            sata6 = 4;
        }
        List<Object> cpuPower = registro.lista("power_connectors.cpu_power");
        int conectores8 = contar(cpuPower, "8-pin");
        int conectores4 = contar(cpuPower, "4-pin");
        if (conectores8 + conectores4 == 0) {
            conectores8 = 1;
        }

        int capacidade = registro.inteiro("memory.max");
        BigDecimal preco = Heuristicas.preco(perfil.preco() + ajusteFormato(formato)
                + (slots >= 4 ? 50 : 0) + (geracoesM2.contains(5) ? 120 : 0));

        return PlacaMae.builder()
                .id(registro.id("placa-mae"))
                .modelo(registro.nomeLimpo())
                .fabricante(registro.fabricante() == null ? "Desconhecido" : registro.fabricante())
                .consumoEstimadoWatts(consumo(formato))
                .precoMedioBRL(preco)
                .garantiaMeses(36)
                .soqueteCPU(soquete)
                .chipset(chipset)
                .formato(formato)
                .comprimentoMm(comprimento(formato))
                .larguraMm(largura(formato))
                .suportaAtualizacaoBIOSSemCPU(
                        Boolean.TRUE.equals(registro.booleano("bios_features.flashback")))
                .vrmFasesCPU(perfil.fases())
                .vrmCorrentePorFaseA(perfil.corrente())
                .vrmPossuiDissipador(perfil.dissipador())
                .quantidadeSlotsMemoria(slots)
                .tipoMemoria(tipoMemoria)
                .canaisMemoria(2)
                .capacidadeMaximaMemoriaGB(capacidade > 0 ? capacidade : slots * 32)
                .frequenciaMemoriaMaximaMTs(frequenciaMemoria(tipoMemoria, nomeChipset))
                .suportaPerfilEXPO(soquete == Soquete.AM4 || soquete == Soquete.AM5)
                .suportaPerfilXMP(true)
                .suportaECC(Boolean.TRUE.equals(registro.booleano("ecc_support")))
                .quantidadeSlotsM2(geracoesM2.size())
                .geracaoMaximaSlotM2(geracoesM2.stream().mapToInt(Integer::intValue).max().orElse(4))
                .geracaoMinimaSlotM2(geracoesM2.stream().mapToInt(Integer::intValue).min().orElse(4))
                .quantidadeSlotsPCIe16(pcie16[0])
                .versaoSlotPCIe16(pcie16[1])
                .pistasSlotPCIe16(16)
                .quantidadePortasSATA(sata6 + sata3)
                .velocidadeSATAGbps(sata6 > 0 ? 6 : 3)
                .quantidadeConectoresCPU8pin(conectores8)
                .quantidadeConectoresCPU4pin(conectores4)
                .quantidadeConectoresATX24pin(1)
                .construir();
    }

    // ------------------------------------------------------------- auxiliares

    /** "AMD B650" -&gt; "B650"; "Intel B760" -&gt; "B760". */
    private static String semFabricante(String chipset) {
        if (chipset == null) {
            return null;
        }
        String resultado = chipset.trim().replaceFirst("(?i)^(AMD|Intel)\\s+", "");
        return resultado.toUpperCase();
    }

    private static Chipset chipsetDe(String nome) {
        if (nome == null) {
            return null;
        }
        try {
            return Chipset.valueOf(nome);
        } catch (IllegalArgumentException foraDoDominio) {
            return null;
        }
    }

    private static FormatoPlacaMae formatoDe(String formato) {
        if (formato == null) {
            return null;
        }
        return switch (formato.trim().toLowerCase()) {
            case "atx" -> FormatoPlacaMae.ATX;
            case "micro atx", "matx" -> FormatoPlacaMae.MATX;
            case "mini-itx", "mini itx" -> FormatoPlacaMae.MINI_ITX;
            case "eatx", "e-atx" -> FormatoPlacaMae.E_ATX;
            default -> null;
        };
    }

    private static TipoMemoria tipoMemoriaDe(String tipo) {
        if (tipo == null) {
            return null;
        }
        return switch (tipo.trim().toUpperCase()) {
            case "DDR4" -> TipoMemoria.DDR4;
            case "DDR5" -> TipoMemoria.DDR5;
            default -> null;
        };
    }

    /** Geracoes PCIe dos slots M.2 (um item por slot com PCIe; SATA puro fica fora). */
    private static List<Integer> geracoesM2(Registro registro) {
        List<Integer> geracoes = new ArrayList<>();
        for (Object slot : registro.lista("m2_slots")) {
            String interfaceSlot = new Registro(slot).texto("interface");
            if (interfaceSlot == null || !interfaceSlot.toUpperCase().contains("PCIE")) {
                continue;
            }
            geracoes.add(geracaoPcie(interfaceSlot));
        }
        return geracoes;
    }

    /** {quantidade de slots x16, geracao do melhor slot x16} ou {@code null}. */
    private static int[] slotPcie16(Registro registro) {
        int quantidade = 0;
        int geracao = 0;
        for (Object slot : registro.lista("pcie_slots")) {
            Registro dados = new Registro(slot);
            if (dados.inteiro("lanes") < 16) {
                continue;
            }
            quantidade += Math.max(1, dados.inteiro("quantity"));
            geracao = Math.max(geracao, geracaoPcie(dados.texto("gen")));
        }
        return quantidade == 0 ? null : new int[] {quantidade, Math.max(1, geracao)};
    }

    /** Extrai a geracao de um texto como "4.0", "PCIe 5.0 x4" ou "PCIe 3.0". */
    static int geracaoPcie(String texto) {
        if (texto == null) {
            return 4;
        }
        java.util.regex.Matcher numero = java.util.regex.Pattern
                .compile("(\\d)(\\.\\d)?").matcher(texto);
        if (numero.find()) {
            return Math.max(1, Integer.parseInt(numero.group(1)));
        }
        return 4;
    }

    private static int contar(List<Object> itens, String procurado) {
        int total = 0;
        for (Object item : itens) {
            if (procurado.equals(item)) {
                total++;
            }
        }
        return total;
    }

    private static double ajusteFormato(FormatoPlacaMae formato) {
        return switch (formato) {
            case ATX -> 100;
            case E_ATX -> 300;
            case MINI_ITX -> 80;
            default -> 0;
        };
    }

    private static int comprimento(FormatoPlacaMae formato) {
        return switch (formato) {
            case E_ATX, ATX -> 305;
            case MATX -> 244;
            case MINI_ITX -> 170;
        };
    }

    private static int largura(FormatoPlacaMae formato) {
        return switch (formato) {
            case E_ATX -> 330;
            case ATX, MATX -> 244;
            case MINI_ITX -> 170;
        };
    }

    private static int consumo(FormatoPlacaMae formato) {
        return switch (formato) {
            case E_ATX -> 45;
            case ATX -> 35;
            case MATX -> 30;
            case MINI_ITX -> 25;
        };
    }

    private static int frequenciaMemoria(TipoMemoria tipo, String chipset) {
        if (tipo == TipoMemoria.DDR4) {
            return 3200;
        }
        return DDR5_RAPIDA.contains(chipset) ? 7600 : 6400;
    }
}
