package pcb.excecao;

/**
 * Indica que o saldo do orçamento da planta não cobre o valor de uma compra ou de um custo operacional
 *
 * @author 267396-282583
 */
public class OrcamentoInsuficienteException extends PlantaException {

    private static final long serialVersionUID = 1L;

    /**
     * Cria a exceção com uma mensagem destinada ao usuário
     *
     * @param mensagem explicação clara do problema ocorrido
     */
    public OrcamentoInsuficienteException(String mensagem) {
        super(mensagem);
    }
}
