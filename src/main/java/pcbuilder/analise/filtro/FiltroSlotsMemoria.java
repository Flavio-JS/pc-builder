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
 * Regra <strong>C03</strong> (parte dura) - quantidade de pentes e capacidade total.
 *
 * <p>O kit e ocultado quando ocupa mais pentes do que a placa tem, ou quando a soma
 * dos pentes passa do limite de memoria da placa-mae <strong>ou</strong> do
 * processador (vale o menor dos dois).</p>
 *
 * <p>A parte "soft" da regra - kit de 1 pente em placa dual channel - nao oculta nada:
 * e um aviso de single channel (HU-004).</p>
 */
public final class FiltroSlotsMemoria implements RegraFiltro {

    @Override
    public CategoriaComponente categoriaAfetada() {
        return CategoriaComponente.MEMORIA;
    }

    @Override
    public Optional<String> motivoIncompatibilidade(Montagem atual, Componente candidato) {
        if (!(candidato instanceof Memoria memoria)) {
            return Optional.empty();
        }
        int pentes = memoria.getQuantidadePentes();
        int totalGB = memoria.getCapacidadePorPenteGB() * pentes;

        Optional<PlacaMae> placaMae = ComponentesDaMontagem.placaMae(atual);
        if (placaMae.isPresent()) {
            int slots = placaMae.get().getQuantidadeSlotsMemoria();
            if (slots > 0 && pentes > slots) {
                return Optional.of(pentes + " pentes x placa-mae com " + slots
                        + " slot(s) de memoria");
            }
            int limitePlaca = placaMae.get().getCapacidadeMaximaMemoriaGB();
            if (limitePlaca > 0 && totalGB > limitePlaca) {
                return Optional.of(totalGB + " GB x limite de " + limitePlaca
                        + " GB (placa-mae)");
            }
        }

        Optional<Processador> cpu = ComponentesDaMontagem.cpu(atual);
        if (cpu.isPresent()) {
            int limiteCpu = cpu.get().getCapacidadeMaximaMemoriaGB();
            if (limiteCpu > 0 && totalGB > limiteCpu) {
                return Optional.of(totalGB + " GB x limite de " + limiteCpu
                        + " GB (processador)");
            }
        }
        return Optional.empty();
    }
}
