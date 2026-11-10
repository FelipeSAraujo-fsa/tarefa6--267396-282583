package pcb.excecao;

/**
 * Indica que uma demanda não existe ou foi descrita com dados inválidos
 *
 * @author 267396-282583
 */
public class DemandaInvalidaException extends PlantaException {

    private static final long serialVersionUID = 1L;

    /**
     * Cria a exceção com uma mensagem destinada ao usuário
     *
     * @param mensagem explicação clara do problema ocorrido
     */
    public DemandaInvalidaException(String mensagem) {
        super(mensagem);
    }
}
