package pcbuilder.analise.limitacao;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import pcbuilder.analise.CalculadoraPcie;
import pcbuilder.enums.CategoriaComponente;
import pcbuilder.enums.TipoMidia;
import pcbuilder.modelo.Armazenamento;
import pcbuilder.modelo.Componente;
import pcbuilder.modelo.Fonte;
import pcbuilder.modelo.Limitacao;
import pcbuilder.modelo.Memoria;
import pcbuilder.modelo.PlacaMae;
import pcbuilder.modelo.PlacaVideo;
import pcbuilder.modelo.Processador;

/**
 * Frases em linguagem simples de cada limitacao (HU-004).
 *
 * <p>O usuario leigo nunca deve ler jargao solto: cada consequencia explica o que
 * acontece na pratica ("vai rodar a 6000 MT/s", "a maquina pode desligar sob carga") e
 * sempre termina lembrando que <strong>a escolha continua valida</strong>. Os textos sao
 * ASCII de proposito, porque o console do Windows trabalha em CP850.</p>
 *
 * <p>Este e o unico lugar que monta o bloco de notificacao - a CLI so imprime as linhas
 * devolvidas por {@link #linhas(Limitacao)}.</p>
 */
public final class TextosLimitacao {

    /** Frase de encerramento obrigatoria de toda notificacao de limitacao. */
    public static final String ESCOLHA_VALIDA = "A escolha continua valida.";
    /** Texto usado quando falta um dado para a regra rodar (so com --detalhar-limitacoes). */
    public static final String SEM_DADOS = "Sem dados para calcular";

    private static final String MARCADOR = "[!]";
    private static final String IDENTACAO = "    ";

    private TextosLimitacao() {
        // classe utilitaria: nao deve ser instanciada
    }

    /**
     * Bloco de notificacao da limitacao, pronto para impressao.
     *
     * <p>Formato da secao 2.3 das historias de usuario:</p>
     * <pre>
     * [!] AVISO - &lt;limitante&gt; limita &lt;afetado&gt;:
     *     &lt;campo&gt;: &lt;valor do novo&gt; &gt; &lt;valor suportado&gt; (&lt;consequencia&gt;)
     *     A escolha continua valida.
     * </pre>
     *
     * @param limitacao limitacao detectada
     * @return as linhas do bloco, na ordem de exibicao
     */
    public static List<String> linhas(Limitacao limitacao) {
        List<String> linhas = new ArrayList<>();
        linhas.add(cabecalho(limitacao));
        linhas.add(IDENTACAO + detalhe(limitacao));
        linhas.add(IDENTACAO + ESCOLHA_VALIDA);
        return linhas;
    }

    /** Nome exibido do componente: fabricante + modelo (como na lista da etapa). */
    public static String nome(Componente componente) {
        if (componente == null) {
            return "?";
        }
        String fabricante = componente.getFabricante();
        if (fabricante == null || fabricante.isBlank()) {
            return componente.getModelo();
        }
        return fabricante + " " + componente.getModelo();
    }

    /** Percentual com sinal de aproximacao e sem casas decimais ({@code ~25%}). */
    public static String percentual(double valor) {
        return String.format(Locale.ROOT, "~%.0f%%", valor);
    }

    /** Percentual sem o til, para limites exatos ({@code 80%}). */
    public static String percentualExato(double valor) {
        return String.format(Locale.ROOT, "%.0f%%", valor);
    }

    private static String cabecalho(Limitacao limitacao) {
        String severidade = limitacao.getSeveridade().name();
        String limitante = nome(limitacao.getComponenteLimitante());
        if (limitacao.getComponenteLimitante().equals(limitacao.getComponenteAfetado())) {
            return MARCADOR + " " + severidade + " - " + limitante + ":";
        }
        return MARCADOR + " " + severidade + " - " + limitante + " limita "
                + nome(limitacao.getComponenteAfetado()) + ":";
    }

    private static String detalhe(Limitacao limitacao) {
        if (limitacao.getCampoComparado() == null || limitacao.getCampoComparado().isBlank()) {
            return limitacao.getConsequencia();
        }
        String operador = "percentualCargaFonte".equals(limitacao.getCampoComparado())
                ? " fora de "
                : " > ";
        return limitacao.getCampoComparado() + ": " + valorOuTraco(limitacao.getValorDoNovo())
                + operador + valorOuTraco(limitacao.getValorSuportado())
                + " (" + limitacao.getConsequencia() + ")";
    }

    private static String valorOuTraco(String valor) {
        return valor == null || valor.isBlank() ? "--" : valor;
    }

    // ------------------------------------------------- consequencias por regra

    /** L02: uso do VRM acima do limite. */
    public static String vrm(PlacaMae placaMae, Processador cpu, double uso, int limite) {
        return "o VRM da placa-mae " + nome(placaMae) + " trabalha a "
                + percentual(uso) + " da capacidade para alimentar o " + nome(cpu)
                + " (limite seguro: " + limite + "%): a placa pode reduzir a frequencia da CPU"
                + " para se proteger do calor.";
    }

    /**
     * Uma linha curta para o bloco de avisos do resumo (HU-010).
     *
     * <p>Formato: {@code [!] <peca afetada>: <campo> <valor novo> > <valor suportado>}.
     * Nao repete a consequencia completa - o resumo precisa ser curto.</p>
     *
     * @param limitacao aviso acumulado
     * @return a linha pronta para impressao
     */
    public static String resumo(Limitacao limitacao) {
        String campo = limitacao.getCampoComparado() == null
                ? ""
                : limitacao.getCampoComparado() + " ";
        String operador = "percentualCargaFonte".equals(limitacao.getCampoComparado())
                ? " fora de "
                : " > ";
        return MARCADOR + " " + nome(limitacao.getComponenteAfetado()) + ": " + campo
                + valorOuTraco(limitacao.getValorDoNovo()) + operador
                + valorOuTraco(limitacao.getValorSuportado());
    }

    /** C02: o perfil EXPO/XMP do kit nao e suportado pela placa-mae. */
    public static String expo(Memoria memoria, PlacaMae placaMae) {
        Integer jedec = memoria.getFrequenciaJEDECBaseMTs();
        return "esta memoria foi feita para o perfil " + perfis(memoria)
                + " e a placa-mae " + nome(placaMae) + " nao suporta esse perfil: o kit vai"
                + " rodar no perfil JEDEC"
                + (jedec != null && jedec > 0 ? " (" + jedec + " MT/s)" : "")
                + ", abaixo da frequencia anunciada.";
    }

    private static String perfis(Memoria memoria) {
        if (memoria.isPerfilEXPO() && memoria.isPerfilXMP()) {
            return "EXPO/XMP";
        }
        return memoria.isPerfilEXPO() ? "EXPO" : "XMP";
    }

    /** C09: a placa pode precisar de atualizacao de BIOS para a CPU. */
    public static String bios(PlacaMae placaMae, Processador cpu) {
        String detalhe = placaMae.isSuportaAtualizacaoBIOSSemCPU()
                ? "a placa atualiza a BIOS sem precisar de uma CPU compativel"
                : "se a BIOS vier antiga, pode ser preciso uma CPU compativel so para atualizar";
        return "esta placa-mae pode exigir atualizacao de BIOS para o processador "
                + nome(cpu) + " (" + detalhe + ").";
    }

    /** L04: a memoria roda abaixo da frequencia anunciada. */
    public static String frequenciaMemoria(Componente limitante, Memoria memoria,
            int frequenciaEfetiva, double perda) {
        return rotulo(limitante) + " suporta ate " + frequenciaEfetiva + " MT/s."
                + " Esta memoria (" + memoria.getFrequenciaMTs() + " MT/s) vai rodar a "
                + frequenciaEfetiva + " MT/s: perda de " + percentual(perda)
                + " da frequencia.";
    }

    /** L04: kit de poucos pentes em placa com mais canais. */
    public static String singleChannel(Memoria memoria, int canais) {
        return "este kit tem " + memoria.getQuantidadePentes() + " pente(s) e a configuracao"
                + " oferece " + canais + " canais: em single channel a banda de memoria cai"
                + " cerca de metade.";
    }

    /** L05: GPU de geracao maior que o slot x16 da placa. */
    public static String pcieGpu(PlacaMae placaMae, PlacaVideo gpu, double perda) {
        int pistasSlot = CalculadoraPcie.pistasEfetivas(gpu.getPistasPCIe(),
                placaMae.getPistasSlotPCIe16());
        return "o slot x16 da placa-mae " + nome(placaMae) + " e "
                + CalculadoraPcie.descricao(placaMae.getVersaoSlotPCIe16(), pistasSlot)
                + " e esta placa de video e "
                + CalculadoraPcie.descricao(gpu.getVersaoPCIe(), gpu.getPistasPCIe())
                + ": a banda cai " + percentual(perda)
                + " (a placa funciona, so nao no maximo).";
    }

    /** L06: pico de energia da GPU acima do trilho de 12 V da fonte. */
    public static String transienteFonte(Fonte fonte, PlacaVideo gpu) {
        return "a placa de video " + nome(gpu) + " tem picos de "
                + gpu.getPicoTransienteWatts() + " W, acima dos "
                + fonte.getPotenciaTrilho12VWatts() + " W do trilho de 12 V da fonte "
                + nome(fonte) + ": a maquina pode desligar sob carga.";
    }

    /** L07: VRAM abaixo do recomendado para a resolucao-alvo. */
    public static String vram(PlacaVideo gpu, int minimoGB) {
        String resolucao = gpu.getResolucaoAlvo() == null
                ? "a resolucao alvo"
                : gpu.getResolucaoAlvo().getRotulo();
        return "a placa de video " + nome(gpu) + " tem " + gpu.getMemoriaVRAMGB()
                + " GB de VRAM, abaixo dos " + minimoGB + " GB recomendados para "
                + resolucao + ": jogos podem engasgar e usar texturas reduzidas.";
    }

    /** L09: fonte trabalhando fora da faixa de maior eficiencia. */
    public static String eficienciaFonte(Fonte fonte, double carga) {
        return "a fonte " + nome(fonte) + " vai trabalhar a " + percentual(carga)
                + " da carga (a faixa mais eficiente e 40-60%): gasta um pouco mais de"
                + " energia e esquenta mais.";
    }

    /** L10: armazenamento mais rapido que o slot/porta da placa. */
    public static String armazenamento(PlacaMae placaMae, Armazenamento armazenamento,
            String capacidadeSlot, String capacidadeMidia, double perda) {
        return "o slot da placa-mae " + nome(placaMae) + " entrega ate " + capacidadeSlot
                + " e este armazenamento chega a " + capacidadeMidia + ": perda de "
                + percentual(perda) + " de velocidade.";
    }

    /** L10 (informativo): HD mecanico ou SSD SATA em relacao ao SSD NVMe. */
    public static String midiaLenta(Armazenamento armazenamento) {
        if (armazenamento.getTipoMidia() == TipoMidia.HDD) {
            return "este HD mecanico funciona, mas e muito mais lento que um SSD NVMe"
                    + " (o sistema demora mais para iniciar e os programas carregam devagar).";
        }
        return "este SSD SATA funciona, mas e mais lento que um SSD NVMe"
                + " (a diferenca aparece no tempo de carregamento do sistema e dos jogos).";
    }

    /** L11: pistas PCIe da CPU insuficientes para GPU + armazenamento. */
    public static String pistasPcie(Processador cpu, int necessarias, int disponiveis) {
        return "o processador " + nome(cpu) + " tem " + disponiveis
                + " pistas PCIe e a configuracao pede " + necessarias
                + ": os slots restantes passam a dividir pistas do chipset (o segundo M.2"
                + " pode desativar uma porta SATA ou reduzir o slot x16 para x8).";
    }

    /** L01: processador mais fraco que a placa de video. */
    public static String gargaloCpuGpu(Processador cpu, PlacaVideo gpu) {
        String resolucao = gpu.getResolucaoAlvo() == null
                ? "a resolucao alvo"
                : gpu.getResolucaoAlvo().getRotulo();
        return "o processador " + nome(cpu) + " (indice "
                + cpu.getIndiceDesempenhoSingleThread() + ") e mais fraco que a placa de"
                + " video " + nome(gpu) + " (indice " + gpu.getIndiceDesempenhoGPU()
                + "): em " + resolucao + " a placa nao vai ser usada por completo"
                + " (gargalo de CPU).";
    }

    /** Sujeito da frase conforme a categoria da peca limitante. */
    private static String rotulo(Componente componente) {
        CategoriaComponente categoria = componente.getCategoria();
        String artigo;
        switch (categoria) {
            case PLACA_MAE:
                artigo = "A placa-mae ";
                break;
            case CPU:
                artigo = "O processador ";
                break;
            case MEMORIA:
                artigo = "A memoria ";
                break;
            default:
                artigo = "A peca ";
                break;
        }
        return artigo + nome(componente);
    }
}

