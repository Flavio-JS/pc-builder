package pcbuilder.relatorio;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Formata valores em reais no padrao pt-BR ({@code R$ 2.199,00}).
 *
 * <p>Os precos vivem no mock como {@link BigDecimal}; a formatacao existe so na
 * camada de apresentacao, para que o total nunca seja somado a partir de texto
 * formatado. Peca sem preco no catalogo aparece como {@link #SEM_PRECO}.</p>
 */
public final class FormatadorMoeda {

    /** Texto usado quando o componente nao tem preco cadastrado. */
    public static final String SEM_PRECO = "R$ --";

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private FormatadorMoeda() {
        // classe utilitaria: nao deve ser instanciada
    }

    /**
     * Formata o valor com simbolo e duas casas decimais.
     *
     * @param valor preco do componente, pode ser {@code null}
     * @return {@code R$ 2.199,00} ou {@link #SEM_PRECO} quando o valor e nulo
     */
    public static String reais(BigDecimal valor) {
        if (valor == null) {
            return SEM_PRECO;
        }
        NumberFormat formato = NumberFormat.getNumberInstance(PT_BR);
        formato.setGroupingUsed(true);
        formato.setMinimumFractionDigits(2);
        formato.setMaximumFractionDigits(2);
        return "R$ " + formato.format(valor);
    }
}
