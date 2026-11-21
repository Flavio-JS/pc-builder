package pcbuilder.analise.filtro;

import java.util.Optional;

import pcbuilder.analise.ComponentesDaMontagem;
import pcbuilder.enums.CategoriaComponente;
import pcbuilder.enums.Soquete;
import pcbuilder.modelo.Componente;
import pcbuilder.modelo.Montagem;
import pcbuilder.modelo.PlacaMae;
import pcbuilder.modelo.Processador;

/**
 * Regra <strong>C01</strong> - soquete da CPU x soquete da placa-mae.
 *
 * <p>O processador so encaixa em placas com o mesmo soquete fisico. Placas de outro
 * soquete ficam em {@code [M] Ver ocultos} com o motivo
 * {@code soquete AM5 x processador LGA1700}.</p>
 *
 * <p>A regra so olha o encaixe: frequencia, VRM e BIOS sao avisos da HU-004 e nunca
 * ocultam uma placa.</p>
 */
public final class FiltroSoquete implements RegraFiltro {

    @Override
    public CategoriaComponente categoriaAfetada() {
        return CategoriaComponente.PLACA_MAE;
    }

    @Override
    public Optional<String> motivoIncompatibilidade(Montagem atual, Componente candidato) {
        if (!(candidato instanceof PlacaMae placaMae)) {
            return Optional.empty();
        }
        Optional<Processador> cpu = ComponentesDaMontagem.cpu(atual);
        if (cpu.isEmpty()) {
            return Optional.empty();
        }
        Soquete soqueteCpu = cpu.get().getSoquete();
        Soquete soquetePlaca = placaMae.getSoqueteCPU();
        if (soqueteCpu == null || soquetePlaca == null || soqueteCpu == soquetePlaca) {
            return Optional.empty();
        }
        return Optional.of("soquete " + soquetePlaca + " x processador " + soqueteCpu);
    }
}
