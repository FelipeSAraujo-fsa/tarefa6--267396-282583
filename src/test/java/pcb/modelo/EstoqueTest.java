package pcb.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pcb.excecao.EntradaInvalidaException;
import pcb.excecao.RecursoInsuficienteException;

class EstoqueTest {

    private Estoque estoque;

    @BeforeEach
    void preparar() {
        estoque = new Estoque(Map.of(MateriaPrima.LAMINADO_FR4, 5, MateriaPrima.PASTA_DE_SOLDA, 100));
    }

    @Test
    void itensNaoInformadosComecamEmZero() {
        assertEquals(0, estoque.consultar(MateriaPrima.KIT_COMPONENTES_SMD));
    }

    @Test
    void consumirReduzAQuantidade() {
        estoque.consumir(Map.of(MateriaPrima.LAMINADO_FR4, 2, MateriaPrima.PASTA_DE_SOLDA, 30));
        assertEquals(3, estoque.consultar(MateriaPrima.LAMINADO_FR4));
        assertEquals(70, estoque.consultar(MateriaPrima.PASTA_DE_SOLDA));
    }

    @Test
    void consumirSemEstoqueSuficienteLancaExcecaoENaoAlteraNada() {
        RecursoInsuficienteException e = assertThrows(RecursoInsuficienteException.class,
                () -> estoque.consumir(Map.of(MateriaPrima.LAMINADO_FR4, 2, MateriaPrima.FLUXO_DE_SOLDA, 1)));
        assertTrue(e.getMessage().contains("Fluxo de solda"));
        assertEquals(5, estoque.consultar(MateriaPrima.LAMINADO_FR4));
        assertEquals(0, estoque.consultar(MateriaPrima.FLUXO_DE_SOLDA));
    }

    @Test
    void adicionarAumentaAQuantidade() {
        estoque.adicionar(MateriaPrima.LAMINADO_FR4, 10);
        assertEquals(15, estoque.consultar(MateriaPrima.LAMINADO_FR4));
    }

    @Test
    void adicionarQuantidadeNaoPositivaLancaExcecao() {
        assertThrows(EntradaInvalidaException.class, () -> estoque.adicionar(MateriaPrima.LAMINADO_FR4, 0));
    }

    @Test
    void estoqueInicialNegativoLancaExcecao() {
        assertThrows(IllegalArgumentException.class,
                () -> new Estoque(Map.of(MateriaPrima.LAMINADO_FR4, -1)));
    }
}
