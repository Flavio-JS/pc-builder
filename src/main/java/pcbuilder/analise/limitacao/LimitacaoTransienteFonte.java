package pcbuilder.analise.limitacao;

import java.util.List;
import java.util.Optional;

import pcbuilder.analise.CalculadoraFonte;
import pcbuilder.analise.ComponentesDaMontagem;
import pcbuilder.enums.CategoriaComponente;
import pcbuilder.enums.SeveridadeLimitacao;
import pcbuilder.modelo.Componente;
import pcbuilder.modelo.Fonte;
import pcbuilder.modelo.Limitacao;
import pcbuilder.modelo.Montagem;
import pcbuilder.modelo.PlacaVideo;

/**
 * Regra <strong>L06</strong> (parte de aviso) - pico transiente da GPU x trilho de 12 V.
 *
 * <p>A parte dura de L06 (fonte menor que o consumo com margem) e o filtro C14, que
 * oculta a fonte. Aqui entra so o {@code RISCO}: picos de milissegundos da GPU acima do
 * que o trilho de 12 V entrega fazem a maquina desligar sob carga - a fonte continua
 * selecionavel, mas com aviso.</p>
 *
 * <p>GPU sem {@code picoTransienteWatts} cadastrado <strong>nao</strong> gera aviso
 * falso: o dado ausente so aparece em {@code --detalhar-limitacoes}.</p>
 */
public final class LimitacaoTransienteFonte implements RegraLimitacao {

    @Override
    public String id() {
        return "L06";
    }

    @Override
    public List<Limitacao> avaliar(Montagem montagem, Componente candidato) {
        if (!regraSeAplica(candidato)) {
            return List.of();
        }
        Optional<Fonte> fonte = ComponentesDaMontagem.fonte(montagem);
        Optional<PlacaVideo> gpu = ComponentesDaMontagem.placaVideo(montagem);
        if (fonte.isEmpty() || gpu.isEmpty() || gpu.get().getPicoTransienteWatts() == null) {
            return List.of();
        }
        int pico = gpu.get().getPicoTransienteWatts();
        int trilho = fonte.get().getPotenciaTrilho12VWatts();
        if (!CalculadoraFonte.picoAcimaDoTrilho(gpu.get(), fonte.get())) {
            return List.of();
        }

        return List.of(Limitacao.builder()
                .regra(id())
                .limitante(fonte.get())
                .afetado(gpu.get())
                .campo("picoTransienteWatts")
                .valorDoNovo(pico + " W")
                .valorSuportado(trilho + " W")
                .consequencia(TextosLimitacao.transienteFonte(fonte.get(), gpu.get()))
                .severidade(SeveridadeLimitacao.RISCO)
                .construir());
    }

    @Override
    public List<String> dadosAusentes(Montagem montagem, Componente candidato) {
        if (!regraSeAplica(candidato)) {
            return List.of();
        }
        Optional<PlacaVideo> gpu = ComponentesDaMontagem.placaVideo(montagem);
        if (ComponentesDaMontagem.fonte(montagem).isPresent() && gpu.isPresent()
                && gpu.get().getPicoTransienteWatts() == null) {
            return List.of("L06: pico transiente da placa de video nao cadastrado");
        }
        return List.of();
    }

    private static boolean regraSeAplica(Componente candidato) {
        CategoriaComponente categoria = candidato.getCategoria();
        return categoria == CategoriaComponente.FONTE
                || categoria == CategoriaComponente.PLACA_VIDEO;
    }
}
