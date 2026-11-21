package pcbuilder.analise.filtro;

import java.util.Optional;

import pcbuilder.analise.ComponentesDaMontagem;
import pcbuilder.enums.CategoriaComponente;
import pcbuilder.modelo.Componente;
import pcbuilder.modelo.Memoria;
import pcbuilder.modelo.Montagem;
import pcbuilder.modelo.PlacaMae;
import pcbuilder.modelo.Processador;

/**
 * Regra <strong>C12</strong> - memoria ECC precisa de suporte da placa e da CPU.
 *
 * <p>Um modulo ECC so entra na lista quando a placa-mae <strong>e</strong> o
 * processador declaram suporte a ECC; caso contrario ele e ocultado, porque a memoria
 * nao funciona no modo de correcao de erros (ou simplesmente nao inicializa).</p>
 *
 * <p>Memoria comum (sem ECC) nunca e ocultada por esta regra.</p>
 */
public final class FiltroMemoriaECC implements RegraFiltro {

    @Override
    public CategoriaComponente categoriaAfetada() {
        return CategoriaComponente.MEMORIA;
    }

    @Override
    public Optional<String> motivoIncompatibilidade(Montagem atual, Componente candidato) {
        if (!(candidato instanceof Memoria memoria) || !memoria.isPossuiECC()) {
            return Optional.empty();
        }
        Optional<PlacaMae> placaMae = ComponentesDaMontagem.placaMae(atual);
        if (placaMae.isPresent() && !placaMae.get().isSuportaECC()) {
            return Optional.of("memoria ECC x placa-mae sem suporte a ECC");
        }
        Optional<Processador> cpu = ComponentesDaMontagem.cpu(atual);
        if (cpu.isPresent() && !cpu.get().isSuportaECC()) {
            return Optional.of("memoria ECC x processador sem suporte a ECC");
        }
        return Optional.empty();
    }
}
