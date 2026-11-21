package pcbuilder.analise.filtro;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import pcbuilder.analise.ComponentesDaMontagem;
import pcbuilder.config.OrdemEtapas;
import pcbuilder.enums.CategoriaComponente;
import pcbuilder.modelo.Componente;
import pcbuilder.modelo.Montagem;

/**
 * Revalida a montagem inteira depois de uma troca (HU-011).
 *
 * <p>Roda as mesmas regras duras do filtro (C01, C02, C03, C04, C05, C10, C12, C14) sobre
 * cada peca <strong>ja escolhida</strong>: a peca e retirada da montagem e comparada com o
 * resto, exatamente como o filtro faz com um candidato. A primeira etapa invalida e
 * devolvida com o motivo para o menu levar o usuario direto a ela.</p>
 *
 * <p>A regra C06 (CPU sem video integrado) nao oculta modelos, entao o caso "trocou a CPU
 * para uma sem iGPU e nao ha placa de video" e tratado explicitamente.</p>
 */
public final class RevalidadorMontagem {

    /**
     * Primeira etapa invalida, na ordem do fluxo.
     *
     * @param montagem montagem com as pecas escolhidas
     * @return a etapa a revisar, ou vazio quando a montagem continua valida
     */
    public Optional<EtapaInvalida> primeiraInvalida(Montagem montagem) {
        List<EtapaInvalida> todas = invalidas(montagem);
        return todas.isEmpty() ? Optional.empty() : Optional.of(todas.get(0));
    }

    /**
     * Todas as etapas invalidas, na ordem do fluxo (um motivo por etapa).
     *
     * @param montagem montagem com as pecas escolhidas
     * @return as etapas a revisar; vazio quando a montagem continua valida
     */
    public List<EtapaInvalida> invalidas(Montagem montagem) {
        Objects.requireNonNull(montagem, "montagem");
        List<EtapaInvalida> invalidas = new ArrayList<>();

        boolean cpuSemVideo = ComponentesDaMontagem.cpu(montagem)
                .map(cpu -> !cpu.isPossuiVideoIntegrado())
                .orElse(false);
        if (cpuSemVideo && montagem.obter(CategoriaComponente.PLACA_VIDEO).isEmpty()) {
            invalidas.add(new EtapaInvalida(4, CategoriaComponente.PLACA_VIDEO,
                    "o processador nao tem video integrado e a placa de video e obrigatoria"));
        }

        for (int etapa = 1; etapa <= OrdemEtapas.TOTAL_ETAPAS; etapa++) {
            CategoriaComponente categoria = OrdemEtapas.categoria(etapa);
            Optional<Componente> peca = montagem.obter(categoria);
            if (peca.isEmpty()) {
                continue;
            }
            Montagem semAPeca = semCategoria(montagem, categoria);
            for (RegraFiltro regra : FiltroEtapa.regrasPadrao()) {
                if (regra.categoriaAfetada() != categoria) {
                    continue;
                }
                Optional<String> motivo = regra.motivoIncompatibilidade(semAPeca, peca.get());
                if (motivo.isPresent()) {
                    invalidas.add(new EtapaInvalida(etapa, categoria, motivo.get()));
                    break;
                }
            }
        }
        return List.copyOf(invalidas);
    }

    private static Montagem semCategoria(Montagem montagem, CategoriaComponente categoria) {
        Montagem copia = new Montagem();
        for (Componente componente : montagem.pecas().values()) {
            if (componente.getCategoria() != categoria) {
                copia.adicionar(componente);
            }
        }
        return copia;
    }

    /** Etapa que precisa ser revista, com o motivo em uma linha. */
    public static final class EtapaInvalida {

        private final int etapa;
        private final CategoriaComponente categoria;
        private final String motivo;

        public EtapaInvalida(int etapa, CategoriaComponente categoria, String motivo) {
            this.etapa = etapa;
            this.categoria = categoria;
            this.motivo = Objects.requireNonNull(motivo, "motivo");
        }

        public int getEtapa() {
            return etapa;
        }

        public CategoriaComponente getCategoria() {
            return categoria;
        }

        public String getMotivo() {
            return motivo;
        }

        @Override
        public String toString() {
            return "etapa " + etapa + " (" + categoria.getRotulo() + "): " + motivo;
        }
    }
}
