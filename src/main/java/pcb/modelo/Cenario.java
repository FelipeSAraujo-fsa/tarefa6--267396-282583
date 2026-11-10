package pcb.modelo;

/**
 * Cenários de operação da planta. Cada cenário aplica fatores multiplicativos ao custo e ao tempo
 * de produção e ao preço de compra de matéria-prima
 *
 * @author 267396-282583
 */
public enum Cenario {

    /** Operação padrão, sem ajustes */
    NORMAL("Operação normal", "Turno regular, preços de mercado.", 1.00, 1.00, 1.00),

    /** Turno extra: produz mais rápido, porém com custo operacional e de insumos maiores */
    ALTA_DEMANDA("Alta demanda",
            "Turno extra: produção 20% mais rápida, custo operacional +25%, insumos +10%.",
            1.25, 0.80, 1.10),

    /** Escassez de componentes no mercado: insumos mais caros */
    CRISE_SUPRIMENTOS("Crise de suprimentos",
            "Escassez de componentes: insumos 50% mais caros.",
            1.00, 1.00, 1.50),

    /** Linha operando com restrições por manutenção programada: mais lenta e mais cara */
    MANUTENCAO_PROGRAMADA("Manutenção programada",
            "Linha reduzida: produção 50% mais lenta, custo operacional +10%.",
            1.10, 1.50, 1.00);

    private final String nome;
    private final String descricao;
    private final double fatorCusto;
    private final double fatorTempo;
    private final double fatorPrecoInsumos;

    Cenario(String nome, String descricao, double fatorCusto, double fatorTempo,
            double fatorPrecoInsumos) {
        this.nome = nome;
        this.descricao = descricao;
        this.fatorCusto = fatorCusto;
        this.fatorTempo = fatorTempo;
        this.fatorPrecoInsumos = fatorPrecoInsumos;
    }

    /**
     * Retorna o nome de exibição do cenário
     *
     * @return nome legível
     */
    public String getNome() {
        return nome;
    }

    /**
     * Retorna a descrição dos efeitos do cenário
     *
     * @return texto explicativo
     */
    public String getDescricao() {
        return descricao;
    }

    /**
     * Retorna o fator aplicado ao custo por hora das máquinas
     *
     * @return multiplicador do custo operacional
     */
    public double getFatorCusto() {
        return fatorCusto;
    }

    /**
     * Retorna o fator aplicado ao tempo de processamento das máquinas
     *
     * @return multiplicador do tempo (menor que 1 acelera, maior que 1 atrasa)
     */
    public double getFatorTempo() {
        return fatorTempo;
    }

    /**
     * Retorna o fator aplicado ao preço de compra de matéria-prima
     *
     * @return multiplicador do preço dos insumos
     */
    public double getFatorPrecoInsumos() {
        return fatorPrecoInsumos;
    }
}
