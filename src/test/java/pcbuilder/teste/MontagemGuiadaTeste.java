package pcbuilder.teste;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import pcbuilder.cli.MenuPrincipal;
import pcbuilder.cli.TabelaComponentes;
import pcbuilder.cli.TelaEtapa;
import pcbuilder.cli.Terminal;
import pcbuilder.cli.etapas.EtapaProcessador;
import pcbuilder.cli.etapas.ResultadoEtapa;
import pcbuilder.config.OrdemEtapas;
import pcbuilder.enums.CategoriaComponente;
import pcbuilder.mock.CatalogoMock;
import pcbuilder.modelo.Componente;
import pcbuilder.modelo.Montagem;
import pcbuilder.modelo.Processador;
import pcbuilder.relatorio.FormatadorMoeda;
import pcbuilder.servico.MontagemGuiada;

/**
 * Testes da HU-002: escolher, cancelar e opcao invalida na etapa 1, categoria vazia e
 * a montagem sobrevivendo ao avanco de etapa.
 *
 * <p>Execucao (sem JUnit):
 * {@code java -cp build/classes;build/classes-test pcbuilder.teste.MontagemGuiadaTeste}.</p>
 */
public final class MontagemGuiadaTeste {

    private static final CatalogoMock CATALOGO = CatalogoMock.criar();
    private static final List<Componente> PROCESSADORES =
            CATALOGO.porCategoria(CategoriaComponente.CPU);

    private MontagemGuiadaTeste() {
        // classe utilitaria: nao deve ser instanciada
    }

    public static void main(String[] args) {
        System.out.println("Testes da HU-002 - montagem guiada (etapa 1)");

        montagemGuardaPecasECusto();
        escolherProcessador();
        opcaoInvalidaReexibeLista();
        cancelarDescartaMontagem();
        cancelamentoRecusadoVoltaParaLista();
        categoriaVazia();
        montagemSobreviveAoAvancar();
        menuPrincipalEscolheProcessador();
        opcaoInvalidaNoMenu();
        tabelaEMoeda();

        System.exit(Assercao.concluir());
    }

    private static void montagemGuardaPecasECusto() {
        Componente cpu = PROCESSADORES.get(0);
        Componente placaMae = CATALOGO.porCategoria(CategoriaComponente.PLACA_MAE).get(0);

        Montagem montagem = new Montagem();
        Assercao.verdadeiro("montagem: comeca vazia", montagem.estaVazia());
        Assercao.iguais("montagem: custo zero sem pecas", BigDecimal.ZERO, montagem.custoTotal());

        montagem.adicionar(cpu);
        montagem.adicionar(placaMae);

        Assercao.iguais("montagem: uma peca por categoria", 2, montagem.quantidadePecas());
        Assercao.iguais("montagem: CPU guardada", cpu,
                montagem.obter(CategoriaComponente.CPU).orElse(null));
        Assercao.iguais("montagem: placa-mae guardada", placaMae,
                montagem.obter(CategoriaComponente.PLACA_MAE).orElse(null));
        Assercao.iguais("montagem: custo total soma os precos",
                cpu.getPrecoMedioBRL().add(placaMae.getPrecoMedioBRL()), montagem.custoTotal());

        Componente outraCpu = PROCESSADORES.get(1);
        montagem.adicionar(outraCpu);
        Assercao.iguais("montagem: trocar a CPU nao cria segunda peca", 2,
                montagem.quantidadePecas());
        Assercao.iguais("montagem: trocar a CPU substitui a anterior", outraCpu,
                montagem.obter(CategoriaComponente.CPU).orElse(null));

        montagem.limpar();
        Assercao.verdadeiro("montagem: limpar descarta as pecas", montagem.estaVazia());
    }

    private static void escolherProcessador() {
        MontagemGuiada fluxo = new MontagemGuiada();
        Saida saida = new Saida("1\n");
        ResultadoEtapa resultado = new EtapaProcessador(saida.terminal, fluxo, PROCESSADORES)
                .executar();
        String texto = saida.texto();
        Componente primeiro = PROCESSADORES.get(0);
        Componente ultimo = PROCESSADORES.get(PROCESSADORES.size() - 1);

        Assercao.iguais("etapa 1: escolher conclui a etapa", ResultadoEtapa.CONCLUIDA, resultado);
        Assercao.verdadeiro("etapa 1: cabecalho da etapa",
                texto.contains("=== Etapa 1 de 6 - Processador (obrigatorio) ==="));
        Assercao.verdadeiro("etapa 1: barra de progresso no topo",
                texto.contains(TelaEtapa.barraProgresso(1)));
        Assercao.verdadeiro("etapa 1: rodape com cancelamento",
                texto.contains(EtapaProcessador.RODAPE));
        Assercao.verdadeiro("etapa 1: cabecalho das colunas",
                texto.contains(TabelaComponentes.cabecalho()));
        Assercao.verdadeiro("etapa 1: lista todos os processadores",
                texto.contains(TabelaComponentes.nomeExibicao(ultimo)));
        Assercao.verdadeiro("etapa 1: contagem de processadores",
                texto.contains(PROCESSADORES.size() + " processadores disponiveis."));
        Assercao.verdadeiro("etapa 1: linha de apoio do processador",
                texto.contains(linhaDeApoio(primeiro)));
        Assercao.verdadeiro("etapa 1: mensagem de selecao",
                texto.contains("Processador selecionado: "
                        + TabelaComponentes.nomeExibicao(primeiro) + " - "
                        + FormatadorMoeda.reais(primeiro.getPrecoMedioBRL())));
        Assercao.iguais("etapa 1: CPU guardada na montagem", primeiro,
                fluxo.getMontagem().obter(CategoriaComponente.CPU).orElse(null));
        Assercao.iguais("etapa 1: nada mais foi escolhido", 1,
                fluxo.getMontagem().quantidadePecas());
    }

    private static void opcaoInvalidaReexibeLista() {
        MontagemGuiada fluxo = new MontagemGuiada();
        Saida saida = new Saida("abc\n" + (PROCESSADORES.size() + 1) + "\n1\n");
        ResultadoEtapa resultado = new EtapaProcessador(saida.terminal, fluxo, PROCESSADORES)
                .executar();
        String texto = saida.texto();

        Assercao.iguais("opcao invalida: etapa conclui no numero valido",
                ResultadoEtapa.CONCLUIDA, resultado);
        Assercao.iguais("opcao invalida: uma mensagem por tentativa", 2,
                contar(texto, EtapaProcessador.MENSAGEM_OPCAO_INVALIDA));
        Assercao.iguais("opcao invalida: lista reexibida a cada tentativa", 3,
                contar(texto, TabelaComponentes.cabecalho()));
        Assercao.iguais("opcao invalida: so a escolha valida entra na montagem",
                PROCESSADORES.get(0),
                fluxo.getMontagem().obter(CategoriaComponente.CPU).orElse(null));
    }

    private static void cancelarDescartaMontagem() {
        MontagemGuiada fluxo = new MontagemGuiada();
        Saida saida = new Saida("0\nS\n");
        ResultadoEtapa resultado = new EtapaProcessador(saida.terminal, fluxo, PROCESSADORES)
                .executar();

        Assercao.iguais("cancelar: resultado da etapa", ResultadoEtapa.CANCELADA, resultado);
        Assercao.verdadeiro("cancelar: pede confirmacao S/N",
                saida.texto().contains("(S/N)"));
        Assercao.verdadeiro("cancelar: avisa que a montagem foi cancelada",
                saida.texto().contains("Montagem cancelada."));
        Assercao.verdadeiro("cancelar: montagem descartada",
                fluxo.getMontagem().estaVazia());
    }

    private static void cancelamentoRecusadoVoltaParaLista() {
        MontagemGuiada fluxo = new MontagemGuiada();
        Saida saida = new Saida("0\nN\n1\n");
        ResultadoEtapa resultado = new EtapaProcessador(saida.terminal, fluxo, PROCESSADORES)
                .executar();

        Assercao.iguais("cancelar: recusar mantem o fluxo", ResultadoEtapa.CONCLUIDA, resultado);
        Assercao.iguais("cancelar: recusar reexibe a lista", 2,
                contar(saida.texto(), TabelaComponentes.cabecalho()));
        Assercao.iguais("cancelar: CPU escolhida depois de recusar",
                PROCESSADORES.get(0),
                fluxo.getMontagem().obter(CategoriaComponente.CPU).orElse(null));

        MontagemGuiada outroFluxo = new MontagemGuiada();
        Saida outraSaida = new Saida("0\nx\nS\n");
        ResultadoEtapa outroResultado =
                new EtapaProcessador(outraSaida.terminal, outroFluxo, PROCESSADORES).executar();
        Assercao.verdadeiro("cancelar: resposta diferente de S/N e recusada",
                outraSaida.texto().contains("Digite S para sim ou N para nao."));
        Assercao.iguais("cancelar: confirma na segunda tentativa",
                ResultadoEtapa.CANCELADA, outroResultado);
    }

    private static void categoriaVazia() {
        MontagemGuiada fluxo = new MontagemGuiada();
        Saida saida = new Saida("");
        ResultadoEtapa resultado = new EtapaProcessador(saida.terminal, fluxo, List.of())
                .executar();

        Assercao.iguais("categoria vazia: sem componentes", ResultadoEtapa.SEM_COMPONENTES,
                resultado);
        Assercao.verdadeiro("categoria vazia: mensagem exigida",
                saida.texto().contains(EtapaProcessador.MENSAGEM_SEM_COMPONENTES));
        Assercao.verdadeiro("categoria vazia: cabecalho da etapa exibido",
                saida.texto().contains("=== Etapa 1 de 6 - Processador (obrigatorio) ==="));
        Assercao.verdadeiro("categoria vazia: nada e escolhido automaticamente",
                fluxo.getMontagem().estaVazia());
    }

    private static void montagemSobreviveAoAvancar() {
        MontagemGuiada fluxo = new MontagemGuiada();
        Saida saida = new Saida("1\n");
        new EtapaProcessador(saida.terminal, fluxo, PROCESSADORES).executar();
        Componente cpu = fluxo.getMontagem().obter(CategoriaComponente.CPU).orElseThrow();

        fluxo.proximaEtapa();
        Assercao.iguais("fluxo: avancou para a etapa 2", 2, fluxo.etapaAtual());
        Assercao.iguais("fluxo: etapa 2 e a placa-mae", CategoriaComponente.PLACA_MAE,
                fluxo.categoriaAtual());
        Assercao.iguais("fluxo: CPU preservada depois do avanco", cpu,
                fluxo.getMontagem().obter(CategoriaComponente.CPU).orElse(null));
        Assercao.falso("fluxo: ainda nao concluiu", fluxo.concluir());
        Assercao.iguais("fluxo: total de etapas", 6, fluxo.totalEtapas());

        while (!fluxo.concluir()) {
            fluxo.proximaEtapa();
        }
        Assercao.iguais("fluxo: termina depois da etapa 6", 7, fluxo.etapaAtual());
        Assercao.verdadeiro("fluxo: etapa 6 marcada como concluida",
                fluxo.etapaConcluida(6));
        Assercao.iguais("fluxo: montagem intacta no fim", cpu,
                fluxo.getMontagem().obter(CategoriaComponente.CPU).orElse(null));

        Assercao.iguais("ordem das etapas: 1", CategoriaComponente.CPU,
                OrdemEtapas.categoria(1));
        Assercao.iguais("ordem das etapas: 6", CategoriaComponente.FONTE,
                OrdemEtapas.categoria(6));
        Assercao.iguais("ordem das etapas: numero da placa-mae", 2,
                OrdemEtapas.numeroDe(CategoriaComponente.PLACA_MAE));
        Assercao.iguais("ordem das etapas: rotulo curto da barra", "Placa-mae",
                OrdemEtapas.rotuloCurto(2));
    }

    private static void menuPrincipalEscolheProcessador() {
        Saida saida = new Saida("1\n1\n0\n");
        int codigo = new MenuPrincipal(CATALOGO, saida.terminal).executar();
        String texto = saida.texto();
        Componente primeiro = PROCESSADORES.get(0);

        Assercao.iguais("menu: codigo de saida de sucesso", 0, codigo);
        Assercao.verdadeiro("menu: opcao montar guiado",
                texto.contains("[1] Montar PC (guiado)"));
        Assercao.verdadeiro("menu: opcao sair", texto.contains("[0] Sair"));
        Assercao.verdadeiro("menu: etapa 1 exibida",
                texto.contains("=== Etapa 1 de 6 - Processador (obrigatorio) ==="));
        Assercao.verdadeiro("menu: processador escolhido e anunciado",
                texto.contains("Processador selecionado: "
                        + TabelaComponentes.nomeExibicao(primeiro)));
        Assercao.verdadeiro("menu: avancou para a etapa 2",
                texto.contains("=== Etapa 2 de 6 - Placa-Mae (obrigatorio) ==="));
        Assercao.verdadeiro("menu: barra marca a CPU como concluida",
                texto.contains("[+Processador]"));
        Assercao.verdadeiro("menu: barra marca a placa-mae como atual",
                texto.contains(">>[Placa-mae]<<"));
    }

    private static void opcaoInvalidaNoMenu() {
        Saida saida = new Saida("9\n0\n");
        int codigo = new MenuPrincipal(CATALOGO, saida.terminal).executar();

        Assercao.iguais("menu: opcao invalida nao encerra com erro", 0, codigo);
        Assercao.iguais("menu: mensagem de opcao invalida", 1,
                contar(saida.texto(), MenuPrincipal.MENSAGEM_OPCAO_INVALIDA));
        Assercao.falso("menu: opcao invalida nao abre a montagem",
                saida.texto().contains("=== Etapa 1 de 6"));
    }

    private static void tabelaEMoeda() {
        Assercao.iguais("moeda: formato pt-BR", "R$ 2.199,00",
                FormatadorMoeda.reais(new BigDecimal("2199")));
        Assercao.iguais("moeda: peca sem preco", FormatadorMoeda.SEM_PRECO,
                FormatadorMoeda.reais(null));
        Assercao.iguais("tabela: separador com 100 colunas", TabelaComponentes.LARGURA,
                TabelaComponentes.separador().length());
        Assercao.iguais("tabela: moldura com 100 colunas", TabelaComponentes.LARGURA,
                TabelaComponentes.moldura().length());

        String linha = TabelaComponentes.linha(265, "AMD Ryzen 7 9700X", "R$ 1.919,90",
                "8 nucleos | AM5 | video integrado: sim");
        Assercao.verdadeiro("tabela: linha cabe na largura fixa",
                linha.length() <= TabelaComponentes.LARGURA);
        Assercao.verdadeiro("tabela: cabecalho cabe na largura fixa",
                TabelaComponentes.cabecalho().length() <= TabelaComponentes.LARGURA);
        Assercao.verdadeiro("tabela: numero da opcao alinhado", linha.startsWith(" 265)"));
        Assercao.verdadeiro("tabela: modelo longo e cortado",
                TabelaComponentes.linha(1, "X".repeat(60), "R$ 1,00", "").contains("..."));
        Assercao.iguais("tabela: nome exibido junta fabricante e modelo",
                PROCESSADORES.get(0).getFabricante() + " " + PROCESSADORES.get(0).getModelo(),
                TabelaComponentes.nomeExibicao(PROCESSADORES.get(0)));
    }

    private static String linhaDeApoio(Componente componente) {
        Processador cpu = (Processador) componente;
        return cpu.getNucleosFisicos() + " nucleos | " + cpu.getSoquete()
                + " | video integrado: " + (cpu.isPossuiVideoIntegrado() ? "sim" : "nao");
    }

    /** Quantas vezes o trecho aparece no texto. */
    private static int contar(String texto, String trecho) {
        int total = 0;
        int posicao = texto.indexOf(trecho);
        while (posicao >= 0) {
            total++;
            posicao = texto.indexOf(trecho, posicao + trecho.length());
        }
        return total;
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
