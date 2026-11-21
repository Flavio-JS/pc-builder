package pcbuilder.analise.filtro;

import java.util.Optional;

import pcbuilder.analise.RequisitosFonte;
import pcbuilder.enums.CategoriaComponente;
import pcbuilder.modelo.Componente;
import pcbuilder.modelo.Fonte;
import pcbuilder.modelo.Montagem;

/**
 * Regra <strong>C10</strong> - conectores fisicos da fonte.
 *
 * <p>A fonte precisa ter <strong>todos</strong> os conectores que a configuracao pede,
 * conforme {@link RequisitosFonte} (a mesma fonte usada no cabecalho da etapa 6):
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
        for (RequisitosFonte.Requisito requisito : RequisitosFonte.conectores(atual)) {
            if (requisito.getQuantidade()
                    > RequisitosFonte.disponivel(fonte, requisito.getTipo())) {
                return Optional.of("sem conector de "
                        + requisito.getTipo().getRotuloFiltro() + " (a "
                        + requisito.getTipo().getOrigem() + " exige "
                        + requisito.getQuantidade() + ")");
            }
        }
        return Optional.empty();
    }
}

