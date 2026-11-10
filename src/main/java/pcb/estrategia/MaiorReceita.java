package pcb.estrategia;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import pcb.modelo.Demanda;

/**
  Estratégia por receita: produz primeiro a demanda de maior receita prevista; em empate, a mais
  antiga.
 
  @author 267396-282583
 */
public class MaiorReceita implements EstrategiaProducao {

    /**
      Cria a estratégia de maior receita.
     */
    public MaiorReceita() {
    }

    @Override
    public String getNome() {
        return "Maior receita";
    }

    @Override
    public String getDescricao() {
        return "Produz a demanda de maior receita prevista; empates seguem a ordem de chegada.";
    }

    @Override
    public Optional<Demanda> escolherProxima(List<Demanda> pendentes) {
        return pendentes.stream().min(
                Comparator.comparingDouble(Demanda::getReceitaPrevista).reversed()
                        .thenComparingInt(Demanda::getId));
    }
}
