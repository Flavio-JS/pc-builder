package pcbuilder.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.util.Optional;

/**
 * Entrada e saida do programa em linha de comando.
 *
 * <p>Concentra a leitura do teclado e a escrita na tela para que as telas nao
 * dependam diretamente de {@code System.in} / {@code System.out}. Nos testes basta
 * entregar um {@link Reader} com o texto digitado e um {@link PrintStream} de
 * memoria.</p>
 *
 * <p>A entrada termina (EOF) quando o usuario fecha o console ou quando a entrada vem
 * de um arquivo: nesse caso {@link #lerLinha()} devolve {@link Optional#empty()} e
 * quem chamou decide encerrar o fluxo sem erro.</p>
 */
public final class Terminal {

    private static final String RESPOSTA_SIM = "S";
    private static final String RESPOSTA_NAO = "N";

    private final BufferedReader entrada;
    private final PrintStream saida;

    /** Terminal ligado ao teclado e a saida padrao do processo. */
    public Terminal() {
        this(new InputStreamReader(System.in));
    }

    /**
     * Terminal com saida padrao, usado nos testes para injetar apenas a entrada.
     *
     * @param entrada origem dos caracteres digitados
     */
    public Terminal(Reader entrada) {
        this(entrada, System.out);
    }

    /**
     * @param entrada origem dos caracteres digitados
     * @param saida destino do texto impresso
     */
    public Terminal(Reader entrada, PrintStream saida) {
        this.entrada = new BufferedReader(entrada);
        this.saida = saida;
    }

    /** Imprime o texto sem quebrar linha (usado em prompts). */
    public void imprimir(String texto) {
        saida.print(texto);
        saida.flush();
    }

    /** Imprime o texto e quebra a linha. */
    public void linha(String texto) {
        saida.println(texto);
        saida.flush();
    }

    /** Imprime uma linha em branco. */
    public void linhaEmBranco() {
        linha("");
    }

    /**
     * Imprime a pergunta, espera a linha digitada e devolve o texto sem espacos nas
     * pontas.
     *
     * @param pergunta texto exibido antes da leitura
     * @return o que foi digitado, ou vazio quando a entrada terminou
     */
    public Optional<String> perguntar(String pergunta) {
        imprimir(pergunta + " ");
        return lerLinha();
    }

    /**
     * Le uma linha do teclado.
     *
     * @return o texto digitado (sem espacos nas pontas) ou vazio no fim da entrada
     * @throws IllegalStateException quando a entrada nao pode ser lida
     */
    public Optional<String> lerLinha() {
        try {
            String linha = entrada.readLine();
            return linha == null ? Optional.empty() : Optional.of(linha.trim());
        } catch (IOException causa) {
            throw new IllegalStateException("Falha ao ler a entrada do teclado.", causa);
        }
    }

    /**
     * Pede uma confirmacao {@code S/N}, repetindo a pergunta enquanto a resposta nao
     * for valida.
     *
     * @param pergunta texto da pergunta (o sufixo {@code (S/N)} e acrescentado)
     * @return {@code true} para sim; {@code false} para nao ou fim da entrada
     */
    public boolean confirmar(String pergunta) {
        while (true) {
            Optional<String> resposta = perguntar(pergunta + " (S/N)");
            if (resposta.isEmpty()) {
                return false;
            }
            String valor = resposta.get();
            if (RESPOSTA_SIM.equalsIgnoreCase(valor)) {
                return true;
            }
            if (RESPOSTA_NAO.equalsIgnoreCase(valor)) {
                return false;
            }
            linha("Digite S para sim ou N para nao.");
        }
    }
}
