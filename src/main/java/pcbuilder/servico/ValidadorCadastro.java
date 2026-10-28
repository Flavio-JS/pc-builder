package pcbuilder.servico;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import pcbuilder.enums.CategoriaComponente;
import pcbuilder.enums.Certificado80Plus;
import pcbuilder.enums.Chipset;
import pcbuilder.enums.FabricanteChip;
import pcbuilder.enums.FormatoArmazenamento;
import pcbuilder.enums.FormatoPlacaMae;
import pcbuilder.enums.InterfaceArmazenamento;
import pcbuilder.enums.Modularidade;
import pcbuilder.enums.NivelProblema;
import pcbuilder.enums.PadraoATX;
import pcbuilder.enums.Resolucao;
import pcbuilder.enums.Soquete;
import pcbuilder.enums.TipoMemoria;
import pcbuilder.enums.TipoMemoriaVRAM;
import pcbuilder.enums.TipoMidia;
import pcbuilder.modelo.Armazenamento;
import pcbuilder.modelo.Componente;
import pcbuilder.modelo.Fonte;
import pcbuilder.modelo.Memoria;
import pcbuilder.modelo.PlacaMae;
import pcbuilder.modelo.PlacaVideo;
import pcbuilder.modelo.Processador;

/**
 * Aplica as <strong>10 verificacoes</strong> do checklist de cadastro
 * (secao 18 do modelo de dados) sobre os componentes carregados.
 *
 * <p>Cada verificacao produz um {@link ProblemaCadastro} classificado como
 * {@code ERRO} (o registro e descartado) ou {@code AVISO} (o registro e usavel com
 * ressalva). A classe nao imprime nada e nao encerra o programa: devolve um
 * {@link ResultadoValidacao} para quem chamou decidir o que fazer.</p>
 *
 * <p>As verificacoes 2, 3, 9 e 10 existem para proteger a base de registros
 * "sujos" ("65W" em um campo de watts, campo calculado gravado, campo curinga).
 * Como as classes de dominio sao tipadas e imutaveis, esses defeitos nao nascem no
 * compilador: eles viriam de uma fonte externa e, por isso, a validacao precisa
 * saber reconhece-los.</p>
 */
public final class ValidadorCadastro {

    /** 1 - todo campo obrigatorio preenchido. */
    public static final int OBRIGATORIOS = 1;
    /** 2 - nenhum campo com numero + texto. */
    public static final int NUMERO_COM_TEXTO = 2;
    /** 3 - nenhum campo com unidade duplicada. */
    public static final int UNIDADE_DUPLICADA = 3;
    /** 4 - enums dentro do dominio da secao 12. */
    public static final int DOMINIO_ENUM = 4;
    /** 5 - valores positivos onde so positivo faz sentido. */
    public static final int VALORES_POSITIVOS = 5;
    /** 6 - {@code null} usado so onde o campo nao se aplica. */
    public static final int APLICABILIDADE_NULL = 6;
    /** 7 - coerencia interna minima. */
    public static final int COERENCIA = 7;
    /** 8 - identificador unico. */
    public static final int ID_UNICO = 8;
    /** 9 - nenhum campo derivado gravado. */
    public static final int CAMPO_DERIVADO = 9;
    /** 10 - nenhum campo generico usado como curinga. */
    public static final int CAMPO_GENERICO = 10;

    /** Campos de texto livre: sao os unicos que podem guardar letras. */
    private static final Set<String> CAMPOS_TEXTUAIS = Set.of("id", "modelo", "fabricante", "observacoes");

    /** Campos comuns obrigatorios em qualquer categoria (secao 2). */
    private static final List<String> CAMPOS_OBRIGATORIOS_COMUNS =
            List.of("id", "modelo", "fabricante", "categoria", "precoMedioBRL");

    /** Campos "Obrig. Sim" das secoes 3 a 8, por categoria. */
    private static final Map<CategoriaComponente, List<String>> CAMPOS_OBRIGATORIOS =
            criarCamposObrigatorios();

    /** Campos em que zero nao tem significado: precisam ser maiores que zero. */
    private static final Set<String> CAMPOS_POSITIVOS = Set.of(
            // CPU
            "nucleosFisicos", "threads", "frequenciaBaseMHz", "frequenciaBoostMHz", "tdpWatts",
            "consumoMaximoWatts", "canaisMemoria", "frequenciaMemoriaNativaMTs",
            "capacidadeMaximaMemoriaGB", "versaoPCIeMaxima", "pistasPCIeLivres",
            "indiceDesempenhoSingleThread", "indiceDesempenhoMultithread",
            // Placa-mae
            "larguraMm", "vrmFasesCPU", "vrmCorrentePorFaseA", "quantidadeSlotsMemoria",
            "frequenciaMemoriaMaximaMTs", "geracaoMaximaSlotM2", "geracaoMinimaSlotM2",
            "versaoSlotPCIe16", "pistasSlotPCIe16", "quantidadeConectoresATX24pin",
            // Memoria
            "capacidadePorPenteGB", "quantidadePentes", "frequenciaMTs", "latenciaCAS",
            // Placa de video
            "memoriaVRAMGB", "versaoPCIe", "pistasPCIe", "espessuraSlots", "indiceDesempenhoGPU",
            // Armazenamento
            "capacidadeGB", "leituraSequencialMBs", "escritaSequencialMBs", "consumoMedioWatts",
            // Fonte
            "potenciaTotalWatts", "potenciaTrilho12VWatts", "velocidadeSATAGbps", "comprimentoMm");

    /** Campos calculados (secao 16): nunca podem ser gravados no cadastro. */
    private static final Set<String> CAMPOS_DERIVADOS = Set.of(
            "capacidadeTotalMemoriaGB", "memoriaEmDualChannel", "correnteExigidaCPUAmperes",
            "capacidadeVRMAmperes", "percentualUsoVRM", "folgaTermicaCooler",
            "frequenciaEfetivaMemoriaMTs", "latenciaEfetivaNs", "bandaEfetivaPCIeGBs",
            "perdaBandaPCIePercentual", "consumoTotalSistemaWatts", "consumoComMargemWatts",
            "requisitosFonte", "percentualCargaFonte", "capacidadeTotalArmazenamentoGB",
            "desequilibrioCpuGpu");

    /** Nomes de campo "curinga" que escondem qualquer informacao sem validacao. */
    private static final Set<String> CAMPOS_GENERICOS = Set.of(
            "outrosDados", "especificacoes", "specs", "dados", "extras", "informacoes",
            "camposExtras", "observacaoGeral", "curinga", "diversos", "varios", "misc");

    /** Sufixo do nome -> unidade que o proprio nome do campo ja carrega. */
    private static final Map<String, String> UNIDADES_POR_SUFIXO = criarUnidades();

    /** Campo de enum -> tipo do enum, por categoria (o mesmo nome muda de dominio). */
    private static final Map<CategoriaComponente, Map<String, Class<? extends Enum<?>>>> CAMPOS_ENUM =
            criarCamposEnum();

    // ------------------------------------------------------------------ carga

    /**
     * Valida uma lista de registros. A verificacao 8 (id unico) so faz sentido no
     * conjunto: o primeiro registro com um id fica, os repetidos sao descartados.
     *
     * @param registros registros carregados do mock
     * @return objeto com os problemas e com os registros que passaram
     */
    public ResultadoValidacao validar(List<Componente> registros) {
        List<ProblemaCadastro> problemas = new ArrayList<>();
        List<Componente> validos = new ArrayList<>();
        Set<String> idsVistos = new HashSet<>();

        for (Componente componente : registros) {
            List<ProblemaCadastro> doRegistro = avaliar(componente);
            String id = componente.getId();
            if (id != null && !idsVistos.add(id)) {
                doRegistro.add(problema(ID_UNICO, NivelProblema.ERRO, componente, "id",
                        "identificador duplicado: '" + id + "'"));
            }
            problemas.addAll(doRegistro);
            if (!possuiErro(doRegistro)) {
                validos.add(componente);
            }
        }
        return new ResultadoValidacao(registros, validos, problemas);
    }

    /**
     * Aplica as verificacoes de um registro isolado (1 a 7, 9 e 10).
     *
     * @param componente registro a avaliar
     * @return lista vazia quando o registro esta limpo
     */
    public List<ProblemaCadastro> avaliar(Componente componente) {
        List<ProblemaCadastro> problemas = new ArrayList<>();
        Map<String, Object> campos = componente.campos();

        verificarObrigatorios(componente, campos, problemas);
        verificarCamposProibidos(componente, campos, problemas);
        verificarNumeroComTexto(componente, campos, problemas);
        verificarDominiosEnum(componente, campos, problemas);
        verificarValoresPositivos(componente, campos, problemas);
        verificarAplicabilidade(componente, problemas);
        verificarCoerencia(componente, problemas);

        return problemas;
    }

    // -------------------------------------------------- 1, 9 e 10 (por campo)

    /** Verificacao 1: campos obrigatorios presentes e nao vazios. */
    private void verificarObrigatorios(Componente componente, Map<String, Object> campos,
            List<ProblemaCadastro> problemas) {
        List<String> obrigatorios = new ArrayList<>(CAMPOS_OBRIGATORIOS_COMUNS);
        obrigatorios.addAll(CAMPOS_OBRIGATORIOS.getOrDefault(componente.getCategoria(), List.of()));

        for (String campo : obrigatorios) {
            Object valor = campos.get(campo);
            if (valor == null) {
                problemas.add(problema(OBRIGATORIOS, NivelProblema.ERRO, componente, campo,
                        "campo obrigatorio ausente (null)"));
            } else if (valor instanceof CharSequence texto && texto.toString().isBlank()) {
                problemas.add(problema(OBRIGATORIOS, NivelProblema.ERRO, componente, campo,
                        "campo obrigatorio vazio"));
            }
        }
    }

    /** Verificacoes 9 e 10: nada de campo calculado nem de campo curinga. */
    private void verificarCamposProibidos(Componente componente, Map<String, Object> campos,
            List<ProblemaCadastro> problemas) {
        for (String campo : campos.keySet()) {
            if (CAMPOS_DERIVADOS.contains(campo)) {
                problemas.add(problema(CAMPO_DERIVADO, NivelProblema.ERRO, componente, campo,
                        "campo derivado nao pode ser gravado: calcule em tempo de execucao"));
            }
            if (CAMPOS_GENERICOS.contains(campo)) {
                problemas.add(problema(CAMPO_GENERICO, NivelProblema.ERRO, componente, campo,
                        "campo generico nao e permitido: use um campo atomico por informacao"));
            }
        }
    }

    /** Verificacoes 2 e 3: campo numerico nao pode receber texto nem repetir a unidade. */
    private void verificarNumeroComTexto(Componente componente, Map<String, Object> campos,
            List<ProblemaCadastro> problemas) {
        for (Map.Entry<String, Object> entrada : campos.entrySet()) {
            String campo = entrada.getKey();
            Object valor = entrada.getValue();
            if (valor == null || CAMPOS_TEXTUAIS.contains(campo)) {
                continue;
            }
            if (!(valor instanceof CharSequence texto)) {
                continue;
            }
            String unidade = unidadeDoCampo(campo);
            if (unidade != null && contemUnidade(texto.toString(), unidade)) {
                problemas.add(problema(UNIDADE_DUPLICADA, NivelProblema.ERRO, componente, campo,
                        "valor '" + texto + "' repete a unidade " + unidade + " que ja esta no nome do campo"));
            } else {
                problemas.add(problema(NUMERO_COM_TEXTO, NivelProblema.ERRO, componente, campo,
                        "campo numerico recebeu texto: '" + texto + "'"));
            }
        }
    }

    // ------------------------------------------- 4, 5 e 6 (por componente)

    /** Verificacao 4: todo campo de enum precisa de um valor do dominio fechado. */
    private void verificarDominiosEnum(Componente componente, Map<String, Object> campos,
            List<ProblemaCadastro> problemas) {
        verificarEnum(componente, "categoria", campos.get("categoria"), CategoriaComponente.class, problemas);

        Map<String, Class<? extends Enum<?>>> enums = CAMPOS_ENUM.get(componente.getCategoria());
        if (enums == null) {
            return;
        }
        for (Map.Entry<String, Class<? extends Enum<?>>> entrada : enums.entrySet()) {
            verificarEnum(componente, entrada.getKey(), campos.get(entrada.getKey()), entrada.getValue(), problemas);
        }
    }

    private void verificarEnum(Componente componente, String campo, Object valor,
            Class<? extends Enum<?>> tipo, List<ProblemaCadastro> problemas) {
        if (valor == null) {
            return; // ausencia e tratada pela verificacao 1 quando o campo e obrigatorio
        }
        if (valor instanceof Collection<?> itens) {
            for (Object item : itens) {
                verificarEnum(componente, campo, item, tipo, problemas);
            }
            return;
        }
        if (tipo.isInstance(valor)) {
            return;
        }
        if (valor instanceof CharSequence texto) {
            for (Object constante : tipo.getEnumConstants()) {
                if (((Enum<?>) constante).name().equals(texto.toString())) {
                    return; // texto identico a um valor do dominio: aceitavel
                }
            }
        }
        problemas.add(problema(DOMINIO_ENUM, NivelProblema.ERRO, componente, campo,
                "valor '" + valor + "' fora do dominio " + tipo.getSimpleName()));
    }

    /** Verificacao 5: campos em que zero/negativo nao fazem sentido. */
    private void verificarValoresPositivos(Componente componente, Map<String, Object> campos,
            List<ProblemaCadastro> problemas) {
        for (String campo : CAMPOS_POSITIVOS) {
            Object valor = campos.get(campo);
            if (valor instanceof Number numero && numero.doubleValue() <= 0) {
                problemas.add(problema(VALORES_POSITIVOS, NivelProblema.ERRO, componente, campo,
                        "valor " + numero + " deveria ser maior que zero"));
            }
        }
    }

    /** Verificacao 6: {@code null} so onde o campo realmente nao se aplica. */
    private void verificarAplicabilidade(Componente componente, List<ProblemaCadastro> problemas) {
        if (componente instanceof Armazenamento armazenamento) {
            boolean nvme = armazenamento.getInterfaceArmazenamento() == InterfaceArmazenamento.NVME;
            if (nvme) {
                if (armazenamento.getVersaoPCIe() == null) {
                    problemas.add(problema(APLICABILIDADE_NULL, NivelProblema.ERRO, componente, "versaoPCIe",
                            "NVMe exige versaoPCIe; null so vale para SATA"));
                }
                if (armazenamento.getPistasPCIe() == null) {
                    problemas.add(problema(APLICABILIDADE_NULL, NivelProblema.ERRO, componente, "pistasPCIe",
                            "NVMe exige pistasPCIe; null so vale para SATA"));
                }
                if (armazenamento.getVelocidadeSATAGbps() != null) {
                    problemas.add(problema(APLICABILIDADE_NULL, NivelProblema.AVISO, componente,
                            "velocidadeSATAGbps", "campo nao se aplica a NVMe: use null em vez de valor"));
                }
            } else {
                if (armazenamento.getVelocidadeSATAGbps() == null) {
                    problemas.add(problema(APLICABILIDADE_NULL, NivelProblema.ERRO, componente,
                            "velocidadeSATAGbps", "SATA exige velocidadeSATAGbps"));
                }
                if (armazenamento.getVersaoPCIe() != null) {
                    problemas.add(problema(APLICABILIDADE_NULL, NivelProblema.AVISO, componente, "versaoPCIe",
                            "campo nao se aplica a SATA: use null em vez de valor"));
                }
                if (armazenamento.getPistasPCIe() != null) {
                    problemas.add(problema(APLICABILIDADE_NULL, NivelProblema.AVISO, componente, "pistasPCIe",
                            "campo nao se aplica a SATA: use null em vez de valor"));
                }
            }
        }

        if (componente instanceof Processador processador && processador.getTensaoMaximaVolts() == null) {
            problemas.add(problema(APLICABILIDADE_NULL, NivelProblema.AVISO, componente, "tensaoMaximaVolts",
                    "ausente: a regra L02 usara TENSAO_REFERENCIA_CPU (1.25 V)"));
        }

        if (componente instanceof PlacaVideo placaVideo && placaVideo.getPicoTransienteWatts() == null) {
            problemas.add(problema(APLICABILIDADE_NULL, NivelProblema.AVISO, componente, "picoTransienteWatts",
                    "ausente: a regra L06 nao conseguira avaliar picos de energia"));
        }

        if (componente instanceof Fonte fonte && fonte.getConsumoEstimadoWatts() != null) {
            problemas.add(problema(APLICABILIDADE_NULL, NivelProblema.AVISO, componente, "consumoEstimadoWatts",
                    "fonte fornece energia: o campo deve ficar null"));
        }
    }

    /** Verificacao 7: relacoes que sempre tem de valer dentro do mesmo registro. */
    private void verificarCoerencia(Componente componente, List<ProblemaCadastro> problemas) {
        if (componente instanceof Processador cpu) {
            if (cpu.getFrequenciaBaseMHz() > cpu.getFrequenciaBoostMHz()) {
                problemas.add(problema(COERENCIA, NivelProblema.ERRO, componente, "frequenciaBaseMHz",
                        "frequencia base (" + cpu.getFrequenciaBaseMHz() + " MHz) maior que o boost ("
                                + cpu.getFrequenciaBoostMHz() + " MHz)"));
            }
            if (cpu.getTdpWatts() > cpu.getConsumoMaximoWatts()) {
                problemas.add(problema(COERENCIA, NivelProblema.ERRO, componente, "tdpWatts",
                        "TDP (" + cpu.getTdpWatts() + " W) maior que o consumo maximo ("
                                + cpu.getConsumoMaximoWatts() + " W)"));
            }
            if (cpu.getFrequenciaMaximaSincronaMTs() != null
                    && cpu.getFrequenciaMemoriaNativaMTs() > cpu.getFrequenciaMaximaSincronaMTs()) {
                problemas.add(problema(COERENCIA, NivelProblema.ERRO, componente, "frequenciaMemoriaNativaMTs",
                        "frequencia nativa maior que a maxima sincrona do IMC"));
            }
            verificarIndice(componente, "indiceDesempenhoSingleThread",
                    cpu.getIndiceDesempenhoSingleThread(), problemas);
            verificarIndice(componente, "indiceDesempenhoMultithread",
                    cpu.getIndiceDesempenhoMultithread(), problemas);
        }

        if (componente instanceof PlacaMae placaMae) {
            if (placaMae.getGeracaoMinimaSlotM2() > placaMae.getGeracaoMaximaSlotM2()) {
                problemas.add(problema(COERENCIA, NivelProblema.ERRO, componente, "geracaoMinimaSlotM2",
                        "geracao minima (" + placaMae.getGeracaoMinimaSlotM2()
                                + ") maior que a maxima (" + placaMae.getGeracaoMaximaSlotM2() + ")"));
            }
            if (placaMae.getQuantidadeSlotsM2() == 0 && placaMae.getGeracaoMaximaSlotM2() > 0) {
                problemas.add(problema(COERENCIA, NivelProblema.ERRO, componente, "quantidadeSlotsM2",
                        "sem slot M.2 nao existe geracao de slot M.2"));
            }
        }

        if (componente instanceof Memoria memoria && memoria.getFrequenciaJEDECBaseMTs() != null
                && memoria.getFrequenciaJEDECBaseMTs() > memoria.getFrequenciaMTs()) {
            problemas.add(problema(COERENCIA, NivelProblema.ERRO, componente, "frequenciaJEDECBaseMTs",
                    "frequencia JEDEC maior que a frequencia anunciada"));
        }

        if (componente instanceof PlacaVideo placaVideo) {
            if (placaVideo.getPicoTransienteWatts() != null
                    && placaVideo.getPicoTransienteWatts() < placaVideo.getTdpWatts()) {
                problemas.add(problema(COERENCIA, NivelProblema.ERRO, componente, "picoTransienteWatts",
                        "pico transiente menor que o TDP da placa"));
            }
            if (placaVideo.getFonteRecomendadaWatts() != null
                    && placaVideo.getFonteRecomendadaWatts() < placaVideo.getTdpWatts()) {
                problemas.add(problema(COERENCIA, NivelProblema.ERRO, componente, "fonteRecomendadaWatts",
                        "fonte recomendada menor que o TDP da placa"));
            }
            verificarIndice(componente, "indiceDesempenhoGPU", placaVideo.getIndiceDesempenhoGPU(), problemas);
        }

        if (componente instanceof Armazenamento armazenamento
                && armazenamento.getConsumoPicoWatts() != null
                && armazenamento.getConsumoMedioWatts() != null
                && armazenamento.getConsumoPicoWatts() < armazenamento.getConsumoMedioWatts()) {
            problemas.add(problema(COERENCIA, NivelProblema.ERRO, componente, "consumoPicoWatts",
                    "consumo de pico menor que o consumo medio"));
        }

        if (componente instanceof Fonte fonte && fonte.getPotenciaTrilho12VWatts() > fonte.getPotenciaTotalWatts()) {
            problemas.add(problema(COERENCIA, NivelProblema.ERRO, componente, "potenciaTrilho12VWatts",
                    "trilho 12 V (" + fonte.getPotenciaTrilho12VWatts() + " W) maior que a potencia total ("
                            + fonte.getPotenciaTotalWatts() + " W)"));
        }
    }

    private void verificarIndice(Componente componente, String campo, int indice,
            List<ProblemaCadastro> problemas) {
        if (indice < 0 || indice > 100) {
            problemas.add(problema(COERENCIA, NivelProblema.ERRO, componente, campo,
                    "indice de desempenho fora da faixa 0-100: " + indice));
        }
    }

    // ------------------------------------------------------------- utilidades

    private static ProblemaCadastro problema(int verificacao, NivelProblema nivel, Componente componente,
            String campo, String descricao) {
        return new ProblemaCadastro(verificacao, nivel, componente.getId(), campo, descricao);
    }

    private static boolean possuiErro(List<ProblemaCadastro> problemas) {
        for (ProblemaCadastro problema : problemas) {
            if (problema.isErro()) {
                return true;
            }
        }
        return false;
    }

    /** Devolve a unidade que o nome do campo ja carrega ({@code tdpWatts} -&gt; "W"). */
    private static String unidadeDoCampo(String campo) {
        if (campo == null) {
            return null;
        }
        for (Map.Entry<String, String> sufixo : UNIDADES_POR_SUFIXO.entrySet()) {
            if (campo.endsWith(sufixo.getKey())) {
                return sufixo.getValue();
            }
        }
        return null;
    }

    /** {@code true} quando o texto traz um numero seguido da unidade (ex.: "65W"). */
    private static boolean contemUnidade(String texto, String unidade) {
        return Pattern.compile("(?i)\\d\\s*" + Pattern.quote(unidade)).matcher(texto).find();
    }

    private static Map<String, String> criarUnidades() {
        Map<String, String> unidades = new LinkedHashMap<>();
        unidades.put("Watts", "W");
        unidades.put("Volts", "V");
        unidades.put("Amperes", "A");
        unidades.put("Percentual", "%");
        unidades.put("Gbps", "Gb/s");
        unidades.put("MTs", "MT/s");
        unidades.put("GHz", "GHz");
        unidades.put("MHz", "MHz");
        unidades.put("MBs", "MB/s");
        unidades.put("GBs", "GB/s");
        unidades.put("Mm", "mm");
        unidades.put("GB", "GB");
        unidades.put("TB", "TB");
        unidades.put("Hz", "Hz");
        return unidades;
    }

    /** Campos "Obrig. Sim" das secoes 3 a 8 (as contagens batem com a secao 19). */
    private static Map<CategoriaComponente, List<String>> criarCamposObrigatorios() {
        Map<CategoriaComponente, List<String>> mapa = new EnumMap<>(CategoriaComponente.class);

        mapa.put(CategoriaComponente.CPU, List.of(
                "soquete", "nucleosFisicos", "threads", "frequenciaBaseMHz", "frequenciaBoostMHz",
                "tdpWatts", "consumoMaximoWatts", "tiposMemoriaSuportados", "canaisMemoria",
                "frequenciaMemoriaNativaMTs", "capacidadeMaximaMemoriaGB", "suportaECC",
                "versaoPCIeMaxima", "pistasPCIeLivres", "possuiVideoIntegrado",
                "desbloqueadoOverclock", "indiceDesempenhoSingleThread", "indiceDesempenhoMultithread"));

        mapa.put(CategoriaComponente.PLACA_MAE, List.of(
                "soqueteCPU", "chipset", "formato", "comprimentoMm", "larguraMm",
                "suportaAtualizacaoBIOSSemCPU", "vrmFasesCPU", "vrmCorrentePorFaseA",
                "quantidadeSlotsMemoria", "tipoMemoria", "canaisMemoria",
                "capacidadeMaximaMemoriaGB", "frequenciaMemoriaMaximaMTs", "suportaPerfilEXPO",
                "suportaPerfilXMP", "suportaECC", "quantidadeSlotsM2", "geracaoMaximaSlotM2",
                "geracaoMinimaSlotM2", "quantidadeSlotsPCIe16", "versaoSlotPCIe16",
                "pistasSlotPCIe16", "quantidadePortasSATA", "velocidadeSATAGbps",
                "quantidadeConectoresCPU8pin", "quantidadeConectoresATX24pin"));

        mapa.put(CategoriaComponente.MEMORIA, List.of(
                "tipo", "capacidadePorPenteGB", "quantidadePentes", "frequenciaMTs",
                "latenciaCAS", "perfilEXPO", "perfilXMP", "possuiECC"));

        mapa.put(CategoriaComponente.PLACA_VIDEO, List.of(
                "fabricanteChip", "memoriaVRAMGB", "versaoPCIe", "pistasPCIe", "comprimentoMm",
                "espessuraSlots", "tdpWatts", "quantidadeConectores16pin",
                "quantidadeConectores8pin", "quantidadeConectores6pin", "resolucaoAlvo",
                "indiceDesempenhoGPU"));

        mapa.put(CategoriaComponente.ARMAZENAMENTO, List.of(
                "tipoMidia", "interface", "formato", "capacidadeGB", "leituraSequencialMBs",
                "escritaSequencialMBs", "consumoMedioWatts"));

        mapa.put(CategoriaComponente.FONTE, List.of(
                "potenciaTotalWatts", "potenciaTrilho12VWatts", "certificacao80Plus",
                "modularidade", "padraoATX", "quantidadeConectores16pin12VHPWR",
                "quantidadeConectoresPCIe8pin", "quantidadeConectoresPCIe6pin",
                "quantidadeConectoresCPU8pin", "quantidadeConectoresCPU4pin",
                "quantidadeConectoresSATA", "comprimentoMm"));

        return mapa;
    }

    /** Dominio fechado de cada campo de enum, por categoria (secao 12). */
    private static Map<CategoriaComponente, Map<String, Class<? extends Enum<?>>>> criarCamposEnum() {
        Map<CategoriaComponente, Map<String, Class<? extends Enum<?>>>> mapa =
                new EnumMap<>(CategoriaComponente.class);

        mapa.put(CategoriaComponente.CPU, Map.of(
                "soquete", Soquete.class,
                "tiposMemoriaSuportados", TipoMemoria.class));

        mapa.put(CategoriaComponente.PLACA_MAE, Map.of(
                "soqueteCPU", Soquete.class,
                "chipset", Chipset.class,
                "formato", FormatoPlacaMae.class,
                "tipoMemoria", TipoMemoria.class));

        mapa.put(CategoriaComponente.MEMORIA, Map.of("tipo", TipoMemoria.class));

        mapa.put(CategoriaComponente.PLACA_VIDEO, Map.of(
                "fabricanteChip", FabricanteChip.class,
                "tipoMemoriaVRAM", TipoMemoriaVRAM.class,
                "resolucaoAlvo", Resolucao.class));

        mapa.put(CategoriaComponente.ARMAZENAMENTO, Map.of(
                "tipoMidia", TipoMidia.class,
                "interface", InterfaceArmazenamento.class,
                "formato", FormatoArmazenamento.class));

        mapa.put(CategoriaComponente.FONTE, Map.of(
                "certificacao80Plus", Certificado80Plus.class,
                "modularidade", Modularidade.class,
                "padraoATX", PadraoATX.class));

        return mapa;
    }
}

