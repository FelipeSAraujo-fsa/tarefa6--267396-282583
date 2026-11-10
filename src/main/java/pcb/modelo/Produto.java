package pcb.modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Modelo de placa de circuito impresso fabricada pela planta.
 *
 * <p>Um produto define o preço de venda por unidade, a sequência de etapas (tipos de máquina) pela
 * qual cada placa passa e a quantidade de matéria-prima consumida por unidade produzida.</p>
 *
 * @author 267396-282583
 */
public class Produto {

    private final String nome;
    private final double precoUnitario;
    private final List<TipoMaquina> etapas;
    private final Map<MateriaPrima, Integer> insumosPorUnidade;

    /**
     * Cria um produto.
     *
     * @param nome nome comercial do produto
     * @param precoUnitario preço de venda por placa, em reais (deve ser positivo)
     * @param etapas sequência de etapas do processo (não pode ser vazia)
     * @param insumosPorUnidade matéria-prima consumida por placa produzida
     * @throws IllegalArgumentException se algum argumento for nulo ou inconsistente (erro de
     *         programação na montagem do catálogo, não de entrada do usuário)
     */
    public Produto(String nome, double precoUnitario, List<TipoMaquina> etapas,
            Map<MateriaPrima, Integer> insumosPorUnidade) {
        if (nome == null || nome.isBlank() || etapas == null || etapas.isEmpty()
                || insumosPorUnidade == null || precoUnitario <= 0) {
            throw new IllegalArgumentException("Definição de produto inconsistente: " + nome);
        }
        this.nome = nome;
        this.precoUnitario = precoUnitario;
        this.etapas = Collections.unmodifiableList(new ArrayList<>(etapas));
        this.insumosPorUnidade = new EnumMap<>(MateriaPrima.class);
        this.insumosPorUnidade.putAll(insumosPorUnidade);
    }

    /**
     * Retorna o nome do produto.
     *
     * @return nome comercial
     */
    public String getNome() {
        return nome;
    }

    /**
     * Retorna o preço de venda por unidade.
     *
     * @return preço em reais
     */
    public double getPrecoUnitario() {
        return precoUnitario;
    }

    /**
     * Retorna as etapas do processo, em ordem.
     *
     * @return lista imutável de tipos de máquina
     */
    public List<TipoMaquina> getEtapas() {
        return etapas;
    }

    /**
     * Calcula a matéria-prima necessária para produzir uma quantidade de placas.
     *
     * @param quantidade número de placas (valor positivo)
     * @return mapa com a quantidade total de cada matéria-prima necessária
     */
    public Map<MateriaPrima, Integer> insumosPara(int quantidade) {
        Map<MateriaPrima, Integer> total = new EnumMap<>(MateriaPrima.class);
        for (Map.Entry<MateriaPrima, Integer> e : insumosPorUnidade.entrySet()) {
            total.put(e.getKey(), e.getValue() * quantidade);
        }
        return total;
    }

    /**
     * Retorna quantas etapas o processo do produto possui.
     *
     * @return número de etapas
     */
    public int getQuantidadeEtapas() {
        return etapas.size();
    }
}
