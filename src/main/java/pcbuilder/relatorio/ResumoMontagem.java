package pcbuilder.relatorio;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import pcbuilder.config.OrdemEtapas;
import pcbuilder.enums.CategoriaComponente;
import pcbuilder.modelo.Componente;
import pcbuilder.modelo.Limitacao;
import pcbuilder.modelo.Montagem;
import pcbuilder.servico.ValidadorMontagem;

/**
 * Resumo final da montagem (HU-010): uma linha por etapa, total em {@link BigDecimal} e
 * os avisos acumulados.
 *
 * <p>Usa <strong>somente as instancias ja escolhidas</strong> na {@link Montagem} - nunca
 * reconsulta o catalogo, entao o preco exibido e o da peca efetivamente selecionada. A
 * etapa 4 sem GPU aparece como {@code (nao selecionado)} e nao soma no total.</p>
 */
public final class ResumoMontagem {

    /** Texto da etapa sem peca (GPU pulada na etapa 4). */
    public static final String NAO_SELECIONADO = "(nao selecionado)";

    private final Montagem montagem;
    private final List<LinhaResumo> linhas;
    private final BigDecimal total;
    private final boolean pecaSemPreco;
    private final List<Limitacao> avisos;
    private final List<CategoriaComponente> pendencias;

    /**
     * @param montagem montagem (parcial ou completa) que sera exibida
     */
    public ResumoMontagem(Montagem montagem) {
        this.montagem = Objects.requireNonNull(montagem, "montagem");
        List<LinhaResumo> construidas = new ArrayList<>();
        boolean semPreco = false;
        for (int etapa = 1; etapa <= OrdemEtapas.TOTAL_ETAPAS; etapa++) {
            CategoriaComponente categoria = OrdemEtapas.categoria(etapa);
            Componente componente = montagem.obter(categoria).orElse(null);
            if (componente != null && componente.getPrecoMedioBRL() == null) {
                semPreco = true;
            }
            construidas.add(new LinhaResumo(etapa, categoria, componente));
        }
        this.linhas = List.copyOf(construidas);
        this.total = montagem.custoTotal();
        this.pecaSemPreco = semPreco;
        this.avisos = montagem.getLimitacoes();
        this.pendencias = ValidadorMontagem.pendencias(montagem);
    }

    /** Linhas na ordem fixa das seis etapas. */
    public List<LinhaResumo> getLinhas() {
        return linhas;
    }

    /** Total das pecas presentes; peca sem preco nao entra. */
    public BigDecimal getTotal() {
        return total;
    }

    /** {@code true} quando alguma peca escolhida nao tem preco no catalogo. */
    public boolean possuiPecaSemPreco() {
        return pecaSemPreco;
    }

    /** Avisos de limitacao acumulados (HU-004). */
    public List<Limitacao> getAvisos() {
        return avisos;
    }

    /** {@code true} quando nenhuma etapa obrigatoria esta pendente. */
    public boolean completa() {
        return pendencias.isEmpty();
    }

    /** Categorias obrigatorias ainda sem peca. */
    public List<CategoriaComponente> getPendencias() {
        return pendencias;
    }

    /** Rotulos das pendencias ({@code Placa-Mae, Memoria, ...}). */
    public String descricaoPendencias() {
        return ValidadorMontagem.descricaoPendencias(montagem);
    }

    /** Uma linha do resumo: etapa, categoria, peca escolhida e preco. */
    public static final class LinhaResumo {

        private final int etapa;
        private final CategoriaComponente categoria;
        private final Componente componente;

        LinhaResumo(int etapa, CategoriaComponente categoria, Componente componente) {
            this.etapa = etapa;
            this.categoria = categoria;
            this.componente = componente;
        }

        public int getEtapa() {
            return etapa;
        }

        public CategoriaComponente getCategoria() {
            return categoria;
        }

        /** {@code true} quando a etapa tem peca escolhida. */
        public boolean isSelecionada() {
            return componente != null;
        }

        /** Nome da peca ou {@link #NAO_SELECIONADO}. */
        public String getPeca() {
            return componente == null ? NAO_SELECIONADO : FormatadorComponente.nome(componente);
        }

        /** Preco da peca (pode ser {@code null}); {@code null} tambem quando nao ha peca. */
        public BigDecimal getPreco() {
            return componente == null ? null : componente.getPrecoMedioBRL();
        }

        /** Preco pronto para a coluna ({@code 2.199,00} ou {@code R$ --}). */
        public String getPrecoFormatado() {
            return FormatadorMoeda.numero(getPreco());
        }
    }
}
