package pcbuilder.config;

import java.util.List;

import pcbuilder.enums.CategoriaComponente;

/**
 * Ordem fixa das etapas da montagem guiada (secao 2.1 das historias de usuario).
 *
 * <p>A sequencia nao e configuravel em tempo de execucao: o processador define o
 * soquete, a placa-mae define o tipo e o teto de memoria, a placa de video define os
 * conectores e a fonte fecha o fluxo por ultimo, quando todas as pecas ja permitem
 * somar consumo e conectores. Centralizar a ordem aqui evita que a CLI e o servico
 * usem sequencias diferentes.</p>
 */
public final class OrdemEtapas {

    /** Quantidade de etapas do fluxo guiado. */
    public static final int TOTAL_ETAPAS = 6;

    private static final List<CategoriaComponente> CATEGORIAS = List.of(
            CategoriaComponente.CPU,
            CategoriaComponente.PLACA_MAE,
            CategoriaComponente.MEMORIA,
            CategoriaComponente.PLACA_VIDEO,
            CategoriaComponente.ARMAZENAMENTO,
            CategoriaComponente.FONTE);

    /** Rotulos curtos usados na barra de progresso textual do topo da etapa. */
    private static final List<String> ROTULOS_CURTOS = List.of(
            "Processador", "Placa-mae", "Memoria", "Video", "Armazenamento", "Fonte");

    private OrdemEtapas() {
        // classe utilitaria: nao deve ser instanciada
    }

    /** Categorias na ordem de execucao (lista imutavel). */
    public static List<CategoriaComponente> categorias() {
        return CATEGORIAS;
    }

    /**
     * Categoria da etapa informada.
     *
     * @param etapa numero da etapa, de {@code 1} (processador) a {@code 6} (fonte)
     * @return categoria daquela etapa
     * @throws IllegalArgumentException quando o numero esta fora do fluxo
     */
    public static CategoriaComponente categoria(int etapa) {
        exigirEtapaValida(etapa);
        return CATEGORIAS.get(etapa - 1);
    }

    /**
     * Numero da etapa de uma categoria do fluxo.
     *
     * @param categoria categoria ativa (CPU, PLACA_MAE, MEMORIA, PLACA_VIDEO,
     *        ARMAZENAMENTO ou FONTE)
     * @return numero da etapa, de 1 a 6
     * @throws IllegalArgumentException quando a categoria nao faz parte do fluxo
     */
    public static int numeroDe(CategoriaComponente categoria) {
        int indice = CATEGORIAS.indexOf(categoria);
        if (indice < 0) {
            throw new IllegalArgumentException("Categoria fora do fluxo guiado: " + categoria);
        }
        return indice + 1;
    }

    /** Rotulo curto da etapa, usado na barra de progresso. */
    public static String rotuloCurto(int etapa) {
        exigirEtapaValida(etapa);
        return ROTULOS_CURTOS.get(etapa - 1);
    }

    /**
     * Indica se a etapa e obrigatoria.
     *
     * <p>Nesta HU todas as etapas sao obrigatorias. A etapa 4 (placa de video) passa
     * a ser condicional na HU-007, quando a obrigatoriedade depende de
     * {@code cpu.possuiVideoIntegrado} (regra C06).</p>
     */
    public static boolean obrigatoria(int etapa) {
        exigirEtapaValida(etapa);
        return true;
    }

    private static void exigirEtapaValida(int etapa) {
        if (etapa < 1 || etapa > TOTAL_ETAPAS) {
            throw new IllegalArgumentException("Etapa fora do fluxo guiado: " + etapa);
        }
    }
}
