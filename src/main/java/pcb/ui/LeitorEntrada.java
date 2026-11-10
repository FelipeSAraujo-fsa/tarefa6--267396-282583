package pcb.ui;

import java.util.Scanner;
import pcb.excecao.EntradaInvalidaException;

/**
 * Leitor de entrada do usuário com validação. Entradas inválidas geram
 * {@link EntradaInvalidaException} com mensagem explicativa, em vez de encerrar o programa.
 *
 * @author 267396-282583
 */
public class LeitorEntrada {

    private final Scanner scanner;

    /**
     * Cria o leitor sobre um {@link Scanner}.
     *
     * @param scanner fonte de entrada (normalmente o teclado)
     */
    public LeitorEntrada(Scanner scanner) {
        this.scanner = scanner;
    }

    /**
     * Exibe um rótulo e lê uma linha de texto.
     *
     * @param rotulo texto exibido antes da leitura
     * @return linha digitada, sem espaços nas extremidades
     * @throws java.util.NoSuchElementException se a entrada for encerrada (fim de arquivo)
     */
    public String lerLinha(String rotulo) {
        System.out.print(rotulo + ": ");
        return scanner.nextLine().trim();
    }

    /**
     * Lê um número inteiro dentro de um intervalo.
     *
     * @param rotulo texto exibido antes da leitura
     * @param minimo menor valor aceito (inclusive)
     * @param maximo maior valor aceito (inclusive)
     * @return o valor digitado
     * @throws EntradaInvalidaException se nada for digitado, o texto não for um inteiro ou o valor
     *         estiver fora do intervalo
     * @throws java.util.NoSuchElementException se a entrada for encerrada (fim de arquivo)
     */
    public int lerInteiro(String rotulo, int minimo, int maximo) {
        String texto = lerLinha(rotulo);
        if (texto.isEmpty()) {
            throw new EntradaInvalidaException(String.format(
                    "Nenhum valor foi digitado. Informe um número entre %d e %d.", minimo, maximo));
        }
        int valor;
        try {
            valor = Integer.parseInt(texto);
        } catch (NumberFormatException e) {
            throw new EntradaInvalidaException(String.format(
                    "\"%s\" não é um número inteiro válido. Informe um número entre %d e %d.",
                    texto, minimo, maximo), e);
        }
        if (valor < minimo || valor > maximo) {
            throw new EntradaInvalidaException(String.format(
                    "O valor %d não é válido. Informe um número entre %d e %d.", valor, minimo, maximo));
        }
        return valor;
    }

    /**
     * Aguarda o usuário pressionar ENTER. Se a entrada já foi encerrada, retorna imediatamente.
     */
    public void pausar() {
        System.out.print("Pressione ENTER para continuar...");
        if (scanner.hasNextLine()) {
            scanner.nextLine();
        }
        System.out.println();
    }
}
