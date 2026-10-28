package pcbuilder.enums;

/** Resolucao alvo da placa de video (secao 12 do modelo de dados). */
public enum Resolucao {

    FHD_1080P("1080p"),
    QHD_1440P("1440p"),
    UHD_4K("4K");

    private final String rotulo;

    Resolucao(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
