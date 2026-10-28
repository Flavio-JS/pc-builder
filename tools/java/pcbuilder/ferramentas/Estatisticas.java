package pcbuilder.ferramentas;

import java.util.Map;
import java.util.TreeMap;

/**
 * Contadores da conversao: quantos registros entraram e por que os demais foram
 * ignorados. O resumo e impresso no console e copiado para
 * {@code docs/base-mockada.md}.
 */
final class Estatisticas {

    private final Map<String, Integer> incluidos = new TreeMap<>();
    private final Map<String, Integer> ignorados = new TreeMap<>();

    void incluir(String categoria, int quantidade) {
        incluidos.merge(categoria, quantidade, Integer::sum);
    }

    void ignorar(String motivo) {
        ignorados.merge(motivo, 1, Integer::sum);
    }

    int total() {
        int total = 0;
        for (Integer quantidade : incluidos.values()) {
            total += quantidade;
        }
        return total;
    }

    void imprimir() {
        System.out.println("Componentes incluidos:");
        for (Map.Entry<String, Integer> entrada : incluidos.entrySet()) {
            System.out.printf("  %-14s %6d%n", entrada.getKey(), entrada.getValue());
        }
        System.out.printf("  %-14s %6d%n", "TOTAL", total());

        System.out.println("Registros ignorados:");
        if (ignorados.isEmpty()) {
            System.out.println("  (nenhum)");
        }
        for (Map.Entry<String, Integer> entrada : ignorados.entrySet()) {
            System.out.printf("  %-46s %6d%n", entrada.getKey(), entrada.getValue());
        }
    }
}
