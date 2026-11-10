package pcb.excecao;

/**
 * Raiz das exceções de domínio da fábrica de PCBs
 *
 * @author 267396-282583
 */
public class PlantaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Cria a exceção com uma mensagem destinada ao usuário
     *
     * @param mensagem explicação clara do problema ocorrido
     */
    public PlantaException(String mensagem) {
        super(mensagem);
    }

    /**
     * Cria a exceção preservando a causa original
     *
     * @param mensagem explicação clara do problema ocorrido
     * @param causa exceção que originou o problema
     */
    public PlantaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
