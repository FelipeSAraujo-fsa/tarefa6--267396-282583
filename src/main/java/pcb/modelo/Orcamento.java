package pcb.modelo;

import pcb.excecao.OrcamentoInsuficienteException;
import pcb.util.Formatacao;

/**
 * Caixa/orçamento da planta, usado para pagar compras de matéria-prima e custos operacionais e
 * que recebe a receita das produções concluídas.
 *
 * @author 267396-282583
 */
public class Orcamento {

    private double saldo;

    /**
     * Cria o orçamento com um saldo inicial.
     *
     * @param saldoInicial saldo em reais (não negativo)
     * @throws IllegalArgumentException se o saldo inicial for negativo
     */
    public Orcamento(double saldoInicial) {
        if (saldoInicial < 0) {
            throw new IllegalArgumentException("Saldo inicial negativo.");
        }
        this.saldo = saldoInicial;
    }

    /**
     * Verifica se o saldo cobre um valor, sem alterá-lo.
     *
     * @param valor valor necessário, em reais
     * @throws OrcamentoInsuficienteException se o saldo for menor que o valor
     */
    public void garantirSaldo(double valor) {
        if (valor > saldo + 1e-9) {
            throw new OrcamentoInsuficienteException(String.format(
                    "Orçamento insuficiente. Necessário %s, saldo disponível %s.",
                    Formatacao.moeda(valor), Formatacao.moeda(saldo)));
        }
    }

    /**
     * Debita um valor do saldo.
     *
     * @param valor valor a pagar, em reais
     * @throws OrcamentoInsuficienteException se o saldo não cobrir o valor; nesse caso o saldo
     *         permanece inalterado
     */
    public void debitar(double valor) {
        garantirSaldo(valor);
        saldo -= valor;
    }

    /**
     * Credita um valor ao saldo (receita).
     *
     * @param valor valor recebido, em reais
     */
    public void creditar(double valor) {
        saldo += valor;
    }

    /**
     * Retorna o saldo atual.
     *
     * @return saldo em reais
     */
    public double getSaldo() {
        return saldo;
    }
}
