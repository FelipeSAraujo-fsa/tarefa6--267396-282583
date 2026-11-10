package pcb.modelo;

/**
 * Estado de saúde de uma parte interna de uma {@link Maquina}, derivado do seu desgaste
 * (0 = novo, 1 = esgotado). Uso restrito ao pacote: o restante do sistema não conhece as partes.
 *
 * @author 267396-282583
 */
enum EstadoComponente {

    /** Desgaste baixo, funcionamento pleno. */
    OK("OK"),

    /** Desgaste avançado: ainda funciona, porém com desempenho reduzido. */
    DEGRADADO("Degradado"),

    /** Componente esgotado: não funciona mais até ser substituído. */
    FALHA("FALHA");

    private static final double LIMITE_DEGRADACAO = 0.5;

    private final String descricao;

    EstadoComponente(String descricao) {
        this.descricao = descricao;
    }

    /**
     * Classifica um nível de desgaste.
     *
     * @param desgaste valor entre 0 (novo) e 1 (esgotado)
     * @return estado correspondente
     */
    static EstadoComponente deDesgaste(double desgaste) {
        if (desgaste >= 1.0) {
            return FALHA;
        }
        return desgaste >= LIMITE_DEGRADACAO ? DEGRADADO : OK;
    }

    String getDescricao() {
        return descricao;
    }
}
