package pcb.estrategia;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import pcb.modelo.Demanda;

/**
 * Estratégia FIFO: produz primeiro a demanda mais antiga
 *
 * @author 267396-282583
 */
public class OrdemDeChegada implements EstrategiaProducao {

    /**
     * Cria a estratégia de ordem de chegada.
     */
    public OrdemDeChegada() {
    }

    @Override
    public String getNome() {
        return "Ordem de chegada (FIFO)";
    }

    @Override
    public String getDescricao() {
        return "Produz a demanda registrada há mais tempo.";
    }

    @Override
    public Optional<Demanda> escolherProxima(List<Demanda> pendentes) {
        return pendentes.stream().min(Comparator.comparingInt(Demanda::getId));
    }
}
