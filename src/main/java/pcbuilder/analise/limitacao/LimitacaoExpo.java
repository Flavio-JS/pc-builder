package pcbuilder.analise.limitacao;

import java.util.List;
import java.util.Optional;

import pcbuilder.analise.ComponentesDaMontagem;
import pcbuilder.enums.CategoriaComponente;
import pcbuilder.enums.SeveridadeLimitacao;
import pcbuilder.modelo.Componente;
import pcbuilder.modelo.Limitacao;
import pcbuilder.modelo.Memoria;
import pcbuilder.modelo.Montagem;
import pcbuilder.modelo.PlacaMae;

/**
 * Alerta <strong>C02</strong> (parte de aviso) - perfil de memoria EXPO/XMP x placa-mae.
 *
 * <p>O kit encaixa no slot (a parte dura da C02 e o filtro de tipo DDR), mas se a placa
 * nao suporta <strong>nenhum</strong> dos perfis do kit, ele roda no perfil JEDEC - mais
 * lento que a frequencia anunciada. Na pratica e um aviso para o usuario nao pagar por
 * velocidade que nao vai usar; a escolha continua valida.</p>
 *
 * <p>Se o kit tem EXPO e XMP e a placa suporta ao menos um dos dois, nao ha aviso: o
 * perfil suportado entrega a frequencia.</p>
 */
public final class LimitacaoExpo implements RegraLimitacao {

    @Override
    public String id() {
        return "C02";
    }

    @Override
    public List<Limitacao> avaliar(Montagem montagem, Componente candidato) {
        if (!regraSeAplica(candidato)) {
            return List.of();
        }
        Optional<Memoria> memoria = ComponentesDaMontagem.memoria(montagem);
        Optional<PlacaMae> placaMae = ComponentesDaMontagem.placaMae(montagem);
        if (memoria.isEmpty() || placaMae.isEmpty()) {
            return List.of();
        }

        boolean kitExpo = memoria.get().isPerfilEXPO();
        boolean kitXmp = memoria.get().isPerfilXMP();
        if (!kitExpo && !kitXmp) {
            return List.of();
        }
        boolean perfilSuportado = kitExpo && placaMae.get().isSuportaPerfilEXPO()
                || kitXmp && placaMae.get().isSuportaPerfilXMP();
        if (perfilSuportado) {
            return List.of();
        }

        return List.of(Limitacao.builder()
                .regra(id())
                .limitante(placaMae.get())
                .afetado(memoria.get())
                .campo("perfisDeMemoria")
                .valorDoNovo(perfisDoKit(memoria.get()))
                .valorSuportado("EXPO: " + (placaMae.get().isSuportaPerfilEXPO() ? "sim" : "nao")
                        + ", XMP: " + (placaMae.get().isSuportaPerfilXMP() ? "sim" : "nao"))
                .consequencia(TextosLimitacao.expo(memoria.get(), placaMae.get()))
                .severidade(SeveridadeLimitacao.AVISO)
                .construir());
    }

    private static String perfisDoKit(Memoria memoria) {
        if (memoria.isPerfilEXPO() && memoria.isPerfilXMP()) {
            return "EXPO/XMP";
        }
        return memoria.isPerfilEXPO() ? "EXPO" : "XMP";
    }

    private static boolean regraSeAplica(Componente candidato) {
        CategoriaComponente categoria = candidato.getCategoria();
        return categoria == CategoriaComponente.MEMORIA
                || categoria == CategoriaComponente.PLACA_MAE;
    }
}
