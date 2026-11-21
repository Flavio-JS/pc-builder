package pcbuilder.teste;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import pcbuilder.analise.filtro.FiltroEtapa;
import pcbuilder.analise.filtro.ListaFiltrada;
import pcbuilder.analise.filtro.RevalidadorMontagem;
import pcbuilder.analise.limitacao.AvaliadorLimitacoes;
import pcbuilder.cli.MenuPrincipal;
import pcbuilder.cli.TabelaComponentes;
import pcbuilder.cli.TelaResumo;
import pcbuilder.cli.Terminal;
import pcbuilder.cli.etapas.EtapaPlacaVideo;
import pcbuilder.cli.etapas.EtapaProcessador;
import pcbuilder.cli.etapas.EtapaMemoria;
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
import pcbuilder.relatorio.FormatadorComponente;
import pcbuilder.relatorio.FormatadorMoeda;
import pcbuilder.relatorio.ResumoMontagem;
import pcbuilder.servico.MontagemGuiada;

/**
 * Testes da HU-011: troca de peca a partir do resumo, remocao da GPU opcional e
 * revalidacao da montagem inteira.
 *
 * <p>Execucao (sem JUnit):
 * {@code java -cp build/classes;build/classes-test pcbuilder.teste.TrocaPecaTeste}.</p>
 */
public final class TrocaPecaTeste {

    private static final CatalogoMock CATALOGO = CatalogoMock.criar();
    private static final FiltroEtapa FILTRO = new FiltroEtapa(CATALOGO);
    private static final AvaliadorLimitacoes AVALIADOR = new AvaliadorLimitacoes();

    private TrocaPecaTeste() {
        // classe utilitaria: nao deve ser instanciada
    }

    public static void main(String[] args) {
        System.out.println("Testes da HU-011 - troca de peca no resumo");

        montagemGuiadaReabreESubstitui();
        removerPecaLimpaAvisos();
        revalidaSoquete();
        revalidaConectoresDaFonte();
        revalidaPotenciaDaFonte();
        revalidaGpuObrigatoria();
        telaEscolheEtapa();
        etapaVideoRemovePeca();
        etapaTrocaPedeConfirmacao();
        etapaProcessadorVoltaDoResumo();
        menuTrocaMemoria();
        menuTrocaInvalidaVoltaSemAlterar();

        System.exit(Assercao.concluir());
    }

    // ------------------------------------------------------ reabrir/substituir

    private static void montagemGuiadaReabreESubstitui() {
        Processador cpu = cpu(Soquete.AM5, true, 100, "1000.00");
        PlacaMae placa = placa(1, "500.00");
        Memoria original = memoria("300.00");
        Montagem montagem = montagem(cpu, placa, original, ssd("200.00"),
                fonte(750, 1, 0, 1, "400.00"));
        MontagemGuiada fluxo = new MontagemGuiada(montagem);

        Assercao.iguais("troca: fluxo comeca na etapa 1", 1, fluxo.etapaAtual());
        Assercao.iguais("troca: reabrir a etapa 3", 3, fluxo.reabrirEtapa(3));

        Memoria nova = memoria("450.00");
        fluxo.substituir(CategoriaComponente.MEMORIA, nova);

        Assercao.iguais("troca: memoria substituida", nova,
                montagem.obter(CategoriaComponente.MEMORIA).orElse(null));
        Assercao.iguais("troca: CPU preservada", cpu,
                montagem.obter(CategoriaComponente.CPU).orElse(null));
        Assercao.iguais("troca: placa-mae preservada", placa,
                montagem.obter(CategoriaComponente.PLACA_MAE).orElse(null));
        Assercao.iguais("troca: total recalculado", new BigDecimal("2550.00"),
                new ResumoMontagem(montagem).getTotal());

        boolean categoriaErrada = false;
        try {
            fluxo.substituir(CategoriaComponente.MEMORIA, placa);
        } catch (IllegalArgumentException causa) {
            categoriaErrada = true;
        }
        Assercao.verdadeiro("troca: peca de outra categoria e rejeitada", categoriaErrada);

        boolean etapaInvalida = false;
        try {
            fluxo.reabrirEtapa(7);
        } catch (IllegalArgumentException causa) {
            etapaInvalida = true;
        }
        Assercao.verdadeiro("troca: etapa fora do fluxo e rejeitada", etapaInvalida);
    }

    private static void removerPecaLimpaAvisos() {
        Processador cpu = cpu(Soquete.AM5, true, 100, "1000.00");
        PlacaMae placa = placa(1, "500.00");
        Memoria memoria = memoria("300.00");
        PlacaVideo gpu = gpu(1, 0, 220, "800.00");
        Montagem montagem = montagem(cpu, placa, memoria, gpu);

        Limitacao aviso = Limitacao.builder()
                .regra("L05")
                .limitante(placa)
                .afetado(gpu)
                .campo("versaoPCIe")
                .valorDoNovo("PCIe 4.0 x16")
                .valorSuportado("PCIe 3.0 x16")
                .consequencia("teste")
                .construir();
        montagem.registrarLimitacoes(List.of(aviso));

        Assercao.iguais("remover: devolve a GPU", gpu,
                montagem.remover(CategoriaComponente.PLACA_VIDEO).orElse(null));
        Assercao.verdadeiro("remover: GPU fora da montagem",
                montagem.obter(CategoriaComponente.PLACA_VIDEO).isEmpty());
        Assercao.verdadeiro("remover: aviso da GPU descartado",
                montagem.getLimitacoes().isEmpty());
        Assercao.verdadeiro("remover: categoria vazia nao falha",
                montagem.remover(CategoriaComponente.PLACA_VIDEO).isEmpty());
    }

    // ---------------------------------------------------------- revalidacao

    private static void revalidaSoquete() {
        Montagem montagem = montagem(cpu(Soquete.AM5, true, 100, "1000.00"),
                placa(1, "500.00"), memoria("300.00"), ssd("200.00"),
                fonte(1500, 0, 0, 1, "900.00"));
        RevalidadorMontagem revalidador = new RevalidadorMontagem();

        Assercao.verdadeiro("revalida: montagem valida",
                revalidador.primeiraInvalida(montagem).isEmpty());

        montagem.adicionar(cpu(Soquete.LGA1700, true, 100, "1000.00"));
        RevalidadorMontagem.EtapaInvalida invalida = revalidador.primeiraInvalida(montagem)
                .orElseThrow(() -> new IllegalStateException("soquete nao foi revalidado"));

        Assercao.iguais("revalida: etapa 2 apontada", 2, invalida.getEtapa());
        Assercao.iguais("revalida: categoria Placa-Mae", CategoriaComponente.PLACA_MAE,
                invalida.getCategoria());
        Assercao.iguais("revalida: motivo do soquete",
                "soquete AM5 x processador LGA1700", invalida.getMotivo());
        Assercao.iguais("revalida: lista completa de etapas", 1,
                revalidador.invalidas(montagem).size());
    }

    private static void revalidaConectoresDaFonte() {
        Montagem montagem = montagem(cpu(Soquete.AM5, false, 100, "1000.00"),
                placa(1, "500.00"), memoria("300.00"), ssd("200.00"),
                gpu(0, 1, 200, "800.00"), fonte(1500, 0, 1, 1, "900.00"));
        RevalidadorMontagem revalidador = new RevalidadorMontagem();

        Assercao.verdadeiro("revalida: montagem com GPU 1x8 e fonte valida",
                revalidador.primeiraInvalida(montagem).isEmpty());

        montagem.adicionar(gpu(1, 0, 200, "800.00"));
        RevalidadorMontagem.EtapaInvalida invalida = revalidador.primeiraInvalida(montagem)
                .orElseThrow(() -> new IllegalStateException("conector nao foi revalidado"));

        Assercao.iguais("revalida: etapa 6 apontada", 6, invalida.getEtapa());
        Assercao.iguais("revalida: categoria Fonte", CategoriaComponente.FONTE,
                invalida.getCategoria());
        Assercao.iguais("revalida: motivo do conector",
                "sem conector de 16 pinos (a placa de video exige 1)", invalida.getMotivo());
    }

    private static void revalidaPotenciaDaFonte() {
        Montagem montagem = montagem(cpu(Soquete.AM5, false, 1000, "1000.00"),
                placa(1, "500.00"), memoria("300.00"), ssd("200.00"),
                gpu(0, 1, 100, "800.00"), fonte(750, 0, 1, 1, "400.00"));
        RevalidadorMontagem revalidador = new RevalidadorMontagem();

        RevalidadorMontagem.EtapaInvalida invalida = revalidador.primeiraInvalida(montagem)
                .orElseThrow(() -> new IllegalStateException("potencia nao foi revalidada"));

        Assercao.iguais("revalida: potencia aponta a etapa 6", 6, invalida.getEtapa());
        Assercao.verdadeiro("revalida: motivo da potencia",
                invalida.getMotivo().contains("com margem de 40%"));
    }

    private static void revalidaGpuObrigatoria() {
        Montagem montagem = montagem(cpu(Soquete.AM5, true, 100, "1000.00"),
                placa(1, "500.00"), memoria("300.00"), ssd("200.00"),
                fonte(1500, 0, 0, 1, "900.00"));
        RevalidadorMontagem revalidador = new RevalidadorMontagem();

        Assercao.verdadeiro("revalida: sem GPU e valido com iGPU",
                revalidador.primeiraInvalida(montagem).isEmpty());

        montagem.adicionar(cpu(Soquete.AM5, false, 100, "1000.00"));
        RevalidadorMontagem.EtapaInvalida invalida = revalidador.primeiraInvalida(montagem)
                .orElseThrow(() -> new IllegalStateException("GPU obrigatoria nao foi revalidada"));

        Assercao.iguais("revalida: etapa 4 apontada", 4, invalida.getEtapa());
        Assercao.iguais("revalida: categoria Placa de Video",
                CategoriaComponente.PLACA_VIDEO, invalida.getCategoria());
        Assercao.verdadeiro("revalida: motivo do video integrado",
                invalida.getMotivo().contains("video integrado"));
    }

    // ---------------------------------------------------------- tela / etapas

    private static void telaEscolheEtapa() {
        Montagem montagem = montagemCompleta();
        ResumoMontagem resumo = new ResumoMontagem(montagem);

        Saida escolha = new Saida("3\n");
        Assercao.iguais("tela: escolhe a etapa 3", 3,
                new TelaResumo(escolha.terminal).escolherEtapa(resumo));
        String texto = escolha.texto();
        Assercao.verdadeiro("tela: titulo da troca", texto.contains(TelaResumo.TITULO_TROCA));
        Assercao.verdadeiro("tela: lista a etapa 1",
                texto.contains("1) Processador - AMD Ryzen Teste - R$ 1.000,00"));
        Assercao.verdadeiro("tela: etapa sem peca",
                texto.contains("4) Placa de Video - (nao selecionado)"));
        Assercao.verdadeiro("tela: oferece voltar", texto.contains("[0] Voltar"));

        Saida voltar = new Saida("0\n");
        Assercao.iguais("tela: 0 volta ao resumo", 0,
                new TelaResumo(voltar.terminal).escolherEtapa(resumo));

        Saida invalida = new Saida("9\n2\n");
        Assercao.iguais("tela: etapa invalida pede de novo", 2,
                new TelaResumo(invalida.terminal).escolherEtapa(resumo));
        Assercao.verdadeiro("tela: mensagem de etapa invalida",
                invalida.texto().contains(TelaResumo.MENSAGEM_ETAPA_INVALIDA));
    }

    private static void etapaVideoRemovePeca() {
        Processador cpu = cpu(Soquete.AM5, true, 100, "1000.00");
        PlacaMae placa = placa(1, "500.00");
        Memoria memoria = memoria("300.00");
        Armazenamento ssd = ssd("200.00");
        Fonte fonte = fonte(750, 1, 0, 1, "400.00");
        PlacaVideo gpu = gpu(1, 0, 220, "800.00");
        Montagem montagem = montagem(cpu, placa, memoria, ssd, fonte, gpu);
        MontagemGuiada fluxo = new MontagemGuiada(montagem);
        fluxo.reabrirEtapa(4);

        Saida saida = new Saida("R\n");
        ResultadoEtapa resultado = new EtapaPlacaVideo(saida.terminal, fluxo, FILTRO,
                AVALIADOR, true).executar();

        Assercao.iguais("GPU: [R] remove e conclui", ResultadoEtapa.CONCLUIDA, resultado);
        Assercao.verdadeiro("GPU: peca removida da montagem",
                montagem.obter(CategoriaComponente.PLACA_VIDEO).isEmpty());
        String texto = saida.texto();
        Assercao.verdadeiro("GPU: rodape oferece remover",
                texto.contains(EtapaPlacaVideo.RODAPE_REMOVER));
        Assercao.verdadeiro("GPU: mensagem de remocao",
                texto.contains(EtapaPlacaVideo.MENSAGEM_REMOVIDA));

        Montagem semGpu = montagem(cpu, placa, memoria, ssd, fonte);
        MontagemGuiada fluxoSemGpu = new MontagemGuiada(semGpu);
        fluxoSemGpu.reabrirEtapa(4);
        Saida volta = new Saida("0\n");
        ResultadoEtapa resultadoVolta = new EtapaPlacaVideo(volta.terminal, fluxoSemGpu,
                FILTRO, AVALIADOR, true).executar();

        Assercao.iguais("GPU: sem peca o [0] volta ao resumo", ResultadoEtapa.VOLTAR,
                resultadoVolta);
        Assercao.falso("GPU: sem peca nao oferece remover",
                volta.texto().contains(EtapaPlacaVideo.RODAPE_REMOVER));
        Assercao.verdadeiro("GPU: rodape de voltar", volta.texto().contains("[0] Voltar"));
    }

    private static void etapaTrocaPedeConfirmacao() {
        Processador cpu = cpuDoCatalogo(true);
        PlacaMae placa = (PlacaMae) FILTRO.filtrar(CategoriaComponente.PLACA_MAE, montagem(cpu))
                .getCompativeis().get(0);
        Memoria reservada = memoria("999.00");

        Montagem montagemN = montagem(cpu, placa, reservada);
        MontagemGuiada fluxoN = new MontagemGuiada(montagemN);
        fluxoN.reabrirEtapa(3);
        Saida negar = new Saida("1\nN\n0\n");
        ResultadoEtapa resultadoN = new EtapaMemoria(negar.terminal, fluxoN, FILTRO,
                AVALIADOR).executar();

        Assercao.iguais("troca: N recusa e volta ao resumo", ResultadoEtapa.VOLTAR,
                resultadoN);
        Assercao.iguais("troca: N mantem a peca atual", reservada,
                montagemN.obter(CategoriaComponente.MEMORIA).orElse(null));
        Assercao.verdadeiro("troca: pergunta S/N", negar.texto().contains("Trocar ")
                && negar.texto().contains("(S/N)"));

        Montagem montagemS = montagem(cpu, placa, reservada);
        MontagemGuiada fluxoS = new MontagemGuiada(montagemS);
        fluxoS.reabrirEtapa(3);
        Componente nova = FILTRO.filtrar(CategoriaComponente.MEMORIA, montagemS)
                .getCompativeis().get(0);
        boolean aviso = !AVALIADOR.avaliar(montagemS, nova).isEmpty();
        Saida aceitar = new Saida("1\nS\n" + flag(aviso));
        ResultadoEtapa resultadoS = new EtapaMemoria(aceitar.terminal, fluxoS, FILTRO,
                AVALIADOR).executar();

        Assercao.iguais("troca: S aplica a troca", ResultadoEtapa.CONCLUIDA, resultadoS);
        Assercao.iguais("troca: S troca a peca", nova,
                montagemS.obter(CategoriaComponente.MEMORIA).orElse(null));
    }

    private static void etapaProcessadorVoltaDoResumo() {
        Processador cpu = cpu(Soquete.AM5, true, 100, "1000.00");
        Processador outra = cpu(Soquete.AM5, true, 120, "1200.00");
        Montagem montagem = montagem(cpu);
        MontagemGuiada fluxo = new MontagemGuiada(montagem);
        fluxo.reabrirEtapa(1);

        Saida saida = new Saida("0\n");
        ResultadoEtapa resultado = new EtapaProcessador(saida.terminal, fluxo,
                List.of(cpu, outra), AVALIADOR, true).executar();

        Assercao.iguais("CPU: [0] no resumo volta sem cancelar", ResultadoEtapa.VOLTAR,
                resultado);
        Assercao.iguais("CPU: montagem preservada", cpu,
                montagem.obter(CategoriaComponente.CPU).orElse(null));
        Assercao.verdadeiro("CPU: rodape de voltar", saida.texto().contains("[0] Voltar"));

        Montagem paraCancelar = montagem(cpu);
        MontagemGuiada fluxoNormal = new MontagemGuiada(paraCancelar);
        Saida cancelar = new Saida("0\nS\n");
        ResultadoEtapa resultadoCancelar = new EtapaProcessador(cancelar.terminal,
                fluxoNormal, List.of(cpu), AVALIADOR).executar();

        Assercao.iguais("CPU: [0] normal cancela", ResultadoEtapa.CANCELADA,
                resultadoCancelar);
        Assercao.verdadeiro("CPU: cancelar descarta a montagem", paraCancelar.estaVazia());
    }

    // ------------------------------------------------------- menu (integracao)

    private static void menuTrocaMemoria() {
        int indiceCpu = indiceCpu(true);
        Processador cpu = cpuDoCatalogo(true);
        Componente placa = FILTRO.filtrar(CategoriaComponente.PLACA_MAE, montagem(cpu))
                .getCompativeis().get(0);
        Montagem base = montagem(cpu, placa);
        ListaFiltrada listaMemorias = FILTRO.filtrar(CategoriaComponente.MEMORIA, base);
        Componente memoria1 = listaMemorias.getCompativeis().get(0);
        Componente memoria2 = listaMemorias.getCompativeis().get(1);
        Montagem comMemoria = montagem(cpu, placa, memoria1);
        Componente armazenamento = FILTRO.filtrar(CategoriaComponente.ARMAZENAMENTO,
                comMemoria).getCompativeis().get(0);
        Montagem comArmazenamento = montagem(cpu, placa, memoria1, armazenamento);
        Componente fonte = FILTRO.filtrar(CategoriaComponente.FONTE, comArmazenamento)
                .getCompativeis().get(0);

        boolean avisoPlaca = !AVALIADOR.avaliar(montagem(cpu), placa).isEmpty();
        boolean avisoMemoria1 = !AVALIADOR.avaliar(base, memoria1).isEmpty();
        boolean avisoMemoria2 = !AVALIADOR.avaliar(base, memoria2).isEmpty();
        boolean avisoArmazenamento = !AVALIADOR.avaliar(comMemoria, armazenamento).isEmpty();
        boolean avisoFonte = !AVALIADOR.avaliar(comArmazenamento, fonte).isEmpty();

        String entrada = "1\n" + indiceCpu + "\n1\n" + flag(avisoPlaca)
                + "1\n" + flag(avisoMemoria1) + "0\n"
                + "1\n" + flag(avisoArmazenamento) + "1\n" + flag(avisoFonte)
                + "T\n3\n2\nS\n" + flag(avisoMemoria2) + "0\n";
        Saida saida = new Saida(entrada);
        int codigo = new MenuPrincipal(CATALOGO, saida.terminal).executar();

        Assercao.iguais("menu troca: execucao sem erro", 0, codigo);
        String texto = saida.texto();
        Assercao.verdadeiro("menu troca: lista de etapas",
                texto.contains(TelaResumo.TITULO_TROCA));
        Assercao.verdadeiro("menu troca: nova memoria escolhida",
                texto.contains("Memoria selecionada: "
                        + TabelaComponentes.nomeExibicao(memoria2)));
        Assercao.iguais("menu troca: resumo antes e depois", 2,
                contar(texto, TelaResumo.TITULO));

        BigDecimal totalDepois = soma(cpu.getPrecoMedioBRL(), placa.getPrecoMedioBRL(),
                memoria2.getPrecoMedioBRL(), armazenamento.getPrecoMedioBRL(),
                fonte.getPrecoMedioBRL());
        Assercao.verdadeiro("menu troca: total recalculado",
                texto.contains("TOTAL: " + FormatadorMoeda.reais(totalDepois)));
        Assercao.verdadeiro("menu troca: demais pecas preservadas",
                texto.contains(TabelaComponentes.nomeExibicao(placa))
                        && texto.contains(TabelaComponentes.nomeExibicao(armazenamento))
                        && texto.contains(TabelaComponentes.nomeExibicao(fonte)));
    }

    private static void menuTrocaInvalidaVoltaSemAlterar() {
        int indiceCpu = indiceCpu(true);
        Processador cpu = cpuDoCatalogo(true);
        Processador outro = CATALOGO.porCategoria(CategoriaComponente.CPU).stream()
                .map(Processador.class::cast)
                .filter(candidato -> candidato.isPossuiVideoIntegrado()
                        && candidato.getSoquete() != cpu.getSoquete())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "base sem CPU iGPU de outro soquete"));
        int indiceOutro = 0;
        List<Componente> cpus = CATALOGO.porCategoria(CategoriaComponente.CPU);
        for (int posicao = 0; posicao < cpus.size(); posicao++) {
            if (cpus.get(posicao) == outro) {
                indiceOutro = posicao + 1;
                break;
            }
        }

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
        Montagem completa = montagem(cpu, placa, memoria, armazenamento, fonte);
        boolean avisoCpu = !AVALIADOR.avaliar(completa, outro).isEmpty();

        String entrada = "1\n" + indiceCpu + "\n1\n" + flag(avisoPlaca)
                + "1\n" + flag(avisoMemoria) + "0\n"
                + "1\n" + flag(avisoArmazenamento) + "1\n" + flag(avisoFonte)
                + "T\n1\n" + indiceOutro + "\nS\n" + flag(avisoCpu)
                + "0\n0\n";
        Saida saida = new Saida(entrada);
        int codigo = new MenuPrincipal(CATALOGO, saida.terminal).executar();

        Assercao.iguais("menu invalido: execucao sem erro", 0, codigo);
        String texto = saida.texto();
        Assercao.verdadeiro("menu invalido: etapa 2 apontada",
                texto.contains("Configuracao invalida: a etapa 2"));
        Assercao.verdadeiro("menu invalido: CPU nova escolhida",
                texto.contains("Processador selecionado: "
                        + TabelaComponentes.nomeExibicao(outro)));

        BigDecimal totalOriginal = soma(cpu.getPrecoMedioBRL(), placa.getPrecoMedioBRL(),
                memoria.getPrecoMedioBRL(), armazenamento.getPrecoMedioBRL(),
                fonte.getPrecoMedioBRL());
        Assercao.iguais("menu invalido: [0] restaura o resumo", 2,
                contar(texto, "TOTAL: " + FormatadorMoeda.reais(totalOriginal)));
        Assercao.iguais("menu invalido: resumo antes e depois", 2,
                contar(texto, TelaResumo.TITULO));
    }

    // ---------------------------------------------------------------- auxiliares

    private static Montagem montagemCompleta() {
        return montagem(cpu(Soquete.AM5, true, 100, "1000.00"), placa(1, "500.00"),
                memoria("300.00"), ssd("200.00"), fonte(750, 1, 0, 1, "400.00"));
    }

    /** Soma os precos informados (ignora nulos), como o resumo faz. */
    private static BigDecimal soma(BigDecimal... valores) {
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal valor : valores) {
            if (valor != null) {
                total = total.add(valor);
            }
        }
        return total;
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

    private static Processador cpu(Soquete soquete, boolean videoIntegrado, int consumoMaximo,
            String preco) {
        return Processador.builder()
                .id("cpu-troca-" + soquete + "-" + consumoMaximo)
                .modelo("Ryzen Teste")
                .fabricante("AMD")
                .precoMedioBRL(preco)
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
                .possuiVideoIntegrado(videoIntegrado)
                .indiceDesempenhoSingleThread(90)
                .indiceDesempenhoMultithread(90)
                .construir();
    }

    private static PlacaMae placa(int conectoresCpu8, String preco) {
        return PlacaMae.builder()
                .id("placa-troca-" + conectoresCpu8)
                .modelo("B650 Teste")
                .fabricante("ASUS")
                .precoMedioBRL(preco)
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

    private static Memoria memoria(String preco) {
        return Memoria.builder()
                .id("ram-troca-" + preco)
                .modelo("Vengeance Teste")
                .fabricante("CORSAIR")
                .precoMedioBRL(preco)
                .tipo(TipoMemoria.DDR5)
                .capacidadePorPenteGB(16)
                .quantidadePentes(2)
                .frequenciaMTs(6000)
                .frequenciaJEDECBaseMTs(4800)
                .latenciaCAS(30)
                .tensaoVolts(1.35)
                .perfilEXPO(true)
                .perfilXMP(true)
                .consumoEstimadoWatts(6)
                .construir();
    }

    private static Armazenamento ssd(String preco) {
        return Armazenamento.builder()
                .id("ssd-troca-" + preco)
                .modelo("SSD Teste")
                .fabricante("Kingston")
                .precoMedioBRL(preco)
                .tipoMidia(TipoMidia.SSD)
                .interfaceArmazenamento(InterfaceArmazenamento.NVME)
                .formato(FormatoArmazenamento.M2_2280)
                .capacidadeGB(2000)
                .versaoPCIe(4)
                .pistasPCIe(4)
                .leituraSequencialMBs(7000)
                .escritaSequencialMBs(7000)
                .consumoEstimadoWatts(5)
                .construir();
    }

    private static Fonte fonte(int potencia, int conectores16, int conectores8,
            int conectoresCpu8, String preco) {
        return Fonte.builder()
                .id("fonte-troca-" + potencia + "-" + conectores16 + "-" + conectores8)
                .modelo("Fonte Teste")
                .fabricante("Corsair")
                .precoMedioBRL(preco)
                .potenciaTotalWatts(potencia)
                .potenciaTrilho12VWatts(potencia)
                .certificacao80Plus(Certificado80Plus.GOLD)
                .quantidadeConectores16pin12VHPWR(conectores16)
                .quantidadeConectoresPCIe8pin(conectores8)
                .quantidadeConectoresCPU8pin(conectoresCpu8)
                .construir();
    }

    private static PlacaVideo gpu(int conectores16, int conectores8, int tdp, String preco) {
        return PlacaVideo.builder()
                .id("gpu-troca-" + conectores16 + "-" + conectores8)
                .modelo("RTX Teste")
                .fabricante("NVIDIA")
                .precoMedioBRL(preco)
                .memoriaVRAMGB(12)
                .tipoMemoriaVRAM(TipoMemoriaVRAM.GDDR6X)
                .versaoPCIe(4)
                .pistasPCIe(16)
                .tdpWatts(tdp)
                .picoTransienteWatts(tdp + 100)
                .resolucaoAlvo(Resolucao.QHD_1440P)
                .indiceDesempenhoGPU(85)
                .quantidadeConectores16pin(conectores16)
                .quantidadeConectores8pin(conectores8)
                .construir();
    }

    private static Montagem montagem(Componente... pecas) {
        Montagem montagem = new Montagem();
        for (Componente peca : pecas) {
            montagem.adicionar(peca);
        }
        return montagem;
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

    /** Linha extra para o Enter do aviso de selecao. */
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
