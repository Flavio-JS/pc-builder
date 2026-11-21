package pcbuilder.analise;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import pcbuilder.config.ParametrosCalculo;
import pcbuilder.enums.CategoriaComponente;
import pcbuilder.modelo.Componente;
import pcbuilder.modelo.Montagem;
import pcbuilder.modelo.PlacaVideo;
import pcbuilder.modelo.Processador;

/**
 * Soma o consumo do sistema escolhido ate aqui (regra L06, usada tambem por C14).
 *
 * <p>CPU e placa de video usam o consumo de pico real ({@code consumoMaximoWatts} e
 * {@code tdpWatts}); as demais pecas usam {@code consumoEstimadoWatts}. Quando uma
 * peca auxiliar ainda nao foi escolhida (ou nao tem o dado), entra uma unica vez a
 * reserva {@link ParametrosCalculo#CONSUMO_BASE_SISTEMA_WATTS}, que cobre fans e
 * perifericos que o fluxo nao coleta.</p>
 *
 * <p>O consumo <strong>nao</strong> e gravado em nenhum componente: e sempre calculado
 * a partir das pecas da montagem (secao 16 do modelo de dados).</p>
 */
public final class CalculadoraConsumo {

    /** Pecas cujo consumo vem de {@code consumoEstimadoWatts} (campo comum). */
    private static final List<CategoriaComponente> AUXILIARES = List.of(
            CategoriaComponente.PLACA_MAE,
            CategoriaComponente.MEMORIA,
            CategoriaComponente.ARMAZENAMENTO);

    private CalculadoraConsumo() {
        // classe utilitaria: nao deve ser instanciada
    }

    /**
     * Consumo total maximo do sistema montado ate aqui.
     *
     * @param montagem pecas ja escolhidas (pode estar parcial)
     * @return consumo em watts, arredondado para cima
     */
    public static int consumoTotalSistemaWatts(Montagem montagem) {
        Objects.requireNonNull(montagem, "montagem");

        double total = ComponentesDaMontagem.cpu(montagem)
                .map(Processador::getConsumoMaximoWatts)
                .orElse(0);
        total += ComponentesDaMontagem.placaVideo(montagem)
                .map(PlacaVideo::getTdpWatts)
                .orElse(0);

        boolean reservaNecessaria = false;
        for (CategoriaComponente categoria : AUXILIARES) {
            Optional<Integer> consumo = consumoEstimado(montagem, categoria);
            if (consumo.isPresent()) {
                total += consumo.get();
            } else {
                reservaNecessaria = true;
            }
        }
        if (reservaNecessaria) {
            total += ParametrosCalculo.CONSUMO_BASE_SISTEMA_WATTS;
        }
        return (int) Math.ceil(total);
    }

    /**
     * Consumo com a margem de seguranca da fonte (regra C14).
     *
     * @param montagem pecas ja escolhidas
     * @return {@code consumoTotalSistemaWatts} x {@code 1,40}, arredondado para cima
     */
    public static int consumoComMargemWatts(Montagem montagem) {
        return (int) Math.ceil(consumoTotalSistemaWatts(montagem)
                * ParametrosCalculo.MARGEM_SEGURANCA_FONTE);
    }

    private static Optional<Integer> consumoEstimado(Montagem montagem,
            CategoriaComponente categoria) {
        return montagem.obter(categoria)
                .map(Componente::getConsumoEstimadoWatts);
    }
}
