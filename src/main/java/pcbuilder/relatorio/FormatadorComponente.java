package pcbuilder.relatorio;

import pcbuilder.modelo.Componente;

/**
 * Nome do componente como o usuario o conhece: fabricante + modelo.
 *
 * <p>O modelo do catalogo nao repete o fabricante (secao 1 do modelo de dados); juntar os
 * dois na hora de exibir deixa a lista e o resumo autoexplicativos. Fica na camada de
 * relatorio para que a CLI e o resumo usem a mesma formatacao.</p>
 */
public final class FormatadorComponente {

    private FormatadorComponente() {
        // classe utilitaria: nao deve ser instanciada
    }

    /**
     * @param componente componente do catalogo
     * @return por exemplo {@code AMD Ryzen 7 9700X}
     */
    public static String nome(Componente componente) {
        if (componente == null) {
            return "";
        }
        String fabricante = componente.getFabricante();
        if (fabricante == null || fabricante.isBlank()) {
            return componente.getModelo();
        }
        return fabricante + " " + componente.getModelo();
    }
}
