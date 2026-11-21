package pcbuilder.analise;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import pcbuilder.config.ParametrosCalculo;
import pcbuilder.enums.CategoriaComponente;
import pcbuilder.modelo.Armazenamento;
import pcbuilder.modelo.Componente;
import pcbuilder.modelo.Montagem;
import pcbuilder.modelo.PlacaVideo;
import pcbuilder.modelo.Processador;

/**
 * Soma o consumo do sistema escolhido ate aqui (regra L06, usada tambem por C14).
 *
 * <p>CPU e placa de video usam o consumo de pico real ({@code consumoMaximoWatts} e
 * {@code tdpWatts}); as demais pecas usam {@code consumoEstimadoWatts} (o armazenamento
 * cai para {@code consumoMedioWatts} quando o campo comum nao esta cadastrado). Quando
 * uma peca auxiliar ainda nao foi escolhida (ou nao tem o dado), entra uma unica vez a
 * reserva {@link ParametrosCalculo#CONSUMO_BASE_SISTEMA_WATTS}, que cobre fans e
 * perifericos que o fluxo nao coleta.</p>
 *
 * <p>O consumo <strong>nao</strong> e gravado em nenhum componente: e sempre calculado
 * a partir das pecas da montagem (secao 16 do modelo de dados). O
 * {@link #detalhar(Montagem) detalhamento} alimenta o comando {@code [C] Ver calculo do
 * consumo} da etapa 6.</p>
 */
public final class CalculadoraConsumo {

    /** Pecas cujo consumo vem de {@code consumoEstimadoWatts} (campo comum). */
    private static final List<CategoriaComponente> AUXILIARES = List.of(
            CategoriaComponente.PLACA_MAE,
            CategoriaComponente.MEMORIA,
            CategoriaComponente.ARMAZENAMENTO);

    private CalculadoraConsumo() {
        // classe utilitaria: nao deve ser instanciada
    }

    /**
     * Consumo total maximo do sistema montado ate aqui.
     *
     * @param montagem pecas ja escolhidas (pode estar parcial)
     * @return consumo em watts
     */
    public static int consumoTotalSistemaWatts(Montagem montagem) {
        int total = 0;
        for (ItemConsumo item : detalhar(montagem)) {
            total += item.getWatts();
        }
        return total;
    }

    /**
     * Consumo com a margem de seguranca da fonte (regra C14).
     *
     * @param montagem pecas ja escolhidas
     * @return {@code consumoTotalSistemaWatts} x {@code 1,40}, arredondado para cima
     */
    public static int consumoComMargemWatts(Montagem montagem) {
        return (int) Math.ceil(consumoTotalSistemaWatts(montagem)
                * ParametrosCalculo.MARGEM_SEGURANCA_FONTE);
    }

    /**
     * Detalha a soma componente por componente, para o comando {@code [C]}.
     *
     * @param montagem pecas ja escolhidas
     * @return as linhas do calculo, na ordem CPU, GPU, placa-mae, RAM, armazenamento e
     *         reserva (quando necessaria)
     */
    public static List<ItemConsumo> detalhar(Montagem montagem) {
        Objects.requireNonNull(montagem, "montagem");
        List<ItemConsumo> itens = new ArrayList<>();

        ComponentesDaMontagem.cpu(montagem).ifPresent(cpu -> itens.add(
                new ItemConsumo("Processador " + nome(cpu), cpu.getConsumoMaximoWatts())));
        ComponentesDaMontagem.placaVideo(montagem).ifPresent(gpu -> itens.add(
                new ItemConsumo("Placa de video " + nome(gpu), gpu.getTdpWatts())));

        boolean reservaNecessaria = false;
        for (CategoriaComponente categoria : AUXILIARES) {
            Optional<Componente> peca = montagem.obter(categoria);
            Integer consumo = peca.map(item -> consumoDe(item, categoria)).orElse(null);
            if (peca.isPresent() && consumo != null) {
                itens.add(new ItemConsumo(rotulo(categoria) + " " + nome(peca.get()), consumo));
            } else {
                reservaNecessaria = true;
            }
        }
        if (reservaNecessaria) {
            itens.add(new ItemConsumo("Reserva (fans/perifericos)",
                    ParametrosCalculo.CONSUMO_BASE_SISTEMA_WATTS));
        }
        return List.copyOf(itens);
    }

    private static String nome(Componente componente) {
        String fabricante = componente.getFabricante();
        if (fabricante == null || fabricante.isBlank()) {
            return componente.getModelo();
        }
        return fabricante + " " + componente.getModelo();
    }

    /**
     * Consumo de uma peca auxiliar: o campo comum quando cadastrado; no armazenamento,
     * cai para {@code consumoMedioWatts} (a base traz o dado especifico da secao 7).
     */
    private static Integer consumoDe(Componente componente, CategoriaComponente categoria) {
        if (componente.getConsumoEstimadoWatts() != null) {
            return componente.getConsumoEstimadoWatts();
        }
        if (categoria == CategoriaComponente.ARMAZENAMENTO
                && componente instanceof Armazenamento armazenamento
                && armazenamento.getConsumoMedioWatts() != null) {
            return (int) Math.ceil(armazenamento.getConsumoMedioWatts());
        }
        return null;
    }

    private static String rotulo(CategoriaComponente categoria) {
        switch (categoria) {
            case PLACA_MAE:
                return "Placa-mae";
            case MEMORIA:
                return "Memoria";
            case ARMAZENAMENTO:
                return "Armazenamento";
            default:
                return categoria.getRotulo();
        }
    }

    /** Uma linha do detalhamento do consumo. */
    public static final class ItemConsumo {

        private final String descricao;
        private final int watts;

        /**
         * @param descricao nome da peca (fabricante + modelo) e papel no calculo
         * @param watts consumo considerado em watts
         */
        public ItemConsumo(String descricao, int watts) {
            this.descricao = Objects.requireNonNull(descricao, "descricao");
            this.watts = watts;
        }

        public String getDescricao() {
            return descricao;
        }

        public int getWatts() {
            return watts;
        }
    }
}

