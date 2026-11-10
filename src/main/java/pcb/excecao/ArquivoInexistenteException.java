package pcb.excecao;

public class ArquivoInexistenteException extends PersistenciaException {

    private static final long serialVersionUID = 1L;

    public ArquivoInexistenteException(String mensagem) {
        super(mensagem);
    }
}
