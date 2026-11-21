package pcbuilder.cli;

import java.util.List;
import java.util.function.Function;

import pcbuilder.analise.filtro.ItemOculto;
import pcbuilder.analise.filtro.ListaFiltrada;
import pcbuilder.analise.limitacao.TextosLimitacao;
import pcbuilder.config.OrdemEtapas;
import pcbuilder.modelo.Componente;
import pcbuilder.modelo.Limitacao;
import pcbuilder.modelo.Montagem;
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
    /** Tecla que relê os avisos de limitacao acumulados (HU-004). */
    public static final String COMANDO_AVISOS = "L";
    /** Comando de rodape das etapas que tem avisos de limitacao. */
    public static final String RODAPE_AVISOS = "[" + COMANDO_AVISOS + "] Avisos";
    /** Prompt exibido depois do bloco de aviso de uma selecao. */
    public static final String MENSAGEM_CONTINUAR = "[Enter] para continuar";
    /** Mensagem exibida quando o filtro nao deixa nenhum item selecionavel. */
    public static final String MENSAGEM_SEM_COMPATIVEIS =
            "Nenhum componente compativel com a sua configuracao ate aqui.";
    /** Mensagem de [L] quando nenhuma limitacao foi aceita ate aqui. */
    public static final String MENSAGEM_SEM_AVISOS = "Nenhum aviso ate aqui.";

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
     * <p>Sem a montagem, a etapa 4 aparece como obrigatoria; as telas do fluxo usam a
     * versao com a montagem para decidir a placa de video (C06).</p>
     *
     * @param etapa numero da etapa (1 a 6)
     */
    public void imprimirCabecalho(int etapa) {
        imprimirCabecalho(etapa, new Montagem());
    }

    /**
     * Imprime {@code === Etapa N de 6 - <Categoria> (obrigatorio|opcional) ===}.
     *
     * @param etapa numero da etapa (1 a 6)
     * @param montagem pecas escolhidas ate aqui (decide a obrigatoriedade da etapa 4)
     */
    public void imprimirCabecalho(int etapa, Montagem montagem) {
        String situacao = OrdemEtapas.obrigatoria(etapa, montagem) ? "obrigatorio" : "opcional";
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
        imprimirListaFiltrada(lista, linhaDeApoio, componente -> List.of());
    }

    /**
     * Lista os itens compativeis marcando com {@code [!]} os que geram limitacao.
     *
     * <p>O marcador vem da avaliacao da HU-004 e <strong>nao</strong> tira o item da
     * lista: ele continua selecionavel. O rodape anuncia quantos itens tem aviso e o
     * comando {@code [L] Avisos} para reler os avisos ja aceitos.</p>
     *
     * @param lista resultado do filtro da etapa
     * @param linhaDeApoio texto curto por componente (nucleos, chipset, watts...)
     * @param limitacoes avisos de cada componente (lista vazia quando nao ha)
     */
    public void imprimirListaFiltrada(ListaFiltrada lista,
            Function<Componente, String> linhaDeApoio,
            Function<Componente, List<Limitacao>> limitacoes) {
        imprimirSeparador();
        terminal.linha(TabelaComponentes.cabecalho());
        imprimirSeparador();
        List<Componente> compativeis = lista.getCompativeis();
        int comAviso = 0;
        for (int indice = 0; indice < compativeis.size(); indice++) {
            Componente componente = compativeis.get(indice);
            String apoio = linhaDeApoio == null ? "" : linhaDeApoio.apply(componente);
            List<Limitacao> avisos = limitacoes == null
                    ? List.of()
                    : limitacoes.apply(componente);
            boolean marcado = avisos != null && !avisos.isEmpty();
            if (marcado) {
                comAviso++;
            }
            terminal.linha(TabelaComponentes.linha(
                    marcado,
                    indice + 1,
                    TabelaComponentes.nomeExibicao(componente),
                    FormatadorMoeda.reais(componente.getPrecoMedioBRL()),
                    apoio));
        }
        imprimirSeparador();
        terminal.linha(textoRodape(lista, comAviso));
    }

    /**
     * Rodape da lista: contagem de compativeis/ocultos, itens com aviso e comandos.
     *
     * @param lista resultado do filtro
     * @param comAviso quantidade de itens marcados com {@code [!]}
     * @return a linha pronta para impressao
     */
    public static String textoRodape(ListaFiltrada lista, int comAviso) {
        String contagem = textoContagem(lista);
        if (comAviso > 0) {
            contagem = contagem + ESPACO_ENTRE_ETAPAS + comAviso + " com aviso [!]";
        }
        if (lista.quantidadeOcultos() > 0) {
            contagem = contagem + ESPACO_ENTRE_ETAPAS + RODAPE_OCULTOS;
        }
        if (comAviso > 0) {
            contagem = contagem + ESPACO_ENTRE_ETAPAS + RODAPE_AVISOS;
        }
        return contagem;
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
        imprimirSemCompativeis(lista, MENSAGEM_SEM_COMPATIVEIS,
                "[0] Voltar para trocar a peca anterior");
    }

    /**
     * Explica uma etapa sem nenhum item compativel com uma mensagem/comando especificos
     * (por exemplo, a etapa 4 opcional oferece {@code [0] Pular}).
     *
     * @param lista resultado do filtro (vazio em {@code compativeis})
     * @param mensagem primeira linha da explicacao
     * @param linhaZero comando exibido no rodape ({@code [0] ...})
     */
    public void imprimirSemCompativeis(ListaFiltrada lista, String mensagem, String linhaZero) {
        terminal.linha(mensagem);
        lista.motivoPredominante()
                .ifPresent(motivo -> terminal.linha("Motivo predominante: " + motivo));
        terminal.linha(linhaZero);
    }

    /**
     * Imprime o bloco de aviso de uma limitacao (HU-004), com severidade, componente
     * limitante, campo, valores, consequencia e o lembrete de que a escolha vale.
     *
     * @param limitacao aviso detectado pelo {@code AvaliadorLimitacoes}
     */
    public void imprimirLimitacao(Limitacao limitacao) {
        for (String linha : TextosLimitacao.linhas(limitacao)) {
            terminal.linha(linha);
        }
    }

    /**
     * Mostra os avisos de uma selecao e espera {@code [Enter] para continuar}.
     *
     * <p>Chamado pelo fluxo logo depois de o usuario escolher um item marcado com
     * {@code [!]}: nada e bloqueado, mas a perda e explicada antes de avancar.</p>
     *
     * @param avisos limitacoes da peca escolhida (vazio quando nao ha nada a avisar)
     */
    public void imprimirAvisoSelecao(List<Limitacao> avisos) {
        if (avisos == null || avisos.isEmpty()) {
            return;
        }
        for (Limitacao limitacao : avisos) {
            imprimirLimitacao(limitacao);
        }
        terminal.perguntar(MENSAGEM_CONTINUAR);
    }

    /**
     * Comando {@code [L] Avisos}: rele os avisos de limitacao acumulados na montagem.
     *
     * @param montagem montagem em andamento
     */
    public void imprimirAvisosDaMontagem(Montagem montagem) {
        imprimirSeparador();
        List<Limitacao> avisos = montagem.getLimitacoes();
        if (avisos.isEmpty()) {
            terminal.linha(MENSAGEM_SEM_AVISOS);
            return;
        }
        terminal.linha("Avisos acumulados (" + avisos.size() + "):");
        for (Limitacao limitacao : avisos) {
            imprimirLimitacao(limitacao);
        }
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
