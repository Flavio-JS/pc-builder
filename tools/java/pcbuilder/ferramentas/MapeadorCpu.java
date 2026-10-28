package pcbuilder.ferramentas;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import pcbuilder.enums.Soquete;
import pcbuilder.enums.TipoMemoria;
import pcbuilder.modelo.Componente;
import pcbuilder.modelo.Processador;

/**
 * Converte os registros de {@code open-db/CPU} para o modelo do projeto.
 *
 * <p>So entram processadores de soquetes que o dominio do projeto representa
 * (AM4, AM5, LGA1200, LGA1700, LGA1851) e que aceitam DDR4 ou DDR5. Os campos que
 * o catalogo nao traz (tensao, IMC, pistas PCIe, indices) sao derivados por
 * soquete e normalizados dentro do proprio catalogo.</p>
 */
final class MapeadorCpu {

    private static final Map<String, Soquete> SOQUETES = Map.of(
            "AM4", Soquete.AM4,
            "AM5", Soquete.AM5,
            "LGA 1200", Soquete.LGA1200,
            "LGA 1700", Soquete.LGA1700,
            "LGA 1851", Soquete.LGA1851,
            "AM3", Soquete.AM3);

    private MapeadorCpu() {
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
        String nomeSoquete = registro.texto("socket");
        Soquete soquete = nomeSoquete == null ? null : SOQUETES.get(nomeSoquete);
        if (soquete == null) {
            estatisticas.ignorar("CPU: soquete fora do dominio");
            return null;
        }

        List<TipoMemoria> tipos = new ArrayList<>();
        for (Object tipo : registro.lista("specifications.memory.types")) {
            if ("DDR5".equals(tipo)) {
                tipos.add(TipoMemoria.DDR5);
            } else if ("DDR4".equals(tipo)) {
                tipos.add(TipoMemoria.DDR4);
            }
        }
        if (tipos.isEmpty()) {
            tipos.addAll(memoriaPadrao(soquete));
        }
        if (tipos.isEmpty()) {
            estatisticas.ignorar("CPU: sem DDR4/DDR5");
            return null;
        }

        int nucleos = registro.inteiro("cores.total");
        int threads = registro.inteiro("cores.threads");
        int base = (int) Math.round(valor(registro.decimal("clocks.performance.base")) * 1000);
        int boost = Math.max(base, (int) Math.round(Math.max(
                valor(registro.decimal("clocks.performance.boost")),
                valor(registro.decimal("clocks.efficiency.boost"))) * 1000));
        int tdp = registro.inteiro("specifications.tdp");
        if (nucleos <= 0 || base <= 0 || tdp <= 0) {
            estatisticas.ignorar("CPU: sem nucleos/clock/TDP");
            return null;
        }

        Candidato candidato = new Candidato();
        candidato.id = registro.id("cpu");
        candidato.modelo = registro.nomeLimpo();
        candidato.fabricante = registro.fabricante() == null ? "Desconhecido" : registro.fabricante();
        candidato.ano = registro.inteiroNulo("metadata.releaseYear");
        candidato.soquete = soquete;
        candidato.nucleos = nucleos;
        candidato.threads = threads > 0 ? threads : nucleos;
        candidato.base = base;
        candidato.boost = boost;
        candidato.tdp = tdp;
        candidato.consumoMaximo = Math.max(registro.inteiro("specifications.ppt"),
                (int) Math.ceil(tdp * 1.25));
        candidato.tensao = tensao(soquete);
        candidato.tiposMemoria = tipos;
        int canais = registro.inteiro("specifications.memory.channels");
        candidato.canais = canais > 0 ? canais : 2;
        candidato.nativa = nativa(soquete);
        candidato.sincrona = sincrona(soquete);
        int capacidade = registro.inteiro("specifications.memory.maxSupport");
        candidato.capacidadeMaxima = capacidade > 0 ? capacidade : 128;
        candidato.ecc = Boolean.TRUE.equals(registro.booleano("specifications.eccSupport"));
        candidato.pcieMaxima = pcieMaxima(soquete);
        candidato.pistas = pistas(soquete);
        candidato.videoIntegrado = registro.inteiro("specifications.integratedGraphics.shaderCount") > 0;
        String nomeMaiusculo = candidato.modelo.toUpperCase();
        candidato.desbloqueado = "AMD".equalsIgnoreCase(candidato.fabricante)
                || nomeMaiusculo.contains("K") || nomeMaiusculo.contains("X");
        candidato.proxySingle = boost;
        candidato.proxyMulti = (double) boost * nucleos;
        candidato.preco = Heuristicas.preco(250 + 55.0 * nucleos + Math.max(0, boost - 3000) * 0.35
                + (candidato.videoIntegrado ? 150 : 0)
                + (soquete == Soquete.AM5 || soquete == Soquete.LGA1851 ? 200 : 0));
        return candidato;
    }

    private static List<Componente> montar(List<Candidato> candidatos, Estatisticas estatisticas) {
        double menorSingle = Double.MAX_VALUE;
        double maiorSingle = -Double.MAX_VALUE;
        double menorMulti = Double.MAX_VALUE;
        double maiorMulti = -Double.MAX_VALUE;
        for (Candidato candidato : candidatos) {
            menorSingle = Math.min(menorSingle, candidato.proxySingle);
            maiorSingle = Math.max(maiorSingle, candidato.proxySingle);
            menorMulti = Math.min(menorMulti, candidato.proxyMulti);
            maiorMulti = Math.max(maiorMulti, candidato.proxyMulti);
        }

        List<Componente> componentes = new ArrayList<>();
        for (Candidato candidato : candidatos) {
            componentes.add(Processador.builder()
                    .id(candidato.id)
                    .modelo(candidato.modelo)
                    .fabricante(candidato.fabricante)
                    .precoMedioBRL(candidato.preco)
                    .garantiaMeses(36)
                    .anoLancamento(candidato.ano)
                    .soquete(candidato.soquete)
                    .nucleosFisicos(candidato.nucleos)
                    .threads(candidato.threads)
                    .frequenciaBaseMHz(candidato.base)
                    .frequenciaBoostMHz(candidato.boost)
                    .tdpWatts(candidato.tdp)
                    .consumoMaximoWatts(candidato.consumoMaximo)
                    .tensaoMaximaVolts(candidato.tensao)
                    .tiposMemoriaSuportados(candidato.tiposMemoria.toArray(new TipoMemoria[0]))
                    .canaisMemoria(candidato.canais)
                    .frequenciaMemoriaNativaMTs(candidato.nativa)
                    .frequenciaMaximaSincronaMTs(candidato.sincrona)
                    .capacidadeMaximaMemoriaGB(candidato.capacidadeMaxima)
                    .suportaECC(candidato.ecc)
                    .versaoPCIeMaxima(candidato.pcieMaxima)
                    .pistasPCIeLivres(candidato.pistas)
                    .possuiVideoIntegrado(candidato.videoIntegrado)
                    .desbloqueadoOverclock(candidato.desbloqueado)
                    .indiceDesempenhoSingleThread(
                            Heuristicas.normalizar(candidato.proxySingle, menorSingle, maiorSingle))
                    .indiceDesempenhoMultithread(
                            Heuristicas.normalizar(candidato.proxyMulti, menorMulti, maiorMulti))
                    .construir());
        }
        estatisticas.incluir("CPU", componentes.size());
        return componentes;
    }

    private static double valor(Double numero) {
        return numero == null ? 0 : numero.doubleValue();
    }

    private static List<TipoMemoria> memoriaPadrao(Soquete soquete) {
        return switch (soquete) {
            case AM5, LGA1851 -> List.of(TipoMemoria.DDR5);
            case LGA1700 -> List.of(TipoMemoria.DDR5, TipoMemoria.DDR4);
            case AM4, LGA1200 -> List.of(TipoMemoria.DDR4);
            default -> List.of();
        };
    }

    private static double tensao(Soquete soquete) {
        return switch (soquete) {
            case AM5 -> 1.25;
            case AM4, LGA1200, LGA1700 -> 1.35;
            case LGA1851 -> 1.4;
            default -> 1.35;
        };
    }

    private static int nativa(Soquete soquete) {
        return switch (soquete) {
            case AM5 -> 5200;
            case AM4 -> 3200;
            case LGA1700 -> 5600;
            case LGA1200 -> 2933;
            case LGA1851 -> 6400;
            default -> 3200;
        };
    }

    private static int sincrona(Soquete soquete) {
        return switch (soquete) {
            case AM5 -> 6000;
            case AM4 -> 3600;
            case LGA1700 -> 7000;
            case LGA1200 -> 3200;
            case LGA1851 -> 7200;
            default -> 3600;
        };
    }

    private static int pcieMaxima(Soquete soquete) {
        return switch (soquete) {
            case AM4, LGA1200 -> 4;
            default -> 5;
        };
    }

    private static int pistas(Soquete soquete) {
        return switch (soquete) {
            case AM4, AM5 -> 24;
            case LGA1700 -> 20;
            case LGA1851 -> 24;
            case LGA1200 -> 16;
            default -> 16;
        };
    }

    /** Dados derivados de um processador antes de normalizar os indices. */
    private static final class Candidato {
        private String id;
        private String modelo;
        private String fabricante;
        private Integer ano;
        private Soquete soquete;
        private int nucleos;
        private int threads;
        private int base;
        private int boost;
        private int tdp;
        private int consumoMaximo;
        private double tensao;
        private List<TipoMemoria> tiposMemoria;
        private int canais;
        private int nativa;
        private int sincrona;
        private int capacidadeMaxima;
        private boolean ecc;
        private int pcieMaxima;
        private int pistas;
        private boolean videoIntegrado;
        private boolean desbloqueado;
        private double proxySingle;
        private double proxyMulti;
        private java.math.BigDecimal preco;
    }
}
