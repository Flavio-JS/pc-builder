package pcbuilder;

import pcbuilder.enums.NivelProblema;
import pcbuilder.mock.CatalogoMock;
import pcbuilder.servico.ProblemaCadastro;
import pcbuilder.servico.ResultadoValidacao;

/**
 * Ponto de entrada da aplicacao de linha de comando.
 *
 * <p>Responsabilidades desta HU:</p>
 * <ol>
 *   <li>carregar a base mockada uma unica vez ({@link CatalogoMock#criar()});</li>
 *   <li>imprimir o resumo da carga;</li>
 *   <li>com {@code --validar-base}, listar os problemas e devolver o codigo de
 *       saida {@code 2} quando existir pelo menos um {@code ERRO}.</li>
 * </ol>
 *
 * <p>Codigos de saida: {@code 0} sucesso, {@code 1} erro de uso, {@code 2} base
 * mockada invalida. A validacao em si nunca chama {@code System.exit}.</p>
 */
public final class Main {

    /** Execucao normal. */
    public static final int SAIDA_SUCESSO = 0;
    /** Argumento invalido. */
    public static final int SAIDA_ERRO_USO = 1;
    /** Base mockada com pelo menos um registro invalido. */
    public static final int SAIDA_BASE_INVALIDA = 2;

    private static final String OPCAO_VALIDAR_BASE = "--validar-base";

    private Main() {
        // classe utilitaria: nao deve ser instanciada
    }

    public static void main(String[] args) {
        int codigo = executar(args);
        if (codigo != SAIDA_SUCESSO) {
            System.exit(codigo);
        }
    }

    /**
     * Executa a aplicacao e devolve o codigo de saida (sem encerrar a JVM).
     *
     * @param args argumentos da linha de comando
     * @return codigo de saida conforme as constantes desta classe
     */
    public static int executar(String[] args) {
        boolean validarBase = false;
        if (args != null) {
            for (String argumento : args) {
                if (OPCAO_VALIDAR_BASE.equals(argumento)) {
                    validarBase = true;
                } else {
                    System.err.println("Opcao desconhecida: " + argumento);
                    System.err.println("Uso: java pcbuilder.Main [--validar-base]");
                    return SAIDA_ERRO_USO;
                }
            }
        }

        CatalogoMock catalogo = CatalogoMock.criar();
        ResultadoValidacao resultado = catalogo.getResultadoValidacao();
        System.out.println(resultado.resumo());

        if (validarBase) {
            imprimirProblemas(resultado);
            if (resultado.possuiErros()) {
                return SAIDA_BASE_INVALIDA;
            }
            System.out.println("Validacao concluida: nenhum registro descartado.");
            return SAIDA_SUCESSO;
        }

        if (resultado.possuiErros()) {
            imprimirProblemas(resultado);
            System.out.println("Base mockada invalida: " + resultado.getTotalErros()
                    + " problema(s) de nivel ERRO. Use --validar-base para o relatorio completo.");
            return SAIDA_BASE_INVALIDA;
        }

        System.out.println("Montagem guiada disponivel a partir da HU-002.");
        return SAIDA_SUCESSO;
    }

    private static void imprimirProblemas(ResultadoValidacao resultado) {
        for (NivelProblema nivel : NivelProblema.values()) {
            for (ProblemaCadastro problema : resultado.getProblemas(nivel)) {
                System.out.println(problema.formatar());
            }
        }
    }
}
