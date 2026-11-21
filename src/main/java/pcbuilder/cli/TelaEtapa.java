package pcbuilder.cli;

import java.util.List;
import java.util.function.Function;

import pcbuilder.analise.filtro.ItemOculto;
import pcbuilder.analise.filtro.ListaFiltrada;
import pcbuilder.config.OrdemEtapas;
import pcbuilder.modelo.Componente;
import pcbuilder.relatorio.FormatadorMoeda;

/**
 * Cabecalho, barra de progresso, lista filtrada e rodape de uma etapa da montagem
 * guiada.
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
 *
 * <p>A partir da HU-003 a tela tambem sabe desenhar o resultado do filtro: os itens
 * compativeis numerados, a contagem de ocultos, o comando {@code [M] Ver ocultos} e a
 * explicacao de uma etapa sem nenhum item compativel.</p>
 */
public final class TelaEtapa {

    /** Tecla que abre a lista de itens ocultos por incompatibilidade (HU-003). */
    public static final String COMANDO_OCULTOS = "M";
    /** Comando de rodape das etapas que tem itens ocultos. */
    public static final String RODAPE_OCULTOS = "[" + COMANDO_OCULTOS + "] Ver ocultos";
    /** Mensagem exibida quando o filtro nao deixa nenhum item selecionavel. */
    public static final String MENSAGEM_SEM_COMPATIVEIS =
            "Nenhum componente compativel com a sua configuracao ate aqui.";

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

    /**
     * Lista os itens compativeis de uma etapa filtrada, com a contagem de ocultos.
     *
     * <p>Os itens ocultos nao entram na numeracao: so o que esta na lista pode ser
     * escolhido. Quando existe algum oculto, a linha de contagem anuncia o comando
     * {@code [M] Ver ocultos}; quem quiser ve-los usa
     * {@link #imprimirOcultos(ListaFiltrada)}.</p>
     *
     * @param lista resultado do filtro da etapa
     * @param linhaDeApoio texto curto por componente (nucleos, chipset, watts...)
     */
    public void imprimirListaFiltrada(ListaFiltrada lista,
            Function<Componente, String> linhaDeApoio) {
        imprimirSeparador();
        terminal.linha(TabelaComponentes.cabecalho());
        imprimirSeparador();
        List<Componente> compativeis = lista.getCompativeis();
        for (int indice = 0; indice < compativeis.size(); indice++) {
            Componente componente = compativeis.get(indice);
            String apoio = linhaDeApoio == null ? "" : linhaDeApoio.apply(componente);
            terminal.linha(TabelaComponentes.linha(
                    indice + 1,
                    TabelaComponentes.nomeExibicao(componente),
                    FormatadorMoeda.reais(componente.getPrecoMedioBRL()),
                    apoio));
        }
        imprimirSeparador();
        String contagem = textoContagem(lista);
        if (lista.quantidadeOcultos() > 0) {
            contagem = contagem + ESPACO_ENTRE_ETAPAS + RODAPE_OCULTOS;
        }
        terminal.linha(contagem);
    }

    /**
     * Contagem exibida no rodape da lista, por exemplo
     * {@code 2 componentes compativeis (1 oculto por incompatibilidade)}.
     *
     * @param lista resultado do filtro da etapa
     * @return a linha de contagem pronta para impressao
     */
    public static String textoContagem(ListaFiltrada lista) {
        int compativeis = lista.getCompativeis().size();
        int ocultos = lista.quantidadeOcultos();
        String textoCompativeis = compativeis == 1
                ? "1 componente compativel"
                : compativeis + " componentes compativeis";
        String textoOcultos;
        if (ocultos == 0) {
            textoOcultos = "nenhum oculto por incompatibilidade";
        } else if (ocultos == 1) {
            textoOcultos = "1 oculto por incompatibilidade";
        } else {
            textoOcultos = ocultos + " ocultos por incompatibilidade";
        }
        return textoCompativeis + " (" + textoOcultos + ")";
    }

    /**
     * Mostra os itens que o filtro removeu, com o motivo em uma linha.
     *
     * <p>E o comando {@code [M] Ver ocultos}: informativo apenas, os itens nao podem ser
     * escolhidos.</p>
     *
     * @param lista resultado do filtro da etapa
     */
    public void imprimirOcultos(ListaFiltrada lista) {
        imprimirSeparador();
        List<ItemOculto> ocultos = lista.getOcultos();
        if (ocultos.isEmpty()) {
            terminal.linha("Nenhum componente foi ocultado nesta etapa.");
            return;
        }
        terminal.linha("Ocultos por incompatibilidade (" + ocultos.size() + "):");
        for (ItemOculto oculto : ocultos) {
            terminal.linha(" " + TabelaComponentes.nomeExibicao(oculto.getComponente())
                    + " - incompativel: " + oculto.getMotivo());
        }
        terminal.linha("Estes itens nao encaixam na sua configuracao e nao podem ser escolhidos.");
    }

    /**
     * Explica uma etapa sem nenhum item compativel: mensagem, motivo predominante e o
     * comando para voltar e trocar a peca anterior.
     *
     * @param lista resultado do filtro (vazio em {@code compativeis})
     */
    public void imprimirSemCompativeis(ListaFiltrada lista) {
        terminal.linha(MENSAGEM_SEM_COMPATIVEIS);
        lista.motivoPredominante()
                .ifPresent(motivo -> terminal.linha("Motivo predominante: " + motivo));
        terminal.linha("[0] Voltar para trocar a peca anterior");
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
