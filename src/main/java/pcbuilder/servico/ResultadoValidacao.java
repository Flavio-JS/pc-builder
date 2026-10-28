package pcbuilder.servico;

import java.util.ArrayList;
import java.util.List;

import pcbuilder.enums.NivelProblema;
import pcbuilder.modelo.Componente;

/**
 * Resultado da carga da base mockada (HU-001).
 *
 * <p>Reune os registros avaliados, os que passaram (sem {@code ERRO}) e a lista de
 * problemas. A validacao devolve este objeto em vez de encerrar o programa, o que
 * permite testar a carga sem {@code System.exit}.</p>
 */
public final class ResultadoValidacao {

    private final List<Componente> registros;
    private final List<Componente> registrosValidos;
    private final List<ProblemaCadastro> problemas;

    ResultadoValidacao(List<Componente> registros, List<Componente> registrosValidos,
            List<ProblemaCadastro> problemas) {
        this.registros = List.copyOf(registros);
        this.registrosValidos = List.copyOf(registrosValidos);
        this.problemas = List.copyOf(problemas);
    }

    /** Todos os registros que a carga tentou carregar. */
    public List<Componente> getRegistros() {
        return registros;
    }

    /** Registros sem nenhum problema de nivel {@code ERRO} - vao para o catalogo. */
    public List<Componente> getRegistrosValidos() {
        return registrosValidos;
    }

    public List<ProblemaCadastro> getProblemas() {
        return problemas;
    }

    public List<ProblemaCadastro> getProblemas(NivelProblema nivel) {
        List<ProblemaCadastro> filtrados = new ArrayList<>();
        for (ProblemaCadastro problema : problemas) {
            if (problema.getNivel() == nivel) {
                filtrados.add(problema);
            }
        }
        return filtrados;
    }

    public List<ProblemaCadastro> getProblemasDe(String idComponente) {
        List<ProblemaCadastro> filtrados = new ArrayList<>();
        for (ProblemaCadastro problema : problemas) {
            if (problema.getIdComponente() != null && problema.getIdComponente().equals(idComponente)) {
                filtrados.add(problema);
            }
        }
        return filtrados;
    }

    public int getTotalRegistros() {
        return registros.size();
    }

    public int getTotalValidos() {
        return registrosValidos.size();
    }

    public int contarNivel(NivelProblema nivel) {
        return getProblemas(nivel).size();
    }

    public int getTotalErros() {
        return contarNivel(NivelProblema.ERRO);
    }

    public int getTotalAvisos() {
        return contarNivel(NivelProblema.AVISO);
    }

    public boolean possuiErros() {
        return getTotalErros() > 0;
    }

    /**
     * Linha de resumo exigida pela HU-001, por exemplo:
     * {@code Base carregada: 17230 componentes, 0 erro(s), 0 aviso(s).}
     */
    public String resumo() {
        return "Base carregada: " + getTotalRegistros() + " componentes, "
                + getTotalErros() + " erro(s), " + getTotalAvisos() + " aviso(s).";
    }
}
