package pcb.modelo;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Contrato de diagnóstico estrutural da planta. A auditoria se propaga em cascata: o agrupador
 * ({@link LinhaDeProducao}) pede o relatório de cada {@link Maquina}, e cada máquina pede o
 * relatório de cada uma das suas partes internas
 *
 * @author 267396-282583
 */
public interface Auditavel {

    /** Recuo aplicado a cada nível da hierarquia*/
    String INDENTACAO = "  ";

    /**
     * Gera o relatório de diagnóstico do elemento e de tudo que ele contém
     *
     * @return texto (possivelmente com várias linhas) descrevendo a saúde estrutural
     */
    String gerarRelatorioDiagnostico();

    /**
     * Recua todas as linhas de um bloco de texto
     *
     * @param bloco texto de uma ou mais linhas
     * @param niveis quantidade de níveis de recuo
     * @return bloco recuado
     */
    static String indentar(String bloco, int niveis) {
        String prefixo = INDENTACAO.repeat(niveis);
        return Arrays.stream(bloco.split("\n"))
                .map(linha -> prefixo + linha)
                .collect(Collectors.joining("\n"));
    }

    /**
     * Transforma um bloco em item filho da hierarquia: a primeira linha ganha o marcador
     * {@code "> "} e as demais são alinhadas sob ele
     *
     * @param bloco texto de uma ou mais linhas
     * @return bloco formatado como item
     */
    static String comoItem(String bloco) {
        String[] linhas = bloco.split("\n");
        StringBuilder sb = new StringBuilder("> ").append(linhas[0]);
        for (int i = 1; i < linhas.length; i++) {
            sb.append('\n').append(INDENTACAO).append(linhas[i]);
        }
        return sb.toString();
    }
}
