package pcb.modelo;

import pcb.excecao.DemandaInvalidaException;
import pcb.excecao.EstadoDemandaInvalidaException;

/**
 * Pedido de fabricação de um lote de placas de um {@link Produto}
 *
 * <p>Regras: quantidade e prazo devem ser positivos; apenas demandas {@link StatusDemanda#PENDENTE}
 * podem ser concluídas ou canceladas</p>
 *
 * @author 267396-282583
 */
public class Demanda {

    private final int id;
    private final Produto produto;
    private final int quantidade;
    private final int prazoDias;
    private StatusDemanda status = StatusDemanda.PENDENTE;

    /**
     * Cria uma demanda pendente
     *
     * @param id identificador único da demanda
     * @param produto produto solicitado
     * @param quantidade número de placas (positivo)
     * @param prazoDias prazo de entrega em dias (positivo)
     * @throws DemandaInvalidaException se o produto for nulo ou quantidade/prazo não forem positivos
     */
    public Demanda(int id, Produto produto, int quantidade, int prazoDias) {
        if (produto == null) {
            throw new DemandaInvalidaException("O produto da demanda não foi informado.");
        }
        if (quantidade <= 0) {
            throw new DemandaInvalidaException("A quantidade da demanda deve ser maior que zero.");
        }
        if (prazoDias <= 0) {
            throw new DemandaInvalidaException("O prazo da demanda deve ser de pelo menos 1 dia.");
        }
        this.id = id;
        this.produto = produto;
        this.quantidade = quantidade;
        this.prazoDias = prazoDias;
    }

    /**
     * Verifica se a demanda ainda pode receber operações que a finalizam.
     *
     * @param operacao verbo no infinitivo usado na mensagem de erro (por exemplo, "produzir")
     * @throws EstadoDemandaInvalidaException se a demanda não estiver pendente
     */
    public void exigirPendente(String operacao) {
        if (status != StatusDemanda.PENDENTE) {
            throw new EstadoDemandaInvalidaException(String.format(
                    "Não é possível %s a demanda #%d: ela já está %s.",
                    operacao, id, status.getDescricao().toLowerCase()));
        }
    }

    /**
     * Marca a demanda como concluída
     *
     * @throws EstadoDemandaInvalidaException se a demanda não estiver pendente
     */
    public void concluir() {
        exigirPendente("concluir");
        status = StatusDemanda.CONCLUIDA;
    }

    /**
     * Marca a demanda como cancelada.
     *
     * @throws EstadoDemandaInvalidaException se a demanda não estiver pendente
     */
    public void cancelar() {
        exigirPendente("cancelar");
        status = StatusDemanda.CANCELADA;
    }

    /**
     * Calcula a receita que a demanda gera quando produzida
     *
     * @return preço unitário do produto multiplicado pela quantidade
     */
    public double getReceitaPrevista() {
        return produto.getPrecoUnitario() * quantidade;
    }

    /**
     * Retorna o identificador
     *
     * @return id da demanda
     */
    public int getId() {
        return id;
    }

    /**
     * Retorna o produto solicitado
     *
     * @return produto
     */
    public Produto getProduto() {
        return produto;
    }

    /**
     * Retorna a quantidade solicitada
     *
     * @return número de placas
     */
    public int getQuantidade() {
        return quantidade;
    }

    /**
     * Retorna o prazo de entrega
     *
     * @return prazo em dias
     */
    public int getPrazoDias() {
        return prazoDias;
    }

    /**
     * Retorna a situação atual
     *
     * @return status da demanda
     */
    public StatusDemanda getStatus() {
        return status;
    }

    /**
     * Retorna o nome do produto da demanda (método repassador)
     *
     * @return nome comercial do produto
     */
    public String getNomeProduto() {
        return produto.getNome();
    }

    /**
     * Retorna o texto de exibição do status (método repassador)
     *
     * @return descrição do status
     */
    public String getDescricaoStatus() {
        return status.getDescricao();
    }
}
