package pcb.modelo;

/**
 * Matérias-primas consumidas na fabricação de placas de circuito impresso.
 *
 * <p>Cada item possui nome de exibição, unidade de medida e preço-base por unidade. O preço
 * efetivo de compra depende do {@link Cenario} de operação vigente.</p>
 *
 * @author 267396-282583
 */
public enum MateriaPrima {

    /** Painel de laminado de fibra de vidro com cobre (substrato da placa). */
    LAMINADO_FR4("Laminado FR-4", "painel", 18.00),

    /** Pasta de solda SAC305 aplicada antes do posicionamento de componentes SMD. */
    PASTA_DE_SOLDA("Pasta de solda SAC305", "g", 0.40),

    /** Fluxo líquido usado na soldagem por onda. */
    FLUXO_DE_SOLDA("Fluxo de solda", "ml", 0.60),

    /** Conjunto de componentes eletrônicos SMD necessários para uma placa. */
    KIT_COMPONENTES_SMD("Kit de componentes SMD", "kit", 25.00);

    private final String nome;
    private final String unidade;
    private final double precoUnitario;

    MateriaPrima(String nome, String unidade, double precoUnitario) {
        this.nome = nome;
        this.unidade = unidade;
        this.precoUnitario = precoUnitario;
    }

    /**
     * Retorna o nome de exibição.
     *
     * @return nome legível da matéria-prima
     */
    public String getNome() {
        return nome;
    }

    /**
     * Retorna a unidade de medida do estoque.
     *
     * @return unidade (painel, g, ml, kit)
     */
    public String getUnidade() {
        return unidade;
    }

    /**
     * Retorna o preço-base por unidade, sem aplicar fator de cenário.
     *
     * @return preço em reais por unidade
     */
    public double getPrecoUnitario() {
        return precoUnitario;
    }
}
