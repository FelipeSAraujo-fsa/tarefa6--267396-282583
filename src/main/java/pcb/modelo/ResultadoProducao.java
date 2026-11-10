package pcb.modelo;

/**
 * Resumo de uma produção concluída.
 *
 * @param idDemanda número da demanda produzida
 * @param produto nome do produto
 * @param quantidade placas produzidas (processadas)
 * @param aprovadas placas aprovadas, após o efeito da qualidade das máquinas
 * @param horas tempo total de máquina, em horas
 * @param custoOperacional custo operacional da produção, em reais
 * @param receita receita obtida (apenas placas aprovadas são vendidas), em reais
 * @author 267396-282583
 */
public record ResultadoProducao(int idDemanda, String produto, int quantidade, int aprovadas,
        double horas, double custoOperacional, double receita) {

    /**
     * Calcula o resultado operacional da produção.
     *
     * @return receita menos custo operacional, em reais
     */
    public double lucroOperacional() {
        return receita - custoOperacional;
    }

    /**
     * Calcula a quantidade de placas reprovadas.
     *
     * @return placas descartadas
     */
    public int refugo() {
        return quantidade - aprovadas;
    }

    /**
     * Calcula a fração de placas aprovadas.
     *
     * @return valor entre 0 e 1
     */
    public double rendimento() {
        return quantidade == 0 ? 0 : aprovadas / (double) quantidade;
    }
}
