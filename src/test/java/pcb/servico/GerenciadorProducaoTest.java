package pcb.servico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pcb.excecao.DemandaInvalidaException;
import pcb.excecao.EntradaInvalidaException;
import pcb.excecao.EstadoDemandaInvalidaException;
import pcb.excecao.MaquinaIndisponivelException;
import pcb.excecao.OrcamentoInsuficienteException;
import pcb.excecao.RecursoInsuficienteException;
import pcb.modelo.Demanda;
import pcb.modelo.MateriaPrima;
import pcb.modelo.Produto;
import pcb.modelo.ResultadoProducao;
import pcb.modelo.StatusDemanda;

class GerenciadorProducaoTest {

    private GerenciadorProducao gerenciador;
    private Produto sensorIot;

    @BeforeEach
    void preparar() {
        gerenciador = GerenciadorProducao.criarPlantaPadrao();
        sensorIot = gerenciador.getProdutos().get(0);
    }

    @Test
    void criarDemandaAtribuiIdsSequenciais() {
        assertEquals(1, gerenciador.criarDemanda(sensorIot, 10, 5).getId());
        assertEquals(2, gerenciador.criarDemanda(sensorIot, 10, 5).getId());
        assertEquals(2, gerenciador.listarDemandas().size());
    }

    @Test
    void criarDemandaInvalidaLancaExcecao() {
        assertThrows(DemandaInvalidaException.class, () -> gerenciador.criarDemanda(sensorIot, 0, 5));
        assertTrue(gerenciador.listarDemandas().isEmpty());
    }

    @Test
    void producaoConsomeEstoqueConcluiDemandaEAtualizaSaldo() {
        Demanda demanda = gerenciador.criarDemanda(sensorIot, 10, 5);
        ResultadoProducao resultado = gerenciador.produzir(demanda.getId());

        assertEquals(StatusDemanda.CONCLUIDA, demanda.getStatus());
        assertEquals(10, resultado.aprovadas());
        assertEquals(950.0, resultado.receita());
        assertEquals(190, gerenciador.consultarEstoque(MateriaPrima.LAMINADO_FR4));
        assertEquals(970, gerenciador.consultarEstoque(MateriaPrima.PASTA_DE_SOLDA));
        assertEquals(190, gerenciador.consultarEstoque(MateriaPrima.KIT_COMPONENTES_SMD));
        assertEquals(25000.0 - resultado.custoOperacional() + resultado.receita(),
                gerenciador.getSaldo(), 1e-6);
    }

    @Test
    void producaoDesgastaAsMaquinasUsadas() {
        gerenciador.produzir(gerenciador.criarDemanda(sensorIot, 10, 5).getId());
        assertTrue(gerenciador.getMaquinas().stream().anyMatch(m -> m.getHorasAcumuladas() > 0));
    }

    @Test
    void estoqueInsuficienteImpedeProducaoSemAlterarNada() {
        Demanda demanda = gerenciador.criarDemanda(sensorIot, 5000, 5);
        assertThrows(RecursoInsuficienteException.class, () -> gerenciador.produzir(demanda.getId()));
        assertEquals(StatusDemanda.PENDENTE, demanda.getStatus());
        assertEquals(200, gerenciador.consultarEstoque(MateriaPrima.LAMINADO_FR4));
        assertEquals(25000.0, gerenciador.getSaldo());
    }

    @Test
    void orcamentoInsuficienteImpedeProducaoSemAlterarNada() {
        gerenciador.comprarMateriaPrima(MateriaPrima.KIT_COMPONENTES_SMD, 1000);
        assertEquals(0.0, gerenciador.getSaldo());
        Demanda demanda = gerenciador.criarDemanda(sensorIot, 10, 5);
        assertThrows(OrcamentoInsuficienteException.class, () -> gerenciador.produzir(demanda.getId()));
        assertEquals(StatusDemanda.PENDENTE, demanda.getStatus());
        assertEquals(200, gerenciador.consultarEstoque(MateriaPrima.LAMINADO_FR4));
    }

    @Test
    void maquinaEmManutencaoImpedeProducao() {
        gerenciador.alternarManutencao(1);
        Demanda demanda = gerenciador.criarDemanda(sensorIot, 10, 5);
        assertThrows(MaquinaIndisponivelException.class, () -> gerenciador.produzir(demanda.getId()));
        assertEquals(StatusDemanda.PENDENTE, demanda.getStatus());
    }

    @Test
    void naoProduzDemandaJaConcluida() {
        Demanda demanda = gerenciador.criarDemanda(sensorIot, 10, 5);
        gerenciador.produzir(demanda.getId());
        assertThrows(EstadoDemandaInvalidaException.class, () -> gerenciador.produzir(demanda.getId()));
        assertEquals(190, gerenciador.consultarEstoque(MateriaPrima.LAMINADO_FR4));
    }

    @Test
    void demandaInexistenteLancaExcecao() {
        assertThrows(DemandaInvalidaException.class, () -> gerenciador.produzir(99));
        assertThrows(DemandaInvalidaException.class, () -> gerenciador.cancelarDemanda(99));
    }

    @Test
    void produzirProximaSemPendentesLancaExcecao() {
        assertThrows(DemandaInvalidaException.class, gerenciador::produzirProxima);
    }

    @Test
    void produzirProximaUsaAEstrategiaVigente() {
        Demanda barata = gerenciador.criarDemanda(sensorIot, 10, 20);
        Demanda urgente = gerenciador.criarDemanda(sensorIot, 10, 2);
        gerenciador.selecionarEstrategia(2);
        gerenciador.produzirProxima();
        assertEquals(StatusDemanda.CONCLUIDA, urgente.getStatus());
        assertEquals(StatusDemanda.PENDENTE, barata.getStatus());
    }

    @Test
    void cancelarDemandaPendenteMudaOStatus() {
        Demanda demanda = gerenciador.criarDemanda(sensorIot, 10, 5);
        gerenciador.cancelarDemanda(demanda.getId());
        assertEquals(StatusDemanda.CANCELADA, demanda.getStatus());
        assertTrue(gerenciador.listarPendentes().isEmpty());
    }

    @Test
    void compraDebitaSaldoEAumentaEstoque() {
        double custo = gerenciador.comprarMateriaPrima(MateriaPrima.FLUXO_DE_SOLDA, 100);
        assertEquals(60.0, custo, 1e-9);
        assertEquals(600, gerenciador.consultarEstoque(MateriaPrima.FLUXO_DE_SOLDA));
        assertEquals(24940.0, gerenciador.getSaldo(), 1e-9);
    }

    @Test
    void compraInvalidaOuSemSaldoNaoAlteraNada() {
        assertThrows(EntradaInvalidaException.class,
                () -> gerenciador.comprarMateriaPrima(MateriaPrima.FLUXO_DE_SOLDA, 0));
        assertThrows(OrcamentoInsuficienteException.class,
                () -> gerenciador.comprarMateriaPrima(MateriaPrima.KIT_COMPONENTES_SMD, 2000));
        assertEquals(500, gerenciador.consultarEstoque(MateriaPrima.FLUXO_DE_SOLDA));
        assertEquals(200, gerenciador.consultarEstoque(MateriaPrima.KIT_COMPONENTES_SMD));
        assertEquals(25000.0, gerenciador.getSaldo());
    }

    @Test
    void transferirMaquinaMudaDeLinhaSemPerderADemaisMaquinas() {
        int total = gerenciador.getMaquinas().size();
        gerenciador.transferirMaquina(1, 2);
        assertEquals(total, gerenciador.getMaquinas().size());
        assertEquals(2, gerenciador.getLinhas().get(0).getQuantidadeMaquinas());
        assertEquals(4, gerenciador.getLinhas().get(1).getQuantidadeMaquinas());
    }

    @Test
    void transferirParaALinhaDeOrigemLancaExcecao() {
        assertThrows(EntradaInvalidaException.class, () -> gerenciador.transferirMaquina(1, 1));
        assertThrows(EntradaInvalidaException.class, () -> gerenciador.buscarMaquina(99));
    }
}
