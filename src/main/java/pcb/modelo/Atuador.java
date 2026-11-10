package pcb.modelo;

import pcb.util.Formatacao;

/**
 * Parte da {@link Maquina} (relação 1:N): eixo, bico ou esteira que movimenta placa e ferramenta
 *
 * @author 267396-282583
 */
class Atuador implements Auditavel {

    private static final double PERDA_MAXIMA_CONTRIBUICAO = 0.30;

    private final int numero;
    private final double vidaUtilHoras;
    double desgaste;

    /**
     * Cria um atuador novo
     *
     * @param numero posição do atuador dentro da máquina, começando em 1
     * @param vidaUtilHoras horas de uso até o esgotamento (positivo)
     * @throws IllegalArgumentException se a vida útil não for positiva
     */
    Atuador(int numero, double vidaUtilHoras) {
        if (vidaUtilHoras <= 0) {
            throw new IllegalArgumentException("Vida útil do atuador deve ser positiva.");
        }
        this.numero = numero;
        this.vidaUtilHoras = vidaUtilHoras;
    }

    void desgastar(double horas) {
        desgaste = Math.min(1.0, desgaste + horas / vidaUtilHoras);
    }

    void substituir() {
        desgaste = 0;
    }

    boolean temDesgaste() {
        return desgaste > 0;
    }

    EstadoComponente getEstado() {
        return EstadoComponente.deDesgaste(desgaste);
    }

    /**
     * Contribuição deste atuador para a vazão da máquina
     *
     * @return 1 quando novo, diminuindo com o desgaste; 0 se esgotado
     */
    double getContribuicao() {
        return getEstado() == EstadoComponente.FALHA ? 0 : 1 - PERDA_MAXIMA_CONTRIBUICAO * desgaste;
    }

    @Override
    public String gerarRelatorioDiagnostico() {
        return String.format("Atuador %d: %s (desgaste %s)", numero, getEstado().getDescricao(),
                Formatacao.percentual(desgaste));
    }
}
