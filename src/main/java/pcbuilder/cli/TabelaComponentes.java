package pcbuilder.cli;

import pcbuilder.modelo.Componente;

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

    private static final int LARGURA_MODELO = 33;
    private static final int LARGURA_PRECO = 12;
    private static final String RODAPE_TRUNCADO = "...";

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
        String fabricante = componente.getFabricante();
        if (fabricante == null || fabricante.isBlank()) {
            return componente.getModelo();
        }
        return fabricante + " " + componente.getModelo();
    }

    /** Linha de titulo das colunas {@code #}, {@code Modelo}, {@code Preco (R$)} e apoio. */
    public static String cabecalho() {
        return String.format(" %3s  %-" + LARGURA_MODELO + "s  %" + LARGURA_PRECO + "s  %s",
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
        return String.format(" %3d) %-" + LARGURA_MODELO + "s  %" + LARGURA_PRECO + "s  %s",
                numero,
                truncar(modelo, LARGURA_MODELO),
                preco == null ? "" : preco,
                apoio == null ? "" : apoio);
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
