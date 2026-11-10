package pcb.excecao;

/**
 * Indica que a operação solicitada é incompatível com o estado atual da demanda, por exemplo produzir uma demanda já concluída ou cancelar uma demanda já produzida
 *
 * @author 267396-282583
 */
public class EstadoDemandaInvalidaException extends DemandaInvalidaException {

    private static final long serialVersionUID = 1L;

    /**
     * Cria a exceção com uma mensagem destinada ao usuário.
     *
     * @param mensagem explicação clara do problema ocorrido
     */
    public EstadoDemandaInvalidaException(String mensagem) {
        super(mensagem);
    }
}
