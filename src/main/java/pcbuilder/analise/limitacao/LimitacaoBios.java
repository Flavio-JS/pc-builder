package pcbuilder.analise.limitacao;

import java.util.List;
import java.util.Optional;

import pcbuilder.analise.ComponentesDaMontagem;
import pcbuilder.analise.RequisitoBIOS;
import pcbuilder.enums.CategoriaComponente;
import pcbuilder.enums.SeveridadeLimitacao;
import pcbuilder.modelo.Componente;
import pcbuilder.modelo.Limitacao;
import pcbuilder.modelo.Montagem;
import pcbuilder.modelo.PlacaMae;
import pcbuilder.modelo.Processador;

/**
 * Alerta <strong>C09</strong> - BIOS minima para a CPU.
 *
 * <p>A placa encaixa no soquete, mas pode ter saido de fabrica com uma BIOS anterior ao
 * processador e precisar de atualizacao. No fluxo guiado isso e <strong>somente
 * aviso</strong> (HU-005): a placa nunca e ocultada, e o texto lembra se ela atualiza a
 * BIOS sem uma CPU compativel (USB BIOS Flashback).</p>
 *
 * <p>O requisito vem de {@link RequisitoBIOS}, uma heuristica por ano de lancamento -
 * ver o Javadoc daquela classe para a limitacao da base.</p>
 */
public final class LimitacaoBios implements RegraLimitacao {

    @Override
    public String id() {
        return "C09";
    }

    @Override
    public List<Limitacao> avaliar(Montagem montagem, Componente candidato) {
        if (!regraSeAplica(candidato)) {
            return List.of();
        }
        Optional<Processador> cpu = ComponentesDaMontagem.cpu(montagem);
        Optional<PlacaMae> placaMae = ComponentesDaMontagem.placaMae(montagem);
        if (cpu.isEmpty() || placaMae.isEmpty()
                || !RequisitoBIOS.podeExigirAtualizacao(cpu.get(), placaMae.get())) {
            return List.of();
        }
        int anoCpu = cpu.get().getAnoLancamento();
        int anoChipset = RequisitoBIOS.anoDoChipset(placaMae.get().getChipset());
        String chipset = placaMae.get().getChipset() == null
                ? "--"
                : placaMae.get().getChipset().name();

        return List.of(Limitacao.builder()
                .regra(id())
                .limitante(placaMae.get())
                .afetado(cpu.get())
                .campo("requisitoBIOS")
                .valorDoNovo("CPU de " + anoCpu)
                .valorSuportado("chipset " + chipset + " (" + anoChipset + ")")
                .consequencia(TextosLimitacao.bios(placaMae.get(), cpu.get()))
                .severidade(SeveridadeLimitacao.AVISO)
                .construir());
    }

    @Override
    public List<String> dadosAusentes(Montagem montagem, Componente candidato) {
        if (!regraSeAplica(candidato)) {
            return List.of();
        }
        Optional<Processador> cpu = ComponentesDaMontagem.cpu(montagem);
        Optional<PlacaMae> placaMae = ComponentesDaMontagem.placaMae(montagem);
        if (cpu.isPresent() && cpu.get().getAnoLancamento() == null) {
            return List.of("C09: ano de lancamento do processador nao cadastrado");
        }
        if (placaMae.isPresent() && RequisitoBIOS.anoDoChipset(placaMae.get().getChipset()) <= 0) {
            return List.of("C09: geracao do chipset nao mapeada para estimar a BIOS minima");
        }
        return List.of();
    }

    private static boolean regraSeAplica(Componente candidato) {
        CategoriaComponente categoria = candidato.getCategoria();
        return categoria == CategoriaComponente.CPU
                || categoria == CategoriaComponente.PLACA_MAE;
    }
}
