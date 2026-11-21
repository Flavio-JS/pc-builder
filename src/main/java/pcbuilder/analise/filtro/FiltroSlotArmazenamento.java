package pcbuilder.analise.filtro;

import java.util.Optional;

import pcbuilder.analise.ComponentesDaMontagem;
import pcbuilder.enums.CategoriaComponente;
import pcbuilder.enums.InterfaceArmazenamento;
import pcbuilder.modelo.Armazenamento;
import pcbuilder.modelo.Componente;
import pcbuilder.modelo.Montagem;
import pcbuilder.modelo.PlacaMae;

/**
 * Regra <strong>C04</strong> - armazenamento x slots da placa-mae.
 *
 * <p>SSD NVMe precisa de pelo menos um slot M.2; SSD/HD SATA precisa de pelo menos uma
 * porta SATA. Quem nao tem o encaixe fica em {@code [M] Ver ocultos}.</p>
 *
 * <p>A montagem guarda no maximo um armazenamento por vez e o candidato substitui o
 * anterior, por isso a conta olha a capacidade total da placa e nao desconta a peca
 * ja escolhida.</p>
 */
public final class FiltroSlotArmazenamento implements RegraFiltro {

    @Override
    public CategoriaComponente categoriaAfetada() {
        return CategoriaComponente.ARMAZENAMENTO;
    }

    @Override
    public Optional<String> motivoIncompatibilidade(Montagem atual, Componente candidato) {
        if (!(candidato instanceof Armazenamento armazenamento)) {
            return Optional.empty();
        }
        InterfaceArmazenamento conexao = armazenamento.getInterfaceArmazenamento();
        Optional<PlacaMae> placaMae = ComponentesDaMontagem.placaMae(atual);
        if (conexao == null || placaMae.isEmpty()) {
            return Optional.empty();
        }

        if (conexao == InterfaceArmazenamento.NVME
                && placaMae.get().getQuantidadeSlotsM2() <= 0) {
            return Optional.of("placa-mae sem slot M.2 livre (o SSD NVMe precisa de um)");
        }
        if (conexao == InterfaceArmazenamento.SATA
                && placaMae.get().getQuantidadePortasSATA() <= 0) {
            return Optional.of("placa-mae sem porta SATA livre (o SSD/HD SATA precisa de uma)");
        }
        return Optional.empty();
    }
}
