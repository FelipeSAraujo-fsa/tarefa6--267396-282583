package pcb.persistencia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pcb.excecao.ArquivoInexistenteException;
import pcb.excecao.DadosInvalidosException;
import pcb.excecao.PersistenciaException;
import pcb.modelo.Cenario;
import pcb.modelo.Demanda;
import pcb.modelo.LinhaDeProducao;
import pcb.modelo.Maquina;
import pcb.modelo.MateriaPrima;
import pcb.modelo.StatusDemanda;
import pcb.servico.GerenciadorProducao;

class PersistenciaPlantaTest {

    @TempDir
    Path pasta;

    private Path arquivo;
    private PersistenciaPlanta persistencia;
    private GerenciadorProducao original;

    @BeforeEach
    void preparar() {
        arquivo = pasta.resolve("dados").resolve("planta.xml");
        persistencia = new PersistenciaPlanta(arquivo.toString());
        original = plantaEmUso();
    }

    private static GerenciadorProducao plantaEmUso() {
        GerenciadorProducao g = GerenciadorProducao.criarPlantaPadrao();
        g.criarDemanda(g.getProdutos().get(0), 10, 5);
        g.criarDemanda(g.getProdutos().get(1), 20, 7);
        g.criarDemanda(g.getProdutos().get(2), 5, 3);
        g.produzir(1);
        g.cancelarDemanda(2);
        g.comprarMateriaPrima(MateriaPrima.FLUXO_DE_SOLDA, 40);
        g.setCenario(Cenario.ALTA_DEMANDA);
        g.selecionarEstrategia(3);
        g.alternarManutencao(4);
        g.transferirMaquina(1, 2);
        return g;
    }

    private String lerArquivo() throws IOException {
        return Files.readString(arquivo, StandardCharsets.UTF_8);
    }

    private void corromper(String de, String para) throws IOException {
        persistencia.salvar(original);
        String texto = lerArquivo();
        assertTrue(texto.contains(de), "trecho esperado não encontrado: " + de);
        Files.writeString(arquivo, texto.replace(de, para), StandardCharsets.UTF_8);
    }

    private void assertCarregamentoInvalido(String de, String para) throws IOException {
        corromper(de, para);
        assertThrows(DadosInvalidosException.class, persistencia::carregar);
    }

    @Test
    void salvarCriaArquivoXmlSemDeixarTemporario() throws IOException {
        assertFalse(persistencia.existe());
        persistencia.salvar(original);
        assertTrue(persistencia.existe());
        assertTrue(lerArquivo().contains("<planta"));
    }

    @Test
    void carregarRecuperaEstoqueSaldoEConfiguracao() {
        persistencia.salvar(original);
        GerenciadorProducao carregada = persistencia.carregar();

        for (MateriaPrima mp : MateriaPrima.values()) {
            assertEquals(original.consultarEstoque(mp), carregada.consultarEstoque(mp));
        }
        assertEquals(original.getSaldo(), carregada.getSaldo());
        assertEquals(original.getCenario(), carregada.getCenario());
        assertEquals(original.getNomeEstrategia(), carregada.getNomeEstrategia());
    }

    @Test
    void carregarRecuperaDemandasComSeusStatus() {
        persistencia.salvar(original);
        GerenciadorProducao carregada = persistencia.carregar();

        List<Demanda> esperadas = original.listarDemandas();
        List<Demanda> recuperadas = carregada.listarDemandas();
        assertEquals(esperadas.size(), recuperadas.size());
        for (int i = 0; i < esperadas.size(); i++) {
            assertEquals(esperadas.get(i).getId(), recuperadas.get(i).getId());
            assertEquals(esperadas.get(i).getNomeProduto(), recuperadas.get(i).getNomeProduto());
            assertEquals(esperadas.get(i).getQuantidade(), recuperadas.get(i).getQuantidade());
            assertEquals(esperadas.get(i).getPrazoDias(), recuperadas.get(i).getPrazoDias());
            assertEquals(esperadas.get(i).getStatus(), recuperadas.get(i).getStatus());
        }
        assertEquals(StatusDemanda.CONCLUIDA, recuperadas.get(0).getStatus());
        assertEquals(StatusDemanda.CANCELADA, recuperadas.get(1).getStatus());
        assertEquals(StatusDemanda.PENDENTE, recuperadas.get(2).getStatus());
    }

    @Test
    void carregarRecuperaLinhasMaquinasEComponentes() {
        persistencia.salvar(original);
        GerenciadorProducao carregada = persistencia.carregar();

        List<LinhaDeProducao> esperadas = original.getLinhas();
        List<LinhaDeProducao> recuperadas = carregada.getLinhas();
        assertEquals(esperadas.size(), recuperadas.size());
        for (int i = 0; i < esperadas.size(); i++) {
            assertEquals(esperadas.get(i).getNome(), recuperadas.get(i).getNome());
            assertEquals(esperadas.get(i).getQuantidadeMaquinas(), recuperadas.get(i).getQuantidadeMaquinas());
        }
        List<Maquina> maquinasEsperadas = original.getMaquinas();
        List<Maquina> maquinasRecuperadas = carregada.getMaquinas();
        for (int i = 0; i < maquinasEsperadas.size(); i++) {
            Maquina e = maquinasEsperadas.get(i);
            Maquina r = maquinasRecuperadas.get(i);
            assertEquals(e.getNome(), r.getNome());
            assertEquals(e.getTipo(), r.getTipo());
            assertEquals(e.getCapacidadePorHora(), r.getCapacidadePorHora());
            assertEquals(e.getCustoPorHora(), r.getCustoPorHora());
            assertEquals(e.getSituacao(), r.getSituacao());
            assertEquals(e.getHorasAcumuladas(), r.getHorasAcumuladas());
            assertEquals(e.getEficiencia(), r.getEficiencia());
            assertEquals(e.getRendimentoQualidade(), r.getRendimentoQualidade());
            assertEquals(e.lerTemperaturaAtual(), r.lerTemperaturaAtual());
            assertEquals(e.isHabilitada(), r.isHabilitada());
            assertEquals(e.getTemperaturaOperacaoC(), r.getTemperaturaOperacaoC());
            assertEquals(e.getDesgastes(), r.getDesgastes());
        }
        assertEquals(original.gerarRelatorioDiagnostico(), carregada.gerarRelatorioDiagnostico());
    }

    @Test
    void carregadaContinuaNumerandoDemandasEProduzindo() {
        persistencia.salvar(original);
        GerenciadorProducao carregada = persistencia.carregar();

        Demanda nova = carregada.criarDemanda(carregada.getProdutos().get(0), 5, 4);
        assertEquals(4, nova.getId());
        assertEquals(StatusDemanda.PENDENTE, carregada.buscarDemanda(3).getStatus());
    }

    @Test
    void caracteresEspeciaisNosNomesSaoPreservados() {
        String nome = "Linha A & \"B\" <1>";
        original.criarLinha(nome);
        persistencia.salvar(original);
        GerenciadorProducao carregada = persistencia.carregar();
        assertEquals(nome, carregada.getLinhas().get(carregada.getLinhas().size() - 1).getNome());
    }

    @Test
    void salvarNovamenteAtualizaOArquivo() {
        persistencia.salvar(original);
        original.comprarMateriaPrima(MateriaPrima.LAMINADO_FR4, 10);
        original.criarDemanda(original.getProdutos().get(0), 1, 1);
        persistencia.salvar(original);

        GerenciadorProducao carregada = persistencia.carregar();
        assertEquals(original.consultarEstoque(MateriaPrima.LAMINADO_FR4),
                carregada.consultarEstoque(MateriaPrima.LAMINADO_FR4));
        assertEquals(original.getSaldo(), carregada.getSaldo());
        assertEquals(4, carregada.listarDemandas().size());
    }

    @Test
    void carregarArquivoInexistenteLancaExcecaoEspecifica() {
        assertThrows(ArquivoInexistenteException.class, persistencia::carregar);
    }

    @Test
    void carregarArquivoVazioLancaExcecao() throws IOException {
        Files.createDirectories(arquivo.getParent());
        Files.write(arquivo, new byte[0]);
        DadosInvalidosException e = assertThrows(DadosInvalidosException.class, persistencia::carregar);
        assertTrue(e.getMessage().contains("vazio"));
    }

    @Test
    void carregarArquivoQueNaoEXmlLancaExcecao() throws IOException {
        Files.createDirectories(arquivo.getParent());
        Files.writeString(arquivo, "isto não é xml", StandardCharsets.UTF_8);
        assertThrows(DadosInvalidosException.class, persistencia::carregar);
    }

    @Test
    void carregarXmlTruncadoLancaExcecao() throws IOException {
        persistencia.salvar(original);
        String texto = lerArquivo();
        Files.writeString(arquivo, texto.substring(0, texto.length() / 2), StandardCharsets.UTF_8);
        assertThrows(DadosInvalidosException.class, persistencia::carregar);
    }

    @Test
    void carregarXmlComRaizDiferenteLancaExcecao() throws IOException {
        Files.createDirectories(arquivo.getParent());
        Files.writeString(arquivo, "<outra/>", StandardCharsets.UTF_8);
        assertThrows(DadosInvalidosException.class, persistencia::carregar);
    }

    @Test
    void saldoNegativoEIncompativelComODominio() throws IOException {
        assertCarregamentoInvalido("saldo=\"" + original.getSaldo() + "\"", "saldo=\"-1.0\"");
    }

    @Test
    void estoqueNegativoEIncompativelComODominio() throws IOException {
        assertCarregamentoInvalido("quantidade=\"190\"", "quantidade=\"-5\"");
    }

    @Test
    void statusDesconhecidoEInvalido() throws IOException {
        assertCarregamentoInvalido("status=\"PENDENTE\"", "status=\"ARQUIVADA\"");
    }

    @Test
    void prazoZeroEIncompativelComODominio() throws IOException {
        assertCarregamentoInvalido("prazoDias=\"5\"", "prazoDias=\"0\"");
    }

    @Test
    void produtoDesconhecidoEInvalido() throws IOException {
        assertCarregamentoInvalido("Placa Sensor IoT (SMD)", "Placa Inexistente");
    }

    @Test
    void desgasteForaDoIntervaloEInvalido() throws IOException {
        assertCarregamentoInvalido("<cabecote desgaste=\"" + original.getMaquinas().get(0).getDesgastes().get(0)
                + "\"", "<cabecote desgaste=\"1.5\"");
    }

    @Test
    void numeroMalFormadoEInvalido() throws IOException {
        assertCarregamentoInvalido("capacidadePorHora=\"100\"", "capacidadePorHora=\"cem\"");
    }

    @Test
    void tipoDeMaquinaDesconhecidoEInvalido() throws IOException {
        assertCarregamentoInvalido("tipo=\"CORTE_LASER\"", "tipo=\"SERRA\"");
    }

    @Test
    void idsDeDemandaForaDeSequenciaSaoInvalidos() throws IOException {
        assertCarregamentoInvalido("id=\"2\"", "id=\"5\"");
    }

    @Test
    void estrategiaDesconhecidaEInvalida() throws IOException {
        assertCarregamentoInvalido("estrategia=\"" + original.getNomeEstrategia() + "\"", "estrategia=\"Aleatória\"");
    }

    @Test
    void estruturaForaDeOrdemEInvalida() throws IOException {
        assertCarregamentoInvalido("<estoque>", "<estoque>\n<estoque>");
    }

    @Test
    void falhaDeCarregamentoNaoAlteraAPlantaEmMemoria() throws IOException {
        double saldo = original.getSaldo();
        int demandas = original.listarDemandas().size();
        corromper("status=\"PENDENTE\"", "status=\"ARQUIVADA\"");
        assertThrows(PersistenciaException.class, persistencia::carregar);
        assertEquals(saldo, original.getSaldo());
        assertEquals(demandas, original.listarDemandas().size());
    }

    @Test
    void falhaDeEscritaLancaExcecaoEspecifica() throws IOException {
        Path arquivoComum = pasta.resolve("arquivo.txt");
        Files.writeString(arquivoComum, "x", StandardCharsets.UTF_8);
        PersistenciaPlanta invalida = new PersistenciaPlanta(arquivoComum.resolve("planta.xml").toString());
        assertThrows(PersistenciaException.class, () -> invalida.salvar(original));
    }

    @Test
    void falhaDeLeituraLancaExcecaoEspecifica() throws IOException {
        Files.createDirectories(arquivo);
        assertThrows(PersistenciaException.class, persistencia::carregar);
    }
}
