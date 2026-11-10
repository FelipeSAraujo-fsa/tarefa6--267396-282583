package pcb.estrategia;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import pcb.modelo.Demanda;

/**
  Estratégia por urgência: produz primeiro a demanda de menor prazo de entrega; em empate, a mais
  antiga.
 
  @author 267396-282583
 */
public class MaiorUrgencia implements EstrategiaProducao {

    /**
     * Cria a estratégia de maior urgência.
     */
    public MaiorUrgencia() {
    }

    @Override
    public String getNome() {
        return "Maior urgência (menor prazo)";
    }

    @Override
    public String getDescricao() {
        return "Produz a demanda com o menor prazo de entrega; empates seguem a ordem de chegada.";
    }

    @Override
    public Optional<Demanda> escolherProxima(List<Demanda> pendentes) {
        return pendentes.stream().min(
                Comparator.comparingInt(Demanda::getPrazoDias).thenComparingInt(Demanda::getId));
    }
}
