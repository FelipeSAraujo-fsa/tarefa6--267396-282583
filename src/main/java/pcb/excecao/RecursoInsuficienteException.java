package pcb.excecao;

/**
 * Indica falta de matéria-prima em estoque para atender a quantidade necessária de uma produção
 *
 * @author 267396-282583
 */
public class RecursoInsuficienteException extends PlantaException {

    private static final long serialVersionUID = 1L;

    /**
     * Cria a exceção com uma mensagem destinada ao usuário
     *
     * @param mensagem explicação clara do problema ocorrido
     */
    public RecursoInsuficienteException(String mensagem) {
        super(mensagem);
    }
}
