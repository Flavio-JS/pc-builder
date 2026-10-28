package pcbuilder.cli;

import pcbuilder.config.OrdemEtapas;

/**
 * Cabecalho, barra de progresso e rodape de uma etapa da montagem guiada.
 *
 * <p>A barra de progresso lista as seis etapas sempre na mesma ordem e marca a etapa
 * atual:</p>
 *
 * <pre>
 * [+Processador] &gt;&gt;[Placa-mae]&lt;&lt; [Memoria] [Video] [Armazenamento] [Fonte]
 * </pre>
 *
 * <ul>
 *   <li>{@code [+Nome]} - etapa ja concluida;</li>
 *   <li>{@code >>[Nome]<<} - etapa atual;</li>
 *   <li>{@code [Nome]} - etapa pendente.</li>
 * </ul>
 */
public final class TelaEtapa {

    private static final String CONCLUIDA_ABRE = "[+";
    private static final String ATUAL_ABRE = ">>[";
    private static final String ATUAL_FECHA = "]<<";
    private static final String PENDENTE_ABRE = "[";
    private static final String PENDENTE_FECHA = "]";
    private static final String ESPACO_ENTRE_ETAPAS = "   ";

    private final Terminal terminal;

    public TelaEtapa(Terminal terminal) {
        this.terminal = terminal;
    }

    /**
     * Imprime {@code === Etapa N de 6 - <Categoria> (obrigatorio|opcional) ===}.
     *
     * @param etapa numero da etapa (1 a 6)
     */
    public void imprimirCabecalho(int etapa) {
        String situacao = OrdemEtapas.obrigatoria(etapa) ? "obrigatorio" : "opcional";
        terminal.linha("=== Etapa " + etapa + " de " + OrdemEtapas.TOTAL_ETAPAS + " - "
                + OrdemEtapas.categoria(etapa).getRotulo() + " (" + situacao + ") ===");
    }

    /** Imprime a barra de progresso textual no topo da etapa. */
    public void imprimirBarraProgresso(int etapaAtual) {
        terminal.linha(barraProgresso(etapaAtual));
    }

    /**
     * Barra de progresso textual das seis etapas.
     *
     * @param etapaAtual etapa em que o usuario esta
     * @return a linha completa, pronta para impressao
     */
    public static String barraProgresso(int etapaAtual) {
        StringBuilder barra = new StringBuilder();
        for (int etapa = 1; etapa <= OrdemEtapas.TOTAL_ETAPAS; etapa++) {
            if (etapa > 1) {
                barra.append(ESPACO_ENTRE_ETAPAS);
            }
            String rotulo = OrdemEtapas.rotuloCurto(etapa);
            if (etapa == etapaAtual) {
                barra.append(ATUAL_ABRE).append(rotulo).append(ATUAL_FECHA);
            } else if (etapa < etapaAtual) {
                barra.append(CONCLUIDA_ABRE).append(rotulo).append(PENDENTE_FECHA);
            } else {
                barra.append(PENDENTE_ABRE).append(rotulo).append(PENDENTE_FECHA);
            }
        }
        return barra.toString();
    }

    /** Separador horizontal da tela. */
    public void imprimirSeparador() {
        terminal.linha(TabelaComponentes.separador());
    }

    /**
     * Rodape com os comandos disponiveis na etapa.
     *
     * @param comandos textos como {@code [0] Cancelar montagem}, na ordem de exibicao
     */
    public void imprimirRodape(String... comandos) {
        imprimirSeparador();
        terminal.linha(String.join(ESPACO_ENTRE_ETAPAS, comandos));
    }
}
