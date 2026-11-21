package pcbuilder.analise.limitacao;

import java.util.List;
import java.util.Optional;

import pcbuilder.analise.ComponentesDaMontagem;
import pcbuilder.enums.CategoriaComponente;
import pcbuilder.enums.SeveridadeLimitacao;
import pcbuilder.modelo.Componente;
import pcbuilder.modelo.Limitacao;
import pcbuilder.modelo.Montagem;
import pcbuilder.modelo.Processador;

/**
 * Regra <strong>L11</strong> - pistas PCIe da CPU x GPU + armazenamento NVMe.
 *
 * <p>Quando a soma das pistas das pecas passa do que a CPU oferece, os slots restantes
 * dividem pistas do chipset: o segundo M.2 pode desativar uma porta SATA ou reduzir o
 * slot x16 para x8. O conjunto continua funcionando - e aviso, nunca bloqueio.</p>
 */
public final class LimitacaoPistasPcie implements RegraLimitacao {

    @Override
    public String id() {
        return "L11";
    }

    @Override
    public List<Limitacao> avaliar(Montagem montagem, Componente candidato) {
        CategoriaComponente categoria = candidato.getCategoria();
        if (categoria != CategoriaComponente.CPU
                && categoria != CategoriaComponente.PLACA_VIDEO
                && categoria != CategoriaComponente.ARMAZENAMENTO) {
            return List.of();
        }
        Optional<Processador> cpu = ComponentesDaMontagem.cpu(montagem);
        if (cpu.isEmpty() || cpu.get().getPistasPCIeLivres() <= 0) {
            return List.of();
        }
        int necessarias = pistasNecessarias(montagem);
        if (necessarias <= 0 || necessarias <= cpu.get().getPistasPCIeLivres()) {
            return List.of();
        }
        Componente afetado = montagem.obter(CategoriaComponente.PLACA_VIDEO)
                .orElseGet(() -> montagem.obter(CategoriaComponente.ARMAZENAMENTO)
                        .orElse(cpu.get()));

        return List.of(Limitacao.builder()
                .regra(id())
                .limitante(cpu.get())
                .afetado(afetado)
                .campo("pistasPCIe")
                .valorDoNovo(necessarias + " pistas")
                .valorSuportado(cpu.get().getPistasPCIeLivres() + " pistas")
                .consequencia(TextosLimitacao.pistasPcie(cpu.get(), necessarias,
                        cpu.get().getPistasPCIeLivres()))
                .severidade(SeveridadeLimitacao.AVISO)
                .construir());
    }

    private static int pistasNecessarias(Montagem montagem) {
        int total = 0;
        if (ComponentesDaMontagem.placaVideo(montagem).isPresent()) {
            total += ComponentesDaMontagem.placaVideo(montagem).get().getPistasPCIe();
        }
        if (ComponentesDaMontagem.armazenamento(montagem).isPresent()) {
            Integer pistas = ComponentesDaMontagem.armazenamento(montagem).get().getPistasPCIe();
            total += pistas == null ? 0 : pistas;
        }
        return total;
    }
}
