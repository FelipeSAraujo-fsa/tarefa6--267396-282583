package pcb.modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import pcb.excecao.EntradaInvalidaException;

/**
 * Agrupador de máquinas (por exemplo, uma linha SMT). É uma relação de <b>agregação</b>: as
 * máquinas existem independentemente da linha e podem ser retiradas de uma e adicionadas a outra
 * (o ciclo de vida delas não depende do agrupador).
 *
 * <p>A coleção interna nunca é exposta: {@link #getMaquinas()} devolve uma visão imutável e as
 * alterações passam por {@link #adicionarMaquina(Maquina)} e {@link #removerMaquina(Maquina)}.</p>
 *
 * @author 267396-282583
 */
public final class LinhaDeProducao implements Auditavel {

    private final String nome;
    private final List<Maquina> maquinas = new ArrayList<>();

    /**
     * Cria uma linha vazia.
     *
     * @param nome nome da linha (não vazio)
     * @throws IllegalArgumentException se o nome for nulo ou vazio
     */
    public LinhaDeProducao(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("A linha de produção precisa de um nome.");
        }
        this.nome = nome.trim();
    }

    /**
     * Cria uma linha já com algumas máquinas.
     *
     * @param nome nome da linha (não vazio)
     * @param iniciais máquinas que passam a compor a linha
     * @throws IllegalArgumentException se o nome for inválido
     * @throws EntradaInvalidaException se alguma máquina for nula ou repetida
     */
    public LinhaDeProducao(String nome, List<Maquina> iniciais) {
        this(nome);
        for (Maquina m : iniciais) {
            adicionarMaquina(m);
        }
    }

    /**
     * Adiciona uma máquina à linha.
     *
     * @param maquina máquina a agrupar
     * @throws EntradaInvalidaException se a máquina for nula ou já estiver na linha
     */
    public void adicionarMaquina(Maquina maquina) {
        if (maquina == null) {
            throw new EntradaInvalidaException("A máquina não foi informada.");
        }
        if (contem(maquina)) {
            throw new EntradaInvalidaException(
                    maquina.getNome() + " já pertence à linha " + nome + ".");
        }
        maquinas.add(maquina);
    }

    /**
     * Retira uma máquina da linha (a máquina continua existindo e pode ir para outra linha).
     *
     * @param maquina máquina a retirar
     * @return {@code true} se a máquina estava na linha
     */
    public boolean removerMaquina(Maquina maquina) {
        return maquinas.remove(maquina);
    }

    /**
     * Informa se a máquina pertence a esta linha.
     *
     * @param maquina máquina procurada
     * @return {@code true} se está na linha
     */
    public boolean contem(Maquina maquina) {
        return maquinas.contains(maquina);
    }

    /**
     * Procura a primeira máquina operacional de um tipo de processo.
     *
     * @param tipo tipo de processo exigido
     * @return a máquina, ou vazio se não houver nenhuma operacional desse tipo nesta linha
     */
    public Optional<Maquina> buscarMaquinaOperacional(TipoMaquina tipo) {
        return maquinas.stream().filter(m -> m.getTipo() == tipo && m.isOperacional()).findFirst();
    }

    /**
     * Retorna o nome da linha.
     *
     * @return nome de exibição
     */
    public String getNome() {
        return nome;
    }

    /**
     * Retorna quantas máquinas a linha possui.
     *
     * @return número de máquinas
     */
    public int getQuantidadeMaquinas() {
        return maquinas.size();
    }

    /**
     * Retorna as máquinas da linha.
     *
     * @return visão imutável da lista de máquinas
     */
    public List<Maquina> getMaquinas() {
        return Collections.unmodifiableList(maquinas);
    }

    /**
     * Gera o diagnóstico da linha, invocando o de cada máquina.
     *
     * @return texto hierárquico e indentado
     */
    @Override
    public String gerarRelatorioDiagnostico() {
        long operacionais = maquinas.stream().filter(Maquina::isOperacional).count();
        StringBuilder sb = new StringBuilder(String.format("%s [%d máquina(s), %d operacional(is)]",
                nome, maquinas.size(), operacionais));
        if (maquinas.isEmpty()) {
            sb.append('\n').append(Auditavel.indentar("(linha sem máquinas)", 1));
        }
        for (Maquina m : maquinas) {
            sb.append('\n')
                    .append(Auditavel.indentar(Auditavel.comoItem(m.gerarRelatorioDiagnostico()), 1));
        }
        return sb.toString();
    }
}
