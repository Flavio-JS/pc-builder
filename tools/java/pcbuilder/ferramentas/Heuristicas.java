package pcbuilder.ferramentas;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Regras de derivacao e formatacao usadas pelo conversor OpenDB -> base do projeto.
 *
 * <p>O catalogo OpenDB nao traz todos os campos que o modelo de dados exige (preco,
 * tensoes, VRM, indices de desempenho, velocidades de SSD...). Estas heuristicas
 * preenchem o que falta de forma deterministica; cada regra esta documentada em
 * {@code docs/base-mockada.md}.</p>
 */
final class Heuristicas {

    private static final Pattern ACENTOS = Pattern.compile("\\p{M}");
    private static final Pattern UNIDADES = Pattern.compile(
            "(?i)\\b\\d+(\\.\\d+)?\\s*(GB|TB|MBPS|MBPS|GHz|MHz|MT/s|Gbps|Gb/s|MB/s|W|MM|RPM|IOPS)\\b");
    private static final Pattern DDR = Pattern.compile("(?i)\\bDDR[0-9](-\\d+)?\\b");
    private static final Pattern LATENCIA = Pattern.compile("(?i)\\bCL\\d+\\b");
    private static final Pattern CERTIFICACAO = Pattern.compile(
            "(?i)\\b80\\+\\s*(gold|bronze|silver|platinum|titanium|white)?\\b");
    private static final Pattern MODULARIDADE = Pattern.compile("(?i)\\b(fully|semi)[- ]?modular\\b");
    private static final Pattern PCIE = Pattern.compile("\\bPCIe\\s*\\d+(\\.\\d+)?\\b");
    private static final Pattern VERSAO = Pattern.compile("\\b\\d+\\.\\d+\\b");
    private static final Pattern X_LANES = Pattern.compile("\\bx\\d+\\b");
    private static final Pattern TAMANHOS = Pattern.compile("\\b(2280|2242|22110|2230)\\b");
    private static final Pattern PALAVRAS = Pattern.compile(
            "(?i)\\b(atx|matx|itx|micro|mini|m\\.2|m2|nvme|ssd|hdd|sata|dimm|u-dimm|udimm|so-dimm|"
            + "ram|kit|desktop|memory|internal|processor|cpu|motherboard|mainboard|graphics card|gpu|"
            + "power supply|psu|combo|certified|oem|tray|bulk|non-modular|modular|"
            + "lga\\s?\\d+|am\\d\\+?|gddr\\d+x?|hbm\\d?|nand|3d|"
            + "solid state drive|hard drive|solid state)\\b");
    private static final Pattern CORES = Pattern.compile(
            "(?i)\\b(black|white|gray|grey|red|blue|silver|purple|pink|green|orange|gold|"
            + "rgb|argb|wifi|wi-fi|edition)\\b");
    private static final String[] CORTES = {
        " High-Speed", " Read Write", " Compatible", " and ", " with ", " up to "};
    private static final int LIMITE_NOME = 60;
    private static final int LIMITE_SLUG = 40;

    private Heuristicas() {
        // classe utilitaria: nao deve ser instanciada
    }

    /**
     * Reduz o nome comercial do catalogo a um "modelo" sem as especificacoes que ja
     * vivem em campos proprios (capacidade, frequencia, potencia, cor, formato).
     *
     * @param nome       {@code metadata.name} do catalogo
     * @param fabricante fabricante, para remover o prefixo repetido
     * @return nome limpo (ou o original, se a limpeza esvaziar o texto)
     */
    static String limparNome(String nome, String fabricante) {
        if (nome == null || nome.isBlank()) {
            return nome;
        }
        String resultado = nome;
        if (fabricante != null && !fabricante.isBlank()) {
            resultado = resultado.replaceFirst("(?i)^\\s*" + Pattern.quote(fabricante) + "\\b\\s*", "");
        }
        resultado = resultado.replaceAll("\\([^)]*\\)", " ");
        resultado = UNIDADES.matcher(resultado).replaceAll(" ");
        resultado = DDR.matcher(resultado).replaceAll(" ");
        resultado = LATENCIA.matcher(resultado).replaceAll(" ");
        resultado = CERTIFICACAO.matcher(resultado).replaceAll(" ");
        resultado = MODULARIDADE.matcher(resultado).replaceAll(" ");
        resultado = PCIE.matcher(resultado).replaceAll(" ");
        resultado = VERSAO.matcher(resultado).replaceAll(" ");
        resultado = X_LANES.matcher(resultado).replaceAll(" ");
        resultado = TAMANHOS.matcher(resultado).replaceAll(" ");
        resultado = PALAVRAS.matcher(resultado).replaceAll(" ");
        resultado = CORES.matcher(resultado).replaceAll(" ");
        resultado = resultado.replaceAll("[\\s/|,;:]+", " ").trim();
        resultado = cortarMarketing(resultado);
        if (resultado.length() > LIMITE_NOME) {
            resultado = resultado.substring(0, LIMITE_NOME);
        }
        resultado = resultado.replaceAll("^[\\s-]+|[\\s-]+$", "");
        return resultado.isBlank() ? nome.trim() : resultado;
    }

    /** Corta o texto no primeiro termo de marketing ("High-Speed", "and", "up to"...). */
    private static String cortarMarketing(String nome) {
        String resultado = nome;
        for (String marcador : CORTES) {
            int posicao = resultado.indexOf(marcador);
            if (posicao > 0) {
                resultado = resultado.substring(0, posicao);
            }
        }
        return resultado;
    }

    /** Gera o identificador do registro: prefixo + modelo + 12 caracteres do UUID. */
    static String id(String prefixo, String nome, String opendbId) {
        StringBuilder resultado = new StringBuilder(prefixo).append('-').append(slug(nome));
        if (opendbId != null && !opendbId.isBlank()) {
            String sufixo = opendbId.replace("-", "");
            resultado.append('-').append(sufixo.substring(0, Math.min(12, sufixo.length())));
        }
        return resultado.toString().replaceAll("-+", "-");
    }

    private static String slug(String texto) {
        String semAcento = ACENTOS.matcher(
                Normalizer.normalize(texto == null ? "" : texto, Normalizer.Form.NFD)).replaceAll("");
        String slug = semAcento.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
        slug = slug.replaceAll("^-+|-+$", "");
        if (slug.length() > LIMITE_SLUG) {
            slug = slug.substring(0, LIMITE_SLUG).replaceAll("-+$", "");
        }
        return slug.isBlank() ? "sem-nome" : slug;
    }

    /** Preco ficticio arredondado para a dezena de reais menos dez centavos. */
    static BigDecimal preco(double valorBruto) {
        double valor = Math.max(49.90, Math.round(valorBruto / 10.0) * 10.0 - 0.10);
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Normaliza um indicador de desempenho bruto para a escala 1-100 do modelo.
     *
     * @param valor  indicador do componente
     * @param minimo menor indicador do catalogo
     * @param maximo maior indicador do catalogo
     */
    static int normalizar(double valor, double minimo, double maximo) {
        if (maximo <= minimo) {
            return 50;
        }
        long indice = Math.round(1 + ((valor - minimo) / (maximo - minimo)) * 99);
        return (int) Math.max(1, Math.min(100, indice));
    }

    /** Arredonda para o multiplo de 50 mais proximo (usado em fonte recomendada). */
    static int multiplo50(double valor) {
        return (int) Math.max(50, Math.round(valor / 50.0) * 50);
    }
}
