package pcb.modelo;

import java.util.ArrayList;
import java.util.List;
import pcb.excecao.MaquinaIndisponivelException;
import pcb.excecao.ManutencaoDesnecessariaException;
import pcb.util.Formatacao;

/**
 * Equipamento da linha de produção (cortadora a laser, forno de reflow, AOI etc.): o "Todo" da
 * relação de composição.
 *
 * <p><b>Composição estrita.</b> Uma máquina é formada por três tipos de parte, todos criados no
 * construtor e nunca expostos ao restante do sistema:</p>
 * <ul>
 *   <li>1 {@link CabecoteDeProcesso} (1:1) - define a velocidade;</li>
 *   <li>1 {@link SensorTermico} (1:1) - define a qualidade (rendimento) e a leitura de temperatura;</li>
 *   <li>1..N {@link Atuador} (1:N) - definem a vazão.</li>
 * </ul>
 * <p>As partes não existem fora da máquina: quando ela é descartada, elas vão junto. O código
 * cliente só conversa com a máquina (Lei de Deméter), que delega às partes, por exemplo em
 * {@link #registrarUso(double)}, {@link #repararComponentes()} e
 * {@link #gerarRelatorioDiagnostico()}.</p>
 *
 * <p>Uma máquina é <em>agregada</em> a uma {@link LinhaDeProducao}: pode ser retirada de uma linha
 * e colocada em outra sem perder suas partes nem seu histórico.</p>
 *
 * @author 267396-282583
 */
public class Maquina implements Auditavel {

    private static final double VIDA_CABECOTE_HORAS = 40.0;
    private static final double VIDA_SENSOR_HORAS = 60.0;
    private static final double VIDA_ATUADOR_BASE_HORAS = 50.0;
    private static final double VIDA_ATUADOR_ACRESCIMO_HORAS = 10.0;
    private static final double HORAS_DE_REPARO = 3.0;

    private final String nome;
    private final TipoMaquina tipo;
    private final int capacidadePorHora;
    private final double custoPorHora;

    // Partes: ciclo de vida controlado exclusivamente por esta classe (composição).
    private final CabecoteDeProcesso cabecote;
    private final SensorTermico sensor;
    private final List<Atuador> atuadores;

    private boolean habilitada = true;
    private double horasAcumuladas;

    /**
     * Cria uma máquina operacional, com todas as partes novas. O construtor recebe apenas valores
     * primitivos de configuração e instancia as partes ele mesmo.
     *
     * @param nome identificação do equipamento (modelo)
     * @param tipo tipo de processo que executa
     * @param capacidadePorHora placas processadas por hora (positivo)
     * @param custoPorHora custo operacional por hora, em reais (positivo)
     * @param temperaturaOperacaoC temperatura de trabalho do processo, em graus Celsius
     * @param quantidadeAtuadores número de atuadores da máquina (pelo menos 1)
     * @throws IllegalArgumentException se algum valor for inválido (erro de configuração)
     */
    public Maquina(String nome, TipoMaquina tipo, int capacidadePorHora, double custoPorHora,
            double temperaturaOperacaoC, int quantidadeAtuadores) {
        if (nome == null || nome.isBlank() || tipo == null || capacidadePorHora <= 0
                || custoPorHora <= 0 || quantidadeAtuadores < 1) {
            throw new IllegalArgumentException("Definição de máquina inconsistente: " + nome);
        }
        this.nome = nome;
        this.tipo = tipo;
        this.capacidadePorHora = capacidadePorHora;
        this.custoPorHora = custoPorHora;

        this.cabecote = new CabecoteDeProcesso(VIDA_CABECOTE_HORAS);
        this.sensor = new SensorTermico(temperaturaOperacaoC, VIDA_SENSOR_HORAS);
        this.atuadores = new ArrayList<>();
        for (int i = 1; i <= quantidadeAtuadores; i++) {
            double vida = VIDA_ATUADOR_BASE_HORAS + VIDA_ATUADOR_ACRESCIMO_HORAS * (i - 1);
            this.atuadores.add(new Atuador(i, vida));
        }
    }

    // ------------------------------------------------------------------ produção

    /**
     * Calcula o tempo necessário para processar um lote, considerando a eficiência atual das
     * partes (peças desgastadas deixam a máquina mais lenta).
     *
     * @param quantidade número de placas do lote
     * @param cenario cenário de operação vigente
     * @return tempo em horas
     * @throws MaquinaIndisponivelException se a máquina não tem eficiência (parada por falha)
     */
    public double horasPara(int quantidade, Cenario cenario) {
        double eficiencia = getEficiencia();
        if (eficiencia <= 0) {
            throw new MaquinaIndisponivelException(nome + " está parada por falha de componente.");
        }
        return quantidade / (capacidadePorHora * eficiencia) * cenario.getFatorTempo();
    }

    /**
     * Calcula o custo operacional de usar a máquina por um período.
     *
     * @param horas tempo de uso, em horas
     * @param cenario cenário de operação vigente
     * @return custo em reais
     */
    public double custoPara(double horas, Cenario cenario) {
        return horas * custoPorHora * cenario.getFatorCusto();
    }

    /**
     * Registra horas de uso efetivo. O desgaste é propagado a todas as partes internas.
     *
     * @param horas horas trabalhadas a acumular (não negativo)
     */
    public void registrarUso(double horas) {
        horasAcumuladas += horas;
        cabecote.desgastar(horas);
        sensor.desgastar(horas);
        atuadores.forEach(a -> a.desgastar(horas));
    }

    /**
     * Calcula a eficiência atual: velocidade do cabeçote multiplicada pela vazão média dos
     * atuadores.
     *
     * @return valor entre 0 (parada) e 1 (todas as partes novas)
     */
    public double getEficiencia() {
        double vazao = atuadores.stream().mapToDouble(Atuador::getContribuicao).average().orElse(0);
        return cabecote.getFatorVelocidade() * vazao;
    }

    /**
     * Retorna a fração de placas aprovadas que a máquina entrega no estado atual do sensor.
     *
     * @return valor entre 0 e 1
     */
    public double getRendimentoQualidade() {
        return sensor.getFatorQualidade();
    }

    /**
     * Lê a temperatura do processo (método repassador: o cliente não acessa o sensor).
     *
     * @return temperatura lida, em graus Celsius
     */
    public double lerTemperaturaAtual() {
        return sensor.lerTemperatura();
    }

    // ------------------------------------------------------------------ manutenção

    /**
     * Alterna a máquina entre habilitada e em manutenção programada.
     *
     * @return situação da máquina após a chamada
     */
    public SituacaoMaquina alternarManutencao() {
        habilitada = !habilitada;
        return getSituacao();
    }

    /**
     * Informa se alguma parte da máquina tem desgaste a corrigir.
     *
     * @return {@code true} se vale a pena executar {@link #repararComponentes()}
     */
    public boolean possuiDesgaste() {
        return cabecote.temDesgaste() || sensor.temDesgaste()
                || atuadores.stream().anyMatch(Atuador::temDesgaste);
    }

    /**
     * Calcula o custo de um reparo geral (substituição/recalibração de todas as partes).
     *
     * @param cenario cenário de operação vigente
     * @return custo em reais
     */
    public double custoReparo(Cenario cenario) {
        return custoPara(HORAS_DE_REPARO, cenario);
    }

    /**
     * Verifica se o reparo é necessário, sem alterar nada.
     *
     * @throws ManutencaoDesnecessariaException se todas as partes estão sem desgaste
     */
    public void exigirDesgasteParaReparo() {
        if (!possuiDesgaste()) {
            throw new ManutencaoDesnecessariaException(
                    nome + " não precisa de reparo: todos os componentes estão sem desgaste.");
        }
    }

    /**
     * Executa o reparo geral: substitui o cabeçote e os atuadores e recalibra o sensor.
     */
    public void repararComponentes() {
        cabecote.substituir();
        sensor.recalibrar();
        atuadores.forEach(Atuador::substituir);
    }

    // ------------------------------------------------------------------ situação

    /**
     * Retorna a situação atual: em manutenção (decisão do usuário), parada por falha (parte
     * essencial esgotada) ou operacional.
     *
     * @return situação da máquina
     */
    public SituacaoMaquina getSituacao() {
        if (!habilitada) {
            return SituacaoMaquina.EM_MANUTENCAO;
        }
        return temFalhaEssencial() ? SituacaoMaquina.PARADA_POR_FALHA : SituacaoMaquina.OPERACIONAL;
    }

    /**
     * Informa se a máquina pode ser usada.
     *
     * @return {@code true} se está operacional
     */
    public boolean isOperacional() {
        return getSituacao() == SituacaoMaquina.OPERACIONAL;
    }

    private boolean temFalhaEssencial() {
        return cabecote.getEstado() == EstadoComponente.FALHA
                || sensor.getEstado() == EstadoComponente.FALHA
                || atuadores.stream().allMatch(a -> a.getEstado() == EstadoComponente.FALHA);
    }

    private long contarAtuadoresOperantes() {
        return atuadores.stream().filter(a -> a.getEstado() != EstadoComponente.FALHA).count();
    }

    // ------------------------------------------------------------------ auditoria em cascata

    /**
     * Gera o diagnóstico da máquina, invocando a checagem de cada parte interna.
     *
     * @return texto hierárquico e indentado
     */
    @Override
    public String gerarRelatorioDiagnostico() {
        StringBuilder atuadoresBloco = new StringBuilder(String.format("Atuadores: %d/%d operantes",
                contarAtuadoresOperantes(), atuadores.size()));
        for (Atuador a : atuadores) {
            atuadoresBloco.append('\n')
                    .append(Auditavel.indentar(Auditavel.comoItem(a.gerarRelatorioDiagnostico()), 1));
        }
        String cabecalho = String.format("%s (%s): %s | eficiência %s | rendimento %s", nome,
                tipo.getDescricao(), getSituacao().getDescricao(),
                Formatacao.percentual(getEficiencia()), Formatacao.percentual(getRendimentoQualidade()));
        return cabecalho + '\n'
                + Auditavel.indentar(Auditavel.comoItem(cabecote.gerarRelatorioDiagnostico()), 1) + '\n'
                + Auditavel.indentar(Auditavel.comoItem(sensor.gerarRelatorioDiagnostico()), 1) + '\n'
                + Auditavel.indentar(Auditavel.comoItem(atuadoresBloco.toString()), 1);
    }

    // ------------------------------------------------------------------ consultas

    /**
     * Retorna o nome do equipamento.
     *
     * @return modelo/identificação
     */
    public String getNome() {
        return nome;
    }

    /**
     * Retorna o tipo de processo da máquina.
     *
     * @return tipo de máquina
     */
    public TipoMaquina getTipo() {
        return tipo;
    }

    /**
     * Retorna o nome de exibição do tipo de processo.
     *
     * @return descrição legível do tipo
     */
    public String getDescricaoTipo() {
        return tipo.getDescricao();
    }

    /**
     * Retorna a capacidade nominal.
     *
     * @return placas por hora
     */
    public int getCapacidadePorHora() {
        return capacidadePorHora;
    }

    /**
     * Retorna o custo por hora nominal (sem fator de cenário).
     *
     * @return custo em reais por hora
     */
    public double getCustoPorHora() {
        return custoPorHora;
    }

    /**
     * Retorna o total de horas já trabalhadas.
     *
     * @return horas acumuladas
     */
    public double getHorasAcumuladas() {
        return horasAcumuladas;
    }

    public boolean isHabilitada() {
        return habilitada;
    }

    public double getTemperaturaOperacaoC() {
        return sensor.temperaturaNominalC;
    }

    public List<Double> getDesgastes() {
        List<Double> desgastes = new ArrayList<>();
        desgastes.add(cabecote.desgaste);
        desgastes.add(sensor.descalibracao);
        for (Atuador a : atuadores) {
            desgastes.add(a.desgaste);
        }
        return desgastes;
    }

    public void restaurarEstado(boolean habilitada, double horas, List<Double> desgastes) {
        boolean valido = horas >= 0 && desgastes.size() == atuadores.size() + 2;
        for (double d : desgastes) {
            valido = valido && d >= 0 && d <= 1;
        }
        if (!valido) {
            throw new IllegalArgumentException("Estado inconsistente para a máquina " + nome);
        }
        this.habilitada = habilitada;
        this.horasAcumuladas = horas;
        cabecote.desgaste = desgastes.get(0);
        sensor.descalibracao = desgastes.get(1);
        for (int i = 0; i < atuadores.size(); i++) {
            atuadores.get(i).desgaste = desgastes.get(i + 2);
        }
    }
}
