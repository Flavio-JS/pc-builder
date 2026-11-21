package pcbuilder.teste;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import pcbuilder.analise.filtro.FiltroEtapa;
import pcbuilder.analise.filtro.ListaFiltrada;
import pcbuilder.analise.limitacao.AvaliadorLimitacoes;
import pcbuilder.cli.MenuPrincipal;
import pcbuilder.cli.TabelaComponentes;
import pcbuilder.cli.Terminal;
import pcbuilder.cli.etapas.EtapaArmazenamento;
import pcbuilder.cli.etapas.ResultadoEtapa;
import pcbuilder.enums.CategoriaComponente;
import pcbuilder.enums.Chipset;
import pcbuilder.enums.FormatoArmazenamento;
import pcbuilder.enums.FormatoPlacaMae;
import pcbuilder.enums.InterfaceArmazenamento;
import pcbuilder.enums.Soquete;
import pcbuilder.enums.TipoMemoria;
import pcbuilder.enums.TipoMidia;
import pcbuilder.mock.CatalogoMock;
import pcbuilder.modelo.Armazenamento;
import pcbuilder.modelo.Componente;
import pcbuilder.modelo.Limitacao;
import pcbuilder.modelo.Memoria;
import pcbuilder.modelo.Montagem;
import pcbuilder.modelo.PlacaMae;
import pcbuilder.modelo.Processador;
import pcbuilder.servico.MontagemGuiada;
import pcbuilder.servico.resumos.ResumoArmazenamento;

/**
 * Testes da HU-008: etapa 5 (armazenamento) com SSD e HD juntos, filtro C04, avisos L10
 * (banda e midia lenta), {@code [M] Ver ocultos} e a mensagem de selecao.
 *
 * <p>Execucao (sem JUnit):
 * {@code java -cp build/classes;build/classes-test pcbuilder.teste.EtapaArmazenamentoTeste}.</p>
 */
public final class EtapaArmazenamentoTeste {

    private static final CatalogoMock CATALOGO = CatalogoMock.criar();
    private static final FiltroEtapa FILTRO = new FiltroEtapa(CATALOGO);
    private static final AvaliadorLimitacoes AVALIADOR = new AvaliadorLimitacoes();

    private EtapaArmazenamentoTeste() {
        // classe utilitaria: nao deve ser instanciada
    }

    public static void main(String[] args) {
        System.out.println("Testes da HU-008 - etapa 5 (armazenamento)");

        resumoDaLinhaDeApoio();
        listaUneSsdEHd();
        nvmeOcultoSemSlotM2();
        sataOcultoSemPorta();
        listaVaziaComMotivo();
        avisoDeBandaDoSlot();
        avisoDeMidiaLenta();
        menuEscolheArmazenamento();

        System.exit(Assercao.concluir());
    }

    // ------------------------------------------------------------ linha de apoio

    private static void resumoDaLinhaDeApoio() {
        Armazenamento nvme = armazenamento(TipoMidia.SSD, InterfaceArmazenamento.NVME,
                FormatoArmazenamento.M2_2280, 2000, 7000);
        Assercao.iguais("resumo: SSD NVMe 2 TB",
                "SSD NVMe M.2 2280 | 2 TB | 7000 MB/s",
                ResumoArmazenamento.linhaDeApoio(nvme));

        Armazenamento hdd = armazenamento(TipoMidia.HDD, InterfaceArmazenamento.SATA,
                FormatoArmazenamento.SATA_3_5, 2000, 200);
        Assercao.iguais("resumo: HD SATA 3.5\"",
                "HD SATA 3.5\" | 2 TB | 200 MB/s",
                ResumoArmazenamento.linhaDeApoio(hdd));

        Armazenamento pequeno = armazenamento(TipoMidia.SSD, InterfaceArmazenamento.NVME,
                FormatoArmazenamento.M2_2280, 512, 3400);
        Assercao.iguais("resumo: capacidade em GB",
                "SSD NVMe M.2 2280 | 512 GB | 3400 MB/s",
                ResumoArmazenamento.linhaDeApoio(pequeno));

        Assercao.iguais("resumo: componente que nao e armazenamento", "",
                ResumoArmazenamento.linhaDeApoio(cpu(true)));
    }

    // -------------------------------------------------------- C04 (lista unica)

    private static void listaUneSsdEHd() {
        Montagem montagem = montagem(cpu(true), placa(2, 4, 4), memoria());
        MontagemGuiada fluxo = fluxoNaEtapa5(montagem);
        ListaFiltrada lista = FILTRO.filtrar(CategoriaComponente.ARMAZENAMENTO, montagem);

        Assercao.verdadeiro("etapa 5: a lista tem SSD",
                lista.getCompativeis().stream().map(Armazenamento.class::cast)
                        .anyMatch(item -> item.getTipoMidia() == TipoMidia.SSD));
        Assercao.verdadeiro("etapa 5: a lista tem HD",
                lista.getCompativeis().stream().map(Armazenamento.class::cast)
                        .anyMatch(item -> item.getTipoMidia() == TipoMidia.HDD));

        Componente alvo = lista.getCompativeis().get(0);
        List<Limitacao> avisos = AVALIADOR.avaliar(montagem, alvo);
        Saida saida = new Saida("1\n" + (avisos.isEmpty() ? "" : "\n"));
        ResultadoEtapa resultado = new EtapaArmazenamento(saida.terminal, fluxo, FILTRO)
                .executar();

        Assercao.iguais("etapa 5: escolher a primeira midia conclui",
                ResultadoEtapa.CONCLUIDA, resultado);
        Assercao.iguais("etapa 5: midia guardada na montagem", alvo,
                montagem.obter(CategoriaComponente.ARMAZENAMENTO).orElse(null));

        String texto = saida.texto();
        Assercao.verdadeiro("etapa 5: cabecalho",
                texto.contains("=== Etapa 5 de 6 - Armazenamento (obrigatorio) ==="));
        Assercao.verdadeiro("etapa 5: barra marca as etapas anteriores",
                texto.contains("[+Placa-mae]") && texto.contains(">>[Armazenamento]<<"));
        Assercao.verdadeiro("etapa 5: linha de apoio da midia",
                texto.contains(apoioAlinhado(lista, alvo)));
        Assercao.verdadeiro("etapa 5: mensagem de selecao",
                texto.contains("Armazenamento selecionado: "
                        + TabelaComponentes.nomeExibicao(alvo)));
        Assercao.verdadeiro("etapa 5: rodape oferece [0] Voltar",
                texto.contains(EtapaArmazenamento.RODAPE));
    }

    // ------------------------------------------------- C04 (slots e portas)

    private static void nvmeOcultoSemSlotM2() {
        Montagem montagem = montagem(cpu(true), placa(0, 4, 4), memoria());
        MontagemGuiada fluxo = fluxoNaEtapa5(montagem);

        Saida saida = new Saida("M\n0\n");
        ResultadoEtapa resultado = new EtapaArmazenamento(saida.terminal, fluxo, FILTRO)
                .executar();

        Assercao.iguais("C04: sem slot M.2 o [0] volta", ResultadoEtapa.VOLTAR, resultado);
        Assercao.verdadeiro("C04: motivo do slot M.2",
                saida.texto().contains("placa-mae sem slot M.2 livre"));

        ListaFiltrada lista = FILTRO.filtrar(CategoriaComponente.ARMAZENAMENTO, montagem);
        Assercao.verdadeiro("C04: so SATA fica visivel sem slot M.2",
                lista.getCompativeis().stream().map(Armazenamento.class::cast)
                        .allMatch(item -> item.getInterfaceArmazenamento()
                                == InterfaceArmazenamento.SATA));
    }

    private static void sataOcultoSemPorta() {
        Montagem montagem = montagem(cpu(true), placa(2, 4, 0), memoria());
        MontagemGuiada fluxo = fluxoNaEtapa5(montagem);

        Saida saida = new Saida("M\n0\n");
        ResultadoEtapa resultado = new EtapaArmazenamento(saida.terminal, fluxo, FILTRO)
                .executar();

        Assercao.iguais("C04: sem porta SATA o [0] volta", ResultadoEtapa.VOLTAR, resultado);
        Assercao.verdadeiro("C04: motivo da porta SATA",
                saida.texto().contains("placa-mae sem porta SATA livre"));

        ListaFiltrada lista = FILTRO.filtrar(CategoriaComponente.ARMAZENAMENTO, montagem);
        Assercao.verdadeiro("C04: so NVMe fica visivel sem porta SATA",
                lista.getCompativeis().stream().map(Armazenamento.class::cast)
                        .allMatch(item -> item.getInterfaceArmazenamento()
                                == InterfaceArmazenamento.NVME));
    }

    private static void listaVaziaComMotivo() {
        Montagem montagem = montagem(cpu(true), placa(0, 4, 0), memoria());
        MontagemGuiada fluxo = fluxoNaEtapa5(montagem);

        Saida saida = new Saida("0\n");
        ResultadoEtapa resultado = new EtapaArmazenamento(saida.terminal, fluxo, FILTRO)
                .executar();

        Assercao.iguais("C04: sem midia compativel a etapa volta", ResultadoEtapa.VOLTAR,
                resultado);
        String texto = saida.texto();
        Assercao.verdadeiro("C04: mensagem de nenhum compativel",
                texto.contains(pcbuilder.cli.TelaEtapa.MENSAGEM_SEM_COMPATIVEIS));
        Assercao.verdadeiro("C04: motivo predominante da midia",
                texto.contains("sem slot M.2 livre") || texto.contains("sem porta SATA livre"));
        Assercao.verdadeiro("C04: oferece [0] Voltar", texto.contains("[0] Voltar"));
    }

    // ---------------------------------------------------------- L10 (avisos)

    private static void avisoDeBandaDoSlot() {
        Montagem montagem = montagem(cpu(true), placa(1, 3, 4), memoria());
        MontagemGuiada fluxo = fluxoNaEtapa5(montagem);
        ListaFiltrada lista = FILTRO.filtrar(CategoriaComponente.ARMAZENAMENTO, montagem);

        int numero = 0;
        Armazenamento alvo = null;
        for (int indice = 0; indice < lista.getCompativeis().size(); indice++) {
            Armazenamento item = (Armazenamento) lista.getCompativeis().get(indice);
            boolean perdeBanda = item.getInterfaceArmazenamento() == InterfaceArmazenamento.NVME
                    && item.getVersaoPCIe() != null && item.getVersaoPCIe() >= 4
                    && AVALIADOR.avaliar(montagem, item).stream().anyMatch(limitacao ->
                            "L10".equals(limitacao.getRegraId())
                                    && "leituraSequencialMBs".equals(
                                            limitacao.getCampoComparado()));
            if (perdeBanda) {
                numero = indice + 1;
                alvo = item;
                break;
            }
        }
        Assercao.verdadeiro("L10: existe NVMe de geracao maior que o slot", alvo != null);

        Saida saida = new Saida(numero + "\n\n");
        ResultadoEtapa resultado = new EtapaArmazenamento(saida.terminal, fluxo, FILTRO)
                .executar();

        Assercao.iguais("L10: SSD com perda de banda continua selecionavel",
                ResultadoEtapa.CONCLUIDA, resultado);
        String texto = saida.texto();
        Assercao.verdadeiro("L10: linha marcada com [!]",
                linhaCom(texto, apoioAlinhado(lista, alvo))
                        .startsWith(TabelaComponentes.MARCADOR_LIMITACAO));
        Assercao.verdadeiro("L10: aviso cita a perda de velocidade",
                texto.contains("perda de") && texto.contains("velocidade"));
        Assercao.verdadeiro("L10: aviso acumulado na montagem",
                montagem.getLimitacoes().stream().anyMatch(limitacao ->
                        "L10".equals(limitacao.getRegraId())
                                && "leituraSequencialMBs".equals(limitacao.getCampoComparado())));
    }

    private static void avisoDeMidiaLenta() {
        Montagem montagem = montagem(cpu(true), placa(2, 4, 4), memoria());
        MontagemGuiada fluxo = fluxoNaEtapa5(montagem);
        ListaFiltrada lista = FILTRO.filtrar(CategoriaComponente.ARMAZENAMENTO, montagem);

        int numero = 0;
        Armazenamento hdd = null;
        for (int indice = 0; indice < lista.getCompativeis().size(); indice++) {
            Armazenamento item = (Armazenamento) lista.getCompativeis().get(indice);
            if (item.getTipoMidia() == TipoMidia.HDD) {
                numero = indice + 1;
                hdd = item;
                break;
            }
        }
        Assercao.verdadeiro("L10: existe HD compativel na lista", hdd != null);

        Saida saida = new Saida(numero + "\n\n");
        ResultadoEtapa resultado = new EtapaArmazenamento(saida.terminal, fluxo, FILTRO)
                .executar();

        Assercao.iguais("L10: HD continua selecionavel", ResultadoEtapa.CONCLUIDA, resultado);
        String texto = saida.texto();
        Assercao.verdadeiro("L10: aviso do HD mecanico",
                texto.contains("HD mecanico") && texto.contains("lento"));
        Assercao.verdadeiro("L10: aviso de midia acumulado",
                montagem.getLimitacoes().stream().anyMatch(limitacao ->
                        "L10".equals(limitacao.getRegraId())
                                && "tipoMidia".equals(limitacao.getCampoComparado())));

        Armazenamento sata = lista.getCompativeis().stream().map(Armazenamento.class::cast)
                .filter(item -> item.getTipoMidia() == TipoMidia.SSD
                        && item.getInterfaceArmazenamento() == InterfaceArmazenamento.SATA)
                .findFirst()
                .orElse(null);
        Assercao.verdadeiro("L10: SSD SATA tambem e informado",
                sata != null && AVALIADOR.avaliar(montagem, sata).stream()
                        .anyMatch(limitacao -> "L10".equals(limitacao.getRegraId())
                                && "tipoMidia".equals(limitacao.getCampoComparado())
                                && limitacao.getConsequencia().contains("SSD SATA")));
    }

    // ------------------------------------------------------- fluxo pelo menu

    private static void menuEscolheArmazenamento() {
        int indiceCpu = indiceCpu(true);
        Processador cpu = cpuDoCatalogo(true);
        Componente placa = FILTRO.filtrar(CategoriaComponente.PLACA_MAE, montagem(cpu))
                .getCompativeis().get(0);
        Montagem base = montagem(cpu, placa);
        Componente memoria = FILTRO.filtrar(CategoriaComponente.MEMORIA, base)
                .getCompativeis().get(0);
        Montagem comMemoria = montagem(cpu, placa, memoria);
        Componente armazenamento = FILTRO.filtrar(CategoriaComponente.ARMAZENAMENTO,
                comMemoria).getCompativeis().get(0);

        boolean avisoPlaca = !AVALIADOR.avaliar(montagem(cpu), placa).isEmpty();
        boolean avisoMemoria = !AVALIADOR.avaliar(base, memoria).isEmpty();
        boolean avisoArmazenamento = !AVALIADOR.avaliar(comMemoria, armazenamento).isEmpty();

        String entrada = "1\n" + indiceCpu + "\n1\n" + (avisoPlaca ? "\n" : "")
                + "1\n" + (avisoMemoria ? "\n" : "") + "0\n"
                + "1\n" + (avisoArmazenamento ? "\n" : "");
        Saida saida = new Saida(entrada);
        int codigo = new MenuPrincipal(CATALOGO, saida.terminal).executar();

        Assercao.iguais("menu: execucao termina sem erro", 0, codigo);
        String texto = saida.texto();
        Assercao.verdadeiro("menu: etapa 5 exibida",
                texto.contains("=== Etapa 5 de 6 - Armazenamento (obrigatorio) ==="));
        Assercao.verdadeiro("menu: armazenamento confirmado",
                texto.contains("Armazenamento selecionado: "
                        + TabelaComponentes.nomeExibicao(armazenamento) + " - "
                        + pcbuilder.relatorio.FormatadorMoeda.reais(
                                armazenamento.getPrecoMedioBRL())));
        Assercao.verdadeiro("menu: avanca para a etapa 6",
                texto.contains("=== Etapa 6 de 6 - Fonte (obrigatorio) ==="));
    }

    // ---------------------------------------------------------------- auxiliares

    private static Processador cpu(boolean videoIntegrado) {
        return Processador.builder()
                .id("cpu-teste-arm-" + videoIntegrado)
                .modelo("Ryzen Teste Armazenamento")
                .fabricante("AMD")
                .soquete(Soquete.AM5)
                .nucleosFisicos(8)
                .threads(16)
                .frequenciaBaseMHz(3800)
                .frequenciaBoostMHz(5500)
                .tdpWatts(65)
                .consumoMaximoWatts(100)
                .tensaoMaximaVolts(1.25)
                .tiposMemoriaSuportados(TipoMemoria.DDR5)
                .canaisMemoria(2)
                .frequenciaMemoriaNativaMTs(5200)
                .frequenciaMaximaSincronaMTs(6000)
                .capacidadeMaximaMemoriaGB(192)
                .pistasPCIeLivres(24)
                .possuiVideoIntegrado(videoIntegrado)
                .indiceDesempenhoSingleThread(90)
                .indiceDesempenhoMultithread(90)
                .construir();
    }

    private static PlacaMae placa(int slotsM2, int geracaoM2, int portasSata) {
        return PlacaMae.builder()
                .id("placa-arm-" + slotsM2 + "-" + geracaoM2 + "-" + portasSata)
                .modelo("B650 Teste")
                .fabricante("ASUS")
                .soqueteCPU(Soquete.AM5)
                .chipset(Chipset.B650)
                .formato(FormatoPlacaMae.MATX)
                .quantidadeSlotsMemoria(4)
                .tipoMemoria(TipoMemoria.DDR5)
                .canaisMemoria(2)
                .capacidadeMaximaMemoriaGB(192)
                .frequenciaMemoriaMaximaMTs(6000)
                .suportaPerfilEXPO(true)
                .suportaPerfilXMP(true)
                .vrmFasesCPU(8)
                .vrmCorrentePorFaseA(50)
                .vrmPossuiDissipador(true)
                .quantidadeSlotsM2(slotsM2)
                .geracaoMaximaSlotM2(geracaoM2)
                .geracaoMinimaSlotM2(geracaoM2)
                .quantidadePortasSATA(portasSata)
                .velocidadeSATAGbps(6)
                .quantidadeSlotsPCIe16(1)
                .versaoSlotPCIe16(4)
                .pistasSlotPCIe16(16)
                .construir();
    }

    private static Memoria memoria() {
        return Memoria.builder()
                .id("ram-teste-armazenamento")
                .modelo("Kit Teste 2x16")
                .fabricante("Teste")
                .tipo(TipoMemoria.DDR5)
                .capacidadePorPenteGB(16)
                .quantidadePentes(2)
                .frequenciaMTs(6000)
                .frequenciaJEDECBaseMTs(4800)
                .latenciaCAS(30)
                .tensaoVolts(1.35)
                .perfilEXPO(true)
                .perfilXMP(true)
                .possuiECC(false)
                .alturaMm(44)
                .consumoEstimadoWatts(6)
                .construir();
    }

    private static Armazenamento armazenamento(TipoMidia tipo,
            InterfaceArmazenamento conexao, FormatoArmazenamento formato, int capacidadeGB,
            int leituraMBs) {
        Armazenamento.Builder construtor = Armazenamento.builder()
                .id("arm-teste-" + tipo + "-" + conexao + "-" + capacidadeGB)
                .modelo("Midia Teste " + capacidadeGB)
                .fabricante("Teste")
                .tipoMidia(tipo)
                .interfaceArmazenamento(conexao)
                .formato(formato)
                .capacidadeGB(capacidadeGB)
                .leituraSequencialMBs(leituraMBs)
                .escritaSequencialMBs(leituraMBs)
                .consumoEstimadoWatts(5);
        if (conexao == InterfaceArmazenamento.NVME) {
            construtor.versaoPCIe(4).pistasPCIe(4);
        } else {
            construtor.velocidadeSATAGbps(6);
        }
        return construtor.construir();
    }

    private static Montagem montagem(Componente... pecas) {
        Montagem montagem = new Montagem();
        for (Componente peca : pecas) {
            montagem.adicionar(peca);
        }
        return montagem;
    }

    private static MontagemGuiada fluxoNaEtapa5(Montagem montagem) {
        MontagemGuiada fluxo = new MontagemGuiada(montagem);
        fluxo.proximaEtapa();
        fluxo.proximaEtapa();
        fluxo.proximaEtapa();
        fluxo.proximaEtapa();
        return fluxo;
    }

    private static Processador cpuDoCatalogo(boolean videoIntegrado) {
        return CATALOGO.porCategoria(CategoriaComponente.CPU).stream()
                .map(Processador.class::cast)
                .filter(cpu -> cpu.isPossuiVideoIntegrado() == videoIntegrado)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "base sem CPU com videoIntegrado=" + videoIntegrado));
    }

    private static int indiceCpu(boolean videoIntegrado) {
        List<Componente> cpus = CATALOGO.porCategoria(CategoriaComponente.CPU);
        for (int indice = 0; indice < cpus.size(); indice++) {
            if (((Processador) cpus.get(indice)).isPossuiVideoIntegrado() == videoIntegrado) {
                return indice + 1;
            }
        }
        throw new IllegalStateException("base sem CPU com videoIntegrado=" + videoIntegrado);
    }

    /** Linha de apoio do alvo, com as colunas alinhadas como na tela. */
    private static String apoioAlinhado(ListaFiltrada lista, Componente alvo) {
        int indice = lista.getCompativeis().indexOf(alvo);
        List<String> apoios = lista.getCompativeis().stream()
                .map(ResumoArmazenamento::linhaDeApoio).toList();
        return TabelaComponentes.alinharApoios(apoios).get(indice);
    }

    private static String linhaCom(String texto, String trecho) {
        for (String linha : texto.split("\\R")) {
            if (linha.contains(trecho)) {
                return linha;
            }
        }
        return "";
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
