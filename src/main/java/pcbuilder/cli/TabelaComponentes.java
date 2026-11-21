package pcbuilder.cli;

import java.util.ArrayList;
import java.util.List;

import pcbuilder.modelo.Componente;
import pcbuilder.relatorio.FormatadorComponente;

/**
 * Monta as listas de componentes em texto, com molduras e largura fixa.
 *
 * <p>A tabela tem sempre {@link #LARGURA} colunas para o console nao "pular" quando o
 * modelo e longo: nomes maiores que a coluna sao cortados com {@code ...} e o nome
 * completo continua sendo exibido na mensagem de selecao. A formatacao e responsavel
 * por deixar a lista legivel para quem nao conhece as especificacoes tecnicas.</p>
 *
 * <p>Os textos sao somente ASCII de proposito: o console do Windows trabalha em CP850
 * e acentos apareceriam corrompidos.</p>
 */
public final class TabelaComponentes {

    /** Largura fixa de todas as linhas da interface. */
    public static final int LARGURA = 100;

    /** Marcador de item compativel que gera aviso de limitacao (HU-004). */
    public static final String MARCADOR_LIMITACAO = "[!]";

    private static final int LARGURA_MODELO = 33;
    private static final int LARGURA_PRECO = 12;
    private static final String RODAPE_TRUNCADO = "...";
    private static final String PREFIXO_MARCADOR = "   ";
    private static final String SEPARADOR_APOIO = " | ";

    private TabelaComponentes() {
        // classe utilitaria: nao deve ser instanciada
    }

    /**
     * Nome do componente como o usuario o conhece: fabricante + modelo.
     *
     * <p>O modelo do catalogo nao repete o fabricante (secao 1 do modelo de dados);
     * juntar os dois na hora de exibir deixa a lista autoexplicativa.</p>
     *
     * @param componente componente do catalogo
     * @return por exemplo {@code AMD Ryzen 7 9700X}
     */
    public static String nomeExibicao(Componente componente) {
        return FormatadorComponente.nome(componente);
    }

    /** Linha de titulo das colunas {@code #}, {@code Modelo}, {@code Preco (R$)} e apoio. */
    public static String cabecalho() {
        return PREFIXO_MARCADOR + String.format(
                " %3s  %-" + LARGURA_MODELO + "s  %" + LARGURA_PRECO + "s  %s",
                "#", "Modelo", "Preco (R$)", "Apoio");
    }

    /**
     * Uma linha da lista de componentes.
     *
     * @param numero posicao na lista, comecando em 1
     * @param modelo nome exibido na coluna {@code Modelo}
     * @param preco preco ja formatado ({@code R$ 2.199,00}) ou {@code null}
     * @param apoio linha de apoio da categoria ({@code 8 nucleos | AM5 | ...})
     * @return linha pronta, com no maximo {@link #LARGURA} colunas
     */
    public static String linha(int numero, String modelo, String preco, String apoio) {
        return linha(false, numero, modelo, preco, apoio);
    }

    /**
     * Uma linha da lista de componentes, com o marcador {@code [!]} antes do numero.
     *
     * <p>O {@code [!]} indica um item compativel que gera aviso de limitacao (HU-004).
     * Ele continua selecionavel: o marcador e informativo, nunca um bloqueio.</p>
     *
     * @param possuiLimitacao {@code true} quando o item gera algum aviso
     * @param numero posicao na lista, comecando em 1
     * @param modelo nome exibido na coluna {@code Modelo}
     * @param preco preco ja formatado ({@code R$ 2.199,00}) ou {@code null}
     * @param apoio linha de apoio da categoria ({@code 8 nucleos | AM5 | ...})
     * @return linha pronta, com no maximo {@link #LARGURA} colunas
     */
    public static String linha(boolean possuiLimitacao, int numero, String modelo, String preco,
            String apoio) {
        String formato = (possuiLimitacao ? MARCADOR_LIMITACAO : PREFIXO_MARCADOR)
                + " %3d) %-" + LARGURA_MODELO + "s  %" + LARGURA_PRECO + "s  %s";
        return String.format(formato,
                numero,
                truncar(modelo, LARGURA_MODELO),
                preco == null ? "" : preco,
                apoio == null ? "" : apoio);
    }

    /**
     * Alinha as colunas separadas por {@code |} das linhas de apoio.
     *
     * <p>Para cada posicao de coluna e usada a maior largura entre os itens exibidos;
     * assim os {@code |} ficam na mesma coluna em toda a lista. A ultima coluna nao
     * recebe espacos a direita, para nao poluir a tela.</p>
     *
     * @param apoios linhas de apoio originais, na ordem de exibicao
     * @return as linhas com os campos alinhados
     */
    public static List<String> alinharApoios(List<String> apoios) {
        if (apoios == null || apoios.isEmpty()) {
            return List.of();
        }
        List<String[]> campos = new ArrayList<>();
        int maiorQuantidade = 0;
        for (String apoio : apoios) {
            String[] partes = apoio == null || apoio.isBlank()
                    ? new String[0]
                    : apoio.split("\\s*\\|\\s*", -1);
            campos.add(partes);
            maiorQuantidade = Math.max(maiorQuantidade, partes.length);
        }
        int[] larguras = new int[maiorQuantidade];
        for (String[] partes : campos) {
            for (int indice = 0; indice < partes.length; indice++) {
                larguras[indice] = Math.max(larguras[indice], partes[indice].length());
            }
        }
        List<String> alinhados = new ArrayList<>();
        for (String[] partes : campos) {
            StringBuilder linha = new StringBuilder();
            for (int indice = 0; indice < partes.length; indice++) {
                if (indice > 0) {
                    linha.append(SEPARADOR_APOIO);
                }
                linha.append(partes[indice]);
                if (indice < partes.length - 1) {
                    for (int coluna = partes[indice].length(); coluna < larguras[indice];
                            coluna++) {
                        linha.append(' ');
                    }
                }
            }
            alinhados.add(linha.toString());
        }
        return alinhados;
    }

    /** Moldura superior/inferior da tela. */
    public static String moldura() {
        return "=".repeat(LARGURA);
    }

    /** Separador entre as partes da tela. */
    public static String separador() {
        return "-".repeat(LARGURA);
    }

    /**
     * Corta o texto para caber na coluna.
     *
     * @param texto texto original, pode ser {@code null}
     * @param largura colunas disponiveis
     * @return texto original ou os primeiros caracteres seguidos de {@code ...}
     */
    public static String truncar(String texto, int largura) {
        if (texto == null) {
            return "";
        }
        if (largura <= 0 || texto.length() <= largura) {
            return texto;
        }
        int corte = Math.max(0, largura - RODAPE_TRUNCADO.length());
        return texto.substring(0, corte) + RODAPE_TRUNCADO;
    }
}
