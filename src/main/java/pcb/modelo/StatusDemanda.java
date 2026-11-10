package pcb.modelo;

/**
 * Situação de uma {@link Demanda} ao longo do seu ciclo de vida.
 *
 * <p>Transições permitidas: {@code PENDENTE -> CONCLUIDA} e {@code PENDENTE -> CANCELADA}.
 * Estados finais não aceitam novas operações.</p>
 *
 * @author 267396-282583
 */
public enum StatusDemanda {

    /** Aguardando produção. */
    PENDENTE("Pendente"),

    /** Produzida com sucesso (estado final). */
    CONCLUIDA("Concluída"),

    /** Cancelada antes da produção (estado final). */
    CANCELADA("Cancelada");

    private final String descricao;

    StatusDemanda(String descricao) {
        this.descricao = descricao;
    }

    /**
     * Retorna o nome de exibição do status.
     *
     * @return descrição legível
     */
    public String getDescricao() {
        return descricao;
    }
}
