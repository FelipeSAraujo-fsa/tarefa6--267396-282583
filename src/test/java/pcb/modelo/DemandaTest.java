package pcb.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pcb.excecao.DemandaInvalidaException;
import pcb.excecao.EstadoDemandaInvalidaException;

class DemandaTest {

    private Produto produto;

    @BeforeEach
    void preparar() {
        produto = CatalogoProdutos.padrao().get(0);
    }

    @Test
    void novaDemandaComecaPendenteECalculaReceita() {
        Demanda demanda = new Demanda(1, produto, 3, 5);
        assertEquals(StatusDemanda.PENDENTE, demanda.getStatus());
        assertEquals(285.0, demanda.getReceitaPrevista());
    }

    @Test
    void concluirMudaOStatus() {
        Demanda demanda = new Demanda(1, produto, 3, 5);
        demanda.concluir();
        assertEquals(StatusDemanda.CONCLUIDA, demanda.getStatus());
    }

    @Test
    void concluirDuasVezesLancaExcecao() {
        Demanda demanda = new Demanda(1, produto, 3, 5);
        demanda.concluir();
        assertThrows(EstadoDemandaInvalidaException.class, demanda::concluir);
    }

    @Test
    void cancelarDemandaConcluidaLancaExcecao() {
        Demanda demanda = new Demanda(1, produto, 3, 5);
        demanda.concluir();
        assertThrows(EstadoDemandaInvalidaException.class, demanda::cancelar);
        assertEquals(StatusDemanda.CONCLUIDA, demanda.getStatus());
    }

    @Test
    void dadosInvalidosLancamExcecao() {
        assertThrows(DemandaInvalidaException.class, () -> new Demanda(1, null, 3, 5));
        assertThrows(DemandaInvalidaException.class, () -> new Demanda(1, produto, 0, 5));
        assertThrows(DemandaInvalidaException.class, () -> new Demanda(1, produto, 3, 0));
    }
}
