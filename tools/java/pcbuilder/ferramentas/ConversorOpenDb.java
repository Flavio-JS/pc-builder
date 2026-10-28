package pcbuilder.ferramentas;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import pcbuilder.enums.NivelProblema;
import pcbuilder.json.Json;
import pcbuilder.modelo.Componente;
import pcbuilder.servico.ProblemaCadastro;
import pcbuilder.servico.ResultadoValidacao;
import pcbuilder.servico.ValidadorCadastro;

/**
 * Gera {@code src/main/resources/base/componentes.json} a partir do catalogo
 * OpenDB.
 *
 * <p>Uso (a partir da pasta {@code pc-builder}):</p>
 * <pre>
 * java -cp "build/classes;build/tools" pcbuilder.ferramentas.ConversorOpenDb \
 *      --entrada ../buildcores-open-db/open-db \
 *      --saida src/main/resources/base/componentes.json
 * </pre>
 *
 * <p>O conversor monta os objetos do proprio dominio, valida cada um com
 * {@link ValidadorCadastro} e so escreve o arquivo com os registros aprovados. O
 * codigo de saida e {@code 2} quando algum registro e descartado, para o problema
 * ser corrigido nas heuristicas em vez de passar despercebido.</p>
 */
public final class ConversorOpenDb {

    private static final int AMOSTRA_DE_ERROS = 10;

    private ConversorOpenDb() {
        // classe utilitaria: nao deve ser instanciada
    }

    public static void main(String[] args) throws IOException {
        Path entrada = Path.of("..", "buildcores-open-db", "open-db");
        Path saida = Path.of("src", "main", "resources", "base", "componentes.json");

        for (int indice = 0; indice < args.length; indice++) {
            switch (args[indice]) {
                case "--entrada" -> entrada = Path.of(args[++indice]);
                case "--saida" -> saida = Path.of(args[++indice]);
                default -> {
                    System.err.println("Opcao desconhecida: " + args[indice]);
                    System.err.println("Uso: ConversorOpenDb [--entrada <pasta>] [--saida <arquivo>]");
                    System.exit(1);
                }
            }
        }
        if (!Files.isDirectory(entrada)) {
            System.err.println("Pasta do catalogo nao encontrada: " + entrada.toAbsolutePath());
            System.err.println("Baixe o catalogo em https://github.com/buildcores/buildcores-open-db");
            System.err.println("(a base ja gerada em src/main/resources/base nao precisa dele)");
            System.exit(1);
        }

        long inicio = System.currentTimeMillis();
        Estatisticas estatisticas = new Estatisticas();
        List<Componente> componentes = new ArrayList<>();
        componentes.addAll(MapeadorCpu.converter(ler(entrada.resolve("CPU")), estatisticas));
        componentes.addAll(MapeadorPlacaMae.converter(ler(entrada.resolve("Motherboard")), estatisticas));
        componentes.addAll(MapeadorMemoria.converter(ler(entrada.resolve("RAM")), estatisticas));
        componentes.addAll(MapeadorPlacaVideo.converter(ler(entrada.resolve("GPU")), estatisticas));
        componentes.addAll(MapeadorArmazenamento.converter(ler(entrada.resolve("Storage")), estatisticas));
        componentes.addAll(MapeadorFonte.converter(ler(entrada.resolve("PSU")), estatisticas));

        List<Componente> unicos = removerIdsRepetidos(componentes, estatisticas);
        ResultadoValidacao validacao = new ValidadorCadastro().validar(unicos);
        escrever(saida, validacao.getRegistrosValidos(), entrada);

        estatisticas.imprimir();
        System.out.println("Arquivo gerado: " + saida.toAbsolutePath());
        System.out.println("Tamanho: " + (Files.size(saida) / 1024) + " KB");
        System.out.println("Tempo: " + (System.currentTimeMillis() - inicio) + " ms");

        if (validacao.possuiErros()) {
            System.out.println();
            System.out.println("ATENCAO: " + validacao.getTotalErros()
                    + " problema(s) de nivel ERRO - registros descartados.");
            List<ProblemaCadastro> erros = validacao.getProblemas(NivelProblema.ERRO);
            for (int indice = 0; indice < Math.min(AMOSTRA_DE_ERROS, erros.size()); indice++) {
                System.out.println("  " + erros.get(indice).formatar());
            }
            System.exit(2);
        }
    }

    private static List<Object> ler(Path pasta) throws IOException {
        List<Object> registros = new ArrayList<>();
        if (!Files.isDirectory(pasta)) {
            System.out.println("Pasta ausente, ignorada: " + pasta);
            return registros;
        }
        List<Path> arquivos = new ArrayList<>();
        try (var listagem = Files.list(pasta)) {
            listagem.filter(caminho -> caminho.getFileName().toString().endsWith(".json"))
                    .forEach(arquivos::add);
        }
        arquivos.sort(null);
        for (Path arquivo : arquivos) {
            registros.add(Json.ler(Files.readString(arquivo, StandardCharsets.UTF_8)));
        }
        return registros;
    }

    /** Mantem um unico registro por id (o catalogo pode repetir a mesma peca). */
    private static List<Componente> removerIdsRepetidos(List<Componente> componentes,
            Estatisticas estatisticas) {
        Set<String> ids = new LinkedHashSet<>();
        List<Componente> unicos = new ArrayList<>();
        for (Componente componente : componentes) {
            if (ids.add(componente.getId())) {
                unicos.add(componente);
            } else {
                estatisticas.ignorar("Ignorado: id repetido no catalogo");
            }
        }
        return unicos;
    }

    private static void escrever(Path saida, List<Componente> componentes, Path origem)
            throws IOException {
        Map<String, Object> cabecalho = new LinkedHashMap<>();
        cabecalho.put("versao", Integer.valueOf(1));
        cabecalho.put("geradoEm", LocalDate.now().toString());
        cabecalho.put("origem", origem.toString().replace('\\', '/'));
        cabecalho.put("total", Integer.valueOf(componentes.size()));
        cabecalho.put("porCategoria", contarPorCategoria(componentes));

        StringBuilder json = new StringBuilder(Json.escrever(cabecalho));
        json.setLength(json.length() - 1); // remove o '}' para encaixar o array
        json.append(",\"componentes\":[\n");

        for (int indice = 0; indice < componentes.size(); indice++) {
            Map<String, Object> campos = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entrada : componentes.get(indice).campos().entrySet()) {
                if (entrada.getValue() != null) {
                    campos.put(entrada.getKey(), entrada.getValue());
                }
            }
            json.append(Json.escrever(campos));
            json.append(indice == componentes.size() - 1 ? "\n" : ",\n");
        }
        json.append("]}\n");

        Files.createDirectories(saida.toAbsolutePath().getParent());
        Files.writeString(saida, json.toString(), StandardCharsets.UTF_8);
    }

    private static Map<String, Object> contarPorCategoria(List<Componente> componentes) {
        Map<String, Integer> contagem = new TreeMap<>();
        for (Componente componente : componentes) {
            contagem.merge(componente.getCategoria().name(), Integer.valueOf(1),
                    (atual, novo) -> Integer.valueOf(atual.intValue() + 1));
        }
        return new LinkedHashMap<>(contagem);
    }
}
