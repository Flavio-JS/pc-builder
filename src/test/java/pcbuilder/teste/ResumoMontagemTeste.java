package pcbuilder.teste;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import pcbuilder.cli.TelaResumo;
import pcbuilder.cli.Terminal;
import pcbuilder.enums.CategoriaComponente;
import pcbuilder.enums.Certificado80Plus;
import pcbuilder.enums.Chipset;
import pcbuilder.enums.FormatoArmazenamento;
import pcbuilder.enums.FormatoPlacaMae;
import pcbuilder.enums.InterfaceArmazenamento;
import pcbuilder.enums.Soquete;
import pcbuilder.enums.TipoMemoria;
import pcbuilder.enums.TipoMidia;
import pcbuilder.modelo.Armazenamento;
import pcbuilder.modelo.Componente;
import pcbuilder.modelo.Fonte;
import pcbuilder.modelo.Limitacao;
import pcbuilder.modelo.Memoria;
import pcbuilder.modelo.Montagem;
import pcbuilder.modelo.PlacaMae;
import pcbuilder.modelo.Processador;
import pcbuilder.relatorio.FormatadorMoeda;
import pcbuilder.relatorio.ResumoMontagem;
import pcbuilder.relatorio.TabelaResumo;

/**
 * Testes da HU-010: resumo final com pecas, precos, total, avisos e comandos.
 *
 * <p>Execucao (sem JUnit):
 * {@code java -cp build/classes;build/classes-test pcbuilder.teste.ResumoMontagemTeste}.</p>
 */
public final class ResumoMontagemTeste {

    private ResumoMontagemTeste() {
        // classe utilitaria: nao deve ser instanciada
    }

    public static void main(String[] args) {
        System.out.println("Testes da HU-010 - resumo final");

        resumoCompletoSemGpu();
        resumoComPecaSemPreco();
        pendenciasDaMontagem();
        tabelaEFormatacao();
        telaDoResumo();
        comandosDoResumo();

        System.exit(Assercao.concluir());
    }

    // ------------------------------------------------------------- montagem

    private static void resumoCompletoSemGpu() {
        Montagem montagem = montagem(cpu(true, "1000.00"), placa("500.00"),
                memoria("300.00"), ssd("200.00"), fonte("400.00"));

        ResumoMontagem resumo = new ResumoMontagem(montagem);

        Assercao.iguais("resumo: seis linhas", 6, resumo.getLinhas().size());
        Assercao.iguais("resumo: total soma as pecas", new BigDecimal("2400.00"),
                resumo.getTotal());
        Assercao.verdadeiro("resumo: completa sem GPU quando a CPU tem iGPU",
                resumo.completa());
        Assercao.iguais("resumo: sem pendencias", "", resumo.descricaoPendencias());
        Assercao.falso("resumo: sem peca sem preco", resumo.possuiPecaSemPreco());

        ResumoMontagem.LinhaResumo linhaCpu = resumo.getLinhas().get(0);
        Assercao.iguais("resumo: etapa 1 e a CPU", CategoriaComponente.CPU,
                linhaCpu.getCategoria());
        Assercao.verdadeiro("resumo: CPU selecionada", linhaCpu.isSelecionada());
        Assercao.iguais("resumo: nome da CPU", "AMD Ryzen Teste", linhaCpu.getPeca());
        Assercao.iguais("resumo: preco sem simbolo na coluna", "1.000,00",
                linhaCpu.getPrecoFormatado());

        ResumoMontagem.LinhaResumo linhaGpu = resumo.getLinhas().get(3);
        Assercao.iguais("resumo: etapa 4 e a GPU", CategoriaComponente.PLACA_VIDEO,
                linhaGpu.getCategoria());
        Assercao.falso("resumo: GPU nao selecionada", linhaGpu.isSelecionada());
        Assercao.iguais("resumo: GPU pulada", ResumoMontagem.NAO_SELECIONADO,
                linhaGpu.getPeca());
        Assercao.iguais("resumo: preco da etapa pulada", "R$ --",
                linhaGpu.getPrecoFormatado());
        Assercao.iguais("resumo: categoria da ultima etapa", CategoriaComponente.FONTE,
                resumo.getLinhas().get(5).getCategoria());
    }

    private static void resumoComPecaSemPreco() {
        Montagem montagem = montagem(cpu(true, null), placa("500.00"),
                memoria("300.00"), ssd("200.00"), fonte("400.00"));

        ResumoMontagem resumo = new ResumoMontagem(montagem);

        Assercao.verdadeiro("resumo: detecta peca sem preco", resumo.possuiPecaSemPreco());
        Assercao.iguais("resumo: total ignora a peca sem preco", new BigDecimal("1400.00"),
                resumo.getTotal());
        Assercao.iguais("resumo: preco ausente na coluna", "R$ --",
                resumo.getLinhas().get(0).getPrecoFormatado());
    }

    // ------------------------------------------------------------- pendencias

    private static void pendenciasDaMontagem() {
        ResumoMontagem comVideo = new ResumoMontagem(montagem(cpu(true, "1000.00")));
        Assercao.falso("pendencias: montagem incompleta", comVideo.completa());
        List<CategoriaComponente> pendencias = comVideo.getPendencias();
        Assercao.verdadeiro("pendencias: placa-mae",
                pendencias.contains(CategoriaComponente.PLACA_MAE));
        Assercao.verdadeiro("pendencias: memoria",
                pendencias.contains(CategoriaComponente.MEMORIA));
        Assercao.verdadeiro("pendencias: armazenamento",
                pendencias.contains(CategoriaComponente.ARMAZENAMENTO));
        Assercao.verdadeiro("pendencias: fonte",
                pendencias.contains(CategoriaComponente.FONTE));
        Assercao.falso("pendencias: GPU opcional nao e pendencia",
                pendencias.contains(CategoriaComponente.PLACA_VIDEO));
        Assercao.verdadeiro("pendencias: descricao com os rotulos",
                comVideo.descricaoPendencias().contains("Placa-Mae")
                        && comVideo.descricaoPendencias().contains("Fonte"));

        ResumoMontagem semVideo = new ResumoMontagem(montagem(cpu(false, "1000.00")));
        Assercao.verdadeiro("pendencias: GPU obrigatoria sem iGPU",
                semVideo.getPendencias().contains(CategoriaComponente.PLACA_VIDEO));
    }

    // ---------------------------------------------------------------- tabela

    private static void tabelaEFormatacao() {
        Assercao.iguais("tabela: separador com 100 colunas", 100,
                TabelaResumo.separador().length());
        Assercao.iguais("tabela: cabecalho com 100 colunas", 100,
                TabelaResumo.cabecalho().length());
        Assercao.iguais("tabela: linha com 100 colunas", 100,
                TabelaResumo.linha(1, "Processador", "AMD Ryzen 7 9700X", "2.199,00").length());
        String cabecalho = TabelaResumo.cabecalho();
        Assercao.verdadeiro("tabela: colunas do cabecalho",
                cabecalho.contains("Etapa") && cabecalho.contains("Categoria")
                        && cabecalho.contains("Peca escolhida")
                        && cabecalho.contains("Preco (R$)"));
        Assercao.iguais("tabela: total formatado", "TOTAL: R$ 5.345,00",
                TabelaResumo.total("R$ 5.345,00"));

        Assercao.iguais("moeda: numero sem simbolo", "2.199,00",
                FormatadorMoeda.numero(new BigDecimal("2199")));
        Assercao.iguais("moeda: reais com simbolo", "R$ 2.199,00",
                FormatadorMoeda.reais(new BigDecimal("2199")));
        Assercao.iguais("moeda: numero nulo", FormatadorMoeda.SEM_PRECO,
                FormatadorMoeda.numero(null));
    }

    // ------------------------------------------------------------- tela

    private static void telaDoResumo() {
        Montagem montagem = montagem(cpu(true, "1000.00"), placa("500.00"),
                memoria("300.00"), ssd("200.00"), fonte("400.00"));
        montagem.registrarLimitacoes(List.of(limitacaoDeMemoria(montagem)));
        ResumoMontagem resumo = new ResumoMontagem(montagem);

        Saida saida = new Saida("");
        new TelaResumo(saida.terminal).imprimir(resumo);
        String texto = saida.texto();
        Assercao.verdadeiro("tela: titulo do resumo", texto.contains(TelaResumo.TITULO));
        Assercao.verdadeiro("tela: colunas do resumo",
                texto.contains("Etapa") && texto.contains("Categoria")
                        && texto.contains("Peca escolhida")
                        && texto.contains("Preco (R$)"));
        Assercao.verdadeiro("tela: etapa pulada aparece", texto.contains("(nao selecionado)"));
        Assercao.verdadeiro("tela: total com simbolo",
                texto.contains("TOTAL: R$ 2.400,00"));
        Assercao.verdadeiro("tela: bloco de avisos", texto.contains("Avisos: 1"));
        Assercao.verdadeiro("tela: linha curta do aviso",
                texto.contains("[!]") && texto.contains("frequenciaMTs")
                        && texto.contains("8000 MT/s"));
        Assercao.verdadeiro("tela: rodape de comandos", texto.contains(TelaResumo.RODAPE));
        Assercao.verdadeiro("tela: oferece trocar, nova e sair",
                texto.contains("[T] Trocar uma peca") && texto.contains("[N] Nova montagem")
                        && texto.contains("[0] Sair"));

        ResumoMontagem semPreco = new ResumoMontagem(montagem(cpu(true, null),
                placa("500.00"), memoria("300.00"), ssd("200.00"), fonte("400.00")));
        Saida saidaSemPreco = new Saida("");
        new TelaResumo(saidaSemPreco.terminal).imprimir(semPreco);
        Assercao.verdadeiro("tela: nota de peca sem preco",
                saidaSemPreco.texto().contains(TelaResumo.NOTA_SEM_PRECO));

        Saida pendencias = new Saida("");
        new TelaResumo(pendencias.terminal)
                .imprimirPendencias(new ResumoMontagem(montagem(cpu(true, "1000.00"))));
        Assercao.verdadeiro("tela: mensagem de pendencias",
                pendencias.texto().contains("Complete as etapas pendentes:"));
        Assercao.verdadeiro("tela: pendencia nomeia a placa-mae",
                pendencias.texto().contains("Placa-Mae"));
    }

    private static void comandosDoResumo() {
        ResumoMontagem resumo = new ResumoMontagem(montagem(cpu(true, "1000.00"),
                placa("500.00"), memoria("300.00"), ssd("200.00"), fonte("400.00")));

        Saida nova = new Saida("N\n");
        Assercao.iguais("resumo: [N] pede nova montagem",
                TelaResumo.Resultado.NOVA_MONTAGEM,
                new TelaResumo(nova.terminal).executar(resumo));

        Saida sair = new Saida("0\n");
        Assercao.iguais("resumo: [0] encerra", TelaResumo.Resultado.SAIR,
                new TelaResumo(sair.terminal).executar(resumo));

        Saida troca = new Saida("T\n0\n");
        Assercao.iguais("resumo: [T] depois [0] encerra", TelaResumo.Resultado.SAIR,
                new TelaResumo(troca.terminal).executar(resumo));
        Assercao.verdadeiro("resumo: [T] explica a HU-011",
                troca.texto().contains(TelaResumo.MENSAGEM_TROCA));

        Saida invalida = new Saida("X\n0\n");
        Assercao.iguais("resumo: comando invalido nao encerra antes", TelaResumo.Resultado.SAIR,
                new TelaResumo(invalida.terminal).executar(resumo));
        Assercao.verdadeiro("resumo: mensagem de comando invalido",
                invalida.texto().contains(TelaResumo.MENSAGEM_OPCAO_INVALIDA));
    }

    // ---------------------------------------------------------------- auxiliares

    private static Processador cpu(boolean videoIntegrado, String preco) {
        return Processador.builder()
                .id("cpu-resumo-" + videoIntegrado)
                .modelo("Ryzen Teste")
                .fabricante("AMD")
                .precoMedioBRL(preco)
                .soquete(Soquete.AM5)
                .nucleosFisicos(8)
                .threads(16)
                .frequenciaBaseMHz(3800)
                .frequenciaBoostMHz(5500)
                .tdpWatts(65)
                .consumoMaximoWatts(100)
                .tiposMemoriaSuportados(TipoMemoria.DDR5)
                .canaisMemoria(2)
                .frequenciaMemoriaNativaMTs(5200)
                .capacidadeMaximaMemoriaGB(192)
                .possuiVideoIntegrado(videoIntegrado)
                .construir();
    }

    private static PlacaMae placa(String preco) {
        return PlacaMae.builder()
                .id("placa-resumo")
                .modelo("B650 Teste")
                .fabricante("ASUS")
                .precoMedioBRL(preco)
                .soqueteCPU(Soquete.AM5)
                .chipset(Chipset.B650)
                .formato(FormatoPlacaMae.MATX)
                .tipoMemoria(TipoMemoria.DDR5)
                .quantidadeSlotsMemoria(4)
                .capacidadeMaximaMemoriaGB(192)
                .construir();
    }

    private static Memoria memoria(String preco) {
        return Memoria.builder()
                .id("ram-resumo")
                .modelo("Vengeance Teste")
                .fabricante("CORSAIR")
                .precoMedioBRL(preco)
                .tipo(TipoMemoria.DDR5)
                .capacidadePorPenteGB(16)
                .quantidadePentes(2)
                .frequenciaMTs(6000)
                .latenciaCAS(30)
                .construir();
    }

    private static Armazenamento ssd(String preco) {
        return Armazenamento.builder()
                .id("ssd-resumo")
                .modelo("SSD Teste")
                .fabricante("Kingston")
                .precoMedioBRL(preco)
                .tipoMidia(TipoMidia.SSD)
                .interfaceArmazenamento(InterfaceArmazenamento.NVME)
                .formato(FormatoArmazenamento.M2_2280)
                .capacidadeGB(2000)
                .leituraSequencialMBs(7000)
                .construir();
    }

    private static Fonte fonte(String preco) {
        return Fonte.builder()
                .id("fonte-resumo")
                .modelo("Fonte Teste")
                .fabricante("Corsair")
                .precoMedioBRL(preco)
                .potenciaTotalWatts(750)
                .potenciaTrilho12VWatts(744)
                .certificacao80Plus(Certificado80Plus.GOLD)
                .construir();
    }

    private static Montagem montagem(Componente... pecas) {
        Montagem montagem = new Montagem();
        for (Componente peca : pecas) {
            montagem.adicionar(peca);
        }
        return montagem;
    }

    private static Limitacao limitacaoDeMemoria(Montagem montagem) {
        Componente placaMae = montagem.obter(CategoriaComponente.PLACA_MAE)
                .orElseThrow(() -> new IllegalStateException("montagem sem placa-mae"));
        Componente memoria = montagem.obter(CategoriaComponente.MEMORIA)
                .orElseThrow(() -> new IllegalStateException("montagem sem memoria"));
        return Limitacao.builder()
                .regra("L04")
                .limitante(placaMae)
                .afetado(memoria)
                .campo("frequenciaMTs")
                .valorDoNovo("8000 MT/s")
                .valorSuportado("6000 MT/s")
                .consequencia("a memoria vai rodar a 6000 MT/s")
                .construir();
    }

    /** Terminal com a entrada digitada e a saida capturada em memoria. */
    private static final class Saida {

        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        private final Terminal terminal;

        Saida(String entrada) {
            PrintStream stream = new PrintStream(bytes, true, StandardCharsets.UTF_8);
            this.terminal = new Terminal(new StringReader(entrada), stream);
        }

        String texto() {
            return bytes.toString(StandardCharsets.UTF_8);
        }
    }
}
