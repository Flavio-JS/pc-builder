package pcbuilder.analise;

import java.util.Objects;
import java.util.Optional;

import pcbuilder.modelo.Componente;
import pcbuilder.modelo.Memoria;
import pcbuilder.modelo.Montagem;
import pcbuilder.modelo.PlacaMae;
import pcbuilder.modelo.Processador;

/**
 * Calculos de memoria usados pela regra <strong>L04</strong> (HU-004) e pela linha de
 * apoio da etapa 3 (HU-006).
 *
 * <p>Nada aqui e gravado no componente: frequencia efetiva, latencia e capacidade total
 * sao derivadas dos campos cadastrados (secao 16 do modelo de dados).</p>
 */
public final class CalculadoraMemoria {

    private CalculadoraMemoria() {
        // classe utilitaria: nao deve ser instanciada
    }

    /** Capacidade total do kit: {@code capacidadePorPenteGB x quantidadePentes}. */
    public static int capacidadeTotalGB(Memoria memoria) {
        return memoria.getCapacidadePorPenteGB() * memoria.getQuantidadePentes();
    }

    /**
     * O kit ocupa todos os canais que a placa (ou a CPU) oferece?
     *
     * <p>Um kit de 1 pente em placa de 2 canais roda em <em>single channel</em> e perde
     * cerca de metade da banda - e um aviso de L04, nunca um bloqueio.</p>
     *
     * @param memoria kit escolhido
     * @param canaisSuportados canais de memoria da placa-mae (ou da CPU)
     * @return {@code true} quando o kit preenche todos os canais
     */
    public static boolean memoriaEmDualChannel(Memoria memoria, int canaisSuportados) {
        return canaisSuportados <= 0 || memoria.getQuantidadePentes() >= canaisSuportados;
    }

    /**
     * Frequencia efetiva da memoria: {@code min(ram, placa-mae, IMC da CPU)}.
     *
     * @param frequenciaRAM frequencia anunciada do kit
     * @param frequenciaMaximaPlaca teto da placa-mae ({@code 0} quando nao ha placa)
     * @param frequenciaMaximaCPU teto do controlador da CPU ({@code null} quando nao ha
     *        CPU ou o campo nao esta cadastrado)
     * @return a menor frequencia entre as informadas
     */
    public static int frequenciaEfetivaMTs(int frequenciaRAM, int frequenciaMaximaPlaca,
            Integer frequenciaMaximaCPU) {
        int efetiva = frequenciaRAM;
        if (frequenciaMaximaPlaca > 0 && frequenciaMaximaPlaca < efetiva) {
            efetiva = frequenciaMaximaPlaca;
        }
        if (frequenciaMaximaCPU != null && frequenciaMaximaCPU > 0
                && frequenciaMaximaCPU < efetiva) {
            efetiva = frequenciaMaximaCPU;
        }
        return efetiva;
    }

    /**
     * Latencia efetiva em nanossegundos: {@code latenciaCAS / frequenciaEfetiva x 2000}.
     *
     * @param latenciaCAS ciclos do kit
     * @param frequenciaEfetivaMTs frequencia em que o kit vai rodar
     * @return a latencia em ns; {@code 0} quando a frequencia e invalida
     */
    public static double latenciaEfetivaNs(int latenciaCAS, int frequenciaEfetivaMTs) {
        if (frequenciaEfetivaMTs <= 0) {
            return 0;
        }
        return latenciaCAS / (double) frequenciaEfetivaMTs * 2000;
    }

    /**
     * Perda de frequencia do kit, em pontos percentuais.
     *
     * @param frequenciaAnunciada frequencia do kit
     * @param frequenciaEfetiva frequencia em que ele realmente roda
     * @return a perda ({@code 25.0} = ~25%)
     */
    public static double perdaFrequenciaPercentual(int frequenciaAnunciada, int frequenciaEfetiva) {
        if (frequenciaAnunciada <= 0) {
            return 0;
        }
        return (frequenciaAnunciada - frequenciaEfetiva) * 100.0 / frequenciaAnunciada;
    }

    /**
     * Canais de memoria oferecidos pela configuracao (placa-mae e, na falta dela, CPU).
     *
     * @param montagem pecas escolhidas ate a etapa 3
     * @return a quantidade de canais; {@code 0} quando nenhuma peca informa o dado
     */
    public static int canaisSuportados(Montagem montagem) {
        Objects.requireNonNull(montagem, "montagem");
        Optional<PlacaMae> placaMae = ComponentesDaMontagem.placaMae(montagem);
        if (placaMae.isPresent() && placaMae.get().getCanaisMemoria() > 0) {
            return placaMae.get().getCanaisMemoria();
        }
        return ComponentesDaMontagem.cpu(montagem)
                .map(Processador::getCanaisMemoria)
                .orElse(0);
    }

    /**
     * Qual peca impoe o teto de frequencia abaixo do anunciado pelo kit.
     *
     * <p>E o componente nomeado no aviso ("A placa-mae X suporta ate..."). A placa-mae
     * tem prioridade em caso de empate porque e ela que define o teto fisico do slot.</p>
     *
     * @param memoria kit (com a frequencia anunciada)
     * @param placaMae placa escolhida, pode ser {@code null}
     * @param cpu processador escolhido, pode ser {@code null}
     * @return a peca limitante, ou vazio quando nenhuma limita o kit
     */
    public static Optional<Componente> componenteQueLimitaFrequencia(Memoria memoria,
            PlacaMae placaMae, Processador cpu) {
        int teto = memoria.getFrequenciaMTs();
        Componente limitante = null;
        if (placaMae != null && placaMae.getFrequenciaMemoriaMaximaMTs() > 0
                && placaMae.getFrequenciaMemoriaMaximaMTs() < teto) {
            teto = placaMae.getFrequenciaMemoriaMaximaMTs();
            limitante = placaMae;
        }
        if (cpu != null && cpu.getFrequenciaMaximaSincronaMTs() != null
                && cpu.getFrequenciaMaximaSincronaMTs() > 0
                && cpu.getFrequenciaMaximaSincronaMTs() < teto) {
            limitante = cpu;
        }
        return Optional.ofNullable(limitante);
    }
}
