package pcb.ui;

import java.util.List;
import java.util.NoSuchElementException;
import pcb.estrategia.EstrategiaProducao;
import pcb.excecao.EntradaInvalidaException;
import pcb.excecao.PlantaException;
import pcb.modelo.Cenario;
import pcb.modelo.Demanda;
import pcb.modelo.LinhaDeProducao;
import pcb.modelo.Maquina;
import pcb.modelo.MateriaPrima;
import pcb.modelo.Produto;
import pcb.modelo.RegistroAuditoria;
import pcb.modelo.ResultadoProducao;
import pcb.persistencia.PersistenciaPlanta;
import pcb.servico.GerenciadorProducao;
import pcb.util.Formatacao;

/**
 * Camada de interação com o usuário: exibe o menu, lê as opções e trata as exceções lançadas pelo
 * domínio.
 *
 * <p>Três categorias de erro são distinguidas:</p>
 * <ul>
 *   <li><b>entrada inválida</b> ({@link EntradaInvalidaException}): digitação ou escolha errada;</li>
 *   <li><b>regra de negócio</b> (demais {@link PlantaException}): operação recusada pela planta,
 *       sem nenhuma alteração de estado;</li>
 *   <li><b>falha inesperada</b> ({@link RuntimeException}): defeito de programa; é informada e
 *       auditada, e o sistema volta ao menu.</li>
 * </ul>
 *
 * @author 267396-282583
 */
public class MenuPrincipal {

    private static final String LINHA = "========================================";
    private static final int ULTIMA_OPCAO = 15;
    private static final int MAX_QUANTIDADE = 100_000;
    private static final int MAX_PRAZO = 365;
    private static final int MAX_AUDITORIA_EXIBIDA = 20;

    private final PersistenciaPlanta persistencia = new PersistenciaPlanta();
    private GerenciadorProducao gerenciador;
    private final LeitorEntrada leitor;

    /**
     * Cria o menu.
     *
     * @param gerenciador núcleo de regras de negócio da planta
     * @param leitor leitor de entrada validada
     */
    public MenuPrincipal(GerenciadorProducao gerenciador, LeitorEntrada leitor) {
        this.gerenciador = gerenciador;
        this.leitor = leitor;
    }

    /**
     * Executa o laço principal do menu até o usuário escolher sair ou a entrada ser encerrada.
     * Qualquer erro em uma operação é exibido e o laço continua.
     */
    public void executar() {
        boolean ativo = true;
        while (ativo) {
            try {
                exibirMenu();
                int opcao = leitor.lerInteiro("Escolha uma opção", 0, ULTIMA_OPCAO);
                ativo = tratarOpcao(opcao);
            } catch (EntradaInvalidaException e) {
                exibirErro("ENTRADA INVÁLIDA", e.getMessage(), "Voltando ao menu principal.");
            } catch (PlantaException e) {
                exibirErro("ERRO", "Não foi possível concluir a operação:\n" + e.getMessage(),
                        "Nenhuma alteração foi realizada no estado da planta.");
            } catch (NoSuchElementException e) {
                System.out.println("\nEntrada encerrada. Finalizando o sistema.");
                ativo = false;
            } catch (RuntimeException e) {
                gerenciador.registrarFalhaInesperada(e.getClass().getSimpleName() + ": " + e.getMessage());
                exibirErro("FALHA INESPERADA", "Ocorreu um erro interno (" + e.getClass().getSimpleName()
                        + "): " + e.getMessage(), "O sistema continua em funcionamento.");
            }
        }
    }

    private boolean tratarOpcao(int opcao) {
        switch (opcao) {
            case 1 -> listarDemandas();
            case 2 -> novaDemanda();
            case 3 -> produzirDemanda();
            case 4 -> produzirProxima();
            case 5 -> cancelarDemanda();
            case 6 -> escolherEstrategia();
            case 7 -> consultarEstoque();
            case 8 -> comprarMateriaPrima();
            case 9 -> escolherCenario();
            case 10 -> gerenciarMaquinas();
            case 11 -> exibirAuditoria();
            case 12 -> gerenciarLinhas();
            case 13 -> exibirDiagnosticoEstrutural();
            case 14 -> salvarEstado();
            case 15 -> carregarEstado();
            case 0 -> {
                System.out.println("Encerrando o sistema. Até logo!");
                return false;
            }
            default -> throw new EntradaInvalidaException("Opção inexistente: " + opcao);
        }
        return true;
    }

    // ------------------------------------------------------------------ exibição

    private void exibirMenu() {
        System.out.println();
        System.out.println(LINHA);
        System.out.println(" PCBs FH Factory - MENU PRINCIPAL - 267396 / 282583");
        System.out.println(LINHA);
        System.out.printf("Saldo: %s | Cenário: %s%nEstratégia: %s%n", Formatacao.moeda(gerenciador.getSaldo()),
                gerenciador.getNomeCenario(), gerenciador.getNomeEstrategia());
        System.out.println("----------------------------------------");
        System.out.println(" 1 - Listar demandas");
        System.out.println(" 2 - Nova demanda");
        System.out.println(" 3 - Produzir demanda (por número)");
        System.out.println(" 4 - Produzir próxima demanda (pela estratégia)");
        System.out.println(" 5 - Cancelar demanda");
        System.out.println(" 6 - Escolher estratégia de produção");
        System.out.println(" 7 - Consultar matéria-prima");
        System.out.println(" 8 - Comprar matéria-prima");
        System.out.println(" 9 - Escolher cenário de operação");
        System.out.println("10 - Máquinas (status, manutenção e reparo)");
        System.out.println("11 - Auditoria (histórico de eventos)");
        System.out.println("12 - Linhas de produção (agrupadores)");
        System.out.println("13 - Diagnóstico estrutural (auditoria em cascata)");
        System.out.println("14 - Salvar estado da planta");
        System.out.println("15 - Carregar estado da planta");
        System.out.println(" 0 - Sair");
        System.out.println("----------------------------------------");
    }

    private void exibirErro(String titulo, String mensagem, String rodape) {
        System.out.println();
        System.out.println(LINHA);
        System.out.println(titulo);
        System.out.println(LINHA);
        System.out.println(mensagem);
        System.out.println();
        System.out.println(rodape);
        System.out.println();
        leitor.pausar();
    }

    private void exibirSucesso(String mensagem) {
        System.out.println();
        System.out.println(LINHA);
        System.out.println("SUCESSO");
        System.out.println(LINHA);
        System.out.println(mensagem);
        System.out.println();
        leitor.pausar();
    }

    // ------------------------------------------------------------------ opções

    private void listarDemandas() {
        List<Demanda> demandas = gerenciador.listarDemandas();
        System.out.println("\n--- DEMANDAS ---");
        if (demandas.isEmpty()) {
            System.out.println("Nenhuma demanda cadastrada.");
        } else {
            System.out.printf("%-4s %-34s %6s %7s %-10s %14s%n", "Nº", "Produto", "Qtd", "Prazo", "Status", "Receita");
            for (Demanda d : demandas) {
                System.out.printf("%-4d %-34s %6d %5dd  %-10s %14s%n", d.getId(), d.getNomeProduto(),
                        d.getQuantidade(), d.getPrazoDias(), d.getDescricaoStatus(),
                        Formatacao.moeda(d.getReceitaPrevista()));
            }
        }
        leitor.pausar();
    }

    private void novaDemanda() {
        List<Produto> produtos = gerenciador.getProdutos();
        System.out.println("\n--- NOVA DEMANDA ---");
        for (int i = 0; i < produtos.size(); i++) {
            Produto p = produtos.get(i);
            System.out.printf("%d - %s (%s/placa, %d etapas)%n", i + 1, p.getNome(),
                    Formatacao.moeda(p.getPrecoUnitario()), p.getQuantidadeEtapas());
        }
        int escolhido = leitor.lerInteiro("Produto", 1, produtos.size());
        int quantidade = leitor.lerInteiro("Quantidade de placas", 1, MAX_QUANTIDADE);
        int prazo = leitor.lerInteiro("Prazo de entrega (dias)", 1, MAX_PRAZO);
        Demanda d = gerenciador.criarDemanda(produtos.get(escolhido - 1), quantidade, prazo);
        exibirSucesso("Demanda #" + d.getId() + " registrada: " + d.getQuantidade() + " x "
                + d.getNomeProduto() + ".");
    }

    private void produzirDemanda() {
        int id = leitor.lerInteiro("Número da demanda a produzir", 1, Integer.MAX_VALUE);
        exibirResultado(gerenciador.produzir(id));
    }

    private void produzirProxima() {
        exibirResultado(gerenciador.produzirProxima());
    }

    private void exibirResultado(ResultadoProducao r) {
        exibirSucesso(String.format(
                "Demanda #%d produzida: %d x %s.%nAprovadas: %d | Refugo: %d (rendimento %s)%n"
                        + "Tempo total de máquina: %s h%nCusto operacional: %s%n"
                        + "Receita: %s%nResultado operacional: %s",
                r.idDemanda(), r.quantidade(), r.produto(), r.aprovadas(), r.refugo(),
                Formatacao.percentual(r.rendimento()), Formatacao.decimal(r.horas()),
                Formatacao.moeda(r.custoOperacional()), Formatacao.moeda(r.receita()),
                Formatacao.moeda(r.lucroOperacional())));
    }

    private void cancelarDemanda() {
        int id = leitor.lerInteiro("Número da demanda a cancelar", 1, Integer.MAX_VALUE);
        gerenciador.cancelarDemanda(id);
        exibirSucesso("Demanda #" + id + " cancelada.");
    }

    private void escolherEstrategia() {
        List<EstrategiaProducao> estrategias = gerenciador.getEstrategiasDisponiveis();
        System.out.println("\n--- ESTRATÉGIAS DE PRODUÇÃO ---");
        for (int i = 0; i < estrategias.size(); i++) {
            System.out.printf("%d - %s%n    %s%n", i + 1, estrategias.get(i).getNome(),
                    estrategias.get(i).getDescricao());
        }
        gerenciador.selecionarEstrategia(leitor.lerInteiro("Estratégia", 1, estrategias.size()));
        exibirSucesso("Estratégia atual: " + gerenciador.getNomeEstrategia());
    }

    private void consultarEstoque() {
        System.out.println("\n--- MATÉRIA-PRIMA ---");
        for (MateriaPrima mp : MateriaPrima.values()) {
            System.out.printf("%-26s %7d %-7s (%s/%s)%n", mp.getNome(), gerenciador.consultarEstoque(mp),
                    mp.getUnidade(), Formatacao.moeda(mp.getPrecoUnitario()), mp.getUnidade());
        }
        System.out.println("Saldo do orçamento: " + Formatacao.moeda(gerenciador.getSaldo()));
        leitor.pausar();
    }

    private void comprarMateriaPrima() {
        MateriaPrima[] itens = MateriaPrima.values();
        System.out.println("\n--- COMPRA DE MATÉRIA-PRIMA ---");
        for (int i = 0; i < itens.length; i++) {
            System.out.printf("%d - %s (%s/%s no cenário atual)%n", i + 1, itens[i].getNome(),
                    Formatacao.moeda(gerenciador.custoCompra(itens[i], 1)), itens[i].getUnidade());
        }
        MateriaPrima escolhida = itens[leitor.lerInteiro("Item", 1, itens.length) - 1];
        int quantidade = leitor.lerInteiro("Quantidade (" + escolhida.getUnidade() + ")", 1, MAX_QUANTIDADE);
        double custo = gerenciador.comprarMateriaPrima(escolhida, quantidade);
        exibirSucesso(String.format("Comprados %d %s de %s por %s.", quantidade, escolhida.getUnidade(),
                escolhida.getNome(), Formatacao.moeda(custo)));
    }

    private void escolherCenario() {
        Cenario[] cenarios = Cenario.values();
        System.out.println("\n--- CENÁRIOS DE OPERAÇÃO ---");
        for (int i = 0; i < cenarios.length; i++) {
            System.out.printf("%d - %s%n    %s%n", i + 1, cenarios[i].getNome(), cenarios[i].getDescricao());
        }
        gerenciador.setCenario(cenarios[leitor.lerInteiro("Cenário", 1, cenarios.length) - 1]);
        exibirSucesso("Cenário atual: " + gerenciador.getNomeCenario());
    }

    /** Lista as máquinas agrupadas por linha, com a numeração global usada nas operações. */
    private void listarMaquinas() {
        System.out.println("\n--- MÁQUINAS (por linha de produção) ---");
        int numero = 1;
        for (LinhaDeProducao linha : gerenciador.getLinhas()) {
            System.out.println(linha.getNome() + ":");
            if (linha.getQuantidadeMaquinas() == 0) {
                System.out.println("    (sem máquinas)");
            }
            for (Maquina m : linha.getMaquinas()) {
                System.out.printf("  %d - %-24s %-44s %4d/h  %-16s efic. %s  [%s h usadas]%n", numero++,
                        m.getNome(), m.getDescricaoTipo(), m.getCapacidadePorHora(),
                        m.getSituacao().getDescricao(), Formatacao.percentual(m.getEficiencia()),
                        Formatacao.decimal(m.getHorasAcumuladas()));
            }
        }
    }

    private void gerenciarMaquinas() {
        listarMaquinas();
        int total = gerenciador.getMaquinas().size();
        System.out.println("\n1 - Alternar manutenção programada");
        System.out.println("2 - Reparar componentes (reparo geral, tem custo)");
        System.out.println("3 - Diagnóstico detalhado da máquina");
        System.out.println("0 - Voltar");
        int acao = leitor.lerInteiro("Ação", 0, 3);
        if (acao == 0) {
            return;
        }
        int numero = leitor.lerInteiro("Número da máquina", 1, total);
        Maquina m = gerenciador.buscarMaquina(numero);
        switch (acao) {
            case 1 -> {
                gerenciador.alternarManutencao(numero);
                exibirSucesso(m.getNome() + " agora está: " + m.getSituacao().getDescricao() + ".");
            }
            case 2 -> {
                double custo = gerenciador.repararMaquina(numero);
                exibirSucesso(m.getNome() + ": todos os componentes foram substituídos/recalibrados.\n"
                        + "Custo do reparo: " + Formatacao.moeda(custo));
            }
            default -> {
                System.out.println("\n--- DIAGNÓSTICO DA MÁQUINA ---");
                System.out.println(m.gerarRelatorioDiagnostico());
                System.out.println("Temperatura lida pelo sensor: "
                        + Formatacao.decimal(m.lerTemperaturaAtual()) + " °C");
                leitor.pausar();
            }
        }
    }

    private void gerenciarLinhas() {
        System.out.println("\n--- LINHAS DE PRODUÇÃO ---");
        List<LinhaDeProducao> linhas = gerenciador.getLinhas();
        for (int i = 0; i < linhas.size(); i++) {
            LinhaDeProducao l = linhas.get(i);
            System.out.printf("%d - %s (%d máquina(s))%n", i + 1, l.getNome(), l.getQuantidadeMaquinas());
        }
        System.out.println("\n1 - Transferir máquina para outra linha");
        System.out.println("2 - Criar nova linha");
        System.out.println("0 - Voltar");
        int acao = leitor.lerInteiro("Ação", 0, 2);
        if (acao == 1) {
            transferirMaquina();
        } else if (acao == 2) {
            LinhaDeProducao nova = gerenciador.criarLinha(leitor.lerLinha("Nome da nova linha"));
            exibirSucesso("Linha \"" + nova.getNome() + "\" criada.");
        }
    }

    private void transferirMaquina() {
        listarMaquinas();
        int numero = leitor.lerInteiro("Número da máquina a transferir", 1, gerenciador.getMaquinas().size());
        List<LinhaDeProducao> linhas = gerenciador.getLinhas();
        for (int i = 0; i < linhas.size(); i++) {
            System.out.printf("%d - %s%n", i + 1, linhas.get(i).getNome());
        }
        int destino = leitor.lerInteiro("Linha de destino", 1, linhas.size());
        gerenciador.transferirMaquina(numero, destino);
        exibirSucesso(gerenciador.buscarMaquina(numero).getNome() + " transferida para a linha \""
                + linhas.get(destino - 1).getNome() + "\".");
    }

    private void exibirDiagnosticoEstrutural() {
        System.out.println("\n--- DIAGNÓSTICO ESTRUTURAL DA PLANTA ---");
        System.out.println(gerenciador.gerarRelatorioDiagnostico());
        leitor.pausar();
    }

    private void salvarEstado() {
        persistencia.salvar(gerenciador);
        exibirSucesso("Estado da planta salvo em " + persistencia.getArquivo() + ".");
    }

    private void carregarEstado() {
        gerenciador = persistencia.carregar();
        exibirSucesso("Estado da planta carregado de " + persistencia.getArquivo() + ".");
    }

    private void exibirAuditoria() {
        List<RegistroAuditoria> registros = gerenciador.listarAuditoria();
        System.out.println("\n--- AUDITORIA (últimos " + MAX_AUDITORIA_EXIBIDA + " eventos) ---");
        if (registros.isEmpty()) {
            System.out.println("Nenhum evento registrado.");
        }
        int inicio = Math.max(0, registros.size() - MAX_AUDITORIA_EXIBIDA);
        for (RegistroAuditoria r : registros.subList(inicio, registros.size())) {
            System.out.printf("[%tT] %-12s %s%n", r.momento(), r.categoria(), r.descricao());
        }
        leitor.pausar();
    }
}
