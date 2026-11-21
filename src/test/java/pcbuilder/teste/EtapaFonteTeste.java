package pcbuilder.teste;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import pcbuilder.analise.CalculadoraConsumo;
import pcbuilder.analise.ComponentesDaMontagem;
import pcbuilder.analise.RequisitosFonte;
import pcbuilder.analise.filtro.FiltroConectoresFonte;
import pcbuilder.analise.filtro.FiltroEtapa;
import pcbuilder.analise.filtro.FiltroFonteSuficiente;
import pcbuilder.analise.filtro.ListaFiltrada;
import pcbuilder.analise.limitacao.AvaliadorLimitacoes;
import pcbuilder.cli.MenuPrincipal;
import pcbuilder.cli.TabelaComponentes;
import pcbuilder.cli.TelaResumo;
import pcbuilder.cli.Terminal;
import pcbuilder.cli.etapas.EtapaFonte;
import pcbuilder.cli.etapas.ResultadoEtapa;
import pcbuilder.enums.CategoriaComponente;
import pcbuilder.enums.Certificado80Plus;
import pcbuilder.enums.Chipset;
import pcbuilder.enums.FormatoArmazenamento;
import pcbuilder.enums.FormatoPlacaMae;
import pcbuilder.enums.InterfaceArmazenamento;
import pcbuilder.enums.Resolucao;
import pcbuilder.enums.Soquete;
import pcbuilder.enums.TipoMemoria;
import pcbuilder.enums.TipoMemoriaVRAM;
import pcbuilder.enums.TipoMidia;
import pcbuilder.mock.CatalogoMock;
import pcbuilder.modelo.Armazenamento;
import pcbuilder.modelo.Componente;
import pcbuilder.modelo.Fonte;
import pcbuilder.modelo.Limitacao;
import pcbuilder.modelo.Memoria;
import pcbuilder.modelo.Montagem;
import pcbuilder.modelo.PlacaMae;
import pcbuilder.modelo.PlacaVideo;
import pcbuilder.modelo.Processador;
import pcbuilder.servico.MontagemGuiada;
import pcbuilder.servico.resumos.ResumoFonte;

/**
 * Testes da HU-009: etapa 6 (fonte) com requisitos de potencia/conectores, filtro
 * C10/C14, avisos L06/L09, comando {@code [C]} e a mensagem de selecao.
 *
 * <p>Execucao (sem JUnit):
 * {@code java -cp build/classes;build/classes-test pcbuilder.teste.EtapaFonteTeste}.</p>
 */
public final class EtapaFonteTeste {

    private static final CatalogoMock CATALOGO = CatalogoMock.criar();
    private static final FiltroEtapa FILTRO = new FiltroEtapa(CATALOGO);
    private static final AvaliadorLimitacoes AVALIADOR = new AvaliadorLimitacoes();

    private EtapaFonteTeste() {
        // classe utilitaria: nao deve ser instanciada
    }

    public static void main(String[] args) {
        System.out.println("Testes da HU-009 - etapa 6 (fonte)");

        resumoDaLinhaDeApoio();
        requisitosDaFonte();
        detalhamentoDoConsumo();
        filtroDeConectores();
        filtroDePotencia();
        etapaFonte();
        etapaSemFonteCompativel();
        gpuPuladaSemExigenciaPcie();
        avisosDeEficienciaERisco();
        menuConcluiAMontagem();

        System.exit(Assercao.concluir());
    }

    // ------------------------------------------------------------ linha de apoio

    private static void resumoDaLinhaDeApoio() {
        Fonte fonte = fonte(750, 744, 1, 2, 0, 1, Certificado80Plus.GOLD);
        Assercao.iguais("resumo: linha de apoio da fonte",
                "750 W | 80 Plus Gold | 12V: 744 W | 1x 16-pinos | 2x 8-pinos",
                ResumoFonte.linhaDeApoio(fonte));

        Fonte simples = fonte(400, 400, 0, 0, 0, 1, Certificado80Plus.NENHUM);
        Assercao.verdadeiro("resumo: fonte sem selo",
                ResumoFonte.linhaDeApoio(simples).contains("sem selo 80 Plus"));
        Assercao.verdadeiro("resumo: fonte sem conector PCIe",
                ResumoFonte.linhaDeApoio(simples).contains("sem conector PCIe"));
        Assercao.iguais("resumo: componente que nao e fonte", "",
                ResumoFonte.linhaDeApoio(cpu(true, 100)));
    }

    // --------------------------------------------------------- requisitos (C10/C14)

    private static void requisitosDaFonte() {
        Montagem montagem = montagem(cpu(true, 140), placa(1),
                gpu(1, 2, 1, 220, 350, 12, Resolucao.QHD_1440P));

        Assercao.iguais("requisitos: descricao dos conectores",
                "1x 16-pinos (placa de video) + 2x 8-pinos (placa de video)"
                        + " + 1x 6-pinos (placa de video) + 1x 8-pinos (placa-mae)",
                RequisitosFonte.descricaoConectores(montagem));
        Assercao.iguais("requisitos: quatro conectores exigidos", 4,
                RequisitosFonte.conectores(montagem).size());
        Assercao.iguais("requisitos: potencia com margem de 40%",
                CalculadoraConsumo.consumoComMargemWatts(montagem),
                RequisitosFonte.potenciaNecessariaWatts(montagem));

        Montagem semGpu = montagem(cpu(true, 140), placa(1));
        Assercao.iguais("requisitos: sem GPU so a placa-mae exige conector",
                "1x 8-pinos (placa-mae)", RequisitosFonte.descricaoConectores(semGpu));
    }

    // ------------------------------------------------------ consumo detalhado

    private static void detalhamentoDoConsumo() {
        Montagem montagem = montagem(cpu(false, 140), placa(1), memoria(), ssd(5),
                gpu(1, 0, 0, 220, 350, 12, Resolucao.QHD_1440P));

        Assercao.iguais("consumo: total 401 W", 401,
                CalculadoraConsumo.consumoTotalSistemaWatts(montagem));
        Assercao.iguais("consumo: margem de 40%", 562,
                CalculadoraConsumo.consumoComMargemWatts(montagem));

        List<CalculadoraConsumo.ItemConsumo> itens = CalculadoraConsumo.detalhar(montagem);
        Assercao.iguais("consumo: cinco linhas (CPU, GPU, placa, RAM, SSD)", 5, itens.size());
        Assercao.verdadeiro("consumo: CPU na primeira linha",
                itens.get(0).getDescricao().startsWith("Processador"));
        Assercao.iguais("consumo: CPU 140 W", 140, itens.get(0).getWatts());
        Assercao.iguais("consumo: GPU 220 W", 220, itens.get(1).getWatts());
        int soma = itens.stream().mapToInt(CalculadoraConsumo.ItemConsumo::getWatts).sum();
        Assercao.iguais("consumo: linhas somam o total", 401, soma);

        Montagem comMidia = montagem(cpu(false, 140), placa(1), memoria(),
                ssdComConsumoMedio(5.0));
        Assercao.iguais("consumo: armazenamento usa consumoMedioWatts", 181,
                CalculadoraConsumo.consumoTotalSistemaWatts(comMidia));

        Montagem soCpu = montagem(cpu(false, 100));
        Assercao.iguais("consumo: reserva entra sem as pecas auxiliares", 150,
                CalculadoraConsumo.consumoTotalSistemaWatts(soCpu));
        Assercao.verdadeiro("consumo: reserva aparece no detalhamento",
                CalculadoraConsumo.detalhar(soCpu).stream()
                        .anyMatch(item -> item.getDescricao().startsWith("Reserva")));
    }

    // ------------------------------------------------------ C10 / C14 (filtros)

    private static void filtroDeConectores() {
        FiltroConectoresFonte filtro = new FiltroConectoresFonte();

        Montagem comGpu16 = montagem(cpu(false, 140), placa(1),
                gpu(1, 0, 0, 220, 350, 12, Resolucao.QHD_1440P));
        Assercao.iguais("C10: motivo do conector de 16 pinos",
                "sem conector de 16 pinos (a placa de video exige 1)",
                filtro.motivoIncompatibilidade(comGpu16,
                        fonte(850, 840, 0, 2, 0, 1, Certificado80Plus.GOLD)).orElse(null));
        Assercao.verdadeiro("C10: fonte com 16 pinos passa",
                filtro.motivoIncompatibilidade(comGpu16,
                        fonte(850, 840, 1, 0, 0, 1, Certificado80Plus.GOLD)).isEmpty());

        Montagem comGpu8 = montagem(cpu(false, 140), placa(1),
                gpu(0, 2, 0, 220, 350, 12, Resolucao.QHD_1440P));
        Assercao.iguais("C10: motivo do conector de 8 pinos",
                "sem conector de 8 pinos (a placa de video exige 2)",
                filtro.motivoIncompatibilidade(comGpu8,
                        fonte(850, 840, 0, 1, 0, 1, Certificado80Plus.GOLD)).orElse(null));

        Montagem comDoisCpu8 = montagem(cpu(false, 140), placa(2));
        Assercao.iguais("C10: motivo do conector de CPU",
                "sem conector de 8 pinos para a CPU (a placa-mae exige 2)",
                filtro.motivoIncompatibilidade(comDoisCpu8,
                        fonte(850, 840, 0, 0, 0, 1, Certificado80Plus.GOLD)).orElse(null));

        Montagem semGpu = montagem(cpu(true, 140), placa(1));
        Assercao.verdadeiro("C10: sem GPU nenhuma exigencia PCIe",
                filtro.motivoIncompatibilidade(semGpu,
                        fonte(850, 840, 0, 0, 0, 1, Certificado80Plus.GOLD)).isEmpty());
    }

    private static void filtroDePotencia() {
        Montagem montagem = montagem(cpu(false, 500));
        Assercao.iguais("C14: consumo com margem", 770,
                RequisitosFonte.potenciaNecessariaWatts(montagem));

        FiltroFonteSuficiente filtro = new FiltroFonteSuficiente();
        Assercao.verdadeiro("C14: 750 W nao cobre 770 W",
                filtro.motivoIncompatibilidade(montagem,
                        fonte(750, 744, 0, 0, 0, 1, Certificado80Plus.BRONZE))
                        .filter(motivo -> motivo.contains("750 W") && motivo.contains("770 W"))
                        .isPresent());
        Assercao.verdadeiro("C14: 850 W cobre 770 W",
                filtro.motivoIncompatibilidade(montagem,
                        fonte(850, 840, 0, 0, 0, 1, Certificado80Plus.GOLD)).isEmpty());

        ListaFiltrada lista = FILTRO.filtrar(CategoriaComponente.FONTE, montagem);
        Assercao.verdadeiro("C14: lista so traz fontes com 770 W ou mais",
                lista.getCompativeis().stream().map(Fonte.class::cast)
                        .allMatch(item -> item.getPotenciaTotalWatts() >= 770));
    }

    // ------------------------------------------------------ etapa 6 (tela)

    private static void etapaFonte() {
        Montagem montagem = montagem(cpu(false, 140), placa(1), memoria(), ssd(5),
                gpu(1, 0, 0, 220, 350, 12, Resolucao.QHD_1440P));
        MontagemGuiada fluxo = fluxoNaEtapa6(montagem);

        Saida calculo = new Saida("C\n0\n");
        ResultadoEtapa resultadoCalculo = new EtapaFonte(calculo.terminal, fluxo, FILTRO)
                .executar();

        Assercao.iguais("etapa 6: [C] depois [0] volta", ResultadoEtapa.VOLTAR,
                resultadoCalculo);
        String textoCalculo = calculo.texto();
        Assercao.verdadeiro("etapa 6: bloco do consumo",
                textoCalculo.contains("Consumo estimado da sua configuracao: 401 W"
                        + " (com margem de 40%: 562 W)"));
        Assercao.verdadeiro("etapa 6: conectores exigidos",
                textoCalculo.contains("Conectores exigidos: 1x 16-pinos (placa de video)"
                        + " + 1x 8-pinos (placa-mae)"));
        Assercao.verdadeiro("etapa 6: rodape com [C]",
                textoCalculo.contains(EtapaFonte.RODAPE));
        Assercao.verdadeiro("etapa 6: [C] detalha o calculo",
                textoCalculo.contains("Calculo do consumo:"));
        Assercao.verdadeiro("etapa 6: detalhe soma CPU e GPU",
                textoCalculo.contains("Processador ") && textoCalculo.contains(" - 140 W")
                        && textoCalculo.contains("Placa de video ")
                        && textoCalculo.contains(" - 220 W"));
        Assercao.verdadeiro("etapa 6: total com margem no calculo",
                textoCalculo.contains("Total - 401 W (com margem de 40%: 562 W)"));

        ListaFiltrada lista = FILTRO.filtrar(CategoriaComponente.FONTE, montagem);
        Componente alvo = lista.getCompativeis().get(0);
        List<Limitacao> avisos = AVALIADOR.avaliar(montagem, alvo);
        Saida saida = new Saida("1\n" + (avisos.isEmpty() ? "" : "\n"));
        ResultadoEtapa resultado = new EtapaFonte(saida.terminal, fluxo, FILTRO).executar();

        Assercao.iguais("etapa 6: escolher a fonte conclui", ResultadoEtapa.CONCLUIDA,
                resultado);
        Assercao.iguais("etapa 6: fonte guardada na montagem", alvo,
                montagem.obter(CategoriaComponente.FONTE).orElse(null));
        Assercao.verdadeiro("etapa 6: mensagem de selecao",
                saida.texto().contains("Fonte selecionada: "
                        + TabelaComponentes.nomeExibicao(alvo)));
        Assercao.verdadeiro("etapa 6: linha de apoio da fonte",
                saida.texto().contains(apoioAlinhado(lista, alvo)));
    }

    private static void etapaSemFonteCompativel() {
        Montagem montagem = montagem(cpu(false, 5000), placa(1));
        MontagemGuiada fluxo = fluxoNaEtapa6(montagem);

        Saida saida = new Saida("0\n");
        ResultadoEtapa resultado = new EtapaFonte(saida.terminal, fluxo, FILTRO).executar();

        Assercao.iguais("etapa 6: sem fonte compativel volta", ResultadoEtapa.VOLTAR,
                resultado);
        String texto = saida.texto();
        Assercao.verdadeiro("etapa 6: mensagem com os requisitos",
                texto.contains("Nenhuma fonte compativel"));
        Assercao.verdadeiro("etapa 6: mensagem cita a potencia",
                texto.contains(RequisitosFonte.potenciaNecessariaWatts(montagem) + " W"));
        Assercao.verdadeiro("etapa 6: mensagem cita os conectores",
                texto.contains("1x 8-pinos (placa-mae)"));
        Assercao.verdadeiro("etapa 6: oferece [0] Voltar", texto.contains("[0] Voltar"));
    }

    private static void gpuPuladaSemExigenciaPcie() {
        Montagem montagem = montagem(cpu(true, 140), placa(1), memoria(), ssd(5));
        MontagemGuiada fluxo = fluxoNaEtapa6(montagem);

        Saida saida = new Saida("0\n");
        ResultadoEtapa resultado = new EtapaFonte(saida.terminal, fluxo, FILTRO).executar();

        Assercao.iguais("etapa 6: sem GPU o [0] volta", ResultadoEtapa.VOLTAR, resultado);
        Assercao.verdadeiro("etapa 6: so o conector da placa-mae e exigido",
                saida.texto().contains("Conectores exigidos: 1x 8-pinos (placa-mae)"));
    }

    // ------------------------------------------------------ L06 / L09 (avisos)

    private static void avisosDeEficienciaERisco() {
        Montagem montagem = montagem(cpu(false, 140), placa(1), memoria(), ssd(5),
                gpu(1, 0, 0, 220, 350, 12, Resolucao.QHD_1440P));

        Fonte exagerada = fonte(1200, 1188, 1, 2, 0, 1, Certificado80Plus.GOLD);
        Limitacao eficiencia = AVALIADOR.avaliar(montagem, exagerada).stream()
                .filter(limitacao -> "L09".equals(limitacao.getRegraId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("L09 nao gerou aviso"));
        Assercao.iguais("L09: severidade AVISO",
                pcbuilder.enums.SeveridadeLimitacao.AVISO, eficiencia.getSeveridade());
        Assercao.verdadeiro("L09: aviso cita a carga",
                eficiencia.getConsequencia().contains("carga"));

        Fonte trilhoBaixo = fonte(750, 250, 1, 2, 0, 1, Certificado80Plus.GOLD);
        Limitacao risco = AVALIADOR.avaliar(montagem, trilhoBaixo).stream()
                .filter(limitacao -> "L06".equals(limitacao.getRegraId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("L06 nao gerou aviso"));
        Assercao.iguais("L06: severidade RISCO",
                pcbuilder.enums.SeveridadeLimitacao.RISCO, risco.getSeveridade());
        Assercao.verdadeiro("L06: aviso fala em desligar",
                risco.getConsequencia().contains("desligar"));
    }

    private static void menuConcluiAMontagem() {
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
        Montagem comArmazenamento = montagem(cpu, placa, memoria, armazenamento);
        Componente fonte = FILTRO.filtrar(CategoriaComponente.FONTE, comArmazenamento)
                .getCompativeis().get(0);

        boolean avisoPlaca = !AVALIADOR.avaliar(montagem(cpu), placa).isEmpty();
        boolean avisoMemoria = !AVALIADOR.avaliar(base, memoria).isEmpty();
        boolean avisoArmazenamento = !AVALIADOR.avaliar(comMemoria, armazenamento).isEmpty();
        boolean avisoFonte = !AVALIADOR.avaliar(comArmazenamento, fonte).isEmpty();

        String entrada = "1\n" + indiceCpu + "\n1\n" + flag(avisoPlaca)
                + "1\n" + flag(avisoMemoria) + "0\n"
                + "1\n" + flag(avisoArmazenamento) + "1\n" + flag(avisoFonte);
        Saida saida = new Saida(entrada);
        int codigo = new MenuPrincipal(CATALOGO, saida.terminal).executar();

        Assercao.iguais("menu: execucao termina sem erro", 0, codigo);
        String texto = saida.texto();
        Assercao.verdadeiro("menu: etapa 6 exibida",
                texto.contains("=== Etapa 6 de 6 - Fonte (obrigatorio) ==="));
        Assercao.verdadeiro("menu: fonte confirmada",
                texto.contains("Fonte selecionada: "
                        + TabelaComponentes.nomeExibicao(fonte) + " - "
                        + pcbuilder.relatorio.FormatadorMoeda.reais(fonte.getPrecoMedioBRL())));
        Assercao.verdadeiro("menu: resumo final exibido", texto.contains(TelaResumo.TITULO));
        Assercao.verdadeiro("menu: total no resumo", texto.contains("TOTAL: R$"));
        Assercao.verdadeiro("menu: rodape do resumo", texto.contains(TelaResumo.RODAPE));
    }

    // ---------------------------------------------------------------- auxiliares

    private static Processador cpu(boolean videoIntegrado, int consumoMaximo) {
        return Processador.builder()
                .id("cpu-teste-fonte-" + consumoMaximo)
                .modelo("Ryzen Teste Fonte")
                .fabricante("AMD")
                .soquete(Soquete.AM5)
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
                .possuiVideoIntegrado(videoIntegrado)
                .indiceDesempenhoSingleThread(90)
                .indiceDesempenhoMultithread(90)
                .construir();
    }

    private static PlacaMae placa(int conectoresCpu8) {
        return PlacaMae.builder()
                .id("placa-teste-fonte-" + conectoresCpu8)
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
                .quantidadeSlotsM2(2)
                .geracaoMaximaSlotM2(4)
                .geracaoMinimaSlotM2(4)
                .quantidadeSlotsPCIe16(1)
                .versaoSlotPCIe16(4)
                .pistasSlotPCIe16(16)
                .quantidadePortasSATA(4)
                .velocidadeSATAGbps(6)
                .quantidadeConectoresCPU8pin(conectoresCpu8)
                .consumoEstimadoWatts(30)
                .construir();
    }

    private static Memoria memoria() {
        return Memoria.builder()
                .id("ram-teste-fonte")
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

    private static Armazenamento ssd(int consumoWatts) {
        return Armazenamento.builder()
                .id("ssd-teste-fonte")
                .modelo("SSD Teste")
                .fabricante("Teste")
                .tipoMidia(TipoMidia.SSD)
                .interfaceArmazenamento(InterfaceArmazenamento.NVME)
                .formato(FormatoArmazenamento.M2_2280)
                .capacidadeGB(2000)
                .versaoPCIe(4)
                .pistasPCIe(4)
                .leituraSequencialMBs(7000)
                .escritaSequencialMBs(7000)
                .consumoEstimadoWatts(consumoWatts)
                .construir();
    }

    private static Armazenamento ssdComConsumoMedio(double consumoWatts) {
        return Armazenamento.builder()
                .id("ssd-teste-fonte-medio")
                .modelo("SSD Teste Medio")
                .fabricante("Teste")
                .tipoMidia(TipoMidia.SSD)
                .interfaceArmazenamento(InterfaceArmazenamento.NVME)
                .formato(FormatoArmazenamento.M2_2280)
                .capacidadeGB(2000)
                .versaoPCIe(4)
                .pistasPCIe(4)
                .leituraSequencialMBs(7000)
                .escritaSequencialMBs(7000)
                .consumoMedioWatts(consumoWatts)
                .construir();
    }

    private static PlacaVideo gpu(int conectores16, int conectores8, int conectores6, int tdp,
            Integer pico, int vram, Resolucao resolucao) {
        return PlacaVideo.builder()
                .id("gpu-teste-fonte-" + conectores16 + "-" + conectores8)
                .modelo("RTX Teste")
                .fabricante("NVIDIA")
                .memoriaVRAMGB(vram)
                .tipoMemoriaVRAM(TipoMemoriaVRAM.GDDR6X)
                .versaoPCIe(4)
                .pistasPCIe(16)
                .tdpWatts(tdp)
                .picoTransienteWatts(pico)
                .resolucaoAlvo(resolucao)
                .indiceDesempenhoGPU(85)
                .quantidadeConectores16pin(conectores16)
                .quantidadeConectores8pin(conectores8)
                .quantidadeConectores6pin(conectores6)
                .construir();
    }

    private static Fonte fonte(int potencia, int trilho, int conectores16, int conectores8,
            int conectores6, int conectoresCpu8, Certificado80Plus selo) {
        return Fonte.builder()
                .id("fonte-teste-" + potencia + "-" + trilho)
                .modelo("Fonte Teste " + potencia + " W")
                .fabricante("Teste")
                .potenciaTotalWatts(potencia)
                .potenciaTrilho12VWatts(trilho)
                .certificacao80Plus(selo)
                .quantidadeConectores16pin12VHPWR(conectores16)
                .quantidadeConectoresPCIe8pin(conectores8)
                .quantidadeConectoresPCIe6pin(conectores6)
                .quantidadeConectoresCPU8pin(conectoresCpu8)
                .construir();
    }

    private static Montagem montagem(Componente... pecas) {
        Montagem montagem = new Montagem();
        for (Componente peca : pecas) {
            montagem.adicionar(peca);
        }
        return montagem;
    }

    private static MontagemGuiada fluxoNaEtapa6(Montagem montagem) {
        MontagemGuiada fluxo = new MontagemGuiada(montagem);
        for (int etapa = 1; etapa < 6; etapa++) {
            fluxo.proximaEtapa();
        }
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

    /** Linha extra para o aviso de selecao ({@code [Enter]}) quando o item tem aviso. */
    /** Linha de apoio do alvo, com as colunas alinhadas como na tela. */
    private static String apoioAlinhado(ListaFiltrada lista, Componente alvo) {
        int indice = lista.getCompativeis().indexOf(alvo);
        List<String> apoios = lista.getCompativeis().stream()
                .map(ResumoFonte::linhaDeApoio).toList();
        return TabelaComponentes.alinharApoios(apoios).get(indice);
    }

    private static String flag(boolean temAviso) {
        return temAviso ? "\n" : "";
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
