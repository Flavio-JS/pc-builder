package pcbuilder.servico;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import pcbuilder.config.OrdemEtapas;
import pcbuilder.enums.CategoriaComponente;
import pcbuilder.modelo.Montagem;

/**
 * Valida se a montagem tem todas as etapas obrigatorias antes do resumo (HU-010).
 *
 * <p>Usa {@link OrdemEtapas#obrigatoria(int, Montagem)}: a etapa 4 (placa de video) so
 * entra na lista de pendencias quando a CPU escolhida nao tem video integrado. Uma peca
 * opcional ausente (GPU pulada) nao impede o resumo.</p>
 */
public final class ValidadorMontagem {

    private ValidadorMontagem() {
        // classe utilitaria: nao deve ser instanciada
    }

    /**
     * Etapas obrigatorias que ainda estao sem peca.
     *
     * @param montagem montagem em andamento
     * @return as categorias pendentes, na ordem do fluxo
     */
    public static List<CategoriaComponente> pendencias(Montagem montagem) {
        Objects.requireNonNull(montagem, "montagem");
        List<CategoriaComponente> pendencias = new ArrayList<>();
        for (int etapa = 1; etapa <= OrdemEtapas.TOTAL_ETAPAS; etapa++) {
            CategoriaComponente categoria = OrdemEtapas.categoria(etapa);
            if (OrdemEtapas.obrigatoria(etapa, montagem)
                    && montagem.obter(categoria).isEmpty()) {
                pendencias.add(categoria);
            }
        }
        return List.copyOf(pendencias);
    }

    /**
     * @param montagem montagem em andamento
     * @return {@code true} quando nao ha etapa obrigatoria pendente
     */
    public static boolean completa(Montagem montagem) {
        return pendencias(montagem).isEmpty();
    }

    /**
     * Rotulos das etapas pendentes ({@code Placa-Mae, Memoria, ...}).
     *
     * @param montagem montagem em andamento
     * @return os rotulos separados por virgula; vazio quando a montagem esta completa
     */
    public static String descricaoPendencias(Montagem montagem) {
        List<String> rotulos = new ArrayList<>();
        for (CategoriaComponente categoria : pendencias(montagem)) {
            rotulos.add(categoria.getRotulo());
        }
        return String.join(", ", rotulos);
    }
}
