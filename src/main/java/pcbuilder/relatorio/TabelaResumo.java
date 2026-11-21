package pcbuilder.relatorio;

/**
 * Tabela do resumo final (HU-010): colunas {@code Etapa}, {@code Categoria},
 * {@code Peca escolhida} e {@code Preco (R$)}, com largura fixa de 100 colunas.
 *
 * <p>Os textos longos sao cortados com {@code ...} para a tabela nao "pular" no console.
 * O preco ja chega formatado - a tabela nao faz conta.</p>
 */
public final class TabelaResumo {

    /** Largura fixa de todas as linhas do resumo. */
    public static final int LARGURA = 100;

    private static final int LARGURA_ETAPA = 5;
    private static final int LARGURA_CATEGORIA = 16;
    private static final int LARGURA_PECA = 46;
    private static final int LARGURA_PRECO = 14;
    private static final String RODAPE_TRUNCADO = "...";

    private TabelaResumo() {
        // classe utilitaria: nao deve ser instanciada
    }

    /** Linha de titulo das colunas. */
    public static String cabecalho() {
        return linha("Etapa", "Categoria", "Peca escolhida", "Preco (R$)");
    }

    /**
     * Uma linha do resumo.
     *
     * @param etapa numero da etapa (1 a 6)
     * @param categoria rotulo da categoria
     * @param peca nome da peca ou {@code (nao selecionado)}
     * @param preco preco ja formatado ({@code 2.199,00} / {@code R$ --})
     * @return linha com exatamente {@link #LARGURA} colunas
     */
    public static String linha(int etapa, String categoria, String peca, String preco) {
        return linha(String.valueOf(etapa), categoria, peca, preco);
    }

    private static String linha(String etapa, String categoria, String peca, String preco) {
        String texto = String.format(" %-" + LARGURA_ETAPA + "s | %-" + LARGURA_CATEGORIA
                        + "s | %-" + LARGURA_PECA + "s | %" + LARGURA_PRECO + "s",
                etapa,
                truncar(categoria, LARGURA_CATEGORIA),
                truncar(peca, LARGURA_PECA),
                preco == null ? "" : preco);
        return String.format("%-" + LARGURA + "s", texto);
    }

    /** Separador horizontal do resumo. */
    public static String separador() {
        return "-".repeat(LARGURA);
    }

    /**
     * Linha do total.
     *
     * @param valor total ja formatado com simbolo ({@code R$ 5.345,00})
     * @return {@code TOTAL: R$ 5.345,00}
     */
    public static String total(String valor) {
        return "TOTAL: " + valor;
    }

    private static String truncar(String texto, int largura) {
        if (texto == null) {
            return "";
        }
        if (texto.length() <= largura) {
            return texto;
        }
        int corte = Math.max(0, largura - RODAPE_TRUNCADO.length());
        return texto.substring(0, corte) + RODAPE_TRUNCADO;
    }
}
