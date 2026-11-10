package pcb.modelo;

/**
 * Tipos de equipamento existentes na linha de produção de PCBs. Cada etapa do processo de um
 * {@link Produto} exige uma máquina de um destes tipos.
 *
 * @author 267396-282583
 */
public enum TipoMaquina {

    /** Corte a laser do painel e furação/contorno da placa. */
    CORTE_LASER("Corte a laser"),

    /** Posicionamento automático de componentes SMD (pick-and-place). */
    PICK_AND_PLACE("Posicionamento de componentes (pick-and-place)"),

    /** Soldagem por refusão em forno (reflow), usada em componentes SMD. */
    SOLDAGEM_REFLOW("Soldagem por refusão (reflow)"),

    /** Soldagem por onda, usada em componentes de furo passante. */
    SOLDAGEM_ONDA("Soldagem por onda"),

    /** Inspeção óptica automática (AOI) de soldas e posicionamento. */
    INSPECAO_OPTICA("Inspeção óptica automática (AOI)"),

    /** Teste elétrico/funcional da placa pronta. */
    TESTE_FUNCIONAL("Teste funcional");

    private final String descricao;

    TipoMaquina(String descricao) {
        this.descricao = descricao;
    }

    /**
     * Retorna o nome de exibição do tipo de máquina.
     *
     * @return descrição legível
     */
    public String getDescricao() {
        return descricao;
    }
}
