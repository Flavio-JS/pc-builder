package pcbuilder.analise.limitacao;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import pcbuilder.modelo.Componente;
import pcbuilder.modelo.Limitacao;
import pcbuilder.modelo.Montagem;

/**
 * Avalia as limitacoes de uma peca candidata contra as pecas ja escolhidas (HU-004).
 *
 * <p>O avaliador monta uma copia da {@link Montagem} <strong>com o candidato no lugar da
 * peca da mesma categoria</strong> e roda as regras da secao 14 do modelo de dados nessa
 * copia. Assim a mesma regra enxerga o par completo nos dois sentidos: escolher a peca
 * fraca antes ou depois produz o mesmo aviso. A montagem original nao e alterada.</p>
 *
 * <p>Nenhum resultado daqui altera o filtro da HU-003: filtro e aviso sao avaliados
 * separadamente, e uma peca com {@code [!]} continua selecionavel.</p>
 */
public final class AvaliadorLimitacoes {

    /** Regras na ordem sugerida pela secao 14: L01 por ultimo. */
    private static final List<RegraLimitacao> REGRAS_PADRAO = List.of(
            new LimitacaoVrm(),
            new LimitacaoMemoria(),
            new LimitacaoPcieGpu(),
            new LimitacaoTransienteFonte(),
            new LimitacaoVram(),
            new LimitacaoEficienciaFonte(),
            new LimitacaoVelocidadeArmazenamento(),
            new LimitacaoPistasPcie(),
            new LimitacaoGargaloCpuGpu());

    private final List<RegraLimitacao> regras;

    /** Avaliador com as regras de limitacao do escopo (L01, L02, L04-L07, L09-L11). */
    public AvaliadorLimitacoes() {
        this(REGRAS_PADRAO);
    }

    /**
     * @param regras regras aplicadas, na ordem de avaliacao
     */
    public AvaliadorLimitacoes(List<RegraLimitacao> regras) {
        this.regras = List.copyOf(regras);
    }

    /**
     * Limitacoes geradas pela escolha do candidato.
     *
     * @param atual montagem com as pecas escolhidas ate aqui
     * @param candidato peca que o usuario esta escolhendo
     * @return os avisos, na ordem das regras (lista vazia quando a peca nao limita nada)
     */
    public List<Limitacao> avaliar(Montagem atual, Componente candidato) {
        Objects.requireNonNull(atual, "atual");
        Objects.requireNonNull(candidato, "candidato");
        Montagem comCandidato = comCandidato(atual, candidato);
        List<Limitacao> limitacoes = new ArrayList<>();
        for (RegraLimitacao regra : regras) {
            limitacoes.addAll(regra.avaliar(comCandidato, candidato));
        }
        return List.copyOf(limitacoes);
    }

    /**
     * Avalia uma lista inteira de candidatos - e o que a tela usa para marcar {@code [!]}
     * antes do numero de cada item.
     *
     * @param atual montagem com as pecas escolhidas
     * @param candidatos itens que serao exibidos na etapa
     * @return somente os candidatos que geram aviso, com a lista de limitacoes de cada um
     */
    public Map<Componente, List<Limitacao>> avaliarLista(Montagem atual,
            List<? extends Componente> candidatos) {
        Objects.requireNonNull(candidatos, "candidatos");
        Map<Componente, List<Limitacao>> porComponente = new LinkedHashMap<>();
        for (Componente candidato : candidatos) {
            List<Limitacao> limitacoes = avaliar(atual, candidato);
            if (!limitacoes.isEmpty()) {
                porComponente.put(candidato, limitacoes);
            }
        }
        return porComponente;
    }

    /**
     * Dados que faltam para alguma regra calcular a limitacao do candidato.
     *
     * <p>Nao gera aviso na tela: aparece apenas com {@code --detalhar-limitacoes}, para
     * nao inventar perda quando o cadastro esta incompleto.</p>
     *
     * @param atual montagem com as pecas escolhidas
     * @param candidato peca avaliada
     * @return uma linha por dado ausente
     */
    public List<String> pendencias(Montagem atual, Componente candidato) {
        Objects.requireNonNull(atual, "atual");
        Objects.requireNonNull(candidato, "candidato");
        Montagem comCandidato = comCandidato(atual, candidato);
        List<String> pendencias = new ArrayList<>();
        for (RegraLimitacao regra : regras) {
            pendencias.addAll(regra.dadosAusentes(comCandidato, candidato));
        }
        return List.copyOf(pendencias);
    }

    /** Copia a montagem e coloca o candidato na categoria dele (substituindo a peca). */
    private static Montagem comCandidato(Montagem atual, Componente candidato) {
        Montagem copia = new Montagem();
        for (Componente peca : atual.pecas().values()) {
            copia.adicionar(peca);
        }
        copia.adicionar(candidato);
        return copia;
    }
}
