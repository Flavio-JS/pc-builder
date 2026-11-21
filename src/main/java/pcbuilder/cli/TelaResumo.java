package pcbuilder.cli;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import pcbuilder.analise.limitacao.TextosLimitacao;
import pcbuilder.modelo.Limitacao;
import pcbuilder.relatorio.FormatadorMoeda;
import pcbuilder.relatorio.ResumoMontagem;
import pcbuilder.relatorio.TabelaResumo;

/**
 * Tela do resumo final (HU-010): tabela etapa/categoria/peca/preco, total, avisos e
 * rodape de comandos.
 *
 * <p>Comandos: {@code [T] Trocar uma peca} (HU-011), {@code [N] Nova montagem} e
 * {@code [0] Sair}. Enquanto a HU-011 nao existe, {@code [T]} apenas explica que a troca
 * chega nela e reapresenta o resumo.</p>
 */
public final class TelaResumo {

    /** Titulo do resumo. */
    public static final String TITULO = "=== Resumo da sua montagem ===";
    /** Nota de rodape quando alguma peca nao tem preco no catalogo. */
    public static final String NOTA_SEM_PRECO = "(*) inclui peca sem preco no catalogo";
    /** Rodape de comandos do resumo. */
    public static final String RODAPE = "[T] Trocar uma peca   [N] Nova montagem   [0] Sair";
    /** Mensagem de comando invalido. */
    public static final String MENSAGEM_OPCAO_INVALIDA = "Digite uma das opcoes.";
    /** Texto provisorio do comando de troca (implementado na HU-011). */
    public static final String MENSAGEM_TROCA = "A troca de peca entra na HU-011.";

    /** Comando que inicia uma nova montagem. */
    public static final String COMANDO_NOVA = "N";
    /** Comando que encerra a aplicacao. */
    public static final String COMANDO_SAIR = "0";
    /** Comando de troca de peca (HU-011). */
    public static final String COMANDO_TROCAR = "T";

    /** O que o usuario escolheu no rodape. */
    public enum Resultado {
        NOVA_MONTAGEM,
        SAIR
    }

    private final Terminal terminal;

    public TelaResumo(Terminal terminal) {
        this.terminal = terminal;
    }

    /**
     * Imprime a tabela, o total, os avisos e o rodape.
     *
     * @param resumo resumo da montagem
     */
    public void imprimir(ResumoMontagem resumo) {
        terminal.linha(TITULO);
        terminal.linha(TabelaResumo.separador());
        terminal.linha(TabelaResumo.cabecalho());
        terminal.linha(TabelaResumo.separador());
        for (ResumoMontagem.LinhaResumo linha : resumo.getLinhas()) {
            terminal.linha(TabelaResumo.linha(linha.getEtapa(),
                    linha.getCategoria().getRotulo(), linha.getPeca(),
                    linha.getPrecoFormatado()));
        }
        terminal.linha(TabelaResumo.separador());
        terminal.linha(TabelaResumo.total(FormatadorMoeda.reais(resumo.getTotal())));
        if (resumo.possuiPecaSemPreco()) {
            terminal.linha(NOTA_SEM_PRECO);
        }
        List<Limitacao> avisos = resumo.getAvisos();
        if (!avisos.isEmpty()) {
            terminal.linhaEmBranco();
            terminal.linha("Avisos: " + avisos.size());
            for (Limitacao limitacao : avisos) {
                terminal.linha(TextosLimitacao.resumo(limitacao));
            }
        }
        terminal.linha(TabelaResumo.separador());
        terminal.linha(RODAPE);
    }

    /**
     * Mensagem de montagem incompleta.
     *
     * @param resumo resumo com pendencias
     */
    public void imprimirPendencias(ResumoMontagem resumo) {
        terminal.linha("Complete as etapas pendentes: " + resumo.descricaoPendencias() + ".");
    }

    /**
     * Mostra o resumo e processa os comandos do rodape.
     *
     * @param resumo resumo da montagem (completa)
     * @return {@link Resultado#NOVA_MONTAGEM} ou {@link Resultado#SAIR}
     */
    public Resultado executar(ResumoMontagem resumo) {
        while (true) {
            imprimir(resumo);
            Optional<String> entrada = terminal.perguntar(">");
            if (entrada.isEmpty()) {
                return Resultado.SAIR;
            }
            String comando = entrada.get().toUpperCase(Locale.ROOT);
            if (COMANDO_NOVA.equals(comando)) {
                return Resultado.NOVA_MONTAGEM;
            }
            if (COMANDO_SAIR.equals(comando)) {
                return Resultado.SAIR;
            }
            if (COMANDO_TROCAR.equals(comando)) {
                terminal.linha(MENSAGEM_TROCA);
                continue;
            }
            terminal.linha(MENSAGEM_OPCAO_INVALIDA);
        }
    }
}
