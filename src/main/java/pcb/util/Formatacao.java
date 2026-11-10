package pcb.util;

import java.util.Locale;

/**
 * Utilitários de formatação de valores para apresentação ao usuário, sempre no padrão brasileiro.
 *
 * @author 267396-282583
 */
public final class Formatacao {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private Formatacao() {
    }

    /**
     * Formata um valor monetário, por exemplo {@code R$ 1.234,50}.
     *
     * @param valor valor em reais
     * @return texto formatado
     */
    public static String moeda(double valor) {
        return String.format(PT_BR, "R$ %,.2f", valor);
    }

    /**
     * Formata um número decimal com duas casas, por exemplo {@code 3,25}.
     *
     * @param valor número a formatar
     * @return texto formatado
     */
    public static String decimal(double valor) {
        return String.format(PT_BR, "%,.2f", valor);
    }

    /**
     * Formata uma fração como percentual com uma casa decimal, por exemplo {@code 97,5%}.
     *
     * @param fracao valor entre 0 e 1 (1 = 100%)
     * @return texto formatado
     */
    public static String percentual(double fracao) {
        return String.format(PT_BR, "%.1f%%", fracao * 100);
    }
}
