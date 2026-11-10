package pcb.modelo;

import pcb.util.Formatacao;

/**
 * Parte da {@link Maquina} (relação 1:1): o cabeçote que realiza o processo propriamente dito
 *
 * Desgasta com as horas de uso. Quanto mais desgastado, mais lenta fica a máquina; esgotado, a máquina para. Só a própria {@link Maquina} instancia e conhece este objeto
 *
 * @author 267396-282583
 */
class CabecoteDeProcesso implements Auditavel {

    private static final double PERDA_MAXIMA_VELOCIDADE = 0.40;

    private final double vidaUtilHoras;
    double desgaste;

    /**
     * Cria um cabeçote novo
     *
     * @param vidaUtilHoras horas de uso até o esgotamento (positivo)
     * @throws IllegalArgumentException se a vida útil não for positiva
     */
    CabecoteDeProcesso(double vidaUtilHoras) {
        if (vidaUtilHoras <= 0) {
            throw new IllegalArgumentException("Vida útil do cabeçote deve ser positiva.");
        }
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
     * Fator de velocidade imposto pelo cabeçote
     *
     * @return 1 quando novo, diminuindo com o desgaste; 0 se esgotado
     */
    double getFatorVelocidade() {
        return getEstado() == EstadoComponente.FALHA ? 0 : 1 - PERDA_MAXIMA_VELOCIDADE * desgaste;
    }

    @Override
    public String gerarRelatorioDiagnostico() {
        return String.format("Cabeçote de processo: %s (desgaste %s, velocidade %s)",
                getEstado().getDescricao(), Formatacao.percentual(desgaste),
                Formatacao.percentual(getFatorVelocidade()));
    }
}
