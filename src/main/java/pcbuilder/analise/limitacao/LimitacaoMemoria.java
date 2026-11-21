package pcbuilder.analise.limitacao;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import pcbuilder.analise.CalculadoraMemoria;
import pcbuilder.analise.ComponentesDaMontagem;
import pcbuilder.enums.CategoriaComponente;
import pcbuilder.enums.SeveridadeLimitacao;
import pcbuilder.modelo.Componente;
import pcbuilder.modelo.Limitacao;
import pcbuilder.modelo.Memoria;
import pcbuilder.modelo.Montagem;
import pcbuilder.modelo.PlacaMae;
import pcbuilder.modelo.Processador;

/**
 * Regra <strong>L04</strong> - memoria x controlador da CPU x teto da placa-mae.
 *
 * <p>Dois avisos possiveis, ambos <strong>sem bloquear</strong> a escolha:</p>
 * <ul>
 *   <li>o kit roda abaixo da frequencia anunciada
 *       ({@code min(RAM, placa-mae, IMC da CPU)}) - caso obrigatorio da HU-004:
 *       8000 MT/s em placa de 6000 MT/s roda a 6000 MT/s (~25% de perda);</li>
 *   <li>o kit tem menos pentes que os canais da placa (single channel).</li>
 * </ul>
 *
 * <p>Roda quando o candidato e a memoria, a CPU ou a placa-mae, porque trocar qualquer um
 * dos tres muda o teto (a HU-011 reabre etapas nessa ordem).</p>
 */
public final class LimitacaoMemoria implements RegraLimitacao {

    @Override
    public String id() {
        return "L04";
    }

    @Override
    public List<Limitacao> avaliar(Montagem montagem, Componente candidato) {
        CategoriaComponente categoria = candidato.getCategoria();
        if (categoria != CategoriaComponente.MEMORIA
                && categoria != CategoriaComponente.CPU
                && categoria != CategoriaComponente.PLACA_MAE) {
            return List.of();
        }
        Optional<Memoria> memoriaOpt = ComponentesDaMontagem.memoria(montagem);
        if (memoriaOpt.isEmpty()) {
            return List.of();
        }
        Memoria memoria = memoriaOpt.get();
        Optional<PlacaMae> placaMae = ComponentesDaMontagem.placaMae(montagem);
        Optional<Processador> cpu = ComponentesDaMontagem.cpu(montagem);

        List<Limitacao> limitacoes = new ArrayList<>();
        adicionarFrequencia(limitacoes, memoria, placaMae.orElse(null), cpu.orElse(null));
        adicionarCanal(limitacoes, memoria, placaMae.orElse(null), cpu.orElse(null));
        return limitacoes;
    }

    private static void adicionarFrequencia(List<Limitacao> limitacoes, Memoria memoria,
            PlacaMae placaMae, Processador cpu) {
        int frequenciaPlaca = placaMae == null ? 0 : placaMae.getFrequenciaMemoriaMaximaMTs();
        Integer frequenciaCpu = cpu == null ? null : cpu.getFrequenciaMaximaSincronaMTs();
        int efetiva = CalculadoraMemoria.frequenciaEfetivaMTs(memoria.getFrequenciaMTs(),
                frequenciaPlaca, frequenciaCpu);
        if (efetiva >= memoria.getFrequenciaMTs()) {
            return;
        }
        Optional<Componente> limitante = CalculadoraMemoria
                .componenteQueLimitaFrequencia(memoria, placaMae, cpu);
        if (limitante.isEmpty()) {
            return;
        }
        double perda = CalculadoraMemoria.perdaFrequenciaPercentual(
                memoria.getFrequenciaMTs(), efetiva);
        limitacoes.add(Limitacao.builder()
                .regra("L04")
                .limitante(limitante.get())
                .afetado(memoria)
                .campo("frequenciaMTs")
                .valorDoNovo(memoria.getFrequenciaMTs() + " MT/s")
                .valorSuportado(efetiva + " MT/s")
                .consequencia(TextosLimitacao.frequenciaMemoria(limitante.get(), memoria,
                        efetiva, perda))
                .severidade(SeveridadeLimitacao.AVISO)
                .percentualPerda(perda)
                .construir());
    }

    private static void adicionarCanal(List<Limitacao> limitacoes, Memoria memoria,
            PlacaMae placaMae, Processador cpu) {
        int canais = placaMae != null && placaMae.getCanaisMemoria() > 0
                ? placaMae.getCanaisMemoria()
                : cpu == null ? 0 : cpu.getCanaisMemoria();
        if (CalculadoraMemoria.memoriaEmDualChannel(memoria, canais)) {
            return;
        }
        limitacoes.add(Limitacao.builder()
                .regra("L04")
                .limitante(memoria)
                .afetado(memoria)
                .campo("quantidadePentes")
                .valorDoNovo(memoria.getQuantidadePentes() + " pente(s)")
                .valorSuportado(canais + " canais")
                .consequencia(TextosLimitacao.singleChannel(memoria, canais))
                .severidade(SeveridadeLimitacao.AVISO)
                .construir());
    }

    @Override
    public List<String> dadosAusentes(Montagem montagem, Componente candidato) {
        Optional<Processador> cpu = ComponentesDaMontagem.cpu(montagem);
        if (categoriaAvaliada(candidato) && cpu.isPresent()
                && cpu.get().getFrequenciaMaximaSincronaMTs() == null) {
            return List.of("L04: frequencia maxima sincrona do processador nao cadastrada");
        }
        return List.of();
    }

    private static boolean categoriaAvaliada(Componente candidato) {
        CategoriaComponente categoria = candidato.getCategoria();
        return categoria == CategoriaComponente.MEMORIA
                || categoria == CategoriaComponente.CPU
                || categoria == CategoriaComponente.PLACA_MAE;
    }
}
