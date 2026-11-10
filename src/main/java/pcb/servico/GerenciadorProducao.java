package pcb.servico;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import pcb.estrategia.EstrategiaProducao;
import pcb.estrategia.MaiorReceita;
import pcb.estrategia.MaiorUrgencia;
import pcb.estrategia.OrdemDeChegada;
import pcb.excecao.DemandaInvalidaException;
import pcb.excecao.EntradaInvalidaException;
import pcb.excecao.MaquinaIndisponivelException;
import pcb.excecao.PlantaException;
import pcb.modelo.Auditavel;
import pcb.modelo.Auditoria;
import pcb.modelo.CatalogoProdutos;
import pcb.modelo.Cenario;
import pcb.modelo.Demanda;
import pcb.modelo.Estoque;
import pcb.modelo.LinhaDeProducao;
import pcb.modelo.Maquina;
import pcb.modelo.MateriaPrima;
import pcb.modelo.Orcamento;
import pcb.modelo.Produto;
import pcb.modelo.RegistroAuditoria;
import pcb.modelo.ResultadoProducao;
import pcb.modelo.SituacaoMaquina;
import pcb.modelo.StatusDemanda;
import pcb.modelo.TipoMaquina;
import pcb.util.Formatacao;

/**
 * Núcleo de regras de negócio da fábrica de PCBs: gerencia demandas, estoque, orçamento, linhas de
 * produção (agrupadores de máquinas), estratégia de produção, cenário de operação e auditoria.
 *
 * <p>As máquinas pertencem às {@link LinhaDeProducao}; este gerenciador só enxerga as máquinas
 * (nunca as partes internas delas) e usa métodos de alto nível para operá-las.</p>
 *
 * <p><b>Consistência de estado:</b> toda operação que altera a planta primeiro valida todas as
 * condições (estado da demanda, disponibilidade de máquinas, matéria-prima e saldo) e só depois
 * aplica as mudanças. Assim, se uma {@link PlantaException} for lançada, nada foi alterado.</p>
 *
 * <p>Esta classe não interage com o usuário: apenas lança exceções, que são tratadas pela camada de
 * interface.</p>
 *
 * @author 267396-282583
 */
public class GerenciadorProducao implements Auditavel {

    private final Estoque estoque;
    private final Orcamento orcamento;
    private final List<LinhaDeProducao> linhas;
    private final List<Produto> produtos;
    private final List<EstrategiaProducao> estrategias = List.of(
            new OrdemDeChegada(), new MaiorUrgencia(), new MaiorReceita());
    private final List<Demanda> demandas = new ArrayList<>();
    private final Auditoria auditoria = new Auditoria();
    private EstrategiaProducao estrategia = estrategias.get(0);
    private Cenario cenario = Cenario.NORMAL;
    private int proximoIdDemanda = 1;

    /**
     * Cria um gerenciador com os recursos informados.
     *
     * @param estoque estoque inicial de matéria-prima
     * @param orcamento orçamento inicial
     * @param linhas linhas de produção (agrupadores) com os equipamentos da planta
     * @param produtos produtos que a planta sabe fabricar
     */
    public GerenciadorProducao(Estoque estoque, Orcamento orcamento, List<LinhaDeProducao> linhas,
            List<Produto> produtos) {
        this.estoque = estoque;
        this.orcamento = orcamento;
        this.linhas = new ArrayList<>(linhas);
        this.produtos = new ArrayList<>(produtos);
    }

    /**
     * Cria a planta padrão: estoque inicial, orçamento de R$ 25.000,00 e duas linhas de produção
     * com uma máquina de cada processo (corte a laser, pick-and-place e reflow na linha SMT; onda,
     * inspeção óptica e teste na linha de acabamento), além do catálogo de produtos padrão.
     *
     * @return gerenciador pronto para uso, sem demandas
     */
    public static GerenciadorProducao criarPlantaPadrao() {
        Map<MateriaPrima, Integer> inicial = new EnumMap<>(MateriaPrima.class);
        inicial.put(MateriaPrima.LAMINADO_FR4, 200);
        inicial.put(MateriaPrima.PASTA_DE_SOLDA, 1000);
        inicial.put(MateriaPrima.FLUXO_DE_SOLDA, 500);
        inicial.put(MateriaPrima.KIT_COMPONENTES_SMD, 200);

        LinhaDeProducao linhaSmt = new LinhaDeProducao("Linha SMT", List.of(
                new Maquina("Cortadora Laser LS-100", TipoMaquina.CORTE_LASER, 40, 180.00, 35.0, 2),
                new Maquina("Pick&Place PP-2000", TipoMaquina.PICK_AND_PLACE, 100, 220.00, 40.0, 4),
                new Maquina("Forno Reflow RF-8", TipoMaquina.SOLDAGEM_REFLOW, 60, 150.00, 245.0, 3)));
        LinhaDeProducao linhaAcabamento = new LinhaDeProducao("Linha THT e Acabamento", List.of(
                new Maquina("Soldagem por Onda WS-3", TipoMaquina.SOLDAGEM_ONDA, 50, 130.00, 260.0, 2),
                new Maquina("Inspetora Óptica AOI-X", TipoMaquina.INSPECAO_OPTICA, 120, 140.00, 30.0, 3),
                new Maquina("Bancada de Teste BT-5", TipoMaquina.TESTE_FUNCIONAL, 80, 90.00, 25.0, 2)));

        return new GerenciadorProducao(new Estoque(inicial), new Orcamento(25000.00),
                List.of(linhaSmt, linhaAcabamento), CatalogoProdutos.padrao());
    }

    // ------------------------------------------------------------------ demandas

    /**
     * Registra uma nova demanda pendente.
     *
     * @param produto produto solicitado
     * @param quantidade número de placas (positivo)
     * @param prazoDias prazo de entrega em dias (positivo)
     * @return a demanda criada, com identificador atribuído
     * @throws DemandaInvalidaException se o produto for nulo ou quantidade/prazo não forem positivos
     */
    public Demanda criarDemanda(Produto produto, int quantidade, int prazoDias) {
        Demanda demanda = new Demanda(proximoIdDemanda, produto, quantidade, prazoDias);
        proximoIdDemanda++;
        demandas.add(demanda);
        auditoria.registrar("DEMANDA", String.format("Demanda #%d criada: %d x %s, prazo de %d dia(s).",
                demanda.getId(), quantidade, produto.getNome(), prazoDias));
        return demanda;
    }

    /**
     * Busca uma demanda pelo identificador.
     *
     * @param id identificador da demanda
     * @return a demanda encontrada
     * @throws DemandaInvalidaException se não existir demanda com esse identificador
     */
    public Demanda buscarDemanda(int id) {
        return demandas.stream()
                .filter(d -> d.getId() == id)
                .findFirst()
                .orElseThrow(() -> new DemandaInvalidaException(
                        "Não existe demanda com o número " + id + "."));
    }

    /**
     * Cancela uma demanda pendente.
     *
     * @param id identificador da demanda
     * @throws DemandaInvalidaException se a demanda não existir
     * @throws pcb.excecao.EstadoDemandaInvalidaException se a demanda não estiver pendente
     */
    public void cancelarDemanda(int id) {
        try {
            Demanda demanda = buscarDemanda(id);
            demanda.cancelar();
            auditoria.registrar("DEMANDA", "Demanda #" + id + " cancelada.");
        } catch (PlantaException e) {
            registrarFalha("Cancelamento da demanda #" + id, e);
            throw e;
        }
    }

    /**
     * Lista todas as demandas, em ordem de criação.
     *
     * @return lista imutável de demandas
     */
    public List<Demanda> listarDemandas() {
        return Collections.unmodifiableList(demandas);
    }

    /**
     * Lista as demandas que ainda aguardam produção.
     *
     * @return nova lista com as demandas pendentes
     */
    public List<Demanda> listarPendentes() {
        return demandas.stream()
                .filter(d -> d.getStatus() == StatusDemanda.PENDENTE)
                .collect(Collectors.toList());
    }

    // ------------------------------------------------------------------ produção

    /**
     * Produz uma demanda específica.
     *
     * <p>Regras: a demanda deve existir e estar pendente; cada etapa do processo precisa de uma
     * máquina operacional do tipo exigido; deve haver matéria-prima suficiente; o orçamento deve
     * cobrir o custo operacional (tempo de máquina x custo/hora x fator do cenário). Em caso de
     * sucesso, consome a matéria-prima, debita o custo, credita a receita e conclui a demanda.</p>
     *
     * @param idDemanda identificador da demanda
     * @return resumo da produção
     * @throws DemandaInvalidaException se a demanda não existir
     * @throws pcb.excecao.EstadoDemandaInvalidaException se a demanda não estiver pendente
     * @throws MaquinaIndisponivelException se faltar máquina operacional para alguma etapa
     * @throws pcb.excecao.RecursoInsuficienteException se faltar matéria-prima
     * @throws pcb.excecao.OrcamentoInsuficienteException se o saldo não cobrir o custo operacional
     */
    public ResultadoProducao produzir(int idDemanda) {
        try {
            return executarProducao(buscarDemanda(idDemanda));
        } catch (PlantaException e) {
            registrarFalha("Produção da demanda #" + idDemanda, e);
            throw e;
        }
    }

    /**
     * Produz a próxima demanda segundo a estratégia de produção vigente.
     *
     * @return resumo da produção
     * @throws DemandaInvalidaException se não houver demandas pendentes
     * @throws pcb.excecao.RecursoInsuficienteException se faltar matéria-prima
     * @throws MaquinaIndisponivelException se faltar máquina operacional
     * @throws pcb.excecao.OrcamentoInsuficienteException se o saldo não cobrir o custo operacional
     */
    public ResultadoProducao produzirProxima() {
        Demanda escolhida = estrategia.escolherProxima(listarPendentes())
                .orElseThrow(() -> new DemandaInvalidaException(
                        "Não há demandas pendentes para produzir."));
        auditoria.registrar("ESTRATEGIA", String.format("Estratégia '%s' escolheu a demanda #%d.",
                estrategia.getNome(), escolhida.getId()));
        return produzir(escolhida.getId());
    }

    private ResultadoProducao executarProducao(Demanda demanda) {
        // 1) Validações: nada é alterado nesta fase.
        demanda.exigirPendente("produzir");
        Produto produto = demanda.getProduto();
        int quantidade = demanda.getQuantidade();

        Map<Maquina, Double> horasPorMaquina = new LinkedHashMap<>();
        double custoOperacional = 0;
        double horasTotais = 0;
        double somaRendimentos = 0;
        for (TipoMaquina etapa : produto.getEtapas()) {
            Maquina maquina = selecionarMaquina(etapa);
            double horas = maquina.horasPara(quantidade, cenario);
            horasPorMaquina.merge(maquina, horas, Double::sum);
            custoOperacional += maquina.custoPara(horas, cenario);
            horasTotais += horas;
            somaRendimentos += maquina.getRendimentoQualidade();
        }
        // Sensores descalibrados reduzem a qualidade: só as placas aprovadas geram receita.
        double rendimento = somaRendimentos / produto.getQuantidadeEtapas();
        int aprovadas = (int) Math.floor(quantidade * rendimento + 1e-9);
        double receita = aprovadas * produto.getPrecoUnitario();

        Map<MateriaPrima, Integer> insumos = produto.insumosPara(quantidade);
        estoque.verificarDisponibilidade(insumos);
        orcamento.garantirSaldo(custoOperacional);

        // 2) Efetivação: todas as condições foram verificadas, nenhuma etapa abaixo pode falhar.
        estoque.consumir(insumos);
        orcamento.debitar(custoOperacional);
        horasPorMaquina.forEach(this::usarMaquina);
        orcamento.creditar(receita);
        demanda.concluir();

        ResultadoProducao resultado = new ResultadoProducao(demanda.getId(), produto.getNome(),
                quantidade, aprovadas, horasTotais, custoOperacional, receita);
        auditoria.registrar("PRODUCAO", String.format(
                "Demanda #%d produzida: %d x %s, %d aprovada(s), em %s h (custo operacional %s, receita %s).",
                demanda.getId(), quantidade, produto.getNome(), aprovadas,
                Formatacao.decimal(horasTotais), Formatacao.moeda(custoOperacional),
                Formatacao.moeda(receita)));
        return resultado;
    }

    /** Registra o uso de uma máquina (que desgasta suas partes) e audita se ela parou por falha. */
    private void usarMaquina(Maquina maquina, double horas) {
        SituacaoMaquina antes = maquina.getSituacao();
        maquina.registrarUso(horas);
        if (antes != SituacaoMaquina.PARADA_POR_FALHA
                && maquina.getSituacao() == SituacaoMaquina.PARADA_POR_FALHA) {
            auditoria.registrar("MAQUINA", maquina.getNome()
                    + " parou por falha de componente após o desgaste. Reparo necessário.");
        }
    }

    private Maquina selecionarMaquina(TipoMaquina tipo) {
        return linhas.stream()
                .map(l -> l.buscarMaquinaOperacional(tipo))
                .flatMap(Optional::stream)
                .findFirst()
                .orElseThrow(() -> new MaquinaIndisponivelException(String.format(
                        "Nenhuma máquina operacional para a etapa \"%s\" (equipamento em manutenção"
                                + " ou parado por falha de componente).", tipo.getDescricao())));
    }

    // ------------------------------------------------------------------ compras e estoque

    /**
     * Calcula o custo de uma compra no cenário atual (preço-base x fator de preço dos insumos).
     *
     * @param materiaPrima item a comprar
     * @param quantidade unidades (positivo)
     * @return custo total em reais
     * @throws EntradaInvalidaException se o item for nulo ou a quantidade não for positiva
     */
    public double custoCompra(MateriaPrima materiaPrima, int quantidade) {
        if (materiaPrima == null) {
            throw new EntradaInvalidaException("A matéria-prima não foi informada.");
        }
        if (quantidade <= 0) {
            throw new EntradaInvalidaException("A quantidade a comprar deve ser maior que zero.");
        }
        return materiaPrima.getPrecoUnitario() * quantidade * cenario.getFatorPrecoInsumos();
    }

    /**
     * Compra matéria-prima: debita o custo do orçamento e adiciona a quantidade ao estoque.
     *
     * @param materiaPrima item a comprar
     * @param quantidade unidades (positivo)
     * @return custo da compra em reais
     * @throws EntradaInvalidaException se o item for nulo ou a quantidade não for positiva
     * @throws pcb.excecao.OrcamentoInsuficienteException se o saldo não cobrir a compra; nesse
     *         caso estoque e orçamento permanecem inalterados
     */
    public double comprarMateriaPrima(MateriaPrima materiaPrima, int quantidade) {
        try {
            double custo = custoCompra(materiaPrima, quantidade);
            orcamento.debitar(custo);
            estoque.adicionar(materiaPrima, quantidade);
            auditoria.registrar("COMPRA", String.format("Compra de %d %s de %s por %s.",
                    quantidade, materiaPrima.getUnidade(), materiaPrima.getNome(),
                    Formatacao.moeda(custo)));
            return custo;
        } catch (PlantaException e) {
            registrarFalha("Compra de matéria-prima", e);
            throw e;
        }
    }

    // ------------------------------------------------------------------ máquinas e linhas

    /**
     * Busca uma máquina pela numeração global (linhas na ordem, máquinas na ordem de cada linha).
     *
     * @param numero posição da máquina, começando em 1
     * @return a máquina
     * @throws EntradaInvalidaException se o número não corresponder a nenhuma máquina
     */
    public Maquina buscarMaquina(int numero) {
        List<Maquina> todas = getMaquinas();
        if (numero < 1 || numero > todas.size()) {
            throw new EntradaInvalidaException(
                    "Não existe máquina com o número " + numero + ". Informe um valor de 1 a "
                            + todas.size() + ".");
        }
        return todas.get(numero - 1);
    }

    /**
     * Alterna uma máquina entre operacional e em manutenção programada.
     *
     * @param numero posição da máquina na numeração global, começando em 1
     * @return a máquina alterada
     * @throws EntradaInvalidaException se o número não corresponder a nenhuma máquina
     */
    public Maquina alternarManutencao(int numero) {
        Maquina maquina = buscarMaquina(numero);
        SituacaoMaquina situacao = maquina.alternarManutencao();
        auditoria.registrar("MAQUINA", String.format("%s agora está: %s.", maquina.getNome(),
                situacao.getDescricao().toLowerCase()));
        return maquina;
    }

    /**
     * Executa o reparo geral de uma máquina (substitui/recalibra todas as partes), cobrando o
     * custo do orçamento.
     *
     * @param numero posição da máquina na numeração global, começando em 1
     * @return custo do reparo, em reais
     * @throws EntradaInvalidaException se o número não corresponder a nenhuma máquina
     * @throws pcb.excecao.ManutencaoDesnecessariaException se não há desgaste a corrigir
     * @throws pcb.excecao.OrcamentoInsuficienteException se o saldo não cobrir o reparo; nesse caso
     *         nada é alterado
     */
    public double repararMaquina(int numero) {
        try {
            Maquina maquina = buscarMaquina(numero);
            maquina.exigirDesgasteParaReparo();
            double custo = maquina.custoReparo(cenario);
            orcamento.debitar(custo);
            maquina.repararComponentes();
            auditoria.registrar("MANUTENCAO", String.format(
                    "Reparo geral de %s concluído (custo %s).", maquina.getNome(),
                    Formatacao.moeda(custo)));
            return custo;
        } catch (PlantaException e) {
            registrarFalha("Reparo da máquina #" + numero, e);
            throw e;
        }
    }

    /**
     * Cria uma nova linha de produção vazia.
     *
     * @param nome nome da linha (não vazio e diferente das existentes)
     * @return a linha criada
     * @throws EntradaInvalidaException se o nome estiver vazio ou já existir
     */
    public LinhaDeProducao criarLinha(String nome) {
        try {
            if (nome == null || nome.isBlank()) {
                throw new EntradaInvalidaException("O nome da linha não pode ser vazio.");
            }
            boolean existe = linhas.stream().anyMatch(l -> l.getNome().equalsIgnoreCase(nome.trim()));
            if (existe) {
                throw new EntradaInvalidaException("Já existe uma linha chamada \"" + nome.trim() + "\".");
            }
            LinhaDeProducao linha = new LinhaDeProducao(nome);
            linhas.add(linha);
            auditoria.registrar("LINHA", "Linha criada: " + linha.getNome());
            return linha;
        } catch (PlantaException e) {
            registrarFalha("Criação de linha", e);
            throw e;
        }
    }

    /**
     * Transfere uma máquina de uma linha para outra (agregação: a máquina e suas partes são
     * preservadas, apenas muda o agrupador).
     *
     * @param numeroMaquina posição da máquina na numeração global, começando em 1
     * @param numeroLinha posição da linha de destino, começando em 1
     * @throws EntradaInvalidaException se algum número for inválido ou a máquina já estiver na
     *         linha de destino; nesse caso nada é alterado
     */
    public void transferirMaquina(int numeroMaquina, int numeroLinha) {
        try {
            Maquina maquina = buscarMaquina(numeroMaquina);
            LinhaDeProducao destino = buscarLinha(numeroLinha);
            LinhaDeProducao origem = linhas.stream().filter(l -> l.contem(maquina)).findFirst()
                    .orElseThrow(() -> new EntradaInvalidaException(
                            maquina.getNome() + " não pertence a nenhuma linha."));
            if (origem == destino) {
                throw new EntradaInvalidaException(
                        maquina.getNome() + " já pertence à linha " + destino.getNome() + ".");
            }
            origem.removerMaquina(maquina);
            destino.adicionarMaquina(maquina);
            auditoria.registrar("LINHA", String.format("%s transferida de \"%s\" para \"%s\".",
                    maquina.getNome(), origem.getNome(), destino.getNome()));
        } catch (PlantaException e) {
            registrarFalha("Transferência da máquina #" + numeroMaquina, e);
            throw e;
        }
    }

    private LinhaDeProducao buscarLinha(int numero) {
        if (numero < 1 || numero > linhas.size()) {
            throw new EntradaInvalidaException("Não existe linha com o número " + numero
                    + ". Informe um valor de 1 a " + linhas.size() + ".");
        }
        return linhas.get(numero - 1);
    }

    // ------------------------------------------------------------------ auditoria em cascata

    /**
     * Gera o diagnóstico estrutural completo da planta: cada linha (agrupador) pede o relatório de
     * suas máquinas, e cada máquina o de suas partes.
     *
     * @return texto hierárquico e indentado
     */
    @Override
    public String gerarRelatorioDiagnostico() {
        StringBuilder sb = new StringBuilder("Planta de PCBs");
        if (linhas.isEmpty()) {
            sb.append('\n').append(Auditavel.indentar("(sem linhas de produção)", 1));
        }
        for (LinhaDeProducao linha : linhas) {
            sb.append('\n')
                    .append(Auditavel.indentar(Auditavel.comoItem(linha.gerarRelatorioDiagnostico()), 1));
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ configuração

    /**
     * Define a estratégia usada por {@link #produzirProxima()}.
     *
     * @param numero posição da estratégia em {@link #getEstrategiasDisponiveis()}, começando em 1
     * @throws EntradaInvalidaException se o número for inválido
     */
    public void selecionarEstrategia(int numero) {
        if (numero < 1 || numero > estrategias.size()) {
            throw new EntradaInvalidaException(
                    "Estratégia inexistente. Informe um valor de 1 a " + estrategias.size() + ".");
        }
        estrategia = estrategias.get(numero - 1);
        auditoria.registrar("CONFIGURACAO", "Estratégia alterada para: " + estrategia.getNome());
    }

    /**
     * Define o cenário de operação vigente.
     *
     * @param novoCenario cenário desejado
     * @throws EntradaInvalidaException se o cenário for nulo
     */
    public void setCenario(Cenario novoCenario) {
        if (novoCenario == null) {
            throw new EntradaInvalidaException("O cenário não foi informado.");
        }
        cenario = novoCenario;
        auditoria.registrar("CONFIGURACAO", "Cenário alterado para: " + novoCenario.getNome());
    }

    private void registrarFalha(String operacao, PlantaException e) {
        auditoria.registrar("ERRO", operacao + " recusada: " + e.getMessage());
    }

    // ------------------------------------------------------------------ consultas

    /**
     * Retorna as estratégias de produção disponíveis.
     *
     * @return lista imutável de estratégias
     */
    public List<EstrategiaProducao> getEstrategiasDisponiveis() {
        return estrategias;
    }

    /**
     * Retorna a estratégia vigente.
     *
     * @return estratégia de produção
     */
    public EstrategiaProducao getEstrategia() {
        return estrategia;
    }

    /**
     * Retorna o cenário vigente.
     *
     * @return cenário de operação
     */
    public Cenario getCenario() {
        return cenario;
    }

    /**
     * Retorna o nome de exibição da estratégia vigente.
     *
     * @return nome da estratégia
     */
    public String getNomeEstrategia() {
        return estrategia.getNome();
    }

    /**
     * Retorna o nome de exibição do cenário vigente.
     *
     * @return nome do cenário
     */
    public String getNomeCenario() {
        return cenario.getNome();
    }

    /**
     * Consulta a quantidade em estoque de uma matéria-prima.
     *
     * @param materiaPrima item consultado
     * @return quantidade disponível
     */
    public int consultarEstoque(MateriaPrima materiaPrima) {
        return estoque.consultar(materiaPrima);
    }

    /**
     * Retorna o saldo atual do orçamento.
     *
     * @return saldo em reais
     */
    public double getSaldo() {
        return orcamento.getSaldo();
    }

    /**
     * Retorna todos os equipamentos da planta, na ordem de numeração global (linhas em ordem e,
     * dentro de cada uma, suas máquinas).
     *
     * @return lista imutável de máquinas
     */
    public List<Maquina> getMaquinas() {
        return linhas.stream().flatMap(l -> l.getMaquinas().stream()).toList();
    }

    /**
     * Retorna as linhas de produção.
     *
     * @return lista imutável de linhas
     */
    public List<LinhaDeProducao> getLinhas() {
        return Collections.unmodifiableList(linhas);
    }

    /**
     * Retorna o catálogo de produtos fabricáveis.
     *
     * @return lista imutável de produtos
     */
    public List<Produto> getProdutos() {
        return Collections.unmodifiableList(produtos);
    }

    /**
     * Retorna o histórico de auditoria, do mais antigo ao mais recente.
     *
     * @return lista imutável de registros
     */
    public List<RegistroAuditoria> listarAuditoria() {
        return auditoria.listar();
    }

    /**
     * Registra no histórico uma falha inesperada (defeito de programa) ocorrida na interface.
     *
     * @param descricao descrição da falha
     */
    public void registrarFalhaInesperada(String descricao) {
        auditoria.registrar("FALHA", descricao);
    }
}
