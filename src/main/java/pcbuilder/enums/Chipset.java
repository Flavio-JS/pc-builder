package pcbuilder.enums;

/**
 * Chipset da placa-mae (secao 12 do modelo de dados).
 *
 * <p>O dominio foi ampliado em relacao a primeira versao do documento para cobrir
 * os chipsets que existem de fato no catalogo OpenDB nos 5 soquetes suportados
 * (AM4, AM5, LGA1200, LGA1700 e LGA1851). Sem essa ampliacao, processadores AM4 e
 * LGA1200 ficariam sem nenhuma placa-mae compativel no catalogo.</p>
 */
public enum Chipset {

    // AMD AM4
    A320,
    B350,
    B450,
    A520,
    B550,
    X570,
    // AMD AM5
    A620,
    B650,
    B650E,
    X670,
    X670E,
    B850,
    X870,
    X870E,
    // Intel LGA1200
    H410,
    B460,
    H510,
    B560,
    Z490,
    Z590,
    // Intel LGA1700
    H610,
    B660,
    B760,
    Z690,
    Z790,
    // Intel LGA1851
    B860,
    Z890
}
