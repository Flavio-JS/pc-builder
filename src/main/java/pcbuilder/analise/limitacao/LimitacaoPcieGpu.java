package pcbuilder.analise.limitacao;

import java.util.List;
import java.util.Optional;

import pcbuilder.analise.CalculadoraPcie;
import pcbuilder.analise.ComponentesDaMontagem;
import pcbuilder.enums.CategoriaComponente;
import pcbuilder.enums.SeveridadeLimitacao;
import pcbuilder.modelo.Componente;
import pcbuilder.modelo.Limitacao;
import pcbuilder.modelo.Montagem;
import pcbuilder.modelo.PlacaMae;
import pcbuilder.modelo.PlacaVideo;

/**
 * Regra <strong>L05</strong> - placa de video em slot x16 de geracao menor.
 *
 * <p>PCIe e compativel para tras: a GPU de PCIe 5.0 funciona em slot 4.0, so com menos
 * banda. Por isso a placa <strong>nao</strong> e ocultada (quem oculta slot sem pistas
 * suficientes e o filtro C05) - o que se perde vira aviso.</p>
 */
public final class LimitacaoPcieGpu implements RegraLimitacao {

    @Override
    public String id() {
        return "L05";
    }

    @Override
    public List<Limitacao> avaliar(Montagem montagem, Componente candidato) {
        CategoriaComponente categoria = candidato.getCategoria();
        if (categoria != CategoriaComponente.PLACA_VIDEO
                && categoria != CategoriaComponente.PLACA_MAE) {
            return List.of();
        }
        Optional<PlacaVideo> gpu = ComponentesDaMontagem.placaVideo(montagem);
        Optional<PlacaMae> placaMae = ComponentesDaMontagem.placaMae(montagem);
        if (gpu.isEmpty() || placaMae.isEmpty()) {
            return List.of();
        }

        int versaoSlot = placaMae.get().getVersaoSlotPCIe16();
        if (versaoSlot <= 0 || gpu.get().getVersaoPCIe() <= versaoSlot) {
            return List.of();
        }
        double bandaNominal = CalculadoraPcie.bandaGBs(gpu.get().getVersaoPCIe(),
                gpu.get().getPistasPCIe());
        double bandaEfetiva = CalculadoraPcie.bandaEfetivaGBs(gpu.get().getVersaoPCIe(),
                gpu.get().getPistasPCIe(), versaoSlot, placaMae.get().getPistasSlotPCIe16());
        if (bandaNominal <= 0) {
            return List.of();
        }
        double perda = CalculadoraPcie.perdaPercentual(bandaNominal, bandaEfetiva);

        return List.of(Limitacao.builder()
                .regra(id())
                .limitante(placaMae.get())
                .afetado(gpu.get())
                .campo("versaoPCIe")
                .valorDoNovo(CalculadoraPcie.descricao(gpu.get().getVersaoPCIe(),
                        gpu.get().getPistasPCIe()))
                .valorSuportado(CalculadoraPcie.descricao(versaoSlot,
                        CalculadoraPcie.pistasEfetivas(gpu.get().getPistasPCIe(),
                                placaMae.get().getPistasSlotPCIe16())))
                .consequencia(TextosLimitacao.pcieGpu(placaMae.get(), gpu.get(), perda))
                .severidade(SeveridadeLimitacao.AVISO)
                .percentualPerda(perda)
                .construir());
    }
}
