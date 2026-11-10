package pcb.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pcb.excecao.MaquinaIndisponivelException;
import pcb.excecao.ManutencaoDesnecessariaException;

class MaquinaTest {

    private Maquina maquina;

    @BeforeEach
    void preparar() {
        maquina = new Maquina("Cortadora", TipoMaquina.CORTE_LASER, 40, 180.0, 35.0, 2);
    }

    @Test
    void maquinaNovaEstaOperacionalComEficienciaTotal() {
        assertEquals(SituacaoMaquina.OPERACIONAL, maquina.getSituacao());
        assertEquals(1.0, maquina.getEficiencia());
        assertEquals(1.0, maquina.horasPara(40, Cenario.NORMAL));
        assertFalse(maquina.possuiDesgaste());
    }

    @Test
    void cenarioAlteraTempoECusto() {
        assertEquals(0.8, maquina.horasPara(40, Cenario.ALTA_DEMANDA), 1e-9);
        assertEquals(225.0, maquina.custoPara(1.0, Cenario.ALTA_DEMANDA), 1e-9);
    }

    @Test
    void usoDesgastaPartesEReduzEficiencia() {
        maquina.registrarUso(10);
        assertTrue(maquina.possuiDesgaste());
        assertTrue(maquina.getEficiencia() < 1.0);
        assertTrue(maquina.getRendimentoQualidade() < 1.0);
        assertEquals(10.0, maquina.getHorasAcumuladas());
    }

    @Test
    void desgasteTotalParaAMaquinaEReparoRestauraOperacao() {
        maquina.registrarUso(1000);
        assertEquals(SituacaoMaquina.PARADA_POR_FALHA, maquina.getSituacao());
        assertThrows(MaquinaIndisponivelException.class, () -> maquina.horasPara(10, Cenario.NORMAL));
        maquina.repararComponentes();
        assertEquals(SituacaoMaquina.OPERACIONAL, maquina.getSituacao());
        assertEquals(1.0, maquina.getEficiencia());
    }

    @Test
    void reparoSemDesgasteLancaExcecao() {
        assertThrows(ManutencaoDesnecessariaException.class, maquina::exigirDesgasteParaReparo);
    }

    @Test
    void alternarManutencaoTiraAMaquinaDeOperacao() {
        assertEquals(SituacaoMaquina.EM_MANUTENCAO, maquina.alternarManutencao());
        assertFalse(maquina.isOperacional());
        assertEquals(SituacaoMaquina.OPERACIONAL, maquina.alternarManutencao());
    }

    @Test
    void configuracaoInvalidaLancaExcecao() {
        assertThrows(IllegalArgumentException.class,
                () -> new Maquina("", TipoMaquina.CORTE_LASER, 40, 180.0, 35.0, 2));
        assertThrows(IllegalArgumentException.class,
                () -> new Maquina("M", TipoMaquina.CORTE_LASER, 0, 180.0, 35.0, 2));
        assertThrows(IllegalArgumentException.class,
                () -> new Maquina("M", TipoMaquina.CORTE_LASER, 40, 180.0, 35.0, 0));
    }

    @Test
    void restaurarEstadoAplicaOsValoresInformados() {
        maquina.restaurarEstado(false, 12.5, List.of(0.2, 0.3, 0.1, 0.4));
        assertFalse(maquina.isHabilitada());
        assertEquals(12.5, maquina.getHorasAcumuladas());
        assertEquals(List.of(0.2, 0.3, 0.1, 0.4), maquina.getDesgastes());
    }

    @Test
    void restaurarEstadoInconsistenteLancaExcecao() {
        assertThrows(IllegalArgumentException.class,
                () -> maquina.restaurarEstado(true, 0, List.of(1.5, 0.0, 0.0, 0.0)));
        assertThrows(IllegalArgumentException.class,
                () -> maquina.restaurarEstado(true, -1, List.of(0.0, 0.0, 0.0, 0.0)));
        assertThrows(IllegalArgumentException.class,
                () -> maquina.restaurarEstado(true, 0, List.of(0.0, 0.0, 0.0)));
        assertFalse(maquina.possuiDesgaste());
    }
}
