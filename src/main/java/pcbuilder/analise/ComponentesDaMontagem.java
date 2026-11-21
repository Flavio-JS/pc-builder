package pcbuilder.analise;

import java.util.Objects;
import java.util.Optional;

import pcbuilder.enums.CategoriaComponente;
import pcbuilder.modelo.Armazenamento;
import pcbuilder.modelo.Componente;
import pcbuilder.modelo.Fonte;
import pcbuilder.modelo.Memoria;
import pcbuilder.modelo.Montagem;
import pcbuilder.modelo.PlacaMae;
import pcbuilder.modelo.PlacaVideo;
import pcbuilder.modelo.Processador;

/**
 * Leitura tipada das pecas ja escolhidas na {@link Montagem}.
 *
 * <p>A montagem guarda {@link Componente} por categoria, sem tipo especifico. As regras
 * de filtro e de limitacao precisam do tipo concreto (soquete, slots, conectores), e
 * repetir o {@code instanceof} em cada regra seria uma fonte de erro. Esta classe
 * concentra a conversao e devolve vazio quando a peca ainda nao foi escolhida.</p>
 */
public final class ComponentesDaMontagem {

    private ComponentesDaMontagem() {
        // classe utilitaria: nao deve ser instanciada
    }

    /** Processador escolhido, quando ja existe. */
    public static Optional<Processador> cpu(Montagem montagem) {
        return peca(montagem, CategoriaComponente.CPU, Processador.class);
    }

    /** Placa-mae escolhida, quando ja existe. */
    public static Optional<PlacaMae> placaMae(Montagem montagem) {
        return peca(montagem, CategoriaComponente.PLACA_MAE, PlacaMae.class);
    }

    /** Memoria escolhida, quando ja existe. */
    public static Optional<Memoria> memoria(Montagem montagem) {
        return peca(montagem, CategoriaComponente.MEMORIA, Memoria.class);
    }

    /** Placa de video escolhida, quando ja existe (a etapa 4 pode ter sido pulada). */
    public static Optional<PlacaVideo> placaVideo(Montagem montagem) {
        return peca(montagem, CategoriaComponente.PLACA_VIDEO, PlacaVideo.class);
    }

    /** Armazenamento escolhido, quando ja existe. */
    public static Optional<Armazenamento> armazenamento(Montagem montagem) {
        return peca(montagem, CategoriaComponente.ARMAZENAMENTO, Armazenamento.class);
    }

    /** Fonte escolhida, quando ja existe. */
    public static Optional<Fonte> fonte(Montagem montagem) {
        return peca(montagem, CategoriaComponente.FONTE, Fonte.class);
    }

    private static <T extends Componente> Optional<T> peca(Montagem montagem,
            CategoriaComponente categoria, Class<T> tipo) {
        Objects.requireNonNull(montagem, "montagem");
        return montagem.obter(categoria).filter(tipo::isInstance).map(tipo::cast);
    }
}
