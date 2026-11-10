package pcb.modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import pcb.excecao.EntradaInvalidaException;
import pcb.excecao.RecursoInsuficienteException;

/**
 * Estoque de matéria-prima da planta.
 *
 * <p>O consumo é atômico: ou todos os itens solicitados são retirados, ou nada é alterado.</p>
 *
 * @author 267396-282583
 */
public class Estoque {

    private final Map<MateriaPrima, Integer> quantidades = new EnumMap<>(MateriaPrima.class);

    /**
     * Cria o estoque com quantidades iniciais; itens não informados começam em zero.
     *
     * @param inicial quantidades iniciais por matéria-prima (valores não negativos)
     * @throws IllegalArgumentException se alguma quantidade inicial for negativa
     */
    public Estoque(Map<MateriaPrima, Integer> inicial) {
        for (MateriaPrima mp : MateriaPrima.values()) {
            quantidades.put(mp, 0);
        }
        for (Map.Entry<MateriaPrima, Integer> e : inicial.entrySet()) {
            if (e.getValue() < 0) {
                throw new IllegalArgumentException("Estoque inicial negativo para " + e.getKey());
            }
            quantidades.put(e.getKey(), e.getValue());
        }
    }

    /**
     * Consulta a quantidade disponível de um item.
     *
     * @param materiaPrima item consultado
     * @return quantidade em estoque
     */
    public int consultar(MateriaPrima materiaPrima) {
        return quantidades.get(materiaPrima);
    }

    /**
     * Retorna uma cópia somente leitura do estoque completo.
     *
     * @return mapa imutável com a quantidade de cada matéria-prima
     */
    public Map<MateriaPrima, Integer> consultarTudo() {
        return Collections.unmodifiableMap(new EnumMap<>(quantidades));
    }

    /**
     * Adiciona unidades ao estoque (entrada de compra).
     *
     * @param materiaPrima item recebido
     * @param quantidade unidades recebidas (positivo)
     * @throws EntradaInvalidaException se a quantidade não for positiva
     */
    public void adicionar(MateriaPrima materiaPrima, int quantidade) {
        if (quantidade <= 0) {
            throw new EntradaInvalidaException("A quantidade a adicionar deve ser maior que zero.");
        }
        quantidades.put(materiaPrima, quantidades.get(materiaPrima) + quantidade);
    }

    /**
     * Verifica se há estoque suficiente, sem alterá-lo.
     *
     * @param necessario quantidade necessária de cada matéria-prima
     * @throws RecursoInsuficienteException se qualquer item estiver abaixo do necessário; a
     *         mensagem lista todos os itens faltantes
     */
    public void verificarDisponibilidade(Map<MateriaPrima, Integer> necessario) {
        List<String> faltas = new ArrayList<>();
        for (Map.Entry<MateriaPrima, Integer> e : necessario.entrySet()) {
            MateriaPrima mp = e.getKey();
            int requerido = e.getValue();
            int disponivel = consultar(mp);
            if (disponivel < requerido) {
                faltas.add(String.format("%s: necessário %d %s, disponível %d %s (faltam %d)",
                        mp.getNome(), requerido, mp.getUnidade(), disponivel, mp.getUnidade(),
                        requerido - disponivel));
            }
        }
        if (!faltas.isEmpty()) {
            throw new RecursoInsuficienteException(
                    "Matéria-prima insuficiente. " + String.join("; ", faltas) + ".");
        }
    }

    /**
     * Retira do estoque as quantidades informadas, de forma atômica.
     *
     * @param necessario quantidade a consumir de cada matéria-prima
     * @throws RecursoInsuficienteException se faltar qualquer item; nesse caso nada é consumido
     */
    public void consumir(Map<MateriaPrima, Integer> necessario) {
        verificarDisponibilidade(necessario);
        for (Map.Entry<MateriaPrima, Integer> e : necessario.entrySet()) {
            quantidades.put(e.getKey(), quantidades.get(e.getKey()) - e.getValue());
        }
    }
}
