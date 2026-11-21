package pcbuilder.analise.limitacao;

import java.util.List;
import java.util.Optional;

import pcbuilder.analise.ComponentesDaMontagem;
import pcbuilder.config.ParametrosCalculo;
import pcbuilder.enums.CategoriaComponente;
import pcbuilder.enums.SeveridadeLimitacao;
import pcbuilder.modelo.Componente;
import pcbuilder.modelo.Limitacao;
import pcbuilder.modelo.Montagem;
import pcbuilder.modelo.PlacaVideo;

/**
 * Regra <strong>L07</strong> - VRAM da GPU x resolucao-alvo.
 *
 * <p>A resolucao vem de {@code gpu.resolucaoAlvo} (o fluxo nao coleta perfil de uso). VRAM
 * abaixo do minimo da resolucao nao impede a escolha: a placa roda, mas com texturas
 * reduzidas e engasgos.</p>
 */
public final class LimitacaoVram implements RegraLimitacao {

    @Override
    public String id() {
        return "L07";
    }

    @Override
    public List<Limitacao> avaliar(Montagem montagem, Componente candidato) {
        if (candidato.getCategoria() != CategoriaComponente.PLACA_VIDEO) {
            return List.of();
        }
        return ComponentesDaMontagem.placaVideo(montagem)
                .flatMap(LimitacaoVram::avaliarGpu)
                .map(List::of)
                .orElseGet(List::of);
    }

    private static Optional<Limitacao> avaliarGpu(PlacaVideo gpu) {
        int minimo = ParametrosCalculo.vramMinimaGB(gpu.getResolucaoAlvo());
        if (minimo <= 0 || gpu.getMemoriaVRAMGB() >= minimo) {
            return Optional.empty();
        }
        return Optional.of(Limitacao.builder()
                .regra("L07")
                .limitante(gpu)
                .afetado(gpu)
                .campo("memoriaVRAMGB")
                .valorDoNovo(gpu.getMemoriaVRAMGB() + " GB")
                .valorSuportado(minimo + " GB para "
                        + gpu.getResolucaoAlvo().getRotulo())
                .consequencia(TextosLimitacao.vram(gpu, minimo))
                .severidade(SeveridadeLimitacao.AVISO)
                .percentualPerda((minimo - gpu.getMemoriaVRAMGB()) * 100.0 / minimo)
                .construir());
    }
}
