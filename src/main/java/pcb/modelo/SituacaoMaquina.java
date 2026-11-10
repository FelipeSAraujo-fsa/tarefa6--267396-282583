package pcb.modelo;

/**
 * Situação operacional de uma {@link Maquina}, combinando a decisão de manutenção (tomada pelo
 * usuário) com a saúde dos componentes internos.
 *
 * @author 267396-282583
 */
public enum SituacaoMaquina {

    /** Disponível para produzir. */
    OPERACIONAL("OPERACIONAL"),

    /** Retirada de operação manualmente (manutenção programada). */
    EM_MANUTENCAO("EM MANUTENÇÃO"),

    /** Parada automaticamente porque uma parte essencial falhou. */
    PARADA_POR_FALHA("PARADA POR FALHA");

    private final String descricao;

    SituacaoMaquina(String descricao) {
        this.descricao = descricao;
    }

    /**
     * Retorna o texto de exibição da situação.
     *
     * @return descrição legível
     */
    public String getDescricao() {
        return descricao;
    }
}
