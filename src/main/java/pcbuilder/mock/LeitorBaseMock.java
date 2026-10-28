package pcbuilder.mock;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import pcbuilder.enums.CategoriaComponente;
import pcbuilder.enums.Certificado80Plus;
import pcbuilder.enums.Chipset;
import pcbuilder.enums.FabricanteChip;
import pcbuilder.enums.FormatoArmazenamento;
import pcbuilder.enums.FormatoPlacaMae;
import pcbuilder.enums.InterfaceArmazenamento;
import pcbuilder.enums.Modularidade;
import pcbuilder.enums.PadraoATX;
import pcbuilder.enums.Resolucao;
import pcbuilder.enums.Soquete;
import pcbuilder.enums.TipoMemoria;
import pcbuilder.enums.TipoMemoriaVRAM;
import pcbuilder.enums.TipoMidia;
import pcbuilder.json.Json;
import pcbuilder.modelo.Armazenamento;
import pcbuilder.modelo.Componente;
import pcbuilder.modelo.Fonte;
import pcbuilder.modelo.Memoria;
import pcbuilder.modelo.PlacaMae;
import pcbuilder.modelo.PlacaVideo;
import pcbuilder.modelo.Processador;

/**
 * Le a base de componentes do arquivo {@code base/componentes.json}.
 *
 * <p>O arquivo fica em {@code src/main/resources} (copiado para o classpath na
 * compilacao) e usa exatamente os nomes de campo do modelo de dados. Ele e gerado
 * por {@code scripts/gerar-base.sh} a partir do catalogo OpenDB - ver
 * {@code docs/base-mockada.md}. O programa nunca inventa componentes em tempo de
 * execucao: se o arquivo faltar, a carga falha com mensagem explicativa.</p>
 */
public final class LeitorBaseMock {

    /** Caminho do arquivo dentro do classpath. */
    public static final String CAMINHO_PADRAO = "/base/componentes.json";

    private LeitorBaseMock() {
        // classe utilitaria: nao deve ser instanciada
    }

    public static List<Componente> carregar() {
        return carregar(CAMINHO_PADRAO);
    }

    /**
     * Le o arquivo JSON e monta os componentes.
     *
     * @param caminho caminho no classpath (comeca com "/")
     * @return lista imutavel, na ordem do arquivo
     */
    public static List<Componente> carregar(String caminho) {
        String texto;
        try (InputStream entrada = LeitorBaseMock.class.getResourceAsStream(caminho)) {
            if (entrada == null) {
                throw new IllegalStateException("Base nao encontrada no classpath: " + caminho
                        + " (rode scripts/gerar-base.sh e recompile o projeto)");
            }
            texto = new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException causa) {
            throw new IllegalStateException("Falha ao ler a base: " + caminho, causa);
        }

        Object raiz = Json.ler(texto);
        List<Componente> componentes = new ArrayList<>();
        for (Object registro : Json.lista(raiz, "componentes")) {
            Componente componente = criar(registro);
            if (componente != null) {
                componentes.add(componente);
            }
        }
        return List.copyOf(componentes);
    }

    private static Componente criar(Object registro) {
        CategoriaComponente categoria = enumDe(registro, "categoria", CategoriaComponente.class);
        if (categoria == null) {
            return null;
        }
        return switch (categoria) {
            case CPU -> processador(registro);
            case PLACA_MAE -> placaMae(registro);
            case MEMORIA -> memoria(registro);
            case PLACA_VIDEO -> placaVideo(registro);
            case ARMAZENAMENTO -> armazenamento(registro);
            case FONTE -> fonte(registro);
            default -> null;
        };
    }

    // ------------------------------------------------------------- auxiliares

    private static void comuns(Componente.Builder<?> construtor, Object registro) {
        construtor.id(Json.texto(registro, "id"));
        construtor.modelo(Json.texto(registro, "modelo"));
        construtor.fabricante(Json.texto(registro, "fabricante"));
        construtor.consumoEstimadoWatts(Json.inteiro(registro, "consumoEstimadoWatts"));
        construtor.precoMedioBRL(Json.numero(registro, "precoMedioBRL"));
        construtor.garantiaMeses(Json.inteiro(registro, "garantiaMeses"));
        construtor.anoLancamento(Json.inteiro(registro, "anoLancamento"));
        construtor.observacoes(Json.texto(registro, "observacoes"));
    }

    private static int inteiro(Object registro, String campo) {
        Integer valor = Json.inteiro(registro, campo);
        return valor == null ? 0 : valor;
    }

    private static Double decimal(Object registro, String campo) {
        BigDecimal valor = Json.numero(registro, campo);
        return valor == null ? null : Double.valueOf(valor.doubleValue());
    }

    private static <E extends Enum<E>> E enumDe(Object registro, String campo, Class<E> tipo) {
        String valor = Json.texto(registro, campo);
        if (valor == null) {
            return null;
        }
        try {
            return Enum.valueOf(tipo, valor);
        } catch (IllegalArgumentException foraDoDominio) {
            return null;
        }
    }

    private static TipoMemoria[] tiposMemoria(Object registro) {
        List<TipoMemoria> tipos = new ArrayList<>();
        for (Object item : Json.lista(registro, "tiposMemoriaSuportados")) {
            if (!(item instanceof String nome)) {
                continue;
            }
            try {
                tipos.add(TipoMemoria.valueOf(nome));
            } catch (IllegalArgumentException foraDoDominio) {
                // valor fora do dominio: ignorado (a validacao acusa o registro)
            }
        }
        return tipos.toArray(new TipoMemoria[0]);
    }

    // ---------------------------------------------------------------- classes

    private static Processador processador(Object registro) {
        Processador.Builder construtor = Processador.builder();
        comuns(construtor, registro);
        construtor.soquete(enumDe(registro, "soquete", Soquete.class));
        construtor.nucleosFisicos(inteiro(registro, "nucleosFisicos"));
        construtor.threads(inteiro(registro, "threads"));
        construtor.frequenciaBaseMHz(inteiro(registro, "frequenciaBaseMHz"));
        construtor.frequenciaBoostMHz(inteiro(registro, "frequenciaBoostMHz"));
        construtor.tdpWatts(inteiro(registro, "tdpWatts"));
        construtor.consumoMaximoWatts(inteiro(registro, "consumoMaximoWatts"));
        construtor.tensaoMaximaVolts(decimal(registro, "tensaoMaximaVolts"));
        construtor.tiposMemoriaSuportados(tiposMemoria(registro));
        construtor.canaisMemoria(inteiro(registro, "canaisMemoria"));
        construtor.frequenciaMemoriaNativaMTs(inteiro(registro, "frequenciaMemoriaNativaMTs"));
        construtor.frequenciaMaximaSincronaMTs(Json.inteiro(registro, "frequenciaMaximaSincronaMTs"));
        construtor.capacidadeMaximaMemoriaGB(inteiro(registro, "capacidadeMaximaMemoriaGB"));
        construtor.suportaECC(Boolean.TRUE.equals(Json.booleano(registro, "suportaECC")));
        construtor.versaoPCIeMaxima(inteiro(registro, "versaoPCIeMaxima"));
        construtor.pistasPCIeLivres(inteiro(registro, "pistasPCIeLivres"));
        construtor.possuiVideoIntegrado(Boolean.TRUE.equals(Json.booleano(registro, "possuiVideoIntegrado")));
        construtor.desbloqueadoOverclock(Boolean.TRUE.equals(Json.booleano(registro, "desbloqueadoOverclock")));
        construtor.indiceDesempenhoSingleThread(inteiro(registro, "indiceDesempenhoSingleThread"));
        construtor.indiceDesempenhoMultithread(inteiro(registro, "indiceDesempenhoMultithread"));
        return construtor.construir();
    }

    private static PlacaMae placaMae(Object registro) {
        PlacaMae.Builder construtor = PlacaMae.builder();
        comuns(construtor, registro);
        construtor.soqueteCPU(enumDe(registro, "soqueteCPU", Soquete.class));
        construtor.chipset(enumDe(registro, "chipset", Chipset.class));
        construtor.formato(enumDe(registro, "formato", FormatoPlacaMae.class));
        construtor.comprimentoMm(inteiro(registro, "comprimentoMm"));
        construtor.larguraMm(inteiro(registro, "larguraMm"));
        construtor.suportaAtualizacaoBIOSSemCPU(
                Boolean.TRUE.equals(Json.booleano(registro, "suportaAtualizacaoBIOSSemCPU")));
        construtor.vrmFasesCPU(inteiro(registro, "vrmFasesCPU"));
        construtor.vrmCorrentePorFaseA(inteiro(registro, "vrmCorrentePorFaseA"));
        construtor.vrmPossuiDissipador(Json.booleano(registro, "vrmPossuiDissipador"));
        construtor.quantidadeSlotsMemoria(inteiro(registro, "quantidadeSlotsMemoria"));
        construtor.tipoMemoria(enumDe(registro, "tipoMemoria", TipoMemoria.class));
        construtor.canaisMemoria(inteiro(registro, "canaisMemoria"));
        construtor.capacidadeMaximaMemoriaGB(inteiro(registro, "capacidadeMaximaMemoriaGB"));
        construtor.frequenciaMemoriaMaximaMTs(inteiro(registro, "frequenciaMemoriaMaximaMTs"));
        construtor.suportaPerfilEXPO(Boolean.TRUE.equals(Json.booleano(registro, "suportaPerfilEXPO")));
        construtor.suportaPerfilXMP(Boolean.TRUE.equals(Json.booleano(registro, "suportaPerfilXMP")));
        construtor.suportaECC(Boolean.TRUE.equals(Json.booleano(registro, "suportaECC")));
        construtor.quantidadeSlotsM2(inteiro(registro, "quantidadeSlotsM2"));
        construtor.geracaoMaximaSlotM2(inteiro(registro, "geracaoMaximaSlotM2"));
        construtor.geracaoMinimaSlotM2(inteiro(registro, "geracaoMinimaSlotM2"));
        construtor.quantidadeSlotsPCIe16(inteiro(registro, "quantidadeSlotsPCIe16"));
        construtor.versaoSlotPCIe16(inteiro(registro, "versaoSlotPCIe16"));
        construtor.pistasSlotPCIe16(inteiro(registro, "pistasSlotPCIe16"));
        construtor.quantidadePortasSATA(inteiro(registro, "quantidadePortasSATA"));
        construtor.velocidadeSATAGbps(inteiro(registro, "velocidadeSATAGbps"));
        construtor.quantidadeConectoresCPU8pin(inteiro(registro, "quantidadeConectoresCPU8pin"));
        construtor.quantidadeConectoresCPU4pin(inteiro(registro, "quantidadeConectoresCPU4pin"));
        construtor.quantidadeConectoresATX24pin(inteiro(registro, "quantidadeConectoresATX24pin"));
        return construtor.construir();
    }

    private static Memoria memoria(Object registro) {
        Memoria.Builder construtor = Memoria.builder();
        comuns(construtor, registro);
        construtor.tipo(enumDe(registro, "tipo", TipoMemoria.class));
        construtor.capacidadePorPenteGB(inteiro(registro, "capacidadePorPenteGB"));
        construtor.quantidadePentes(inteiro(registro, "quantidadePentes"));
        construtor.frequenciaMTs(inteiro(registro, "frequenciaMTs"));
        construtor.frequenciaJEDECBaseMTs(Json.inteiro(registro, "frequenciaJEDECBaseMTs"));
        construtor.latenciaCAS(inteiro(registro, "latenciaCAS"));
        construtor.tensaoVolts(decimal(registro, "tensaoVolts"));
        construtor.perfilEXPO(Boolean.TRUE.equals(Json.booleano(registro, "perfilEXPO")));
        construtor.perfilXMP(Boolean.TRUE.equals(Json.booleano(registro, "perfilXMP")));
        construtor.possuiECC(Boolean.TRUE.equals(Json.booleano(registro, "possuiECC")));
        construtor.alturaMm(Json.inteiro(registro, "alturaMm"));
        return construtor.construir();
    }

    private static PlacaVideo placaVideo(Object registro) {
        PlacaVideo.Builder construtor = PlacaVideo.builder();
        comuns(construtor, registro);
        construtor.fabricanteChip(enumDe(registro, "fabricanteChip", FabricanteChip.class));
        construtor.memoriaVRAMGB(inteiro(registro, "memoriaVRAMGB"));
        construtor.tipoMemoriaVRAM(enumDe(registro, "tipoMemoriaVRAM", TipoMemoriaVRAM.class));
        construtor.versaoPCIe(inteiro(registro, "versaoPCIe"));
        construtor.pistasPCIe(inteiro(registro, "pistasPCIe"));
        construtor.comprimentoMm(inteiro(registro, "comprimentoMm"));
        construtor.espessuraSlots(inteiro(registro, "espessuraSlots"));
        construtor.tdpWatts(inteiro(registro, "tdpWatts"));
        construtor.picoTransienteWatts(Json.inteiro(registro, "picoTransienteWatts"));
        construtor.fonteRecomendadaWatts(Json.inteiro(registro, "fonteRecomendadaWatts"));
        construtor.quantidadeConectores16pin(inteiro(registro, "quantidadeConectores16pin"));
        construtor.quantidadeConectores8pin(inteiro(registro, "quantidadeConectores8pin"));
        construtor.quantidadeConectores6pin(inteiro(registro, "quantidadeConectores6pin"));
        construtor.resolucaoAlvo(enumDe(registro, "resolucaoAlvo", Resolucao.class));
        construtor.indiceDesempenhoGPU(inteiro(registro, "indiceDesempenhoGPU"));
        construtor.possuiRayTracing(Json.booleano(registro, "possuiRayTracing"));
        return construtor.construir();
    }

    private static Armazenamento armazenamento(Object registro) {
        Armazenamento.Builder construtor = Armazenamento.builder();
        comuns(construtor, registro);
        construtor.tipoMidia(enumDe(registro, "tipoMidia", TipoMidia.class));
        construtor.interfaceArmazenamento(
                enumDe(registro, "interface", InterfaceArmazenamento.class));
        construtor.formato(enumDe(registro, "formato", FormatoArmazenamento.class));
        construtor.capacidadeGB(inteiro(registro, "capacidadeGB"));
        construtor.versaoPCIe(Json.inteiro(registro, "versaoPCIe"));
        construtor.pistasPCIe(Json.inteiro(registro, "pistasPCIe"));
        construtor.velocidadeSATAGbps(Json.inteiro(registro, "velocidadeSATAGbps"));
        construtor.leituraSequencialMBs(inteiro(registro, "leituraSequencialMBs"));
        construtor.escritaSequencialMBs(inteiro(registro, "escritaSequencialMBs"));
        construtor.tbw(Json.inteiro(registro, "tbw"));
        construtor.possuiDRAMCache(Json.booleano(registro, "possuiDRAMCache"));
        construtor.consumoMedioWatts(decimal(registro, "consumoMedioWatts"));
        construtor.consumoPicoWatts(decimal(registro, "consumoPicoWatts"));
        return construtor.construir();
    }

    private static Fonte fonte(Object registro) {
        Fonte.Builder construtor = Fonte.builder();
        comuns(construtor, registro);
        construtor.potenciaTotalWatts(inteiro(registro, "potenciaTotalWatts"));
        construtor.potenciaTrilho12VWatts(inteiro(registro, "potenciaTrilho12VWatts"));
        construtor.certificacao80Plus(enumDe(registro, "certificacao80Plus", Certificado80Plus.class));
        construtor.eficienciaPercentualCarga50(decimal(registro, "eficienciaPercentualCarga50"));
        construtor.modularidade(enumDe(registro, "modularidade", Modularidade.class));
        construtor.padraoATX(enumDe(registro, "padraoATX", PadraoATX.class));
        construtor.quantidadeConectores16pin12VHPWR(inteiro(registro, "quantidadeConectores16pin12VHPWR"));
        construtor.quantidadeConectoresPCIe8pin(inteiro(registro, "quantidadeConectoresPCIe8pin"));
        construtor.quantidadeConectoresPCIe6pin(inteiro(registro, "quantidadeConectoresPCIe6pin"));
        construtor.quantidadeConectoresCPU8pin(inteiro(registro, "quantidadeConectoresCPU8pin"));
        construtor.quantidadeConectoresCPU4pin(inteiro(registro, "quantidadeConectoresCPU4pin"));
        construtor.quantidadeConectoresSATA(inteiro(registro, "quantidadeConectoresSATA"));
        construtor.quantidadeConectoresMolex(Json.inteiro(registro, "quantidadeConectoresMolex"));
        construtor.comprimentoMm(inteiro(registro, "comprimentoMm"));
        construtor.alturaMm(Json.inteiro(registro, "alturaMm"));
        construtor.possuiPFCAtivo(Json.booleano(registro, "possuiPFCAtivo"));
        return construtor.construir();
    }
}
