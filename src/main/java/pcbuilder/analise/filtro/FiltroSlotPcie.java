package pcbuilder.analise.filtro;

import java.util.Optional;

import pcbuilder.analise.ComponentesDaMontagem;
import pcbuilder.enums.CategoriaComponente;
import pcbuilder.modelo.Componente;
import pcbuilder.modelo.Montagem;
import pcbuilder.modelo.PlacaMae;
import pcbuilder.modelo.PlacaVideo;

/**
 * Regra <strong>C05</strong> (parte dura) - placa de video x slot x16.
 *
 * <p>A GPU precisa de um slot x16 livre e de pistas suficientes. Sem slot, ou com um
 * slot menor do que a placa usa, ela e ocultada.</p>
 *
 * <p>PCIe 5.0 em slot 4.0 <strong>nao</strong> e incompativel (o padrao e compativel
 * para tras): a perda de banda e um aviso de L05 (HU-004).</p>
 */
public final class FiltroSlotPcie implements RegraFiltro {

    @Override
    public CategoriaComponente categoriaAfetada() {
        return CategoriaComponente.PLACA_VIDEO;
    }

    @Override
    public Optional<String> motivoIncompatibilidade(Montagem atual, Componente candidato) {
        if (!(candidato instanceof PlacaVideo placaVideo)) {
            return Optional.empty();
        }
        Optional<PlacaMae> placaMae = ComponentesDaMontagem.placaMae(atual);
        if (placaMae.isEmpty()) {
            return Optional.empty();
        }
        if (placaMae.get().getQuantidadeSlotsPCIe16() <= 0) {
            return Optional.of("placa-mae sem slot x16 livre");
        }
        int pistasSlot = placaMae.get().getPistasSlotPCIe16();
        if (pistasSlot > 0 && placaVideo.getPistasPCIe() > pistasSlot) {
            return Optional.of("placa de video com " + placaVideo.getPistasPCIe()
                    + " pistas x slot x" + pistasSlot);
        }
        return Optional.empty();
    }
}
