package pcbuilder.analise.limitacao;

import java.util.List;
import java.util.Optional;

import pcbuilder.analise.CalculadoraVrm;
import pcbuilder.analise.ComponentesDaMontagem;
import pcbuilder.enums.CategoriaComponente;
import pcbuilder.enums.SeveridadeLimitacao;
import pcbuilder.modelo.Componente;
import pcbuilder.modelo.Limitacao;
import pcbuilder.modelo.Montagem;
import pcbuilder.modelo.PlacaMae;
import pcbuilder.modelo.Processador;

/**
 * Regra <strong>L02</strong> - VRM da placa-mae x processador.
 *
 * <p>A regra vale <strong>nos dois sentidos</strong>: escolher a CPU forte e depois uma
 * placa de VRM apertado gera o mesmo aviso de escolher a placa primeiro. Por isso ela
 * roda quando o candidato e a CPU <em>ou</em> a placa-mae.</p>
 */
public final class LimitacaoVrm implements RegraLimitacao {

    @Override
    public String id() {
        return "L02";
    }

    @Override
    public List<Limitacao> avaliar(Montagem montagem, Componente candidato) {
        CategoriaComponente categoria = candidato.getCategoria();
        if (categoria != CategoriaComponente.CPU && categoria != CategoriaComponente.PLACA_MAE) {
            return List.of();
        }
        Optional<Processador> cpu = ComponentesDaMontagem.cpu(montagem);
        Optional<PlacaMae> placaMae = ComponentesDaMontagem.placaMae(montagem);
        if (cpu.isEmpty() || placaMae.isEmpty()) {
            return List.of();
        }

        double uso = CalculadoraVrm.percentualUsoVRM(cpu.get(), placaMae.get());
        int limite = CalculadoraVrm.limiteAlertaPercentual(placaMae.get());
        if (uso <= limite) {
            return List.of();
        }

        return List.of(Limitacao.builder()
                .regra(id())
                .limitante(placaMae.get())
                .afetado(cpu.get())
                .campo("percentualUsoVRM")
                .valorDoNovo(TextosLimitacao.percentual(uso))
                .valorSuportado(limite + "%")
                .consequencia(TextosLimitacao.vrm(placaMae.get(), cpu.get(), uso, limite))
                .severidade(SeveridadeLimitacao.RISCO)
                .percentualPerda(uso - limite)
                .construir());
    }
}
