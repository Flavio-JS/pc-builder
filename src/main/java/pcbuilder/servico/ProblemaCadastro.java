package pcbuilder.servico;

import java.util.Objects;

import pcbuilder.enums.NivelProblema;

/**
 * Um problema encontrado no cadastro de um componente (secao 18 do modelo de dados).
 *
 * <p>Guarda <em>qual</em> das 10 verificacoes falhou, o componente, o campo e uma
 * explicacao curta. Objetos imutaveis: o resultado da validacao nunca muda depois
 * de produzido.</p>
 */
public final class ProblemaCadastro {

    private final int verificacao;
    private final NivelProblema nivel;
    private final String idComponente;
    private final String campo;
    private final String descricao;

    /**
     * @param verificacao numero da verificacao do checklist (1 a 10)
     * @param nivel       {@link NivelProblema#ERRO} (registro descartado) ou
     *                    {@link NivelProblema#AVISO} (registro usavel com ressalva)
     * @param idComponente identificador do registro avaliado (pode ser {@code null})
     * @param campo       nome do campo problemático, conforme o modelo de dados
     * @param descricao   explicacao em uma linha, sem acento (vai para o console)
     */
    public ProblemaCadastro(int verificacao, NivelProblema nivel, String idComponente, String campo, String descricao) {
        this.verificacao = verificacao;
        this.nivel = Objects.requireNonNull(nivel, "nivel");
        this.idComponente = idComponente;
        this.campo = campo;
        this.descricao = Objects.requireNonNull(descricao, "descricao");
    }

    public int getVerificacao() {
        return verificacao;
    }

    public NivelProblema getNivel() {
        return nivel;
    }

    public String getIdComponente() {
        return idComponente;
    }

    public String getCampo() {
        return campo;
    }

    public String getDescricao() {
        return descricao;
    }

    /** {@code true} quando o problema descarta o registro. */
    public boolean isErro() {
        return nivel == NivelProblema.ERRO;
    }

    /** Linha pronta para o console, por exemplo {@code [AVISO] (6) gpu-x | picoTransienteWatts: ...}. */
    public String formatar() {
        return "[" + nivel + "] (" + verificacao + ") " + idComponente + " | " + campo + ": " + descricao;
    }

    @Override
    public String toString() {
        return formatar();
    }
}
