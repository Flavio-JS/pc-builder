package pcbuilder.analise.limitacao;

import java.util.List;
import java.util.Optional;

import pcbuilder.analise.ComponentesDaMontagem;
import pcbuilder.analise.RegraGargaloCpuGpu;
import pcbuilder.enums.CategoriaComponente;
import pcbuilder.enums.SeveridadeLimitacao;
import pcbuilder.modelo.Componente;
import pcbuilder.modelo.Limitacao;
import pcbuilder.modelo.Montagem;
import pcbuilder.modelo.PlacaVideo;
import pcbuilder.modelo.Processador;

/**
 * Regra <strong>L01</strong> - gargalo de CPU em relacao a placa de video.
 *
 * <p>Quando a CPU e mais fraca que a GPU alem da tolerancia da resolucao-alvo, a placa de
 * video nao e usada por completo. E o cenario inverso do saudavel (CPU sobrando) e nao
 * bloqueia nada - apenas avisa que o dinheiro da GPU nao sera todo aproveitado.</p>
 *
 * <p>E a ultima regra da ordem de avaliacao porque depende do conjunto ja validado.</p>
 */
public final class LimitacaoGargaloCpuGpu implements RegraLimitacao {

    @Override
    public String id() {
        return "L01";
    }

    @Override
    public List<Limitacao> avaliar(Montagem montagem, Componente candidato) {
        CategoriaComponente categoria = candidato.getCategoria();
        if (categoria != CategoriaComponente.CPU
                && categoria != CategoriaComponente.PLACA_VIDEO) {
            return List.of();
        }
        Optional<Processador> cpu = ComponentesDaMontagem.cpu(montagem);
        Optional<PlacaVideo> gpu = ComponentesDaMontagem.placaVideo(montagem);
        if (cpu.isEmpty() || gpu.isEmpty()
                || !RegraGargaloCpuGpu.haGargaloDeCpu(cpu.get(), gpu.get())) {
            return List.of();
        }
        double desequilibrio = RegraGargaloCpuGpu.desequilibrioPercentual(cpu.get(), gpu.get());

        return List.of(Limitacao.builder()
                .regra(id())
                .limitante(cpu.get())
                .afetado(gpu.get())
                .campo("indiceDesempenho")
                .valorDoNovo(gpu.get().getIndiceDesempenhoGPU() + " pontos")
                .valorSuportado(cpu.get().getIndiceDesempenhoSingleThread() + " pontos")
                .consequencia(TextosLimitacao.gargaloCpuGpu(cpu.get(), gpu.get()))
                .severidade(SeveridadeLimitacao.AVISO)
                .percentualPerda(Math.abs(desequilibrio))
                .construir());
    }
}
