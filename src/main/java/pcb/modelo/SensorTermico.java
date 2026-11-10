package pcb.modelo;

import pcb.util.Formatacao;

/**
 * Parte da {@link Maquina} (relação 1:1): sensor de temperatura que monitora o processo.
 *
 * <p>Com o tempo o sensor se descalibra: a leitura se afasta da temperatura real e a qualidade do
 * processo cai (mais placas reprovadas). Totalmente descalibrado, a máquina para por segurança.</p>
 *
 * @author 267396-282583
 */
class SensorTermico implements Auditavel {

    private static final double DESVIO_MAXIMO_C = 6.0;
    private static final double PERDA_MAXIMA_QUALIDADE = 0.25;

    final double temperaturaNominalC;
    private final double vidaCalibracaoHoras;
    double descalibracao;

    /**
     * Cria um sensor calibrado.
     *
     * @param temperaturaNominalC temperatura de trabalho do processo, em graus Celsius
     * @param vidaCalibracaoHoras horas de uso até a descalibração total (positivo)
     * @throws IllegalArgumentException se a vida de calibração não for positiva
     */
    SensorTermico(double temperaturaNominalC, double vidaCalibracaoHoras) {
        if (vidaCalibracaoHoras <= 0) {
            throw new IllegalArgumentException("Vida de calibração do sensor deve ser positiva.");
        }
        this.temperaturaNominalC = temperaturaNominalC;
        this.vidaCalibracaoHoras = vidaCalibracaoHoras;
    }

    void desgastar(double horas) {
        descalibracao = Math.min(1.0, descalibracao + horas / vidaCalibracaoHoras);
    }

    void recalibrar() {
        descalibracao = 0;
    }

    boolean temDesgaste() {
        return descalibracao > 0;
    }

    EstadoComponente getEstado() {
        return EstadoComponente.deDesgaste(descalibracao);
    }

    /**
     * Lê a temperatura do processo, incluindo o erro causado pela descalibração.
     *
     * @return temperatura lida, em graus Celsius
     */
    double lerTemperatura() {
        return temperaturaNominalC + descalibracao * DESVIO_MAXIMO_C;
    }

    /**
     * Fator de qualidade do processo imposto pelo sensor.
     *
     * @return 1 quando calibrado, diminuindo com a descalibração; 0 se falhou
     */
    double getFatorQualidade() {
        return getEstado() == EstadoComponente.FALHA ? 0 : 1 - PERDA_MAXIMA_QUALIDADE * descalibracao;
    }

    @Override
    public String gerarRelatorioDiagnostico() {
        return String.format("Sensor térmico: %s (leitura %s °C, descalibração %s)",
                getEstado().getDescricao(), Formatacao.decimal(lerTemperatura()),
                Formatacao.percentual(descalibracao));
    }
}
