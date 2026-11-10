package pcb.excecao;

/**
 * Indica que não há máquina operacional do tipo exigido por uma etapa do processo
 *
 * @author 267396-282583
 */
public class MaquinaIndisponivelException extends PlantaException {

    private static final long serialVersionUID = 1L;

    /**
     * Cria a exceção com uma mensagem destinada ao usuário
     *
     * @param mensagem explicação clara do problema ocorrido
     */
    public MaquinaIndisponivelException(String mensagem) {
        super(mensagem);
    }
}
