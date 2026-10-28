package pcbuilder.teste;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Mini framework de assercao, escrito apenas com a biblioteca padrao do JDK.
 *
 * <p>O trabalho nao usa dependencia externa (nem JUnit), entao os testes rodam por
 * {@code main} e o resultado e apresentado no console. Cada assercao conta como uma
 * verificacao; as falhas ficam acumuladas com a descricao do caso.</p>
 */
public final class Assercao {

    private static int verificacoes;
    private static final List<String> falhas = new ArrayList<>();

    private Assercao() {
        // classe utilitaria: nao deve ser instanciada
    }

    /** Verifica igualdade (com {@code null} aceito dos dois lados). */
    public static void iguais(String caso, Object esperado, Object obtido) {
        verificacoes++;
        if (!Objects.equals(esperado, obtido)) {
            falhas.add(caso + ": esperado <" + esperado + ">, obtido <" + obtido + ">");
        }
    }

    /** Verifica que a condicao e verdadeira. */
    public static void verdadeiro(String caso, boolean condicao) {
        verificacoes++;
        if (!condicao) {
            falhas.add(caso + ": esperava verdadeiro");
        }
    }

    /** Verifica que a condicao e falsa. */
    public static void falso(String caso, boolean condicao) {
        verificacoes++;
        if (condicao) {
            falhas.add(caso + ": esperava falso");
        }
    }

    /**
     * Imprime o balanco e devolve a quantidade de falhas (usa-la como codigo de saida).
     *
     * @return {@code 0} quando todos os casos passaram
     */
    public static int concluir() {
        System.out.println();
        if (falhas.isEmpty()) {
            System.out.println("OK: " + verificacoes + " verificacoes, 0 falha(s).");
            return 0;
        }
        System.out.println("FALHOU: " + falhas.size() + " problema(s) em " + verificacoes + " verificacoes:");
        for (String falha : falhas) {
            System.out.println("  - " + falha);
        }
        return falhas.size();
    }
}
