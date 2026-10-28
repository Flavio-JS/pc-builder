package pcbuilder.json;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Leitor e escritor JSON minimo, escrito apenas com a biblioteca padrao do JDK.
 *
 * <p>O projeto nao pode usar biblioteca externa (Jackson, Gson etc.), mas precisa
 * ler a base {@code base/componentes.json} e gerar esse mesmo arquivo a partir do
 * catalogo OpenDB. Esta classe cobre o subconjunto da especificacao JSON que o
 * projeto usa:</p>
 *
 * <ul>
 *   <li>leitura: objeto, array, texto (com escapes e {@code \\uXXXX}), numero,
 *       {@code true}/{@code false} e {@code null};</li>
 *   <li>numeros viram {@link BigDecimal} (nunca {@code double}), para nao perder
 *       precisao em valores como preco e tensao;</li>
 *   <li>escrita compacta, na ordem de insercao das chaves.</li>
 * </ul>
 */
public final class Json {

    private final String texto;
    private int posicao;

    private Json(String texto) {
        this.texto = texto;
    }

    /**
     * Converte um texto JSON em estruturas do JDK.
     *
     * @param texto conteudo do arquivo
     * @return {@code Map<String,Object>}, {@code List<Object>}, {@code String},
     *         {@link BigDecimal}, {@code Boolean} ou {@code null}
     * @throws IllegalArgumentException quando o texto nao e um JSON valido
     */
    public static Object ler(String texto) {
        Json leitor = new Json(texto);
        leitor.pularEspacos();
        Object valor = leitor.lerValor();
        leitor.pularEspacos();
        if (!leitor.fim()) {
            throw leitor.erro("conteudo extra depois do valor");
        }
        return valor;
    }

    private Object lerValor() {
        if (fim()) {
            throw erro("valor esperado");
        }
        char atual = texto.charAt(posicao);
        return switch (atual) {
            case '{' -> lerObjeto();
            case '[' -> lerLista();
            case '"' -> lerTexto();
            case 't' -> lerPalavra("true", Boolean.TRUE);
            case 'f' -> lerPalavra("false", Boolean.FALSE);
            case 'n' -> lerPalavra("null", null);
            default -> lerNumero();
        };
    }

    private Map<String, Object> lerObjeto() {
        Map<String, Object> objeto = new LinkedHashMap<>();
        posicao++; // consome '{'
        pularEspacos();
        if (!fim() && texto.charAt(posicao) == '}') {
            posicao++;
            return objeto;
        }
        while (true) {
            pularEspacos();
            String chave = lerTexto();
            pularEspacos();
            exigir(':');
            pularEspacos();
            objeto.put(chave, lerValor());
            pularEspacos();
            if (!fim() && texto.charAt(posicao) == ',') {
                posicao++;
                continue;
            }
            exigir('}');
            return objeto;
        }
    }

    private List<Object> lerLista() {
        List<Object> lista = new ArrayList<>();
        posicao++; // consome '['
        pularEspacos();
        if (!fim() && texto.charAt(posicao) == ']') {
            posicao++;
            return lista;
        }
        while (true) {
            pularEspacos();
            lista.add(lerValor());
            pularEspacos();
            if (!fim() && texto.charAt(posicao) == ',') {
                posicao++;
                continue;
            }
            exigir(']');
            return lista;
        }
    }

    private String lerTexto() {
        exigir('"');
        StringBuilder resultado = new StringBuilder();
        while (!fim()) {
            char atual = texto.charAt(posicao++);
            if (atual == '"') {
                return resultado.toString();
            }
            if (atual != '\\') {
                resultado.append(atual);
                continue;
            }
            char escape = texto.charAt(posicao++);
            switch (escape) {
                case '"' -> resultado.append('"');
                case '\\' -> resultado.append('\\');
                case '/' -> resultado.append('/');
                case 'b' -> resultado.append('\b');
                case 'f' -> resultado.append('\f');
                case 'n' -> resultado.append('\n');
                case 'r' -> resultado.append('\r');
                case 't' -> resultado.append('\t');
                case 'u' -> {
                    String hexa = texto.substring(posicao, posicao + 4);
                    posicao += 4;
                    resultado.append((char) Integer.parseInt(hexa, 16));
                }
                default -> throw erro("escape desconhecido: \\" + escape);
            }
        }
        throw erro("texto sem fechamento");
    }

    private Object lerPalavra(String palavra, Object valor) {
        if (!texto.startsWith(palavra, posicao)) {
            throw erro("esperava '" + palavra + "'");
        }
        posicao += palavra.length();
        return valor;
    }

    private BigDecimal lerNumero() {
        int inicio = posicao;
        if (!fim() && (texto.charAt(posicao) == '-' || texto.charAt(posicao) == '+')) {
            posicao++;
        }
        while (!fim() && "0123456789.eE+-".indexOf(texto.charAt(posicao)) >= 0) {
            posicao++;
        }
        String bruto = texto.substring(inicio, posicao);
        if (bruto.isEmpty()) {
            throw erro("numero esperado na posicao " + inicio);
        }
        try {
            return new BigDecimal(bruto);
        } catch (NumberFormatException causa) {
            throw erro("numero invalido: " + bruto);
        }
    }

    private void pularEspacos() {
        while (!fim()) {
            char atual = texto.charAt(posicao);
            if (atual == ' ' || atual == '\n' || atual == '\r' || atual == '\t' || atual == '\uFEFF') {
                posicao++;
            } else {
                return;
            }
        }
    }

    private void exigir(char esperado) {
        if (fim() || texto.charAt(posicao) != esperado) {
            throw erro("esperava '" + esperado + "'");
        }
        posicao++;
    }

    private boolean fim() {
        return posicao >= texto.length();
    }

    private IllegalArgumentException erro(String mensagem) {
        return new IllegalArgumentException("JSON invalido (" + mensagem + ") na posicao " + posicao);
    }

    // -------------------------------------------------------------- consulta

    /** Valor de um campo de um objeto JSON (ou {@code null}). */
    public static Object valor(Object mapa, String campo) {
        if (!(mapa instanceof Map<?, ?> objeto)) {
            return null;
        }
        return objeto.get(campo);
    }

    /** Valor de um caminho aninhado, por exemplo {@code "specifications.tdp"}. */
    public static Object caminho(Object raiz, String caminho) {
        Object atual = raiz;
        for (String parte : caminho.split("\\.")) {
            atual = valor(atual, parte);
        }
        return atual;
    }

    public static String texto(Object mapa, String campo) {
        return valor(mapa, campo) instanceof String texto ? texto : null;
    }

    public static BigDecimal numero(Object mapa, String campo) {
        return valor(mapa, campo) instanceof BigDecimal numero ? numero : null;
    }

    public static Integer inteiro(Object mapa, String campo) {
        BigDecimal numero = numero(mapa, campo);
        return numero == null ? null : Integer.valueOf(numero.intValue());
    }

    public static Boolean booleano(Object mapa, String campo) {
        return valor(mapa, campo) instanceof Boolean valor ? valor : null;
    }

    public static List<Object> lista(Object mapa, String campo) {
        return valor(mapa, campo) instanceof List<?> itens ? new ArrayList<>(itens) : List.of();
    }

    // --------------------------------------------------------------- escrita

    /** Serializa o valor em uma unica linha, sem espacos desnecessarios. */
    public static String escrever(Object valor) {
        StringBuilder saida = new StringBuilder();
        escrever(valor, saida);
        return saida.toString();
    }

    private static void escrever(Object valor, StringBuilder saida) {
        if (valor == null) {
            saida.append("null");
        } else if (valor instanceof Enum<?> constante) {
            escreverTexto(constante.name(), saida);
        } else if (valor instanceof String texto) {
            escreverTexto(texto, saida);
        } else if (valor instanceof BigDecimal numero) {
            saida.append(numero.toPlainString());
        } else if (valor instanceof Integer || valor instanceof Long || valor instanceof Short) {
            saida.append(valor.toString());
        } else if (valor instanceof Number numero) {
            saida.append(BigDecimal.valueOf(numero.doubleValue()).toPlainString());
        } else if (valor instanceof Boolean booleano) {
            saida.append(booleano.booleanValue());
        } else if (valor instanceof Map<?, ?> mapa) {
            escreverMapa(mapa, saida);
        } else if (valor instanceof Collection<?> itens) {
            escreverLista(itens, saida);
        } else {
            escreverTexto(valor.toString(), saida);
        }
    }

    private static void escreverMapa(Map<?, ?> mapa, StringBuilder saida) {
        saida.append('{');
        boolean primeiro = true;
        for (Map.Entry<?, ?> entrada : mapa.entrySet()) {
            if (!primeiro) {
                saida.append(',');
            }
            escreverTexto(String.valueOf(entrada.getKey()), saida);
            saida.append(':');
            escrever(entrada.getValue(), saida);
            primeiro = false;
        }
        saida.append('}');
    }

    private static void escreverLista(Collection<?> itens, StringBuilder saida) {
        saida.append('[');
        boolean primeiro = true;
        for (Object item : itens) {
            if (!primeiro) {
                saida.append(',');
            }
            escrever(item, saida);
            primeiro = false;
        }
        saida.append(']');
    }

    private static void escreverTexto(String texto, StringBuilder saida) {
        saida.append('"');
        for (int indice = 0; indice < texto.length(); indice++) {
            char atual = texto.charAt(indice);
            switch (atual) {
                case '"' -> saida.append("\\\"");
                case '\\' -> saida.append("\\\\");
                case '\n' -> saida.append("\\n");
                case '\r' -> saida.append("\\r");
                case '\t' -> saida.append("\\t");
                default -> {
                    if (atual < 0x20) {
                        saida.append(String.format("\\u%04x", (int) atual));
                    } else {
                        saida.append(atual);
                    }
                }
            }
        }
        saida.append('"');
    }
}
