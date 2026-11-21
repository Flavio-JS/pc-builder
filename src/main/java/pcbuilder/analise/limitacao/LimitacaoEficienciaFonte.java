package pcbuilder.analise.limitacao;

import java.util.List;
import java.util.Optional;

import pcbuilder.analise.CalculadoraConsumo;
import pcbuilder.analise.ComponentesDaMontagem;
import pcbuilder.config.ParametrosCalculo;
import pcbuilder.enums.SeveridadeLimitacao;
import pcbuilder.modelo.Componente;
import pcbuilder.modelo.Fonte;
import pcbuilder.modelo.Limitacao;
import pcbuilder.modelo.Montagem;

/**
 * Regra <strong>L09</strong> - eficiencia da fonte.
 *
 * <p>A fonte entrega com melhor eficiencia entre 40% e 60% da carga. Uma fonte de 1000 W
 * para um PC de 400 W funciona, mas trabalha longe do ponto eficiente: gasta um pouco
 * mais e esquenta mais. Nunca bloqueia - a potencia minima ja e filtrada pela C14.</p>
 */
public final class LimitacaoEficienciaFonte implements RegraLimitacao {

    @Override
    public String id() {
        return "L09";
    }

    @Override
    public List<Limitacao> avaliar(Montagem montagem, Componente candidato) {
        Optional<Fonte> fonteOpt = ComponentesDaMontagem.fonte(montagem);
        if (fonteOpt.isEmpty() || fonteOpt.get().getPotenciaTotalWatts() <= 0) {
            return List.of();
        }
        Fonte fonte = fonteOpt.get();
        int consumo = CalculadoraConsumo.consumoTotalSistemaWatts(montagem);
        double carga = consumo * 100.0 / fonte.getPotenciaTotalWatts();
        if (carga >= ParametrosCalculo.FAIXA_EFICIENCIA_FONTE_MIN_PERCENTUAL
                && carga <= ParametrosCalculo.FAIXA_EFICIENCIA_FONTE_MAX_PERCENTUAL) {
            return List.of();
        }
        String faixa = ParametrosCalculo.FAIXA_EFICIENCIA_FONTE_MIN_PERCENTUAL + "-"
                + ParametrosCalculo.FAIXA_EFICIENCIA_FONTE_MAX_PERCENTUAL + "%";
        return List.of(Limitacao.builder()
                .regra(id())
                .limitante(fonte)
                .afetado(fonte)
                .campo("percentualCargaFonte")
                .valorDoNovo(TextosLimitacao.percentualExato(carga))
                .valorSuportado(faixa)
                .consequencia(TextosLimitacao.eficienciaFonte(fonte, carga))
                .severidade(SeveridadeLimitacao.AVISO)
                .construir());
    }
}
