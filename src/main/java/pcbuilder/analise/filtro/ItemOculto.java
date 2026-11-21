package pcbuilder.analise.filtro;

import java.util.Objects;

import pcbuilder.modelo.Componente;

/**
 * Componente que o filtro removeu da lista da etapa, com o motivo em uma linha.
 *
 * <p>Itens ocultos aparecem somente em {@code [M] Ver ocultos} e nunca podem ser
 * escolhidos: eles nao encaixam fisicamente ou eletricamente na configuracao.</p>
 */
public final class ItemOculto {

    private final Componente componente;
    private final String motivo;

    /**
     * @param componente item removido da lista
     * @param motivo frase curta do porque ele nao encaixa
     */
    public ItemOculto(Componente componente, String motivo) {
        this.componente = Objects.requireNonNull(componente, "componente");
        this.motivo = Objects.requireNonNull(motivo, "motivo");
    }

    public Componente getComponente() {
        return componente;
    }

    public String getMotivo() {
        return motivo;
    }

    @Override
    public String toString() {
        return componente + " - incompativel: " + motivo;
    }
}
