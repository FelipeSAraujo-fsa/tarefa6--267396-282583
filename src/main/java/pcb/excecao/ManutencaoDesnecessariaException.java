package pcb.excecao;

/**
 * Indica que um reparo foi solicitado para uma máquina cujos componentes estão todos sem desgaste
 *
 * @author 267396-282583
 */
public class ManutencaoDesnecessariaException extends PlantaException {

    private static final long serialVersionUID = 1L;

    /**
     * Cria a exceção com uma mensagem destinada ao usuário
     *
     * @param mensagem explicação clara do problema ocorrido
     */
    public ManutencaoDesnecessariaException(String mensagem) {
        super(mensagem);
    }
}
