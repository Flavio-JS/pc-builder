package pcbuilder.analise.filtro;

import java.util.Optional;

import pcbuilder.analise.ComponentesDaMontagem;
import pcbuilder.enums.CategoriaComponente;
import pcbuilder.modelo.Componente;
import pcbuilder.modelo.Fonte;
import pcbuilder.modelo.Montagem;
import pcbuilder.modelo.PlacaMae;
import pcbuilder.modelo.PlacaVideo;

/**
 * Regra <strong>C10</strong> - conectores fisicos da fonte.
 *
 * <p>A fonte precisa ter <strong>todos</strong> os conectores que a configuracao pede:
 * os da placa de video (16, 8 e 6 pinos) e o de 8 pinos da placa-mae. Faltando um, a
 * fonte e ocultada mesmo que tenha potencia sobrando - <strong>nao existe caminho de
 * adaptador</strong> no fluxo, porque adaptador e risco para o usuario leigo.</p>
 *
 * <p>Quando a etapa 4 foi pulada (CPU com video integrado) nao ha exigencia PCIe: so o
 * conector da placa-mae e comparado.</p>
 */
public final class FiltroConectoresFonte implements RegraFiltro {

    @Override
    public CategoriaComponente categoriaAfetada() {
        return CategoriaComponente.FONTE;
    }

    @Override
    public Optional<String> motivoIncompatibilidade(Montagem atual, Componente candidato) {
        if (!(candidato instanceof Fonte fonte)) {
            return Optional.empty();
        }

        Optional<PlacaVideo> placaVideo = ComponentesDaMontagem.placaVideo(atual);
        if (placaVideo.isPresent()) {
            PlacaVideo gpu = placaVideo.get();
            if (gpu.getQuantidadeConectores16pin() > fonte.getQuantidadeConectores16pin12VHPWR()) {
                return Optional.of("sem conector de 16 pinos (a placa de video exige "
                        + gpu.getQuantidadeConectores16pin() + ")");
            }
            if (gpu.getQuantidadeConectores8pin() > fonte.getQuantidadeConectoresPCIe8pin()) {
                return Optional.of("sem conector de 8 pinos (a placa de video exige "
                        + gpu.getQuantidadeConectores8pin() + ")");
            }
            if (gpu.getQuantidadeConectores6pin() > fonte.getQuantidadeConectoresPCIe6pin()) {
                return Optional.of("sem conector de 6 pinos (a placa de video exige "
                        + gpu.getQuantidadeConectores6pin() + ")");
            }
        }

        Optional<PlacaMae> placaMae = ComponentesDaMontagem.placaMae(atual);
        if (placaMae.isPresent() && placaMae.get().getQuantidadeConectoresCPU8pin()
                > fonte.getQuantidadeConectoresCPU8pin()) {
            return Optional.of("sem conector de 8 pinos para a CPU (a placa-mae exige "
                    + placaMae.get().getQuantidadeConectoresCPU8pin() + ")");
        }
        return Optional.empty();
    }
}
