package pcbuilder.cli.etapas;

/**
 * Resultado da execucao de uma etapa do fluxo guiado.
 *
 * <p>A etapa nunca decide o que a CLI faz depois: ela apenas informa o que aconteceu,
 * e o menu principal decide se avanca, se volta ou se encerra a montagem.</p>
 */
public enum ResultadoEtapa {

    /** Peca escolhida e guardada na montagem; o fluxo pode avancar. */
    CONCLUIDA,

    /** O usuario cancelou a montagem (ou a entrada terminou). */
    CANCELADA,

    /** A categoria nao tem nenhum componente na base; nao ha o que escolher. */
    SEM_COMPONENTES
}
