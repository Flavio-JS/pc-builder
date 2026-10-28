package pcbuilder.servico;

import java.util.Objects;

import pcbuilder.config.OrdemEtapas;
import pcbuilder.enums.CategoriaComponente;
import pcbuilder.modelo.Montagem;

/**
 * Orquestrador do fluxo guiado: guarda a {@link Montagem} em andamento e a etapa
 * atual (secao 2.1 das historias de usuario).
 *
 * <p>A classe nao imprime nada e nao le o teclado - ela apenas controla a posicao no
 * fluxo. Quem desenha a tela e qual etapa executar e a camada {@code cli}, o que
 * deixa a regra do fluxo testavel sem terminal.</p>
 *
 * <p>A ordem das etapas vem de {@link OrdemEtapas}; a montagem e a mesma durante todo
 * o fluxo, por isso a CPU escolhida na etapa 1 continua disponivel na etapa 2.</p>
 */
public final class MontagemGuiada {

    /** Numero da etapa usada para representar "fluxo terminado". */
    private static final int APOS_ULTIMA_ETAPA = OrdemEtapas.TOTAL_ETAPAS + 1;

    private final Montagem montagem;
    private int etapaAtual = 1;

    /** Cria o fluxo com uma montagem vazia. */
    public MontagemGuiada() {
        this(new Montagem());
    }

    /**
     * Cria o fluxo sobre uma montagem existente (usado ao reabrir uma etapa na
     * HU-011).
     *
     * @param montagem montagem que o fluxo vai preencher
     */
    public MontagemGuiada(Montagem montagem) {
        this.montagem = Objects.requireNonNull(montagem, "montagem");
    }

    public Montagem getMontagem() {
        return montagem;
    }

    /** Numero da etapa atual (1 a 6); 7 quando todas ja foram percorridas. */
    public int etapaAtual() {
        return etapaAtual;
    }

    /** Categoria que a etapa atual coleta. */
    public CategoriaComponente categoriaAtual() {
        if (concluir()) {
            throw new IllegalStateException("A montagem ja passou da ultima etapa.");
        }
        return OrdemEtapas.categoria(etapaAtual);
    }

    /** Quantidade de etapas do fluxo (6). */
    public int totalEtapas() {
        return OrdemEtapas.TOTAL_ETAPAS;
    }

    /**
     * O fluxo terminou?
     *
     * @return {@code true} depois de percorridas as seis etapas
     */
    public boolean concluir() {
        return etapaAtual >= APOS_ULTIMA_ETAPA;
    }

    /**
     * Avanca uma etapa.
     *
     * @return o novo numero da etapa, ou {@code 7} quando o fluxo terminou
     */
    public int proximaEtapa() {
        if (!concluir()) {
            etapaAtual++;
        }
        return etapaAtual;
    }

    /** A etapa informada ja foi percorrida? */
    public boolean etapaConcluida(int etapa) {
        return etapa >= 1 && etapa < etapaAtual;
    }
}
