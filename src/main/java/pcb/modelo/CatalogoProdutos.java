package pcb.modelo;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Catálogo de produtos padrão da planta de PCBs
 *
 * @author 267396-282583
 */
public final class CatalogoProdutos {

    private CatalogoProdutos() {
    }

    /**
     * Cria a lista de produtos oferecidos pela fábrica
     *
     * <ul>
     *   <li>Placa Sensor IoT: SMD, dupla face, processo completo com reflow.</li>
     *   <li>Placa Fonte Industrial: componentes de furo passante, soldagem por onda.</li>
     *   <li>Placa Controladora Multicamada: SMD e furo passante, maior valor agregado.</li>
     * </ul>
     *
     * @return nova lista de produtos
     */
    public static List<Produto> padrao() {
        Map<MateriaPrima, Integer> iot = new EnumMap<>(MateriaPrima.class);
        iot.put(MateriaPrima.LAMINADO_FR4, 1);
        iot.put(MateriaPrima.PASTA_DE_SOLDA, 3);
        iot.put(MateriaPrima.KIT_COMPONENTES_SMD, 1);

        Map<MateriaPrima, Integer> fonte = new EnumMap<>(MateriaPrima.class);
        fonte.put(MateriaPrima.LAMINADO_FR4, 1);
        fonte.put(MateriaPrima.FLUXO_DE_SOLDA, 4);
        fonte.put(MateriaPrima.KIT_COMPONENTES_SMD, 1);

        Map<MateriaPrima, Integer> controladora = new EnumMap<>(MateriaPrima.class);
        controladora.put(MateriaPrima.LAMINADO_FR4, 2);
        controladora.put(MateriaPrima.PASTA_DE_SOLDA, 5);
        controladora.put(MateriaPrima.FLUXO_DE_SOLDA, 3);
        controladora.put(MateriaPrima.KIT_COMPONENTES_SMD, 2);

        return List.of(
                new Produto("Placa Sensor IoT (SMD)", 95.00,
                        List.of(TipoMaquina.CORTE_LASER, TipoMaquina.PICK_AND_PLACE,
                                TipoMaquina.SOLDAGEM_REFLOW, TipoMaquina.INSPECAO_OPTICA,
                                TipoMaquina.TESTE_FUNCIONAL),
                        iot),
                new Produto("Placa Fonte Industrial (THT)", 85.00,
                        List.of(TipoMaquina.CORTE_LASER, TipoMaquina.SOLDAGEM_ONDA,
                                TipoMaquina.INSPECAO_OPTICA, TipoMaquina.TESTE_FUNCIONAL),
                        fonte),
                new Produto("Placa Controladora Multicamada", 260.00,
                        List.of(TipoMaquina.CORTE_LASER, TipoMaquina.PICK_AND_PLACE,
                                TipoMaquina.SOLDAGEM_REFLOW, TipoMaquina.SOLDAGEM_ONDA,
                                TipoMaquina.INSPECAO_OPTICA, TipoMaquina.TESTE_FUNCIONAL),
                        controladora));
    }
}
