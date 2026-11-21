package pcbuilder.teste;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import pcbuilder.analise.filtro.FiltroEtapa;
import pcbuilder.analise.filtro.ListaFiltrada;
import pcbuilder.analise.limitacao.AvaliadorLimitacoes;
import pcbuilder.analise.limitacao.TextosLimitacao;
import pcbuilder.cli.MenuPrincipal;
import pcbuilder.cli.TabelaComponentes;
import pcbuilder.cli.Terminal;
import pcbuilder.cli.etapas.EtapaPlacaMae;
import pcbuilder.cli.etapas.ResultadoEtapa;
import pcbuilder.enums.CategoriaComponente;
import pcbuilder.enums.Chipset;
import pcbuilder.enums.FormatoPlacaMae;
import pcbuilder.enums.Soquete;
import pcbuilder.enums.TipoMemoria;
import pcbuilder.mock.CatalogoMock;
import pcbuilder.modelo.Componente;
import pcbuilder.modelo.Limitacao;
import pcbuilder.modelo.Montagem;
import pcbuilder.modelo.PlacaMae;
import pcbuilder.modelo.Processador;
import pcbuilder.servico.MontagemGuiada;
import pcbuilder.servico.resumos.ResumoPlacaMae;

/**
 * Testes da HU-005: etapa 2 (placa-mae) com filtro por soquete, avisos de VRM/BIOS,
 * {@code [M] Ver ocultos}, {@code [0] Voltar} e recalculo do filtro ao trocar a CPU.
 *
 * <p>Execucao (sem JUnit):
 * {@code java -cp build/classes;build/classes-test pcbuilder.teste.EtapaPlacaMaeTeste}.</p>
 */
public final class EtapaPlacaMaeTeste {

    private static final CatalogoMock CATALOGO = CatalogoMock.criar();
    private static final FiltroEtapa FILTRO = new FiltroEtapa(CATALOGO);
    private static final AvaliadorLimitacoes AVALIADOR = new AvaliadorLimitacoes();

    private EtapaPlacaMaeTeste() {
        // classe utilitaria: nao deve ser instanciada
    }

    public static void main(String[] args) {
        System.out.println("Testes da HU-005 - etapa 2 (placa-mae)");

        resumoDaLinhaDeApoio();
        biosPodeExigirAtualizacao();
        etapaEscolhePlacaFiltrada();
        etapaVoltarPreservaCpu();
        etapaMostraOcultosComMotivo();
        etapaMarcaVrmApertado();
        trocarCpuReaplicaFiltro();
        menuVoltaTrocaCpuEEscolhePlaca();

        System.exit(Assercao.concluir());
    }

    // -------------------------------------------------------------- linha de apoio

    private static void resumoDaLinhaDeApoio() {
        PlacaMae placa = placa(Chipset.B650, Soquete.AM5, true);
        Assercao.iguais("resumo: linha de apoio da etapa 2",
                "chipset B650 | MATX | 4 slots DDR5 (max 6000 MT/s) | VRM 8x50A",
                ResumoPlacaMae.linhaDeApoio(placa));
        Assercao.iguais("resumo: componente que nao e placa-mae", "",
                ResumoPlacaMae.linhaDeApoio(cpu(Soquete.AM5, null, 100)));
    }

    // ----------------------------------------------------------------- C09 (BIOS)

    private static void biosPodeExigirAtualizacao() {
        Processador cpuNova = cpu(Soquete.AM5, 2024, 100);
        Processador cpuAntiga = cpu(Soquete.AM5, 2022, 100);

        Montagem comCpuNova = montagem(cpuNova);
        List<Limitacao> avisos = AVALIADOR.avaliar(comCpuNova, placa(Chipset.B650, Soquete.AM5, false));
        Limitacao aviso = aviso(avisos, "C09");
        Assercao.verdadeiro("C09: CPU mais nova que o chipset gera aviso", aviso != null);
        Assercao.verdadeiro("C09: aviso diz que pode exigir atualizacao de BIOS",
                aviso.getConsequencia().contains("pode exigir atualizacao de BIOS"));
        Assercao.verdadeiro("C09: sem flashback o texto pede CPU compativel",
                aviso.getConsequencia().contains("CPU compativel"));
        Assercao.iguais("C09: severidade AVISO", pcbuilder.enums.SeveridadeLimitacao.AVISO,
                aviso.getSeveridade());

        List<Limitacao> comFlashback = AVALIADOR.avaliar(comCpuNova,
                placa(Chipset.B650, Soquete.AM5, true));
        Assercao.verdadeiro("C09: com flashback o texto explica a atualizacao sem CPU",
                aviso(comFlashback, "C09").getConsequencia()
                        .contains("sem precisar de uma CPU compativel"));

        Assercao.verdadeiro("C09: CPU da mesma geracao nao gera aviso",
                AVALIADOR.avaliar(montagem(cpuAntiga),
                        placa(Chipset.B650, Soquete.AM5, false)).stream()
                        .noneMatch(limitacao -> "C09".equals(limitacao.getRegraId())));
        Assercao.verdadeiro("C09: chipset atual nao gera aviso",
                AVALIADOR.avaliar(comCpuNova,
                        placa(Chipset.X870, Soquete.AM5, false)).stream()
                        .noneMatch(limitacao -> "C09".equals(limitacao.getRegraId())));

        Processador intel2023 = cpu(Soquete.LGA1700, 2023, 100);
        Assercao.verdadeiro("C09: 13a geracao em chipset 600 gera aviso",
                AVALIADOR.avaliar(montagem(intel2023),
                        placa(Chipset.B660, Soquete.LGA1700, false)).stream()
                        .anyMatch(limitacao -> "C09".equals(limitacao.getRegraId())));
        Assercao.verdadeiro("C09: 13a geracao em chipset 700 nao gera aviso",
                AVALIADOR.avaliar(montagem(intel2023),
                        placa(Chipset.B760, Soquete.LGA1700, false)).stream()
                        .noneMatch(limitacao -> "C09".equals(limitacao.getRegraId())));
    }

    // ------------------------------------------------------------- etapa 2 (tela)

    private static void etapaEscolhePlacaFiltrada() {
        Processador cpu = cpuDoCatalogo(Soquete.AM5);
        MontagemGuiada fluxo = fluxoNaEtapa2(cpu);
        ListaFiltrada lista = FILTRO.filtrar(CategoriaComponente.PLACA_MAE,
                fluxo.getMontagem());
        Componente primeira = lista.getCompativeis().get(0);
        List<Limitacao> avisos = AVALIADOR.avaliar(fluxo.getMontagem(), primeira);

        Saida saida = new Saida("1\n" + (avisos.isEmpty() ? "" : "\n"));
        ResultadoEtapa resultado = new EtapaPlacaMae(saida.terminal, fluxo, FILTRO).executar();

        Assercao.iguais("etapa 2: escolher a primeira placa conclui", ResultadoEtapa.CONCLUIDA,
                resultado);
        Assercao.iguais("etapa 2: placa guardada na montagem", primeira,
                fluxo.getMontagem().obter(CategoriaComponente.PLACA_MAE).orElse(null));
        Assercao.iguais("etapa 2: placa escolhida e do soquete da CPU", Soquete.AM5,
                ((PlacaMae) primeira).getSoqueteCPU());

        String texto = saida.texto();
        Assercao.verdadeiro("etapa 2: cabecalho",
                texto.contains("=== Etapa 2 de 6 - Placa-Mae (obrigatorio) ==="));
        Assercao.verdadeiro("etapa 2: barra marca CPU concluida e placa atual",
                texto.contains("[+Processador]") && texto.contains(">>[Placa-mae]<<"));
        Assercao.verdadeiro("etapa 2: linha de apoio montada pelos campos",
                texto.contains(apoioAlinhado(lista)));
        Assercao.verdadeiro("etapa 2: mensagem de selecao",
                texto.contains("Placa-mae selecionada: "
                        + TabelaComponentes.nomeExibicao(primeira)));
        Assercao.verdadeiro("etapa 2: rodape oferece [0] Voltar",
                texto.contains(EtapaPlacaMae.RODAPE));
        Assercao.verdadeiro("etapa 2: lista so mostra placas do soquete da CPU",
                lista.getCompativeis().stream().map(PlacaMae.class::cast)
                        .allMatch(placa -> placa.getSoqueteCPU() == Soquete.AM5));
    }

    private static void etapaVoltarPreservaCpu() {
        Processador cpu = cpuDoCatalogo(Soquete.AM5);
        MontagemGuiada fluxo = fluxoNaEtapa2(cpu);

        ResultadoEtapa resultado = new EtapaPlacaMae(new Saida("0\n").terminal, fluxo, FILTRO)
                .executar();

        Assercao.iguais("etapa 2: [0] pede para voltar", ResultadoEtapa.VOLTAR, resultado);
        Assercao.iguais("etapa 2: [0] preserva a CPU", cpu,
                fluxo.getMontagem().obter(CategoriaComponente.CPU).orElse(null));
        Assercao.verdadeiro("etapa 2: [0] nao escolhe placa",
                fluxo.getMontagem().obter(CategoriaComponente.PLACA_MAE).isEmpty());
        Assercao.iguais("etapa 2: fluxo continua na etapa 2", 2, fluxo.etapaAtual());
        Assercao.iguais("etapa 2: etapaAnterior volta para a 1", 1, fluxo.etapaAnterior());
        Assercao.iguais("etapa 2: etapaAnterior nao passa da 1", 1, fluxo.etapaAnterior());
    }

    private static void etapaMostraOcultosComMotivo() {
        Processador cpu = cpuDoCatalogo(Soquete.AM5);
        MontagemGuiada fluxo = fluxoNaEtapa2(cpu);

        Saida saida = new Saida("M\n0\n");
        ResultadoEtapa resultado = new EtapaPlacaMae(saida.terminal, fluxo, FILTRO).executar();

        Assercao.iguais("etapa 2: depois de [M] o [0] volta", ResultadoEtapa.VOLTAR, resultado);
        String texto = saida.texto();
        Assercao.verdadeiro("etapa 2: [M] lista os ocultos",
                texto.contains("Ocultos por incompatibilidade"));
        Assercao.verdadeiro("etapa 2: motivo do soquete",
                texto.contains("soquete LGA1700 x processador AM5"));
        Assercao.verdadeiro("etapa 2: ocultos nao podem ser escolhidos",
                texto.contains("nao podem ser escolhidos"));
        Assercao.iguais("etapa 2: nenhuma placa escolhida apos [M] e [0]", 1,
                fluxo.getMontagem().quantidadePecas());
    }

    private static void etapaMarcaVrmApertado() {
        Processador cpuForte = cpu(Soquete.AM5, null, 400);
        MontagemGuiada fluxo = fluxoNaEtapa2(cpuForte);
        ListaFiltrada lista = FILTRO.filtrar(CategoriaComponente.PLACA_MAE,
                fluxo.getMontagem());
        Map<Componente, List<Limitacao>> avisos = AVALIADOR.avaliarLista(
                fluxo.getMontagem(), lista.getCompativeis());

        int numero = 0;
        Componente alvo = null;
        for (int indice = 0; indice < lista.getCompativeis().size(); indice++) {
            Componente candidata = lista.getCompativeis().get(indice);
            if (!avisos.getOrDefault(candidata, List.of()).isEmpty()) {
                numero = indice + 1;
                alvo = candidata;
                break;
            }
        }
        Assercao.verdadeiro("VRM: existe placa AM5 com aviso para CPU forte", alvo != null);

        Saida saida = new Saida(numero + "\n\n");
        ResultadoEtapa resultado = new EtapaPlacaMae(saida.terminal, fluxo, FILTRO).executar();

        Assercao.iguais("VRM: placa com aviso continua selecionavel",
                ResultadoEtapa.CONCLUIDA, resultado);
        Assercao.iguais("VRM: placa marcada foi guardada", alvo,
                fluxo.getMontagem().obter(CategoriaComponente.PLACA_MAE).orElse(null));

        String texto = saida.texto();
        Assercao.verdadeiro("VRM: linha marcada com [!]",
                linhaCom(texto, TabelaComponentes.nomeExibicao(alvo))
                        .startsWith(TabelaComponentes.MARCADOR_LIMITACAO));
        Assercao.verdadeiro("VRM: aviso nomeia o uso do VRM",
                texto.contains("percentualUsoVRM"));
        Assercao.verdadeiro("VRM: aviso mostra a severidade", texto.contains("[!] RISCO"));
        Assercao.verdadeiro("VRM: aviso repete que a escolha vale",
                texto.contains(TextosLimitacao.ESCOLHA_VALIDA));
        Assercao.verdadeiro("VRM: aviso acumulado na montagem",
                fluxo.getMontagem().getLimitacoes().stream()
                        .anyMatch(limitacao -> "L02".equals(limitacao.getRegraId())));
    }

    // --------------------------------------------- voltar / trocar CPU / menu

    private static void trocarCpuReaplicaFiltro() {
        Processador am5 = cpu(Soquete.AM5, null, 100);
        Montagem montagem = montagem(am5);
        MontagemGuiada fluxo = new MontagemGuiada(montagem);
        fluxo.proximaEtapa();

        Saida volta = new Saida("0\n");
        Assercao.iguais("troca: [0] volta para a etapa 1", ResultadoEtapa.VOLTAR,
                new EtapaPlacaMae(volta.terminal, fluxo, FILTRO).executar());
        fluxo.etapaAnterior();

        Processador lga = cpu(Soquete.LGA1700, null, 100);
        montagem.adicionar(lga);
        fluxo.proximaEtapa();

        ListaFiltrada lista = FILTRO.filtrar(CategoriaComponente.PLACA_MAE, montagem);
        Componente primeira = lista.getCompativeis().get(0);
        Assercao.iguais("troca: filtro recalculado para LGA1700", Soquete.LGA1700,
                ((PlacaMae) primeira).getSoqueteCPU());

        List<Limitacao> avisos = AVALIADOR.avaliar(montagem, primeira);
        Saida saida = new Saida("1\n" + (avisos.isEmpty() ? "" : "\n"));
        Assercao.iguais("troca: escolher placa LGA1700 conclui", ResultadoEtapa.CONCLUIDA,
                new EtapaPlacaMae(saida.terminal, fluxo, FILTRO).executar());
        Assercao.iguais("troca: placa guardada e LGA1700", Soquete.LGA1700,
                ((PlacaMae) montagem.obter(CategoriaComponente.PLACA_MAE).orElseThrow())
                        .getSoqueteCPU());
    }

    private static void menuVoltaTrocaCpuEEscolhePlaca() {
        int indiceLga = indiceCpu(Soquete.LGA1700);
        int indiceAm5 = indiceCpu(Soquete.AM5);

        Processador am5 = cpuDoCatalogo(Soquete.AM5);
        Montagem soCpu = montagem(am5);
        Componente primeira = FILTRO.filtrar(CategoriaComponente.PLACA_MAE, soCpu)
                .getCompativeis().get(0);
        boolean temAviso = !AVALIADOR.avaliar(soCpu, primeira).isEmpty();

        String entrada = "1\n" + indiceLga + "\n0\n" + indiceAm5 + "\nS\n1\n"
                + (temAviso ? "\n" : "");
        Saida saida = new Saida(entrada);
        int codigo = new MenuPrincipal(CATALOGO, saida.terminal).executar();

        Assercao.iguais("menu: execucao termina sem erro", 0, codigo);
        String texto = saida.texto();
        Assercao.verdadeiro("menu: a etapa 2 foi reaberta depois de [0]",
                contar(texto, "=== Etapa 2 de 6") >= 2);
        Assercao.verdadeiro("menu: escolheu a placa do novo soquete",
                texto.contains("Placa-mae selecionada: "
                        + TabelaComponentes.nomeExibicao(primeira)));
        Assercao.verdadeiro("menu: avanca para a etapa 3",
                texto.contains("=== Etapa 3 de 6 - Memoria (obrigatorio) ==="));
    }

    // ---------------------------------------------------------------- auxiliares

    private static Processador cpuDoCatalogo(Soquete soquete) {
        return CATALOGO.porCategoria(CategoriaComponente.CPU).stream()
                .map(Processador.class::cast)
                .filter(cpu -> cpu.getSoquete() == soquete)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("base sem CPU " + soquete));
    }

    private static int indiceCpu(Soquete soquete) {
        List<Componente> cpus = CATALOGO.porCategoria(CategoriaComponente.CPU);
        for (int indice = 0; indice < cpus.size(); indice++) {
            if (((Processador) cpus.get(indice)).getSoquete() == soquete) {
                return indice + 1;
            }
        }
        throw new IllegalStateException("base sem CPU " + soquete);
    }

    private static Processador cpu(Soquete soquete, Integer ano, int consumoMaximo) {
        return Processador.builder()
                .id("cpu-teste-" + soquete + "-" + ano + "-" + consumoMaximo)
                .modelo("CPU Teste " + soquete + " " + consumoMaximo + "W")
                .fabricante("Teste")
                .anoLancamento(ano)
                .soquete(soquete)
                .nucleosFisicos(8)
                .threads(16)
                .frequenciaBaseMHz(3800)
                .frequenciaBoostMHz(5500)
                .tdpWatts(65)
                .consumoMaximoWatts(consumoMaximo)
                .tensaoMaximaVolts(1.25)
                .tiposMemoriaSuportados(TipoMemoria.DDR5)
                .canaisMemoria(2)
                .frequenciaMemoriaNativaMTs(5200)
                .frequenciaMaximaSincronaMTs(6000)
                .capacidadeMaximaMemoriaGB(192)
                .pistasPCIeLivres(24)
                .indiceDesempenhoSingleThread(90)
                .indiceDesempenhoMultithread(90)
                .construir();
    }

    private static PlacaMae placa(Chipset chipset, Soquete soquete, boolean flashback) {
        return PlacaMae.builder()
                .id("placa-teste-" + chipset + "-" + soquete)
                .modelo("Placa Teste " + chipset)
                .fabricante("Teste")
                .soqueteCPU(soquete)
                .chipset(chipset)
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
                .suportaAtualizacaoBIOSSemCPU(flashback)
                .construir();
    }

    private static Montagem montagem(Componente... pecas) {
        Montagem montagem = new Montagem();
        for (Componente peca : pecas) {
            montagem.adicionar(peca);
        }
        return montagem;
    }

    private static MontagemGuiada fluxoNaEtapa2(Componente cpu) {
        MontagemGuiada fluxo = new MontagemGuiada(montagem(cpu));
        fluxo.proximaEtapa();
        return fluxo;
    }

    /** Primeiro aviso da regra informada; falha explicita quando a regra nao gerou nada. */
    private static Limitacao aviso(List<Limitacao> avisos, String regraId) {
        return avisos.stream()
                .filter(limitacao -> regraId.equals(limitacao.getRegraId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("A regra " + regraId
                        + " nao gerou aviso. Avisos: " + avisos));
    }

    /** Linha de apoio do primeiro compativel, com as colunas alinhadas como na tela. */
    private static String apoioAlinhado(ListaFiltrada lista) {
        List<String> apoios = lista.getCompativeis().stream()
                .map(ResumoPlacaMae::linhaDeApoio).toList();
        return TabelaComponentes.alinharApoios(apoios).get(0);
    }

    private static String linhaCom(String texto, String trecho) {
        for (String linha : texto.split("\\R")) {
            if (linha.contains(trecho)) {
                return linha;
            }
        }
        return "";
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
