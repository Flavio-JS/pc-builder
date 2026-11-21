package pcbuilder.analise.filtro;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import pcbuilder.analise.ComponentesDaMontagem;
import pcbuilder.enums.CategoriaComponente;
import pcbuilder.enums.TipoMemoria;
import pcbuilder.modelo.Componente;
import pcbuilder.modelo.Memoria;
import pcbuilder.modelo.Montagem;
import pcbuilder.modelo.PlacaMae;
import pcbuilder.modelo.Processador;

/**
 * Regra <strong>C02</strong> - tecnologia de memoria (DDR4 x DDR5).
 *
 * <p>A memoria precisa ser do tipo que a placa-mae aceita <strong>e</strong> que o
 * processador suporta. Kits de outro tipo ficam em {@code [M] Ver ocultos} com motivo
 * {@code DDR4 x placa-mae DDR5}.</p>
 *
 * <p>O perfil EXPO/XMP nao entra aqui: memoria do tipo certo roda mesmo sem o perfil
 * (em JEDEC, mais devagar). Essa perda e um aviso de L04, tratado na HU-004.</p>
 */
public final class FiltroTipoMemoria implements RegraFiltro {

    @Override
    public CategoriaComponente categoriaAfetada() {
        return CategoriaComponente.MEMORIA;
    }

    @Override
    public Optional<String> motivoIncompatibilidade(Montagem atual, Componente candidato) {
        if (!(candidato instanceof Memoria memoria)) {
            return Optional.empty();
        }
        TipoMemoria tipo = memoria.getTipo();
        if (tipo == null) {
            return Optional.empty();
        }

        Optional<PlacaMae> placaMae = ComponentesDaMontagem.placaMae(atual);
        if (placaMae.isPresent() && placaMae.get().getTipoMemoria() != null
                && placaMae.get().getTipoMemoria() != tipo) {
            return Optional.of(tipo + " x placa-mae " + placaMae.get().getTipoMemoria());
        }

        Optional<Processador> cpu = ComponentesDaMontagem.cpu(atual);
        if (cpu.isPresent()) {
            List<TipoMemoria> suportados = cpu.get().getTiposMemoriaSuportados();
            if (!suportados.isEmpty() && !suportados.contains(tipo)) {
                return Optional.of(tipo + " x processador com suporte a " + juntar(suportados));
            }
        }
        return Optional.empty();
    }

    private static String juntar(List<TipoMemoria> tipos) {
        return tipos.stream().map(Enum::name).collect(Collectors.joining("/"));
    }
}
