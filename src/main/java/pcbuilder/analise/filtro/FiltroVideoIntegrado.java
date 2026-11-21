package pcbuilder.analise.filtro;

import java.util.Optional;

import pcbuilder.config.OrdemEtapas;
import pcbuilder.enums.CategoriaComponente;
import pcbuilder.modelo.Componente;
import pcbuilder.modelo.Montagem;

/**
 * Regra <strong>C06</strong> - processador sem video integrado exige placa de video.
 *
 * <p>A regra <strong>nao oculta nenhum modelo de GPU</strong>: qualquer placa de video
 * encaixa no slot x16 e a regra nao compara modelos. O papel dela e decidir se a etapa
 * 4 e obrigatoria ({@link #exigePlacaVideo(Montagem)}) - a decisao de tela esta na
 * HU-007, quando a etapa 4 passa a existir.</p>
 *
 * <p>Ela participa do filtro mesmo assim porque o modelo de dados a classifica como
 * regra de compatibilidade dura, e a HU-011 revalida a montagem inteira: uma montagem
 * sem GPU em CPU sem iGPU e invalida, mesmo que nao haja item a esconder.</p>
 */
public final class FiltroVideoIntegrado implements RegraFiltro {

    @Override
    public CategoriaComponente categoriaAfetada() {
        return CategoriaComponente.PLACA_VIDEO;
    }

    @Override
    public Optional<String> motivoIncompatibilidade(Montagem atual, Componente candidato) {
        // Toda placa de video encaixa; o que a regra decide e a obrigatoriedade da etapa.
        return Optional.empty();
    }

    /**
     * O processador escolhido obriga a ter placa de video?
     *
     * @param atual montagem em andamento
     * @return {@code true} quando ha CPU na montagem e ela nao tem video integrado
     */
    public static boolean exigePlacaVideo(Montagem atual) {
        return OrdemEtapas.exigePlacaVideo(atual);
    }
}
