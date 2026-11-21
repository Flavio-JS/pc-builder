package pcbuilder.teste;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import pcbuilder.analise.CalculadoraMemoria;
import pcbuilder.analise.filtro.FiltroEtapa;
import pcbuilder.analise.filtro.ListaFiltrada;
import pcbuilder.analise.limitacao.AvaliadorLimitacoes;
import pcbuilder.analise.limitacao.TextosLimitacao;
import pcbuilder.cli.MenuPrincipal;
import pcbuilder.cli.TabelaComponentes;
import pcbuilder.cli.Terminal;
import pcbuilder.cli.etapas.EtapaMemoria;
import pcbuilder.cli.etapas.ResultadoEtapa;
import pcbuilder.enums.CategoriaComponente;
import pcbuilder.enums.Chipset;
import pcbuilder.enums.FormatoPlacaMae;
import pcbuilder.enums.Soquete;
import pcbuilder.enums.TipoMemoria;
import pcbuilder.mock.CatalogoMock;
import pcbuilder.modelo.Componente;
import pcbuilder.modelo.Limitacao;
import pcbuilder.modelo.Memoria;
import pcbuilder.modelo.Montagem;
import pcbuilder.modelo.PlacaMae;
import pcbuilder.modelo.Processador;
import pcbuilder.servico.MontagemGuiada;
import pcbuilder.servico.resumos.ResumoMemoria;

/**
 * Testes da HU-006: etapa 3 (memoria) com filtro por tipo/capacidade, avisos de
 * frequencia/single channel/EXPO, {@code [M] Ver ocultos}, {@code [0] Voltar} e a
 * mensagem de selecao com capacidade e canal.
 *
 * <p>Execucao (sem JUnit):
 * {@code java -cp build/classes;build/classes-test pcbuilder.teste.EtapaMemoriaTeste}.</p>
 */
public final class EtapaMemoriaTeste {

    private static final CatalogoMock CATALOGO = CatalogoMock.criar();
    private static final FiltroEtapa FILTRO = new FiltroEtapa(CATALOGO);
    private static final AvaliadorLimitacoes AVALIADOR = new AvaliadorLimitacoes();

    private EtapaMemoriaTeste() {
        // classe utilitaria: nao deve ser instanciada
    }

    public static void main(String[] args) {
        System.out.println("Testes da HU-006 - etapa 3 (memoria)");

        resumoDaLinhaDeApoio();
        canaisDaConfiguracao();
        caso8000x6000();
        ddr4OcultaComMotivo();
        capacidadeAcimaDoLimiteOculta();
        singleChannelGeraAviso();
        expoNaoSuportadoGeraAviso();
        etapaVoltarPreservaEscolhas();
        menuEscolheAMemoria();

        System.exit(Assercao.concluir());
    }

    // -------------------------------------------------------------- linha de apoio

    private static void resumoDaLinhaDeApoio() {
        Memoria kit = memoria(16, 2, 8000, 38, true);
        Assercao.iguais("resumo: linha de apoio da etapa 3",
                "2x16 GB = 32 GB | 8000 MT/s | CL38 | EXPO: sim",
                ResumoMemoria.linhaDeApoio(kit));
        Assercao.iguais("resumo: descricao do kit", "2x16 GB = 32 GB",
                ResumoMemoria.descricaoKit(kit));
        Assercao.iguais("resumo: 2 pentes em 2 canais", "Dual Channel",
                ResumoMemoria.canal(kit, 2));
        Assercao.iguais("resumo: 1 pente em 2 canais", "Single Channel",
                ResumoMemoria.canal(memoria(16, 1, 6000, 30, true), 2));
    }

    private static void canaisDaConfiguracao() {
        Montagem montagem = montagem(cpu(), placa(6000, 4, 192, true, true));
        Assercao.iguais("canais: herdados da placa-mae", 2,
                CalculadoraMemoria.canaisSuportados(montagem));
        Assercao.iguais("canais: sem placa-mae usa a CPU", 2,
                CalculadoraMemoria.canaisSuportados(montagem(cpu())));
    }

    // ----------------------------------------------------------- caso 8000 x 6000

    private static void caso8000x6000() {
        Processador cpu = cpu();
        PlacaMae placa = placa(6000, 4, 192, true, true);
        Montagem montagem = montagem(cpu, placa);
        MontagemGuiada fluxo = fluxoNaEtapa3(montagem);

        ListaFiltrada lista = FILTRO.filtrar(CategoriaComponente.MEMORIA, montagem);
        int numero = 0;
        Memoria alvo = null;
        for (int indice = 0; indice < lista.getCompativeis().size(); indice++) {
            Memoria candidata = (Memoria) lista.getCompativeis().get(indice);
            boolean perdeFrequencia = AVALIADOR.avaliar(montagem, candidata).stream()
                    .anyMatch(limitacao -> "L04".equals(limitacao.getRegraId())
                            && limitacao.getPercentualPerda() != null
                            && limitacao.getPercentualPerda() >= 20);
            if (candidata.getFrequenciaMTs() == 8000 && candidata.getQuantidadePentes() == 2
                    && perdeFrequencia) {
                numero = indice + 1;
                alvo = candidata;
                break;
            }
        }
        Assercao.verdadeiro("etapa 3: existe kit acima do teto da placa", alvo != null);

        List<Limitacao> avisos = AVALIADOR.avaliar(montagem, alvo);
        Limitacao frequencia = aviso(avisos, "L04");
        Assercao.iguais("etapa 3: valor anunciado do kit",
                alvo.getFrequenciaMTs() + " MT/s", frequencia.getValorDoNovo());
        Assercao.iguais("etapa 3: valor suportado pela configuracao", "6000 MT/s",
                frequencia.getValorSuportado());
        Assercao.iguais("etapa 3: perda de frequencia", 25.0, frequencia.getPercentualPerda());

        Saida saida = new Saida(numero + "\n\n");
        ResultadoEtapa resultado = new EtapaMemoria(saida.terminal, fluxo, FILTRO).executar();

        Assercao.iguais("etapa 3: kit acima do teto continua selecionavel",
                ResultadoEtapa.CONCLUIDA, resultado);
        Assercao.iguais("etapa 3: kit guardado na montagem", alvo,
                fluxo.getMontagem().obter(CategoriaComponente.MEMORIA).orElse(null));

        String texto = saida.texto();
        Assercao.verdadeiro("etapa 3: cabecalho",
                texto.contains("=== Etapa 3 de 6 - Memoria (obrigatorio) ==="));
        Assercao.verdadeiro("etapa 3: barra marca CPU e placa concluidas",
                texto.contains("[+Processador]") && texto.contains("[+Placa-mae]")
                        && texto.contains(">>[Memoria]<<"));
        Assercao.verdadeiro("etapa 3: linha marcada com [!]",
                linhaCom(texto, ResumoMemoria.linhaDeApoio(alvo))
                        .startsWith(TabelaComponentes.MARCADOR_LIMITACAO));
        Assercao.verdadeiro("etapa 3: aviso cita 6000 MT/s", texto.contains("6000 MT/s"));
        Assercao.verdadeiro("etapa 3: aviso cita ~25%", texto.contains("~25%"));
        Assercao.verdadeiro("etapa 3: aviso mantem a escolha valida",
                texto.contains(TextosLimitacao.ESCOLHA_VALIDA));
        Assercao.verdadeiro("etapa 3: mensagem de selecao com capacidade e canal",
                texto.contains("Memoria selecionada: "
                        + TabelaComponentes.nomeExibicao(alvo)
                        + " - " + pcbuilder.relatorio.FormatadorMoeda.reais(
                                alvo.getPrecoMedioBRL())
                        + " (" + ResumoMemoria.descricaoKit(alvo) + ", Dual Channel)"));
        Assercao.verdadeiro("etapa 3: aviso acumulado na montagem",
                fluxo.getMontagem().getLimitacoes().stream()
                        .anyMatch(limitacao -> "L04".equals(limitacao.getRegraId())));

        Memoria semAviso = lista.getCompativeis().stream().map(Memoria.class::cast)
                .filter(memoria -> memoria.getFrequenciaMTs() <= 6000)
                .filter(memoria -> memoria.getQuantidadePentes() == 2)
                .filter(memoria -> AVALIADOR.avaliar(montagem, memoria).isEmpty())
                .findFirst()
                .orElse(null);
        Assercao.verdadeiro("etapa 3: kit dentro do teto nao gera aviso", semAviso != null);
    }

    // ------------------------------------------------ C02/C03 (filtro e motivo)

    private static void ddr4OcultaComMotivo() {
        Processador cpu = cpu();
        PlacaMae placa = placa(6000, 4, 192, true, true);
        MontagemGuiada fluxo = fluxoNaEtapa3(montagem(cpu, placa));

        Saida saida = new Saida("M\n0\n");
        ResultadoEtapa resultado = new EtapaMemoria(saida.terminal, fluxo, FILTRO).executar();

        Assercao.iguais("etapa 3: depois de [M] o [0] volta", ResultadoEtapa.VOLTAR, resultado);
        String texto = saida.texto();
        Assercao.verdadeiro("etapa 3: [M] lista as memorias de outro tipo",
                texto.contains("DDR4 x placa-mae DDR5"));
        Assercao.verdadeiro("etapa 3: ocultos nao podem ser escolhidos",
                texto.contains("nao podem ser escolhidos"));
        Assercao.iguais("etapa 3: nenhuma memoria escolhida apos [M] e [0]", 2,
                fluxo.getMontagem().quantidadePecas());
    }

    private static void capacidadeAcimaDoLimiteOculta() {
        Processador cpu = cpu();
        PlacaMae placa = placa(6000, 4, 32, true, true);
        Montagem montagem = montagem(cpu, placa);
        MontagemGuiada fluxo = fluxoNaEtapa3(montagem);

        Saida saida = new Saida("M\n0\n");
        ResultadoEtapa resultado = new EtapaMemoria(saida.terminal, fluxo, FILTRO).executar();

        Assercao.iguais("etapa 3: acima da capacidade volta normalmente",
                ResultadoEtapa.VOLTAR, resultado);
        Assercao.verdadeiro("etapa 3: motivo da capacidade",
                saida.texto().contains("64 GB x limite de 32 GB (placa-mae)"));

        ListaFiltrada lista = FILTRO.filtrar(CategoriaComponente.MEMORIA, montagem);
        Assercao.verdadeiro("etapa 3: nenhum kit visivel passa do limite",
                lista.getCompativeis().stream().map(Memoria.class::cast)
                        .allMatch(memoria -> CalculadoraMemoria.capacidadeTotalGB(memoria) <= 32));
    }

    // ------------------------------------------------------- L04 e C02 (avisos)

    private static void singleChannelGeraAviso() {
        Processador cpu = cpu();
        PlacaMae placa = placa(8000, 4, 192, true, true);
        Montagem montagem = montagem(cpu, placa);
        MontagemGuiada fluxo = fluxoNaEtapa3(montagem);

        ListaFiltrada lista = FILTRO.filtrar(CategoriaComponente.MEMORIA, montagem);
        int numero = 0;
        Memoria alvo = null;
        for (int indice = 0; indice < lista.getCompativeis().size(); indice++) {
            Memoria candidata = (Memoria) lista.getCompativeis().get(indice);
            boolean umPente = AVALIADOR.avaliar(montagem, candidata).stream()
                    .anyMatch(limitacao -> "L04".equals(limitacao.getRegraId())
                            && "quantidadePentes".equals(limitacao.getCampoComparado()));
            if (candidata.getQuantidadePentes() == 1 && umPente) {
                numero = indice + 1;
                alvo = candidata;
                break;
            }
        }
        Assercao.verdadeiro("single channel: existe kit de 1 pente compativel", alvo != null);

        Saida saida = new Saida(numero + "\n\n");
        ResultadoEtapa resultado = new EtapaMemoria(saida.terminal, fluxo, FILTRO).executar();

        Assercao.iguais("single channel: kit continua selecionavel", ResultadoEtapa.CONCLUIDA,
                resultado);
        String texto = saida.texto();
        Assercao.verdadeiro("single channel: aviso explica a perda",
                texto.contains("single channel"));
        Assercao.verdadeiro("single channel: mensagem mostra o canal",
                texto.contains(", Single Channel)"));
        Assercao.verdadeiro("single channel: aviso acumulado",
                fluxo.getMontagem().getLimitacoes().stream().anyMatch(limitacao ->
                        "L04".equals(limitacao.getRegraId())
                                && "quantidadePentes".equals(limitacao.getCampoComparado())));
    }

    private static void expoNaoSuportadoGeraAviso() {
        Processador cpu = cpu();
        PlacaMae placa = placa(6000, 4, 192, false, true);
        Montagem montagem = montagem(cpu, placa);
        MontagemGuiada fluxo = fluxoNaEtapa3(montagem);

        ListaFiltrada lista = FILTRO.filtrar(CategoriaComponente.MEMORIA, montagem);
        int numero = 0;
        Memoria alvo = null;
        for (int indice = 0; indice < lista.getCompativeis().size(); indice++) {
            Memoria candidata = (Memoria) lista.getCompativeis().get(indice);
            boolean perfilPerdido = candidata.isPerfilEXPO() && !candidata.isPerfilXMP()
                    && candidata.getFrequenciaMTs() <= 6000
                    && AVALIADOR.avaliar(montagem, candidata).stream()
                            .anyMatch(limitacao -> "C02".equals(limitacao.getRegraId()));
            if (perfilPerdido) {
                numero = indice + 1;
                alvo = candidata;
                break;
            }
        }
        Assercao.verdadeiro("EXPO: existe kit EXPO-only compativel", alvo != null);

        Saida saida = new Saida(numero + "\n\n");
        ResultadoEtapa resultado = new EtapaMemoria(saida.terminal, fluxo, FILTRO).executar();

        Assercao.iguais("EXPO: kit continua selecionavel", ResultadoEtapa.CONCLUIDA, resultado);
        String texto = saida.texto();
        Assercao.verdadeiro("EXPO: aviso nomeia o perfil e o JEDEC",
                texto.contains("EXPO") && texto.contains("JEDEC"));
        Assercao.verdadeiro("EXPO: linha marcada com [!]",
                linhaCom(texto, ResumoMemoria.linhaDeApoio(alvo))
                        .startsWith(TabelaComponentes.MARCADOR_LIMITACAO));
        Assercao.verdadeiro("EXPO: aviso acumulado",
                fluxo.getMontagem().getLimitacoes().stream()
                        .anyMatch(limitacao -> "C02".equals(limitacao.getRegraId())));

        Memoria comXmp = lista.getCompativeis().stream().map(Memoria.class::cast)
                .filter(memoria -> memoria.isPerfilEXPO() && memoria.isPerfilXMP())
                .findFirst()
                .orElse(null);
        Assercao.verdadeiro("EXPO: kit com XMP nao perde o perfil",
                comXmp != null && AVALIADOR.avaliar(montagem, comXmp).stream()
                        .noneMatch(limitacao -> "C02".equals(limitacao.getRegraId())));
    }

    // ------------------------------------------- voltar e fluxo pelo menu

    private static void etapaVoltarPreservaEscolhas() {
        Processador cpu = cpu();
        PlacaMae placa = placa(6000, 4, 192, true, true);
        Montagem montagem = montagem(cpu, placa);
        MontagemGuiada fluxo = fluxoNaEtapa3(montagem);

        ResultadoEtapa resultado = new EtapaMemoria(new Saida("0\n").terminal, fluxo, FILTRO)
                .executar();

        Assercao.iguais("etapa 3: [0] pede para voltar", ResultadoEtapa.VOLTAR, resultado);
        Assercao.iguais("etapa 3: [0] preserva a CPU", cpu,
                montagem.obter(CategoriaComponente.CPU).orElse(null));
        Assercao.iguais("etapa 3: [0] preserva a placa-mae", placa,
                montagem.obter(CategoriaComponente.PLACA_MAE).orElse(null));
        Assercao.verdadeiro("etapa 3: [0] nao escolhe memoria",
                montagem.obter(CategoriaComponente.MEMORIA).isEmpty());
        Assercao.iguais("etapa 3: fluxo continua na etapa 3", 3, fluxo.etapaAtual());
        Assercao.iguais("etapa 3: etapaAnterior volta para a 2", 2, fluxo.etapaAnterior());
    }

    private static void menuEscolheAMemoria() {
        int indiceCpu = indiceCpu(Soquete.AM5);
        Processador cpu = cpuDoCatalogo(Soquete.AM5);
        Componente placa = FILTRO.filtrar(CategoriaComponente.PLACA_MAE, montagem(cpu))
                .getCompativeis().get(0);
        Montagem base = montagem(cpu, placa);
        Componente memoria = FILTRO.filtrar(CategoriaComponente.MEMORIA, base)
                .getCompativeis().get(0);
        boolean avisoPlaca = !AVALIADOR.avaliar(montagem(cpu), placa).isEmpty();
        boolean avisoMemoria = !AVALIADOR.avaliar(base, memoria).isEmpty();

        String entrada = "1\n" + indiceCpu + "\n1\n" + (avisoPlaca ? "\n" : "")
                + "1\n" + (avisoMemoria ? "\n" : "");
        Saida saida = new Saida(entrada);
        int codigo = new MenuPrincipal(CATALOGO, saida.terminal).executar();

        Assercao.iguais("menu: execucao termina sem erro", 0, codigo);
        String texto = saida.texto();
        Assercao.verdadeiro("menu: memoria escolhida e confirmada",
                texto.contains("Memoria selecionada: "
                        + TabelaComponentes.nomeExibicao(memoria) + " - "
                        + pcbuilder.relatorio.FormatadorMoeda.reais(
                                memoria.getPrecoMedioBRL())));
        Assercao.verdadeiro("menu: avanca para a etapa 4",
                texto.contains("=== Etapa 4 de 6 - Placa de Video"));
    }

    // ---------------------------------------------------------------- auxiliares

    private static Processador cpu() {
        return Processador.builder()
                .id("cpu-teste-memoria")
                .modelo("Ryzen Teste Memoria")
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
                .indiceDesempenhoSingleThread(90)
                .indiceDesempenhoMultithread(90)
                .construir();
    }

    private static PlacaMae placa(int frequenciaMaxima, int slots, int capacidadeMaxima,
            boolean expo, boolean xmp) {
        return PlacaMae.builder()
                .id("placa-teste-" + frequenciaMaxima + "-" + slots + "-" + capacidadeMaxima
                        + "-" + expo)
                .modelo("B650 Teste")
                .fabricante("ASUS")
                .soqueteCPU(Soquete.AM5)
                .chipset(Chipset.B650)
                .formato(FormatoPlacaMae.MATX)
                .quantidadeSlotsMemoria(slots)
                .tipoMemoria(TipoMemoria.DDR5)
                .canaisMemoria(2)
                .capacidadeMaximaMemoriaGB(capacidadeMaxima)
                .frequenciaMemoriaMaximaMTs(frequenciaMaxima)
                .suportaPerfilEXPO(expo)
                .suportaPerfilXMP(xmp)
                .vrmFasesCPU(8)
                .vrmCorrentePorFaseA(50)
                .vrmPossuiDissipador(true)
                .construir();
    }

    private static Memoria memoria(int porPente, int pentes, int frequencia, int latencia,
            boolean expo) {
        return memoria(porPente, pentes, frequencia, latencia, expo, false);
    }

    private static Memoria memoria(int porPente, int pentes, int frequencia, int latencia,
            boolean expo, boolean xmp) {
        return Memoria.builder()
                .id("ram-teste-" + pentes + "x" + porPente + "-" + frequencia
                        + "-" + expo + "-" + xmp)
                .modelo("Kit " + pentes + "x" + porPente + " " + frequencia)
                .fabricante("Teste")
                .tipo(TipoMemoria.DDR5)
                .capacidadePorPenteGB(porPente)
                .quantidadePentes(pentes)
                .frequenciaMTs(frequencia)
                .frequenciaJEDECBaseMTs(4800)
                .latenciaCAS(latencia)
                .tensaoVolts(1.35)
                .perfilEXPO(expo)
                .perfilXMP(xmp)
                .possuiECC(false)
                .alturaMm(44)
                .construir();
    }

    private static Montagem montagem(Componente... pecas) {
        Montagem montagem = new Montagem();
        for (Componente peca : pecas) {
            montagem.adicionar(peca);
        }
        return montagem;
    }

    private static MontagemGuiada fluxoNaEtapa3(Montagem montagem) {
        MontagemGuiada fluxo = new MontagemGuiada(montagem);
        fluxo.proximaEtapa();
        fluxo.proximaEtapa();
        return fluxo;
    }

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

    /** Primeiro aviso da regra informada; falha explicita quando a regra nao gerou nada. */
    private static Limitacao aviso(List<Limitacao> avisos, String regraId) {
        return avisos.stream()
                .filter(limitacao -> regraId.equals(limitacao.getRegraId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("A regra " + regraId
                        + " nao gerou aviso. Avisos: " + avisos));
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
