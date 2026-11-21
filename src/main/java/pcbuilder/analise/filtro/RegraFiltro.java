package pcbuilder.analise.filtro;

import java.util.Optional;

import pcbuilder.enums.CategoriaComponente;
import pcbuilder.modelo.Componente;
import pcbuilder.modelo.Montagem;

/**
 * Regra de compatibilidade <strong>dura</strong> (secao 13 do modelo de dados).
 *
 * <p>Cada implementacao responde a uma unica pergunta: o candidato deve ser
 * <strong>ocultado</strong> da lista da etapa? O motivo devolvido e uma frase curta em
 * linguagem simples, exibida em {@code [M] Ver ocultos} (por exemplo
 * {@code soquete AM5 x processador LGA1700}).</p>
 *
 * <p>Quando a regra nao se aplica ao candidato (categoria diferente, peca ainda nao
 * escolhida ou dado insuficiente) ela devolve {@link Optional#empty()}: item oculto
 * so por falta de dado seria pior do que item visivel com dado incompleto, e o
 * cadastro ja e validado na carga (HU-001).</p>
 *
 * <p>Nenhuma implementacao pode ocultar item por <em>desempenho</em>: frequencia de
 * memoria, VRM, geracao de PCIe e gargalos sao avisos (HU-004), nunca bloqueio.</p>
 */
public interface RegraFiltro {

    /** Categoria a que a regra se aplica (a categoria que esta sendo listada). */
    CategoriaComponente categoriaAfetada();

    /**
     * Avalia o candidato contra as pecas ja escolhidas.
     *
     * @param atual montagem em andamento, com as pecas escolhidas ate a etapa anterior
     * @param candidato componente que seria exibido na lista (nunca {@code null})
     * @return o motivo para ocultar o candidato, ou vazio quando ele encaixa
     */
    Optional<String> motivoIncompatibilidade(Montagem atual, Componente candidato);
}
