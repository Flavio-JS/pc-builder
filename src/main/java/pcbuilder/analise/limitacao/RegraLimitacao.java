package pcbuilder.analise.limitacao;

import java.util.List;

import pcbuilder.modelo.Componente;
import pcbuilder.modelo.Limitacao;
import pcbuilder.modelo.Montagem;

/**
 * Regra de <strong>limitacao</strong> (secao 14 do modelo de dados) - o par
 * "encaixa, mas um segura o outro".
 *
 * <p>Diferente de {@code RegraFiltro} (HU-003), nenhuma implementacao remove item da
 * lista: ela apenas devolve o aviso que a tela mostra com {@code [!]}. A mesma regra roda
 * nos dois sentidos - escolher a peca fraca depois tambem avisa, porque a regra olha o
 * par completo (pecas ja escolhidas + candidato).</p>
 *
 * <p>Regra sem dados suficientes <strong>nao</strong> inventa aviso: ela devolve lista
 * vazia e, quando faz sentido explicar a ausencia, informa o dado que falta em
 * {@link #dadosAusentes(Montagem, Componente)} (exibido apenas com
 * {@code --detalhar-limitacoes}).</p>
 */
public interface RegraLimitacao {

    /** Identificador da regra no modelo de dados ({@code L01}, {@code L04}...). */
    String id();

    /**
     * Avalia o par formado pelas pecas ja escolhidas e pelo candidato.
     *
     * @param montagem montagem <strong>ja com o candidato</strong> no lugar da peca da
     *        mesma categoria
     * @param candidato peca que o usuario esta escolhendo
     * @return as limitacoes encontradas (lista vazia quando o par esta equilibrado ou a
     *         regra nao se aplica)
     */
    List<Limitacao> avaliar(Montagem montagem, Componente candidato);

    /**
     * Dados que faltam para a regra rodar, quando e o caso.
     *
     * @param montagem montagem ja com o candidato
     * @param candidato peca que o usuario esta escolhendo
     * @return descricoes curtas do que falta; vazio quando a regra tem tudo
     */
    default List<String> dadosAusentes(Montagem montagem, Componente candidato) {
        return List.of();
    }
}
