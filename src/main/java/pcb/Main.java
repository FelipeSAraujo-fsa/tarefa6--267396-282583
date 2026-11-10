package pcb;

import java.util.List;
import java.util.Scanner;
import pcb.excecao.PersistenciaException;
import pcb.modelo.Produto;
import pcb.persistencia.PersistenciaPlanta;
import pcb.servico.GerenciadorProducao;
import pcb.ui.LeitorEntrada;
import pcb.ui.MenuPrincipal;

/**
 * Ponto de entrada da aplicação da fábrica de PCBs. Monta a planta padrão, registra algumas
 * demandas de exemplo e inicia o menu interativo.
 *
 * @author 267396-282583
 */
public final class Main {

    private Main() {
    }

    /**
     * Inicia o sistema.
     *
     * @param args argumentos de linha de comando (não utilizados)
     */
    public static void main(String[] args) {
        PersistenciaPlanta persistencia = new PersistenciaPlanta();
        GerenciadorProducao gerenciador;
        if (persistencia.existe()) {
            try {
                gerenciador = persistencia.carregar();
                System.out.println("Estado da planta carregado de " + persistencia.getArquivo() + ".");
            } catch (PersistenciaException e) {
                System.out.println("Não foi possível carregar o estado salvo: " + e.getMessage());
                System.out.println("Iniciando com a planta padrão. O arquivo existente não foi alterado.");
                gerenciador = criarPlantaPadrao();
            }
        } else {
            gerenciador = criarPlantaPadrao();
        }

        try (Scanner scanner = new Scanner(System.in)) {
            new MenuPrincipal(gerenciador, new LeitorEntrada(scanner)).executar();
        }
    }

    private static GerenciadorProducao criarPlantaPadrao() {
        GerenciadorProducao gerenciador = GerenciadorProducao.criarPlantaPadrao();
        List<Produto> produtos = gerenciador.getProdutos();
        gerenciador.criarDemanda(produtos.get(0), 100, 10);
        gerenciador.criarDemanda(produtos.get(1), 50, 5);
        gerenciador.criarDemanda(produtos.get(2), 30, 15);
        return gerenciador;
    }
}
