package pcbuilder.analise.filtro;

import java.util.Optional;

import pcbuilder.analise.CalculadoraConsumo;
import pcbuilder.config.ParametrosCalculo;
import pcbuilder.enums.CategoriaComponente;
import pcbuilder.modelo.Componente;
import pcbuilder.modelo.Fonte;
import pcbuilder.modelo.Montagem;

/**
 * Regra <strong>C14</strong> - potencia da fonte para o conjunto.
 *
 * <p>A fonte e ocultada quando a potencia total nao cobre o consumo do sistema com a
 * margem de seguranca de {@link ParametrosCalculo#MARGEM_SEGURANCA_FONTE} (40%). O
 * calculo vem de {@link CalculadoraConsumo}, o mesmo usado no cabecalho da etapa 6.</p>
 *
 * <p>O pico transiente da GPU contra o trilho de 12 V nao oculta nada: e um
 * {@code RISCO} em forma de aviso (L06, HU-004).</p>
 */
public final class FiltroFonteSuficiente implements RegraFiltro {

    @Override
    public CategoriaComponente categoriaAfetada() {
        return CategoriaComponente.FONTE;
    }

    @Override
    public Optional<String> motivoIncompatibilidade(Montagem atual, Componente candidato) {
        if (!(candidato instanceof Fonte fonte)) {
            return Optional.empty();
        }
        int necessario = CalculadoraConsumo.consumoComMargemWatts(atual);
        if (necessario <= fonte.getPotenciaTotalWatts()) {
            return Optional.empty();
        }
        return Optional.of("fonte de " + fonte.getPotenciaTotalWatts()
                + " W x configuracao que pede " + necessario + " W (com margem de "
                + ParametrosCalculo.margemSegurancaPercentual() + "%)");
    }
}
